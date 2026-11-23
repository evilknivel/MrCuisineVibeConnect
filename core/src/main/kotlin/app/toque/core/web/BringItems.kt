package app.toque.core.web

import app.toque.core.recipes.Recipe
import app.toque.core.recipes.forServings

/** Un article de la liste de courses Bring! : le nom (« Farine ») et la précision (« 500 g »). */
data class BringItem(val name: String, val spec: String)

/**
 * Les ingrédients d'une recette, pour la liste de courses Bring! : un article par ingrédient,
 * quantités pour [servings] personnes (celles de la recette si 0), les doublons réunis.
 */
object BringItems {
    /** Ingrédients qu'on a toujours sous la main : pas mis sur la liste. */
    private val PANTRY = setOf("eau", "wasser", "water", "agua", "aigua", "вода", "eau chaude", "warmes wasser", "lauwarmes wasser")

    fun of(r: Recipe, servings: Int = 0): List<BringItem> {
        val recipe = if (servings > 0 && r.servings > 0 && servings != r.servings) r.forServings(servings) else r
        val out = LinkedHashMap<String, BringItem>()
        for (i in recipe.allIngredients) {
            val name = clean(i.name)
            if (name.isEmpty() || name.lowercase() in PANTRY) continue
            val spec = listOf(i.qty.trim(), i.unit.trim()).filter { it.isNotEmpty() }.joinToString(" ")
            val key = name.lowercase()
            val prev = out[key]
            out[key] = if (prev == null) BringItem(name, spec)
            else BringItem(prev.name, listOf(prev.spec, spec).filter { it.isNotEmpty() }.joinToString(" + "))
        }
        return out.values.toList()
    }

    /** Liste à copier (une ligne par article), pour la coller dans Bring! ou ailleurs. */
    fun text(items: List<BringItem>): String =
        items.joinToString("\n") { if (it.spec.isEmpty()) it.name else it.spec + " " + it.name }

    private fun clean(s: String): String = s.trim().replace(Regex("\\s+"), " ").take(80)
}
