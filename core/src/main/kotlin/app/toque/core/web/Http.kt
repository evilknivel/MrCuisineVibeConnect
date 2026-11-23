package app.toque.core.web

import app.toque.core.json.Json
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder

/** Requête HTTP/1.1 lue par [Http.read]. En-têtes en minuscules. */
class HttpRequest(
    val method: String,
    val path: String,
    val query: Map<String, String>,
    val headers: Map<String, String>,
    val body: ByteArray,
) {
    val text: String get() = String(body, Charsets.UTF_8)
    fun header(name: String): String? = headers[name.lowercase()]
}

class HttpResponse(
    val status: Int,
    val type: String,
    val body: ByteArray,
    val headers: Map<String, String> = emptyMap(),
) {
    companion object {
        fun json(status: Int, value: Any?): HttpResponse =
            HttpResponse(status, "application/json; charset=utf-8", Json.write(value).toByteArray(Charsets.UTF_8))

        fun error(status: Int, message: String, code: String = ""): HttpResponse =
            json(status, linkedMapOf("error" to message, "code" to code))

        fun bytes(type: String, body: ByteArray, headers: Map<String, String> = emptyMap()) = HttpResponse(200, type, body, headers)
    }
}

class HttpError(val status: Int, message: String) : Exception(message)

/**
 * Le strict nécessaire de HTTP/1.1 pour le serveur de recettes : une requête par connexion
 * (Connection: close), tailles limitées, pas de « chunked ». Sans Android, pour être testé ici.
 */
object Http {
    const val MAX_HEAD = 16 * 1024
    const val MAX_BODY = 4 * 1024 * 1024

    fun read(input: InputStream, maxBody: Int = MAX_BODY): HttpRequest {
        val head = readHead(input)
        val lines = head.split("\r\n")
        val first = lines[0].split(' ')
        if (first.size != 3 || !first[2].startsWith("HTTP/1.")) throw HttpError(400, "requête illisible")
        val method = first[0].uppercase()
        val target = first[1]
        if (!target.startsWith("/")) throw HttpError(400, "chemin illisible")
        val headers = HashMap<String, String>()
        for (l in lines.drop(1)) {
            if (l.isEmpty()) continue
            val c = l.indexOf(':')
            if (c <= 0) throw HttpError(400, "en-tête illisible")
            headers[l.substring(0, c).trim().lowercase()] = l.substring(c + 1).trim()
        }
        if (headers["transfer-encoding"] != null) throw HttpError(411, "longueur attendue")
        val length = headers["content-length"]?.let { it.toIntOrNull() ?: throw HttpError(400, "longueur illisible") } ?: 0
        if (length < 0) throw HttpError(400, "longueur illisible")
        if (length > maxBody) throw HttpError(413, "trop gros")
        val body = ByteArray(length)
        var n = 0
        while (n < length) {
            val r = input.read(body, n, length - n)
            if (r < 0) throw HttpError(400, "requête tronquée")
            n += r
        }
        val q = target.indexOf('?')
        val rawPath = if (q < 0) target else target.substring(0, q)
        val path = decode(rawPath)
        if (path.contains("..")) throw HttpError(400, "chemin refusé")
        return HttpRequest(method, path, if (q < 0) emptyMap() else parseQuery(target.substring(q + 1)), headers, body)
    }

    private fun readHead(input: InputStream): String {
        val out = ByteArrayOutputStream(512)
        var last4 = 0
        while (true) {
            val b = input.read()
            if (b < 0) {
                if (out.size() == 0) throw HttpError(0, "connexion fermée")
                throw HttpError(400, "requête tronquée")
            }
            out.write(b)
            last4 = (last4 shl 8) or b
            if (last4 == 0x0D0A0D0A) break
            if (out.size() > MAX_HEAD) throw HttpError(431, "en-têtes trop longs")
        }
        val s = out.toString("ISO-8859-1")
        return s.substring(0, s.length - 4)
    }

    fun parseQuery(q: String): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        for (part in q.split('&')) {
            if (part.isEmpty()) continue
            val e = part.indexOf('=')
            if (e < 0) out[decode(part)] = "" else out[decode(part.substring(0, e))] = decode(part.substring(e + 1))
        }
        return out
    }

    private fun decode(s: String): String = try {
        URLDecoder.decode(s.replace("+", "%2B"), "UTF-8")
    } catch (_: Exception) {
        throw HttpError(400, "encodage illisible")
    }

    fun write(out: OutputStream, r: HttpResponse) {
        val sb = StringBuilder()
        sb.append("HTTP/1.1 ").append(r.status).append(' ').append(reason(r.status)).append("\r\n")
        sb.append("Content-Type: ").append(r.type).append("\r\n")
        sb.append("Content-Length: ").append(r.body.size).append("\r\n")
        sb.append("Connection: close\r\n")
        sb.append("Cache-Control: no-store\r\n")
        sb.append("X-Content-Type-Options: nosniff\r\n")
        sb.append("Referrer-Policy: no-referrer\r\n")
        sb.append("X-Frame-Options: DENY\r\n")
        for ((k, v) in r.headers) sb.append(k).append(": ").append(v).append("\r\n")
        sb.append("\r\n")
        out.write(sb.toString().toByteArray(Charsets.UTF_8))
        out.write(r.body)
        out.flush()
    }

    fun reason(status: Int): String = when (status) {
        200 -> "OK"; 201 -> "Created"; 204 -> "No Content"
        400 -> "Bad Request"; 401 -> "Unauthorized"; 403 -> "Forbidden"; 404 -> "Not Found"
        405 -> "Method Not Allowed"; 409 -> "Conflict"; 411 -> "Length Required"; 413 -> "Payload Too Large"
        422 -> "Unprocessable Entity"; 429 -> "Too Many Requests"; 431 -> "Request Header Fields Too Large"
        502 -> "Bad Gateway"; 503 -> "Service Unavailable"
        else -> if (status < 400) "OK" else "Error"
    }
}
