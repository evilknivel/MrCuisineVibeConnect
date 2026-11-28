package app.toque.ui

import android.view.Gravity
import android.widget.GridLayout
import android.widget.LinearLayout
import app.toque.core.i18n.tr
import app.toque.core.machine.PinLock

/**
 * Demande le code de protection avant un réglage de sécurité. Sans code défini, ou peu après un
 * code juste ([PinLock.UNLOCK_MS]), l'action part tout de suite. Pavé numérique à l'écran : la
 * tablette du robot n'a pas toujours de clavier pratique.
 */
object PinGate {

    fun require(a: MainActivity, action: () -> Unit) {
        val lock = a.app.pinLock
        val stored = a.prefs.pin
        if (!lock.needsCode(stored)) { action(); return }
        ask(a, tr("Code de protection"), tr("Ce réglage touche à la sécurité. Tapez le code de protection.")) { pin, show ->
            when (lock.check(pin, stored)) {
                PinLock.Result.OK -> { a.closeOverlay(); action() }
                PinLock.Result.WRONG -> show(tr("Code faux"))
                PinLock.Result.LOCKED -> show(tr("Trop d'essais : attendez %d min", ((lock.lockedForMs + 59_999) / 60_000).toInt()))
            }
        }
    }

    /** Définir ou changer le code : deux saisies identiques (après le code actuel, s'il y en a un). */
    fun set(a: MainActivity, done: () -> Unit) = require(a) {
        ask(a, tr("Nouveau code"), tr("De %d à %d chiffres.", PinLock.MIN_LENGTH, PinLock.MAX_LENGTH)) { first, show ->
            if (!PinLock.isValid(first)) { show(tr("De %d à %d chiffres.", PinLock.MIN_LENGTH, PinLock.MAX_LENGTH)); return@ask }
            ask(a, tr("Nouveau code"), tr("Tapez-le une seconde fois.")) { second, show2 ->
                if (second != first) { show2(tr("Les deux codes sont différents")); return@ask }
                a.prefs.pin = PinLock.create(first)
                a.app.log.write("sécurité", "code de protection défini")
                a.closeOverlay()
                a.toast(tr("Code de protection enregistré"), Ui.OK)
                done()
            }
        }
    }

    /** Retirer le code (après le code actuel). */
    fun clear(a: MainActivity, done: () -> Unit) = require(a) {
        a.prefs.pin = PinLock.Stored("", "")
        a.app.pinLock.relock()
        a.app.log.write("sécurité", "code de protection retiré")
        a.toast(tr("Code de protection retiré"))
        done()
    }

    /** Carte avec pavé numérique ; [onEnter] reçoit le code et une fonction pour afficher une erreur. */
    private fun ask(a: MainActivity, title: String, message: String, onEnter: (String, (String) -> Unit) -> Unit) {
        val card = Ui.card(a, 28f, Ui.SURFACE2)
        card.setPadding(Ui.u(28), Ui.u(24), Ui.u(28), Ui.u(22))
        val head = Ui.horizontal(a)
        head.addView(Icon(a, Icon.Kind.WARNING, Ui.ACCENT), LinearLayout.LayoutParams(Ui.u(30), Ui.u(30)).apply { rightMargin = Ui.u(12) })
        head.addView(Ui.text(a, title, 24f, Ui.TEXT, Ui.MEDIUM))
        card.addView(head)
        val info = Ui.text(a, message, 17f, Ui.TEXT2).apply { setLineSpacing(0f, 1.15f) }
        card.addView(info, Ui.lp(t = 8))
        var code = ""
        val dots = Ui.text(a, "", 34f, Ui.TEXT, Ui.MEDIUM).apply { gravity = Gravity.CENTER; letterSpacing = 0.3f }
        card.addView(dots, Ui.lp(t = 12, b = 8))
        fun paint() { dots.text = if (code.isEmpty()) "–" else "●".repeat(code.length) }
        paint()
        val error = { msg: String ->
            info.text = msg
            info.setTextColor(Ui.WARN)
            code = ""
            paint()
        }
        val grid = GridLayout(a).apply { columnCount = 3 }
        fun key(label: String, style: Ui.Style = Ui.Style.SECONDARY, onClick: () -> Unit) {
            val b = Ui.button(a, label, style, height = 64, textSize = 24f) { onClick() }
            grid.addView(b, GridLayout.LayoutParams().apply {
                width = Ui.u(110); height = Ui.u(64); setMargins(Ui.u(6), Ui.u(6), Ui.u(6), Ui.u(6))
            })
        }
        for (d in listOf("1", "2", "3", "4", "5", "6", "7", "8", "9")) key(d) { if (code.length < PinLock.MAX_LENGTH) { code += d; paint() } }
        key("⌫", Ui.Style.GHOST) { code = code.dropLast(1); paint() }
        key("0") { if (code.length < PinLock.MAX_LENGTH) { code += "0"; paint() } }
        key("OK", Ui.Style.PRIMARY) { if (code.isNotEmpty()) onEnter(code, error) }
        card.addView(grid, LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP).apply { gravity = Gravity.CENTER_HORIZONTAL })
        val cancel = Ui.button(a, tr("Annuler"), Ui.Style.GHOST, height = 52) { a.closeOverlay() }
        card.addView(cancel, Ui.lp(Ui.WRAP, Ui.WRAP, t = 8).apply { gravity = Gravity.END })
        a.showOverlay(card, 440, dismissOnScrim = false)
    }
}
