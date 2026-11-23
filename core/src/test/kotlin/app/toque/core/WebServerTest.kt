package app.toque.core

import app.toque.core.json.Json
import app.toque.core.json.list
import app.toque.core.json.obj
import app.toque.core.json.str
import app.toque.core.machine.Snapshot
import app.toque.core.recipes.Ingredient
import app.toque.core.recipes.IngredientGroup
import app.toque.core.recipes.Recipe
import app.toque.core.recipes.RecipeCodec
import app.toque.core.recipes.RecipeSource
import app.toque.core.recipes.SearchIndex
import app.toque.core.recipes.Step
import app.toque.core.recipes.StepMachine
import app.toque.core.recipes.UserRecipes
import app.toque.core.web.ApiKey
import app.toque.core.web.BringItem
import app.toque.core.web.BringItems
import app.toque.core.web.BringPort
import app.toque.core.web.Http
import app.toque.core.web.HttpError
import app.toque.core.web.HttpRequest
import app.toque.core.web.LocalNet
import app.toque.core.web.Qr
import app.toque.core.web.RecipeApi
import app.toque.core.web.RecipeStore
import app.toque.core.web.ServerGate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class WebServerTest {
    private val lidl = Recipe(
        id = "web-250008", title = "Soupe de potiron", servings = 4, source = RecipeSource.IMPORTED,
        ingredients = listOf(IngredientGroup("", listOf(
            Ingredient("800", "g", "potiron"), Ingredient("1", "", "oignon"), Ingredient("500", "ml", "eau"),
            Ingredient("200", "g", "potiron", "en dés"),
        ))),
        steps = listOf(Step("Mixer.", StepMachine(seconds = 30, speed = 8))),
    )

    private class Store(vararg initial: Recipe) : RecipeStore {
        val map = LinkedHashMap<String, Recipe>().apply { for (r in initial) put(r.id, r) }
        override val index: SearchIndex get() = SearchIndex(map.values.toList())
        override fun get(id: String) = map[id]
        override fun saveUser(r: Recipe) { require(UserRecipes.isUser(r.id)); map[r.id] = r }
        override fun deleteUser(id: String) { map.remove(id) }
        override fun userRecipes() = map.values.filter { UserRecipes.isUser(it.id) }
        override fun saveAll(list: List<Recipe>) { for (r in list) saveUser(r) }
    }

    private class FakeBring : BringPort {
        val added = ArrayList<BringItem>()
        override fun status() = linkedMapOf<String, Any?>("linked" to true)
        override fun login(email: String, password: String) {}
        override fun selectList(uuid: String) {}
        override fun logout() {}
        override fun add(items: List<BringItem>) { added += items }
    }

    private val key = "secretkey"
    private var busy: String? = null
    private val store = Store(lidl)
    private val bring = FakeBring()
    private val api = RecipeApi(store, ApiKey({ key }), { "<html></html>".toByteArray() }, bring, { busy })

    private fun req(method: String, path: String, body: String = "", auth: String? = "Bearer $key", query: Map<String, String> = emptyMap()) =
        HttpRequest(method, path, query, if (auth == null) emptyMap() else mapOf("authorization" to auth), body.toByteArray())

    private fun json(r: app.toque.core.web.HttpResponse) = Json.parse(String(r.body, Charsets.UTF_8)).obj()

    // ---- QR

    @Test fun qrPicksTheSmallestVersionAndHasFinderPatterns() {
        val small = Qr.encode("hello")
        assertEquals(1, small.version)
        assertEquals(21, small.size)
        val url = Qr.encode("http://192.168.178.123:8080/#k=abcdefghjkmnpqrstuvwxyz234")
        assertTrue(url.version in 3..5)
        for (q in listOf(small, url)) {
            // Coins des motifs de repère : sombres ; séparateurs : clairs.
            for ((x, y) in listOf(0 to 0, q.size - 1 to 0, 0 to q.size - 1, 3 to 3)) assertTrue(q[x, y])
            assertFalse(q[7, 0]); assertFalse(q[q.size - 8, 0]); assertFalse(q[0, q.size - 8])
            // Module sombre fixe de la norme.
            assertTrue(q[8, q.size - 8])
        }
        assertEquals(10, Qr.encode("x".repeat(200)).version)
        try {
            Qr.encode("x".repeat(300)); fail()
        } catch (_: IllegalArgumentException) {}
    }

    /** Écrit quelques codes dans build/qr (vérifiés par un décodeur externe pendant le développement). */
    @Test fun qrSamples() {
        val dir = File("build/qr").apply { mkdirs() }
        val samples = listOf("hello", "http://192.168.178.123:8080/#k=abcdefghjkmnpqrstuvwxyz234",
            "http://10.0.0.7:8080/#k=" + "z".repeat(26) + "&ü", "x".repeat(150))
        for ((i, s) in samples.withIndex()) {
            val q = Qr.encode(s)
            File(dir, "$i.txt").writeText(s + "\n" + q.modules.joinToString("\n") { row -> row.joinToString("") { if (it) "1" else "0" } })
        }
    }

    // ---- HTTP

    @Test fun httpRequestIsParsedWithLimits() {
        val raw = "POST /api/recipes?q=soupe%20froide&source=user HTTP/1.1\r\nHost: robot\r\nAuthorization: Bearer k\r\nContent-Length: 4\r\n\r\nabcdEXTRA"
        val r = Http.read(ByteArrayInputStream(raw.toByteArray()))
        assertEquals("POST", r.method)
        assertEquals("/api/recipes", r.path)
        assertEquals("soupe froide", r.query["q"])
        assertEquals("Bearer k", r.header("Authorization"))
        assertEquals("abcd", r.text)
        for ((bad, status) in listOf(
            "GET /x HTTP/1.1\r\nContent-Length: 99999999\r\n\r\n" to 413,
            "GET /../etc HTTP/1.1\r\n\r\n" to 400,
            "GARBAGE\r\n\r\n" to 400,
            "POST / HTTP/1.1\r\nTransfer-Encoding: chunked\r\n\r\n" to 411,
        )) {
            try {
                Http.read(ByteArrayInputStream(bad.toByteArray())); fail(bad)
            } catch (e: HttpError) {
                assertEquals(bad, status, e.status)
            }
        }
        val out = ByteArrayOutputStream()
        Http.write(out, app.toque.core.web.HttpResponse.json(200, mapOf("a" to 1)))
        val s = out.toString("UTF-8")
        assertTrue(s.startsWith("HTTP/1.1 200 OK\r\n"))
        assertTrue(s.contains("Content-Length: 7\r\n"))
        assertTrue(s.endsWith("\r\n\r\n{\"a\":1}"))
    }

    // ---- Accès

    @Test fun wrongKeysLockTheServerForAMinute() {
        var now = 1_000_000L
        val k = ApiKey({ "good" }, { now })
        assertEquals(ApiKey.Verdict.OK, k.check("Bearer good"))
        assertEquals(ApiKey.Verdict.WRONG, k.check(null))
        assertEquals(ApiKey.Verdict.WRONG, k.check("good"))
        repeat(2) { assertEquals(ApiKey.Verdict.WRONG, k.check("Bearer nope")) }
        assertEquals(ApiKey.Verdict.LOCKED, k.check("Bearer nope"))
        // Même la bonne clé attend.
        assertEquals(ApiKey.Verdict.LOCKED, k.check("Bearer good"))
        now += 61_000
        assertEquals(ApiKey.Verdict.OK, k.check("Bearer good"))
        // Une clé vide n'ouvre jamais.
        assertEquals(ApiKey.Verdict.WRONG, ApiKey({ "" }).check("Bearer "))
        val g = ApiKey.generate()
        assertEquals(26, g.length)
        assertTrue(g.all { it in "abcdefghjkmnpqrstuvwxyz23456789" })
    }

    @Test fun onlyLocalAddresses() {
        fun ip(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }
        for (a in listOf(ip(192, 168, 1, 2), ip(10, 0, 0, 1), ip(172, 16, 0, 1), ip(172, 31, 9, 9), ip(127, 0, 0, 1), ip(169, 254, 3, 4)))
            assertTrue(LocalNet.isLocal(a))
        for (a in listOf(ip(8, 8, 8, 8), ip(172, 32, 0, 1), ip(192, 169, 0, 1), ip(100, 64, 0, 1)))
            assertFalse(LocalNet.isLocal(a))
        val v6 = ByteArray(16)
        v6[15] = 1
        assertTrue(LocalNet.isLocal(v6)) // ::1
        val ula = ByteArray(16).also { it[0] = 0xFD.toByte() }
        assertTrue(LocalNet.isLocal(ula))
        val link = ByteArray(16).also { it[0] = 0xFE.toByte(); it[1] = 0x80.toByte() }
        assertTrue(LocalNet.isLocal(link))
        val global = ByteArray(16).also { it[0] = 0x20; it[1] = 0x01 }
        assertFalse(LocalNet.isLocal(global))
        val mapped = ByteArray(16).also { it[10] = -1; it[11] = -1; it[12] = 8; it[13] = 8; it[14] = 8; it[15] = 8 }
        assertFalse(LocalNet.isLocal(mapped))
        mapped[12] = 192.toByte(); mapped[13] = 168.toByte()
        assertTrue(LocalNet.isLocal(mapped))
    }

    @Test fun serverNeverRunsWhileCooking() {
        val idle = Snapshot()
        assertNull(ServerGate.reason(true, idle, cooking = false))
        assertNotNull(ServerGate.reason(false, idle, cooking = false))
        for (s in listOf(idle.copy(jobActive = true), idle.copy(motorOn = true), idle.copy(heaterOn = true), idle.copy(waitingStop = true)))
            assertNotNull(ServerGate.reason(true, s, cooking = false))
        assertNotNull(ServerGate.reason(true, idle, cooking = true))
    }

    // ---- API

    @Test fun pageIsPublicButTheApiNeedsTheKey() {
        assertEquals(200, api.handle(req("GET", "/", auth = null)).status)
        assertEquals(401, api.handle(req("GET", "/api/info", auth = null)).status)
        assertEquals(401, api.handle(req("GET", "/api/recipes", auth = "Bearer wrong")).status)
        val info = json(api.handle(req("GET", "/api/info")))
        assertEquals("toque", info.str("app"))
        assertTrue(info["texts"].obj().isNotEmpty())
        assertTrue(info["programs"].list().isNotEmpty())
        assertEquals(404, api.handle(req("GET", "/etc/passwd")).status)
    }

    @Test fun createEditDeleteMyRecipes() {
        val body = RecipeCodec.encode(Recipe(id = "ignored", title = "Pesto", servings = 2,
            ingredients = listOf(IngredientGroup("", listOf(Ingredient("50", "g", "basilic")))),
            steps = listOf(Step("Mixer", StepMachine(seconds = 10, speed = 7)))))
        val created = api.handle(req("POST", "/api/recipes", body))
        assertEquals(201, created.status)
        val id = json(created).str("id")!!
        assertTrue(UserRecipes.isUser(id))
        assertEquals(RecipeSource.USER, store.map[id]!!.source)

        val edited = RecipeCodec.encode(store.map[id]!!.copy(title = "Pesto rosso"))
        assertEquals(200, api.handle(req("PUT", "/api/recipes/$id", edited)).status)
        assertEquals("Pesto rosso", store.map[id]!!.title)

        val list = json(api.handle(req("GET", "/api/recipes", query = mapOf("source" to "user"))))
        assertEquals(listOf(id), list["recipes"].list().map { it.obj().str("id") })

        assertEquals(200, api.handle(req("DELETE", "/api/recipes/$id")).status)
        assertNull(store.map[id])
    }

    @Test fun unsafeMachineSettingsAreRefused() {
        val tooFast = RecipeCodec.encode(Recipe(id = "", title = "Crème", steps = listOf(Step("Fouetter",
            StepMachine(seconds = 60, speed = 9, accessory = app.toque.core.machine.Accessory.WHISK)))))
        val r = api.handle(req("POST", "/api/recipes", tooFast))
        assertEquals(422, r.status)
        assertEquals(1, store.map.size)
        assertEquals(422, api.handle(req("POST", "/api/recipes", "{\"title\":\"\"}")).status)
        assertEquals(400, api.handle(req("POST", "/api/recipes", "{oops")).status)
    }

    @Test fun lidlRecipesAreReadOnlyButCanBeCopied() {
        assertEquals(403, api.handle(req("PUT", "/api/recipes/${lidl.id}", RecipeCodec.encode(lidl))).status)
        assertEquals(403, api.handle(req("DELETE", "/api/recipes/${lidl.id}")).status)
        assertEquals(lidl, store.map[lidl.id])
        val one = json(api.handle(req("GET", "/api/recipes/${lidl.id}")))
        assertEquals(false, one["editable"])
        assertEquals("lidl", one.str("kind"))

        val copy = api.handle(req("POST", "/api/recipes/${lidl.id}/copy"))
        assertEquals(201, copy.status)
        val id = json(copy).str("id")!!
        assertTrue(UserRecipes.isUser(id))
        assertEquals("Soupe de potiron", store.map[id]!!.title)
        // La copie se modifie, l'original ne bouge pas.
        assertEquals(200, api.handle(req("PUT", "/api/recipes/$id", RecipeCodec.encode(store.map[id]!!.copy(servings = 6)))).status)
        assertEquals(4, store.map[lidl.id]!!.servings)
    }

    @Test fun nothingIsWrittenWhileTheRobotCooks() {
        busy = "cuisine"
        assertEquals(503, api.handle(req("POST", "/api/recipes/${lidl.id}/copy")).status)
        assertEquals(503, api.handle(req("POST", "/api/recipes", RecipeCodec.encode(lidl.copy(id = "")))).status)
        assertEquals(1, store.map.size)
        // Lire reste possible.
        assertEquals(200, api.handle(req("GET", "/api/recipes/${lidl.id}")).status)
    }

    @Test fun importAndExportFiles() {
        val file = RecipeCodec.encodeFile(listOf(lidl.copy(id = "a", title = "A"), lidl.copy(id = "b", title = "")))
        val r = json(api.handle(req("POST", "/api/import", file)))
        assertEquals(listOf("perso-a"), r["imported"].list())
        assertEquals(1, (r["unreadable"] as Number).toInt())
        val out = api.handle(req("GET", "/api/export"))
        val (list, _) = RecipeCodec.decodeFile(String(out.body, Charsets.UTF_8), RecipeSource.USER)
        assertEquals(listOf("perso-a"), list.map { it.id })
    }

    @Test fun shoppingListForBring() {
        val items = BringItems.of(lidl)
        // L'eau reste au robinet ; le potiron en deux lignes devient un article.
        assertEquals(listOf(BringItem("potiron", "800 g + 200 g"), BringItem("oignon", "1")), items)
        assertEquals("800 g + 200 g potiron\n1 oignon", BringItems.text(items))
        val eight = BringItems.of(lidl, servings = 8)
        assertTrue(eight[0].spec, eight[0].spec.filter { it.isDigit() }.startsWith("1600"))
        assertEquals(200, api.handle(req("POST", "/api/recipes/${lidl.id}/bring")).status)
        assertEquals(2, bring.added.size)
        val s = json(api.handle(req("GET", "/api/recipes/${lidl.id}/shopping")))
        assertEquals(2, s["items"].list().size)
    }
}
