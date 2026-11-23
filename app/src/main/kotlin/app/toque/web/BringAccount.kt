package app.toque.web

import app.toque.Http
import app.toque.ToqueApp
import app.toque.core.i18n.tr
import app.toque.core.json.Json
import app.toque.core.json.int
import app.toque.core.json.list
import app.toque.core.json.obj
import app.toque.core.json.str
import app.toque.core.web.BringItem
import app.toque.core.web.BringPort
import java.io.IOException
import java.net.URLEncoder

/**
 * Liste de courses Bring! (facultatif). Bring! n'a pas d'interface publique pour écrire sur une
 * liste : on parle comme son appli Android (interface utilisée aussi par Home Assistant). Le mot de
 * passe sert à la connexion puis est oublié ; seuls les jetons restent dans les réglages.
 * Appels réseau : hors du fil de l'interface.
 */
class BringAccount(private val app: ToqueApp) : BringPort {
    private val base = "https://api.getbring.com/rest/"

    private var state: Map<String, String>
        get() = app.prefs.bring
        set(v) { app.prefs.bring = v }

    val linked: Boolean get() = state["access"].orEmpty().isNotEmpty()
    val listName: String get() = state["listName"].orEmpty()

    private fun headers(auth: Boolean): Map<String, String> {
        val s = state
        val h = linkedMapOf(
            "X-BRING-API-KEY" to "cof4Nc6D8saplXjE3h3HXqHH8m7VU2i1Gs0g85Sp",
            "X-BRING-CLIENT" to "android",
            "X-BRING-APPLICATION" to "bring",
            "X-BRING-COUNTRY" to s["country"].orEmpty().ifEmpty { "DE" },
        )
        if (auth) {
            h["Authorization"] = "Bearer " + s["access"].orEmpty()
            h["X-BRING-USER-UUID"] = s["uuid"].orEmpty()
            h["X-BRING-PUBLIC-USER-UUID"] = s["publicUuid"].orEmpty()
        }
        return h
    }

    private fun form(vararg pairs: Pair<String, String>) =
        pairs.joinToString("&") { URLEncoder.encode(it.first, "UTF-8") + "=" + URLEncoder.encode(it.second, "UTF-8") }

    private fun call(method: String, path: String, body: String?, type: String?, auth: Boolean = true): Map<String, Any?> {
        if (auth) freshToken()
        val h = headers(auth).toMutableMap()
        if (type != null) h["Content-Type"] = type
        val (code, text) = try {
            Http.send(method, base + path, h, body)
        } catch (e: IOException) {
            throw IOException(tr("Bring! injoignable : %s", e.message ?: e.toString()))
        }
        if (code == 401 && auth) {
            // Jeton refusé : on retente une fois avec un jeton neuf.
            refresh()
            val (c2, t2) = Http.send(method, base + path, headers(true) + (if (type != null) mapOf("Content-Type" to type) else emptyMap()), body)
            if (c2 !in 200..299) throw IOException(tr("Bring! refuse (%d)", c2))
            return parse(t2)
        }
        if (code == 401 || code == 400 && !auth) throw IOException(tr("Bring! : e-mail ou mot de passe refusé"))
        if (code !in 200..299) throw IOException(tr("Bring! refuse (%d)", code))
        return parse(text)
    }

    private fun parse(text: String): Map<String, Any?> = if (text.isBlank()) emptyMap() else try {
        Json.parse(text).obj()
    } catch (_: Exception) {
        emptyMap()
    }

    private fun freshToken() {
        val s = state
        if (s["access"].isNullOrEmpty()) throw IOException(tr("Compte Bring! non relié"))
        val expires = s["expires"]?.toLongOrNull() ?: 0L
        if (System.currentTimeMillis() > expires - 60_000) refresh()
    }

