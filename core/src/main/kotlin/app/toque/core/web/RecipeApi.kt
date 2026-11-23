package app.toque.core.web

import app.toque.core.i18n.I18n
import app.toque.core.i18n.tr
import app.toque.core.i18n.trKey
import app.toque.core.json.Json
import app.toque.core.json.obj
import app.toque.core.json.str
import app.toque.core.machine.Accessory
import app.toque.core.machine.Snapshot
import app.toque.core.recipes.CommunityRecipes
import app.toque.core.recipes.Recipe
import app.toque.core.recipes.RecipeCodec
import app.toque.core.recipes.RecipeFilter
import app.toque.core.recipes.RecipeSource
import app.toque.core.recipes.SearchIndex
import app.toque.core.recipes.StepProgram
import app.toque.core.recipes.UserRecipes
import app.toque.core.robot.RobotProfiles
import java.security.MessageDigest

/** Les recettes du robot, vues par le serveur (côté Android : RecipeRepo). Appels hors du fil de l'interface. */
interface RecipeStore {
    val index: SearchIndex
    fun get(id: String): Recipe?
    fun saveUser(r: Recipe)
    fun deleteUser(id: String)
    fun userRecipes(): List<Recipe>
    /** Plusieurs recettes perso d'un coup (import d'un fichier). */
    fun saveAll(list: List<Recipe>)
}

/** Liste de courses Bring! (côté Android : BringAccount). Les erreurs remontent en exceptions, message lisible. */
interface BringPort {
    /** { linked, email, listUuid, listName, lists: [{uuid, name}] } */
    fun status(): Map<String, Any?>
    fun login(email: String, password: String)
    fun selectList(uuid: String)
    fun logout()
    fun add(items: List<BringItem>)
}

/**
 * Le serveur de recettes ne tourne jamais pendant une cuisson : réglage désactivé, base qui chauffe
 * ou tourne, travail en cours (même en pause) ou écran de cuisson guidée ouvert. Renvoie la raison
 * de ne pas tourner, ou null s'il peut tourner.
 */
object ServerGate {
    fun reason(enabled: Boolean, s: Snapshot, cooking: Boolean): String? = when {
        !enabled -> tr("Désactivé")
        s.jobActive || s.motorOn || s.heaterOn || s.waitingStop -> tr("En pause : le robot cuisine")
        cooking -> tr("En pause : cuisson guidée ouverte")
        else -> null
    }
}

/**
 * Clé d'accès (en-tête « Authorization: Bearer … »). Après [MAX_FAILURES] essais faux en une minute,
 * tout est refusé pendant une minute, même la bonne clé : deviner devient sans espoir.
 */
class ApiKey(private val key: () -> String, private val clock: () -> Long = System::currentTimeMillis) {
    enum class Verdict { OK, WRONG, LOCKED }

    private val failures = ArrayDeque<Long>()
    private var lockedUntil = 0L

    @Synchronized
    fun check(header: String?): Verdict {
        val now = clock()
        if (now < lockedUntil) return Verdict.LOCKED
        val given = header?.trim()?.let { if (it.startsWith("Bearer ", true)) it.substring(7).trim() else null }.orEmpty()
        val expected = key()
        if (expected.isNotEmpty() && MessageDigest.isEqual(given.toByteArray(), expected.toByteArray())) return Verdict.OK
        while (failures.isNotEmpty() && now - failures.first() > 60_000) failures.removeFirst()
        failures.addLast(now)
        if (failures.size >= MAX_FAILURES) {
            lockedUntil = now + 60_000
            failures.clear()
            return Verdict.LOCKED
        }
        return Verdict.WRONG
    }

    companion object {
        const val MAX_FAILURES = 5

        /** Nouvelle clé : 128 bits aléatoires, en base 32 sans ambiguïté (26 caractères). */
        fun generate(random: java.security.SecureRandom = java.security.SecureRandom()): String {
            val alphabet = "abcdefghjkmnpqrstuvwxyz23456789"
            return String(CharArray(26) { alphabet[random.nextInt(alphabet.length)] })
        }
    }
}

