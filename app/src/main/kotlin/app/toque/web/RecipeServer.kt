package app.toque.web

import android.os.Handler
import android.os.Looper
import app.toque.ToqueApp
import app.toque.core.i18n.tr
import app.toque.core.machine.Snapshot
import app.toque.core.recipes.Recipe
import app.toque.core.recipes.SearchIndex
import app.toque.core.web.ApiKey
import app.toque.core.web.Http
import app.toque.core.web.HttpError
import app.toque.core.web.HttpResponse
import app.toque.core.web.LocalNet
import app.toque.core.web.RecipeApi
import app.toque.core.web.RecipeStore
import app.toque.core.web.ServerGate
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.ArrayBlockingQueue

/**
 * Serveur de recettes du réseau local : une page web pour écrire et modifier ses recettes depuis un
 * ordinateur ou un téléphone, et la même interface pour un assistant (clé dans le code QR des Réglages).
 *
 * Il ne tourne jamais pendant une cuisson ([ServerGate]) : il s'arrête dès que la base tourne ou
 * chauffe, ou que la cuisson guidée est ouverte, et repart tout seul après. Il ne pilote jamais la
 * machine. Deux fils au plus pour les requêtes, une requête par connexion, délais courts.
 */
class RecipeServer(private val app: ToqueApp) {
    private val main = Handler(Looper.getMainLooper())
    private val listeners = ArrayList<() -> Unit>()
    @Volatile private var snapshot = Snapshot()
    private var socket: ServerSocket? = null
    private var pool: ThreadPoolExecutor? = null
    private var unsubscribe: (() -> Unit)? = null

    /** Cuisson guidée ouverte sur l'écran (posé par MainActivity). */
    @Volatile var cooking = false
        set(v) { if (field != v) { field = v; update() } }

    /** Pourquoi le serveur ne tourne pas, ou null s'il tourne. */
    var reason: String? = tr("Désactivé")
        private set
    var error: String? = null
        private set
    val running: Boolean get() = socket != null

    val bring: BringAccount by lazy { BringAccount(app) }

    private val key = ApiKey({ app.prefs.webKey })

    private val store = object : RecipeStore {
        override val index: SearchIndex get() = app.recipes.index
        override fun get(id: String): Recipe? = app.recipes.get(id)
        override fun saveUser(r: Recipe) = app.recipes.saveUser(r)
        override fun deleteUser(id: String) = app.recipes.deleteUser(id)
        override fun userRecipes(): List<Recipe> = app.recipes.userRecipes()
        override fun saveAll(list: List<Recipe>) {
            app.recipes.upsertAll(list, rebuild = false)
            app.recipes.refresh()
        }
    }

    private val page: ByteArray by lazy { app.assets.open("web/index.html").use { it.readBytes() } }

    private val api by lazy {
        val version = try { app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "" } catch (_: Exception) { "" }
        RecipeApi(store, key, { page }, bring, { ServerGate.reason(true, snapshot, cooking) }, version) { app.log.write("serveur", it) }
    }

    /** À appeler une fois (MainActivity.onCreate) : suit l'état de la machine. */
    fun attach() {
        if (unsubscribe != null) return
        unsubscribe = app.machine.onSnapshot { s ->
            snapshot = s
            update()
        }
    }

    fun onChange(l: () -> Unit): () -> Unit {
        listeners += l
        return { listeners -= l }
    }

    private fun changed() = main.post { for (l in ArrayList(listeners)) l() }

    fun setEnabled(on: Boolean) {
        if (on && app.prefs.webKey.isEmpty()) app.prefs.webKey = ApiKey.generate()
        app.prefs.webServer = on
        lastAttemptMs = 0
        update()
    }

    /** Nouvelle clé : l'ancienne (navigateurs, assistants) ne marche plus. */
    fun newKey() {
        app.prefs.webKey = ApiKey.generate()
        app.log.write("serveur", "nouvelle clé")
        changed()
    }

    /** Démarre ou arrête selon le réglage et la machine. Fil de l'interface. */
    fun update() {
        val r = ServerGate.reason(app.prefs.webServer, snapshot, cooking)
        val now = System.currentTimeMillis()
        // Après un échec (port pris…), nouvel essai toutes les 30 s au plus, pas à chaque état machine.
        if (r == null && socket == null && now - lastAttemptMs > 30_000) { lastAttemptMs = now; start() }
        if (r != null && socket != null) stop(r)
        if (r != null) { lastAttemptMs = 0; error = null }
        val shown = r ?: if (socket == null) (error ?: tr("Arrêté")) else null
        if (shown != reason) { reason = shown; changed() }
    }

    private var lastAttemptMs = 0L

    private fun start() {
        try {
            val s = ServerSocket()
            s.reuseAddress = true
            s.bind(InetSocketAddress(app.prefs.webPort), 8)
            socket = s
            error = null
            val p = ThreadPoolExecutor(1, 2, 30, TimeUnit.SECONDS, ArrayBlockingQueue(8)) { r ->
                Thread(r, "toque-web").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }
            }
            p.rejectedExecutionHandler = ThreadPoolExecutor.DiscardPolicy()
            pool = p
            Thread({ acceptLoop(s, p) }, "toque-web-accept").apply { priority = Thread.MIN_PRIORITY; isDaemon = true }.start()
            app.log.write("serveur", "démarré sur le port ${app.prefs.webPort}")
        } catch (e: Exception) {
            socket = null
            error = tr("Démarrage impossible : %s", e.message ?: e.toString())
            app.log.write("serveur", "démarrage impossible : $e")
        }
    }

    private fun stop(why: String) {
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        pool?.shutdownNow()
        pool = null
        app.log.write("serveur", "arrêté : $why")
    }

    private fun acceptLoop(s: ServerSocket, p: ThreadPoolExecutor) {
        while (!s.isClosed) {
            val c = try { s.accept() } catch (_: SocketException) { break } catch (_: Exception) { continue }
            val local = c.inetAddress?.address?.let { LocalNet.isLocal(it) } ?: false
            if (!local) {
                try { c.close() } catch (_: Exception) {}
                continue
            }
            try {
                p.execute { serve(c) }
            } catch (_: Exception) {
                try { c.close() } catch (_: Exception) {}
            }
        }
    }

    private fun serve(c: Socket) {
        c.use { sock ->
            try {
                sock.soTimeout = 15_000
                val input = sock.getInputStream().buffered()
                val out = sock.getOutputStream().buffered()
                val response = try {
                    api.handle(Http.read(input))
                } catch (e: HttpError) {
                    if (e.status == 0) return
                    HttpResponse.error(e.status, e.message ?: "")
                }
                Http.write(out, response)
            } catch (_: Exception) {
                // Client parti, délai dépassé : rien à faire.
            }
        }
    }

    /** Adresse IPv4 du robot sur le réseau local (Wi-Fi), ou null sans réseau. */
    fun address(): String? = try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback }
            .sortedBy { if (it.name.startsWith("wlan")) 0 else 1 }
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { it is Inet4Address && LocalNet.isLocal(it.address) && !it.isLoopbackAddress }
            ?.hostAddress
    } catch (_: Exception) {
        null
    }

    /** Adresse à ouvrir dans le navigateur ; avec [withKey], la clé suit le « # » (jamais envoyée sur le réseau). */
    fun url(withKey: Boolean): String? {
        val ip = address() ?: return null
        val base = "http://$ip:${app.prefs.webPort}/"
        return if (withKey) base + "#k=" + app.prefs.webKey else base
    }
}
