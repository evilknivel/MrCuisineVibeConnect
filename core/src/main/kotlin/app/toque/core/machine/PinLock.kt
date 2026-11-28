package app.toque.core.machine

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Code de protection des réglages de sécurité (facultatif) : détection et inversion du capteur du
 * bol, montée progressive, enchaînement des étapes, mode machine, choix du robot, retour à l'appli
 * d'origine, serveur de recettes. Sans code, rien n'est demandé.
 *
 * Le code n'est jamais gardé en clair : sel aléatoire + SHA-256 répété ([ROUNDS] fois). Après
 * [MAX_FAILURES] codes faux, plus d'essai pendant un temps qui double à chaque nouvelle série
 * (1 min, 2, 4 … 30 min au plus). Un code juste ouvre les réglages pendant [UNLOCK_MS].
 */
class PinLock(private val clock: () -> Long = System::currentTimeMillis) {

    data class Stored(val salt: String, val hash: String) {
        val isSet: Boolean get() = salt.isNotEmpty() && hash.isNotEmpty()
    }

    enum class Result { OK, WRONG, LOCKED }

    private var failures = 0
    private var series = 0
    private var lockedUntil = 0L
    private var unlockedUntil = 0L

    /** Encore bloqué pendant ce temps (ms), 0 sinon. */
    val lockedForMs: Long get() = maxOf(0L, lockedUntil - clock())

    /** Code juste saisi il y a moins de [UNLOCK_MS] (ou aucun code défini : voir [needsCode]). */
    val unlocked: Boolean get() = clock() < unlockedUntil

    fun needsCode(stored: Stored): Boolean = stored.isSet && !unlocked

    @Synchronized
    fun check(pin: String, stored: Stored): Result {
        if (!stored.isSet) return Result.OK
        val now = clock()
        if (now < lockedUntil) return Result.LOCKED
        if (MessageDigest.isEqual(hash(pin, stored.salt).toByteArray(), stored.hash.toByteArray())) {
            failures = 0
            series = 0
            unlockedUntil = now + UNLOCK_MS
            return Result.OK
        }
        failures++
        if (failures >= MAX_FAILURES) {
            failures = 0
            lockedUntil = now + minOf(MAX_LOCK_MS, LOCK_MS shl minOf(series, 10))
            series++
            return Result.LOCKED
        }
        return Result.WRONG
    }

    /** Referme tout de suite (en quittant les réglages). */
    fun relock() {
        unlockedUntil = 0L
    }

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 8
        const val MAX_FAILURES = 5
        const val LOCK_MS = 60_000L
        const val MAX_LOCK_MS = 30 * 60_000L
        const val UNLOCK_MS = 5 * 60_000L
        const val ROUNDS = 5_000

        fun isValid(pin: String): Boolean = pin.length in MIN_LENGTH..MAX_LENGTH && pin.all { it in '0'..'9' }

        /** Nouveau code : sel aléatoire et empreinte, à enregistrer dans les réglages. */
        fun create(pin: String, random: SecureRandom = SecureRandom()): Stored {
            require(isValid(pin)) { "code de $MIN_LENGTH à $MAX_LENGTH chiffres" }
            val salt = ByteArray(16).also { random.nextBytes(it) }.joinToString("") { "%02x".format(it) }
            return Stored(salt, hash(pin, salt))
        }

        fun hash(pin: String, salt: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            var h = (salt + ":" + pin).toByteArray(Charsets.UTF_8)
            repeat(ROUNDS) {
                md.reset()
                md.update(salt.toByteArray(Charsets.UTF_8))
                h = md.digest(h)
            }
            return h.joinToString("") { "%02x".format(it) }
        }
    }
}