/** Adresses du réseau local (le serveur refuse les autres). */
object LocalNet {
    fun isLocal(a: ByteArray): Boolean {
        if (a.size == 4) {
            val b0 = a[0].toInt() and 0xFF
            val b1 = a[1].toInt() and 0xFF
            return b0 == 10 || b0 == 127 || (b0 == 172 && b1 in 16..31) || (b0 == 192 && b1 == 168) || (b0 == 169 && b1 == 254)
        }
        if (a.size == 16) {
            if (a.sliceArray(0 until 15).all { it.toInt() == 0 } && a[15].toInt() == 1) return true // ::1
            val mapped = (0 until 10).all { a[it].toInt() == 0 } && a[10].toInt() == -1 && a[11].toInt() == -1
            if (mapped) return isLocal(a.sliceArray(12 until 16))
            val b0 = a[0].toInt() and 0xFF
            val b1 = a[1].toInt() and 0xFF
            return (b0 and 0xFE) == 0xFC || (b0 == 0xFE && (b1 and 0xC0) == 0x80) // fc00::/7, fe80::/10
        }
        return false
    }
}

/** Textes de la page web, traduits dans la langue du robot (la page les reçoit avec /api/info). */
object WebTexts {
    private val keys = linkedMapOf(
        "title" to trKey("Toque · recettes"),
        "connect" to trKey("Se connecter"),
        "keyHelp" to trKey("Sur le robot : Réglages › Serveur de recettes. Scannez le code QR, ou tapez la clé affichée."),
        "key" to trKey("Clé"),
        "wrongKey" to trKey("Clé refusée"),
        "locked" to trKey("Trop d'essais : réessayez dans une minute"),
        "offline" to trKey("Robot injoignable (il cuisine peut-être : le serveur s'arrête pendant la cuisson)"),
        "search" to trKey("Rechercher"),
        "mine" to trKey("Mes recettes"),
        "lidl" to trKey("Recettes Lidl"),
        "community" to trKey("Communauté"),
        "all" to trKey("Toutes"),
        "new" to trKey("Nouvelle recette"),
        "importFile" to trKey("Importer un fichier"),
        "exportFile" to trKey("Exporter mes recettes"),
        "none" to trKey("Aucune recette"),
        "readOnly" to trKey("Recette Lidl : lecture seule. Copiez-la pour la modifier."),
        "copy" to trKey("Copier dans mes recettes"),
        "edit" to trKey("Modifier"),
        "delete" to trKey("Supprimer"),
        "confirmDelete" to trKey("Supprimer cette recette ?"),
        "save" to trKey("Enregistrer"),
        "cancel" to trKey("Annuler"),
        "saved" to trKey("Enregistrée"),
        "back" to trKey("Retour"),
        "recipeTitle" to trKey("Titre"),
        "category" to trKey("Catégorie"),
        "tags" to trKey("Mots-clés (séparés par des virgules)"),
        "servings" to trKey("Personnes"),
        "prep" to trKey("Préparation (min)"),
        "total" to trKey("Total (min)"),
        "difficulty" to trKey("Difficulté (1 à 3)"),
        "notes" to trKey("Notes"),
        "ingredients" to trKey("Ingrédients"),
        "group" to trKey("Groupe (facultatif)"),
        "addGroup" to trKey("Ajouter un groupe"),
        "qty" to trKey("Qté"),
        "unit" to trKey("Unité"),
        "name" to trKey("Ingrédient"),
        "note" to trKey("Précision"),
        "addIngredient" to trKey("Ajouter un ingrédient"),
        "steps" to trKey("Étapes"),
        "step" to trKey("Étape"),
        "text" to trKey("Texte"),
        "addStep" to trKey("Ajouter une étape"),
        "machine" to trKey("Réglage machine"),
        "noMachine" to trKey("Aucun"),
        "seconds" to trKey("Durée (s)"),
        "speed" to trKey("Vitesse"),
        "temp" to trKey("Température"),
        "noHeat" to trKey("Sans chauffe"),
        "reverse" to trKey("Sens inverse"),
        "accessory" to trKey("Accessoire"),
        "pulses" to trKey("Impulsions"),
        "weigh" to trKey("Pesée (g)"),
        "weighLabel" to trKey("Ce qu'on pèse"),
        "up" to trKey("Monter"),
        "down" to trKey("Descendre"),
        "remove" to trKey("Retirer"),
        "bring" to trKey("Mettre sur la liste Bring!"),
        "bringCopy" to trKey("Copier la liste de courses"),
        "copied" to trKey("Liste copiée : collez-la dans Bring!"),
        "bringAdded" to trKey("Ajouté à la liste Bring!"),
        "bringSettings" to trKey("Compte Bring!"),
        "bringHelp" to trKey("Facultatif : reliez votre compte Bring! pour envoyer les ingrédients d'un geste. Le mot de passe sert une fois et n'est pas gardé ; le robot garde seulement le jeton de connexion."),
        "bringEmail" to trKey("E-mail Bring!"),
        "bringPassword" to trKey("Mot de passe Bring!"),
        "bringLink" to trKey("Relier"),
        "bringUnlink" to trKey("Délier"),
        "bringList" to trKey("Liste"),
        "bringLinked" to trKey("Relié"),
        "settings" to trKey("Réglages"),
        "robotBusy" to trKey("Le robot cuisine : modifications impossibles pour l'instant"),
        "error" to trKey("Erreur"),
        "jsonEdit" to trKey("JSON"),
        "logout" to trKey("Oublier la clé sur ce navigateur"),
        "fileHelp" to trKey("Fichier de recettes Toque (.json, format toque-recettes-1)"),
    )