    private fun refresh() {
        val s = state
        val rt = s["refresh"].orEmpty()
        if (rt.isEmpty()) throw IOException(tr("Compte Bring! non relié"))
        val (code, text) = Http.send("POST", base + "v2/bringauth/token", headers(false) + mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            form("grant_type" to "refresh_token", "refresh_token" to rt))
        if (code !in 200..299) {
            if (code == 400 || code == 401) {
                logout()
                throw IOException(tr("Connexion Bring! expirée : reliez le compte à nouveau"))
            }
            throw IOException(tr("Bring! refuse (%d)", code))
        }
        val m = parse(text)
        val access = m.str("access_token").orEmpty()
        if (access.isEmpty()) throw IOException(tr("Réponse de Bring! illisible"))
        state = s + mapOf(
            "access" to access,
            "refresh" to (m.str("refresh_token") ?: rt),
            "expires" to (System.currentTimeMillis() + (m.int("expires_in") ?: 3600) * 1000L).toString(),
        )
    }

    override fun status(): Map<String, Any?> {
        val s = state
        return linkedMapOf(
            "linked" to linked,
            "email" to s["email"].orEmpty(),
            "listUuid" to s["list"].orEmpty(),
            "listName" to s["listName"].orEmpty(),
            "lists" to (s["lists"].orEmpty().split('|').mapNotNull { e ->
                val i = e.indexOf(':')
                if (i <= 0) null else linkedMapOf("uuid" to e.substring(0, i), "name" to e.substring(i + 1))
            }),
        )
    }

    @Synchronized
    override fun login(email: String, password: String) {
        val m = call("POST", "v2/bringauth", form("email" to email, "password" to password), "application/x-www-form-urlencoded", auth = false)
        val uuid = m.str("uuid").orEmpty()
        val access = m.str("access_token").orEmpty()
        if (uuid.isEmpty() || access.isEmpty()) throw IOException(tr("Réponse de Bring! illisible"))
        state = mapOf(
            "email" to email,
            "uuid" to uuid,
            "publicUuid" to m.str("publicUuid").orEmpty(),
            "access" to access,
            "refresh" to m.str("refresh_token").orEmpty(),
            "expires" to (System.currentTimeMillis() + (m.int("expires_in") ?: 3600) * 1000L).toString(),
            "list" to m.str("bringListUUID").orEmpty(),
        )
        loadLists()
        app.log.write("bring", "compte relié")
    }

    private fun loadLists() {
        val s = state
        val lists = call("GET", "bringusers/" + s["uuid"] + "/lists", null, null)["lists"].list().map { it.obj() }
            .mapNotNull { l -> val u = l.str("listUuid").orEmpty(); if (u.isEmpty()) null else u to l.str("name").orEmpty().replace('|', ' ').replace(':', ' ') }
        val chosen = lists.firstOrNull { it.first == s["list"] } ?: lists.firstOrNull()
        state = state + mapOf(
            "lists" to lists.joinToString("|") { it.first + ":" + it.second },
            "list" to (chosen?.first ?: s["list"].orEmpty()),
            "listName" to (chosen?.second ?: ""),
        )
    }

    @Synchronized
    override fun selectList(uuid: String) {
        val name = (status()["lists"] as List<*>).map { it.obj() }.firstOrNull { it.str("uuid") == uuid }?.str("name")
            ?: throw IOException(tr("Liste Bring! inconnue"))
        state = state + mapOf("list" to uuid, "listName" to name)
    }

    @Synchronized
    override fun logout() {
        state = emptyMap()
        app.log.write("bring", "compte délié")
    }

    @Synchronized
    override fun add(items: List<BringItem>) {
        val list = state["list"].orEmpty()
        if (list.isEmpty()) throw IOException(tr("Compte Bring! non relié"))
        val body = Json.write(linkedMapOf(
            "changes" to items.map {
                linkedMapOf("accuracy" to "0.0", "altitude" to "0.0", "latitude" to "0.0", "longitude" to "0.0",
                    "itemId" to it.name, "spec" to it.spec, "uuid" to java.util.UUID.randomUUID().toString(), "operation" to "TO_PURCHASE")
            },
            "sender" to "",
        ))
        call("PUT", "v2/bringlists/$list/items", body, "application/json; charset=utf-8")
        app.log.write("bring", "${items.size} articles mis sur la liste")
    }
}
