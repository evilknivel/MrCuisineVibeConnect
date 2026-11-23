# Format `toque-recettes-1`

Quelle der Wahrheit: `RecipeCodec` in `core/src/main/kotlin/app/toque/core/recipes/Recipe.kt`.

## Datei

```json
{
  "format": "toque-recettes-1",
  "recipes": [ { …Rezept… }, { …Rezept… } ]
}
```

Der Import erkennt eine Toque-Datei am Text `toque-recettes-1`; ohne diese Zeile ignoriert der
Roboter die Datei im Download-Ordner. Ein einzelnes Rezept-Objekt (ohne Hülle) passt nur in das
JSON-Feld des Editors auf der Webseite, nicht in den Datei-Import.

## Rezept

| Feld | Typ | Pflicht | Hinweise |
|---|---|---|---|
| `id` | String | nein | Weglassen oder `perso-<slug>`. Beim Import wird jede ID zu `perso-…`; ohne ID aus dem Titel gebildet. Gleiche ID = Update. |
| `title` | String | **ja** | Ohne Titel wird das Rezept verworfen. |
| `category` | String | nein | Eine Kategorie. |
| `tags` | String[] | nein | |
| `servings` | Int | nein | Personen/Portionen; 0 = unbekannt. |
| `servingUnit` | String | nein | Nur wenn keine Personen gezählt werden („Gläser“, „Stück“). |
| `prepMinutes`, `totalMinutes` | Int | nein | Minuten. |
| `difficulty` | Int 1–3 | nein | |
| `source` | String | nein | `"user"` setzen (wird ohnehin erzwungen). |
| `notes` | String | nein | Anpassungen, Quelle, Tipps. Zeilenumbrüche mit `\n`. |
| `language` | String | nein | Leer lassen, wenn in der Sprache des Roboters. |
| `ingredients` | Gruppe[] | * | Mindestens eine Zutat oder ein Schritt. |
| `steps` | Schritt[] | * | |
| `image`, `favorite`, `author`, `rating`, `ratingCount`, `devices` | | nein | Weglassen. |

## Zutatengruppe

```json
{ "group": "Für den Teig", "items": [
  { "qty": "500", "unit": "g", "name": "Weizenmehl Type 550", "note": "" },
  { "qty": "1", "unit": "Pck.", "name": "Trockenhefe" },
  { "qty": "", "unit": "", "name": "Salz", "note": "nach Geschmack" }
] }
```

- `group` leer für eine einzige Gruppe ohne Überschrift.
- `qty` ist **Text**: `"500"`, `"1,5"`, `"½"`, `"2-3"` – Toque rechnet Portionen damit um,
  also Zahl und Einheit trennen, nicht „500 g“ in `qty` schreiben.
- `unit` kurz: `g`, `kg`, `ml`, `l`, `EL`, `TL`, `Prise`, `Stück`, `Zehe(n)`, `Bund`, `Dose`,
  `Pck.`, `Becher`. Bei Stückzutaten leer lassen („2 Eier“ → qty `2`, unit ``, name `Eier`).
- `note`: Zubereitungshinweis („geviertelt“, „zimmerwarm“, „+ etwas zum Bestäuben“).

## Schritt

```json
{ "text": "Zwiebel und Knoblauch in den Mixbehälter geben und zerkleinern.",
  "machine": { "program": "manuel", "seconds": 5, "speed": 5, "tempC": 0,
               "reverse": false, "accessory": "blade", "pulses": 0 },
  "weigh": { "label": "Zwiebel", "grams": 100 },
  "needsReview": true }
```

- `text` oder `machine` muss vorhanden sein; leere Schritte werden verworfen.
- `machine.program`: `manuel` · `petrir` · `vapeur` · `rissoler` · `turbo` (Smart zusätzlich
  `mixer` · `smoothie` · `sous-vide` · `mijoter` · `fermenter`). Ein unbekannter Schlüssel lässt
  den Maschinenschritt **stillschweigend wegfallen** – deshalb immer prüfen.
- `seconds`: ganze Sekunden. `speed`: 0–10. `tempC`: 0 oder Tabellenwert.
- `accessory`: `"blade"` (Messer, Standard) oder `"whisk"` (Rühraufsatz).
- `pulses`: nur bei `turbo` (Anzahl Impulse).
- Bei `petrir`, `vapeur`, `rissoler` zählt nur `seconds` (Stufe/Temperatur sind fest).
- Bei Smart-Programmen `tempC` = gewünschte Temperatur im erlaubten Bereich, 0 = Standard.
- `weigh.grams` > 0, sonst wird die Wiegung ignoriert.

## Vollständiges Beispiel

```json
{
  "format": "toque-recettes-1",
  "recipes": [
    {
      "title": "Kürbissuppe mit Ingwer",
      "category": "Suppen",
      "tags": ["vegetarisch", "Herbst", "thermomix-umgerechnet"],
      "servings": 4, "prepMinutes": 10, "totalMinutes": 35, "difficulty": 1,
      "source": "user",
      "notes": "Nach: Thermomix-Rezept (TM6).\nAngepasst: 98 °C → 95 °C; Pürieren ohne Hitze als eigener Schritt.",
      "ingredients": [
        { "group": "", "items": [
          { "qty": "1", "unit": "", "name": "Zwiebel", "note": "halbiert" },
          { "qty": "20", "unit": "g", "name": "Ingwer", "note": "in Scheiben" },
          { "qty": "20", "unit": "g", "name": "Olivenöl" },
          { "qty": "800", "unit": "g", "name": "Hokkaido-Kürbis", "note": "in Stücken" },
          { "qty": "700", "unit": "g", "name": "Wasser" },
          { "qty": "2", "unit": "TL", "name": "Gemüsebrühe-Pulver" },
          { "qty": "100", "unit": "g", "name": "Sahne" }
        ] }
      ],
      "steps": [
        { "text": "Zwiebel und Ingwer in den Mixbehälter geben und zerkleinern. Mit dem Spatel nach unten schieben.",
          "machine": { "program": "manuel", "seconds": 5, "speed": 5, "tempC": 0, "reverse": false, "accessory": "blade", "pulses": 0 },
          "needsReview": true },
        { "text": "Öl zugeben und andünsten.",
          "machine": { "program": "manuel", "seconds": 180, "speed": 1, "tempC": 120, "reverse": false, "accessory": "blade", "pulses": 0 },
          "weigh": { "label": "Olivenöl", "grams": 20 }, "needsReview": true },
        { "text": "Kürbis, Wasser und Brühe zugeben und garen.",
          "machine": { "program": "manuel", "seconds": 1200, "speed": 1, "tempC": 100, "reverse": false, "accessory": "blade", "pulses": 0 },
          "weigh": { "label": "Wasser", "grams": 700 }, "needsReview": true },
        { "text": "Sahne zugeben. Messbecher einsetzen und fein pürieren (Toque fragt wegen des heißen Inhalts nach).",
          "machine": { "program": "manuel", "seconds": 60, "speed": 8, "tempC": 0, "reverse": false, "accessory": "blade", "pulses": 0 },
          "weigh": { "label": "Sahne", "grams": 100 }, "needsReview": true },
        { "text": "Abschmecken und servieren." }
      ]
    }
  ]
}
```