    fun all(): Map<String, String> = keys.mapValuesTo(LinkedHashMap()) { tr(it.value) }
}

/**
 * Le serveur de recettes, sans le réseau : une requête lue, une réponse. Seules les recettes perso
 * s'écrivent ; une recette Lidl ou de la communauté se copie d'abord dans « Mes recettes ».
 * La page web elle-même ([page]) est servie sans clé : elle ne contient aucune donnée.
 */
class RecipeApi(
    private val store: RecipeStore,
    private val key: ApiKey,
    private val page: () -> ByteArray,
    private val bring: BringPort? = null,
    /** Raison de refuser les écritures (le robot cuisine), ou null. */
    private val busy: () -> String? = { null },
    private val version: String = "",
    private val log: (String) -> Unit = {},
) {
    fun handle(req: HttpRequest): HttpResponse = try {
        route(req)
    } catch (e: ApiError) {
        HttpResponse.error(e.status, e.message ?: "", e.code)
    } catch (e: Exception) {
        log("erreur ${req.method} ${req.path} : $e")
        HttpResponse.error(500, e.message ?: e.toString(), "internal")
    }

    private class ApiError(val status: Int, message: String, val code: String = "") : Exception(message)

    private fun route(req: HttpRequest): HttpResponse {
        if (req.path == "/" || req.path == "/index.html") {
            if (req.method != "GET") throw ApiError(405, "GET")
            return HttpResponse.bytes("text/html; charset=utf-8", page(),
                mapOf("Content-Security-Policy" to "default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; frame-ancestors 'none'"))
        }
        if (req.path == "/favicon.ico") return HttpResponse(204, "text/plain", ByteArray(0))
        if (!req.path.startsWith("/api/")) throw ApiError(404, tr("Introuvable"), "not_found")
        when (key.check(req.header("authorization"))) {
            ApiKey.Verdict.OK -> {}
            ApiKey.Verdict.WRONG -> throw ApiError(401, tr("Clé refusée"), "wrong_key")
            ApiKey.Verdict.LOCKED -> throw ApiError(429, tr("Trop d'essais : réessayez dans une minute"), "locked")
        }
        val parts = req.path.removePrefix("/api/").trimEnd('/').split('/')
        val m = req.method
        return when {
            parts == listOf("info") && m == "GET" -> info()
            parts == listOf("recipes") && m == "GET" -> list(req)
            parts == listOf("recipes") && m == "POST" -> create(req)
            parts.size == 2 && parts[0] == "recipes" && m == "GET" -> get(parts[1])
            parts.size == 2 && parts[0] == "recipes" && m == "PUT" -> update(parts[1], req)
            parts.size == 2 && parts[0] == "recipes" && m == "DELETE" -> delete(parts[1])
            parts.size == 3 && parts[0] == "recipes" && parts[2] == "copy" && m == "POST" -> copy(parts[1])
            parts.size == 3 && parts[0] == "recipes" && parts[2] == "shopping" && m == "GET" -> shopping(parts[1], req)
            parts.size == 3 && parts[0] == "recipes" && parts[2] == "bring" && m == "POST" -> toBring(parts[1], req)
            parts == listOf("import") && m == "POST" -> import(req)
            parts == listOf("export") && m == "GET" -> export()
            parts == listOf("bring") && m == "GET" -> bringPort().status().let { HttpResponse.json(200, it) }
            parts == listOf("bring", "login") && m == "POST" -> bringLogin(req)
            parts == listOf("bring", "list") && m == "POST" -> bringList(req)
            parts == listOf("bring", "logout") && m == "POST" -> { bringPort().logout(); HttpResponse.json(200, bringPort().status()) }
            parts.isNotEmpty() && parts[0] in setOf("info", "recipes", "import", "export", "bring") -> throw ApiError(405, tr("Méthode refusée"), "method")
            else -> throw ApiError(404, tr("Introuvable"), "not_found")
        }
    }

    private fun info(): HttpResponse {
        val robot = RobotProfiles.active
        val caps = robot.capabilities
        return HttpResponse.json(200, linkedMapOf(
            "app" to "toque",
            "version" to version,
            "language" to I18n.code,
            "robot" to linkedMapOf("id" to robot.id, "name" to robot.name),
            "busy" to busy(),
            "bring" to (bring != null),
            "texts" to WebTexts.all(),
            "programs" to StepProgram.values().filter { caps.has(it.feature) }.map {
                linkedMapOf("key" to it.key, "label" to it.label, "heated" to it.heated)
            },
            "temperatures" to caps.temperaturesC,
            "maxSpeed" to caps.maxSpeed,
            "reverse" to caps.reverse,
            "accessories" to Accessory.values().filter { it in caps.accessories }.map {
                linkedMapOf("key" to it.name.lowercase(), "label" to it.label, "maxSpeed" to it.maxSpeed)
            },
        ))
    }

    private fun kind(id: String): String = when {
        UserRecipes.isUser(id) -> "user"
        CommunityRecipes.isCommunity(id) -> "community"
        else -> "lidl"
    }

    private fun list(req: HttpRequest): HttpResponse {
        val source = when (req.query["source"]) {
            "user" -> RecipeSource.USER
            "lidl" -> RecipeSource.IMPORTED
            "community" -> RecipeSource.COMMUNITY
            else -> null
        }
        val limit = (req.query["limit"]?.toIntOrNull() ?: 100).coerceIn(1, 500)
        val q = req.query["q"].orEmpty().take(200)
        val hits = store.index.search(q, RecipeFilter(source = source), limit)
        return HttpResponse.json(200, linkedMapOf(
            "total" to store.index.entries.size,
            "recipes" to hits.map { h ->
                val e = h.entry
                linkedMapOf("id" to e.id, "title" to e.title, "category" to e.category, "kind" to kind(e.id),
                    "editable" to UserRecipes.isUser(e.id), "totalMinutes" to e.totalMinutes, "machineSteps" to e.machineSteps,
                    "favorite" to e.favorite, "language" to e.language)
            },
        ))
    }

    private fun full(r: Recipe): Map<String, Any?> {
        val m = LinkedHashMap(RecipeCodec.toMap(r))
        m["kind"] = kind(r.id)
        m["editable"] = UserRecipes.isUser(r.id)
        return m
    }

    private fun find(id: String): Recipe = store.get(id) ?: throw ApiError(404, tr("Recette introuvable"), "not_found")

    private fun get(id: String) = HttpResponse.json(200, full(find(id)))

    private fun writable() {
        busy()?.let { throw ApiError(503, it, "busy") }
    }

    private fun parse(req: HttpRequest): Map<String, Any?> = try {
        Json.parse(req.text).obj()
    } catch (e: Exception) {
        throw ApiError(400, tr("JSON illisible : %s", e.message ?: ""), "json")
    }

    /** Une recette envoyée par la page : toujours une recette perso, validée comme sur le robot. */
    private fun recipeFrom(map: Map<String, Any?>, id: String): Recipe {
        val r = try {
            RecipeCodec.fromMap(map, RecipeSource.USER)
        } catch (e: Exception) {
            throw ApiError(400, tr("Recette illisible : %s", e.message ?: ""), "invalid")
        } ?: throw ApiError(422, tr("Donnez un titre à la recette"), "invalid")
        val mine = r.copy(id = id, source = RecipeSource.USER, rating = 0.0, ratingCount = 0)
        UserRecipes.problem(mine)?.let { throw ApiError(422, it, "invalid") }
        return mine
    }

    private fun create(req: HttpRequest): HttpResponse {
        writable()
        val map = parse(req)
        val r = recipeFrom(map, UserRecipes.newId(map.str("title").orEmpty().ifBlank { "recette" }))
        store.saveUser(r)
        log("recette créée depuis le navigateur : ${r.id}")
        return HttpResponse.json(201, full(store.get(r.id) ?: r))
    }

    private fun update(id: String, req: HttpRequest): HttpResponse {
        writable()
        if (!UserRecipes.isUser(id)) throw ApiError(403, tr("Recette Lidl : lecture seule. Copiez-la pour la modifier."), "read_only")
        val old = find(id)
        val r = recipeFrom(parse(req), id).copy(favorite = old.favorite)
        store.saveUser(r)
        log("recette modifiée depuis le navigateur : $id")
        return HttpResponse.json(200, full(store.get(id) ?: r))
    }

    private fun delete(id: String): HttpResponse {
        writable()
        if (!UserRecipes.isUser(id)) throw ApiError(403, tr("Recette Lidl : lecture seule. Copiez-la pour la modifier."), "read_only")
        find(id)
        store.deleteUser(id)
        log("recette supprimée depuis le navigateur : $id")
        return HttpResponse.json(200, linkedMapOf("deleted" to id))
    }

    private fun copy(id: String): HttpResponse {
        writable()
        val c = UserRecipes.copyOf(find(id))
        store.saveUser(c)
        log("copie de $id depuis le navigateur : ${c.id}")
        return HttpResponse.json(201, full(store.get(c.id) ?: c))
    }

    private fun servings(req: HttpRequest): Int = req.query["servings"]?.toIntOrNull()?.coerceIn(0, 99) ?: 0

    private fun shopping(id: String, req: HttpRequest): HttpResponse {
        val items = BringItems.of(find(id), servings(req))
        return HttpResponse.json(200, linkedMapOf(
            "items" to items.map { linkedMapOf("name" to it.name, "spec" to it.spec) },
            "text" to BringItems.text(items),
        ))
    }

    private fun bringPort(): BringPort = bring ?: throw ApiError(404, tr("Bring! indisponible"), "no_bring")

    private fun toBring(id: String, req: HttpRequest): HttpResponse {
        val items = BringItems.of(find(id), servings(req))
        if (items.isEmpty()) throw ApiError(422, tr("Aucun ingrédient"), "empty")
        try {
            bringPort().add(items)
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError(502, e.message ?: e.toString(), "bring")
        }
        return HttpResponse.json(200, linkedMapOf("added" to items.size))
    }

    private fun bringLogin(req: HttpRequest): HttpResponse {
        val m = parse(req)
        val email = m.str("email")?.trim().orEmpty()
        val password = m.str("password").orEmpty()
        if (email.isEmpty() || password.isEmpty()) throw ApiError(422, tr("E-mail et mot de passe"), "invalid")
        try {
            bringPort().login(email, password)
        } catch (e: ApiError) {
            throw e
        } catch (e: Exception) {
            throw ApiError(502, e.message ?: e.toString(), "bring")
        }
        return HttpResponse.json(200, bringPort().status())
    }

    private fun bringList(req: HttpRequest): HttpResponse {
        val uuid = parse(req).str("uuid")?.trim().orEmpty()
        if (uuid.isEmpty()) throw ApiError(422, tr("Liste"), "invalid")
        try {
            bringPort().selectList(uuid)
        } catch (e: Exception) {
            throw ApiError(502, e.message ?: e.toString(), "bring")
        }
        return HttpResponse.json(200, bringPort().status())
    }

    private fun import(req: HttpRequest): HttpResponse {
        writable()
        val (list, bad) = try {
            RecipeCodec.decodeFile(req.text, RecipeSource.USER)
        } catch (e: Exception) {
            throw ApiError(400, tr("JSON illisible : %s", e.message ?: ""), "json")
        }
        val mine = list.map { UserRecipes.imported(it) }
        val ok = ArrayList<Recipe>()
        val problems = ArrayList<String>()
        for (r in mine) {
            val p = UserRecipes.problem(r)
            if (p == null) ok += r else problems += r.title.ifBlank { r.id } + " : " + p
        }
        if (ok.isNotEmpty()) store.saveAll(ok)
        log("import depuis le navigateur : ${ok.size} recettes, ${problems.size + bad} refusées")
        return HttpResponse.json(if (ok.isEmpty()) 422 else 200, linkedMapOf(
            "imported" to ok.map { it.id },
            "unreadable" to bad,
            "problems" to problems,
        ))
    }

    private fun export(): HttpResponse {
        val body = RecipeCodec.encodeFile(store.userRecipes()).toByteArray(Charsets.UTF_8)
        return HttpResponse.bytes("application/json; charset=utf-8", body,
            mapOf("Content-Disposition" to "attachment; filename=\"" + UserRecipes.FILE_NAME + "\""))
    }
}
