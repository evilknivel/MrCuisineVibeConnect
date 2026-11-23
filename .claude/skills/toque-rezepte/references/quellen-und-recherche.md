# Quellen lesen, Lücken schließen, recherchieren

Inhalt: 1. Quellentypen · 2. Fotos und Scans · 3. KI- und Deep-Research-Rezepte · 4. Eigene
Rezepte erfinden · 5. Wann und wie recherchieren · 6. Markieren, was nicht aus der Quelle stammt

## 1. Quellentypen und was zu tun ist

| Quelle | Vorgehen |
|---|---|
| Thermomix (Cookidoo, Rezeptwelt, TM-Bücher) | `thermomix.md` |
| Rezept anderer Küchenmaschine (Lidl-MC, Bosch Cookit, Kenwood, Magimix) | Wie Thermomix: Zeit/Temperatur/Stufe lesen, Regeln anwenden. Lidl-MC-Werte 1:1 und ohne `needsReview`. |
| Klassisches Rezept (Kochbuch, Zeitschrift, Blog, Chefkoch, Oma-Rezept) | `klassische-rezepte.md` |
| Foto/Scan/Screenshot | Abschnitt 2, dann je nach Inhalt oben |
| KI-Rezept, Deep-Research-Bericht | Abschnitt 3 |
| „Mach mir ein Rezept für …“ (kein Rezept vorhanden) | Abschnitt 4 |
| Nur ein Link | Abrufen. Steht auf der Seite schema.org-`Recipe`-JSON (`<script type="application/ld+json">`), das bevorzugen: `recipeIngredient`, `recipeInstructions`, `recipeYield`, `prepTime`/`totalTime` (ISO 8601, `PT1H30M`). Login-/Paywall-Seiten (Cookidoo, manche Zeitschriften): um Text oder Fotos bitten – das ist die eine erlaubte Rückfrage. |
| Video (YouTube, Instagram) | Beschreibung/Untertitel nutzen, wenn erreichbar; sonst aus Titel + allgemeinem Wissen das übliche Rezept nehmen und deutlich als „nach Video, Mengen recherchiert“ kennzeichnen. |
| Mehrere Quellen für dasselbe Gericht | Eine Hauptquelle wählen (die vollständigste), andere nur zum Ergänzen. |

## 2. Fotos und Scans (Kochbücher, Zeitschriften, Karteikarten, Handschrift)

Bilder selbst lesen (das Modell sieht sie). Sorgfältig, denn hier entstehen die meisten Fehler:

1. **Alle Bilder zuerst ansehen**, dann zusammensetzen. Ein Rezept geht oft über zwei Seiten,
   Zutaten links, Zubereitung rechts, Spalten, Kästen („Tipp“, „Variante“, „Dazu passt“),
   Fortsetzung „→ S. 84“. Reihenfolge der Zubereitung an der Nummerierung festmachen, nicht an
   der Position auf dem Foto.
2. **Mehrere Rezepte auf einer Seite**: alle übernehmen, die vollständig sichtbar sind; ein
   angeschnittenes Rezept nur, wenn es sich sicher ergänzen lässt, sonst in der Zusammenfassung
   nennen („‚Apfelchutney‘ unten rechts ist abgeschnitten – nicht übernommen“).
3. **Zahlen doppelt prüfen**, typische Verwechsler: 1/7, 3/8, 5/6, 0/8, ½/1/2, „1 1/2“ (= 1,5),
   ¼ vs. 1/4, l vs. 1, g vs. 9, EL vs. TL (Großbuchstabe = Esslöffel), °C vs. °F, Min. vs. Std.
   Unplausible Werte mit `plausibilitaet.md` gegenprüfen (z. B. „70 g Salz“ im Brot ist sicher
   „7 g“ oder „10 g“).
4. **Schräg, gewellt, unscharf, Spiegelung**: Text trotzdem lesen; was nicht sicher erkennbar
   ist, aus dem Zusammenhang und Abschnitt 5 ergänzen und markieren.
5. **Handschrift**: Abkürzungen auflösen (Msp. = Messerspitze, Pfd. = 500 g, Tasse ≈ 150 ml in
   alten deutschen Rezepten, „1 Pck. Backpulver“ = 15 g, „1 Würfel Hefe“ = 42 g).
6. **Bilder von Gerichten** (Foodfotos) liefern Hinweise (z. B. Konsistenz, Garnitur), aber
   keine Mengen. Kein Bild ins Rezept übernehmen.
7. **Seitenangabe/Buchtitel** in `notes` als Quelle („Nach: <Buch>, S. 83“), wenn erkennbar.

## 3. KI-Rezepte und Deep-Research-Berichte

Solche Texte sind oft lang, enthalten mehrere Varianten, Hintergrund, Quellenlisten und
Prozentangaben. Daraus **ein** kochbares Rezept machen (oder mehrere, wenn der Nutzer das will
oder der Bericht klar getrennte Rezepte enthält):

1. **Variante wählen**: die als Empfehlung/„Grundrezept“/„beste Variante“ markierte; sonst die
   klassischste; Alternativen knapp in `notes` („Variante: statt Sahne 150 g Crème fraîche“).
2. **Bäckerprozente und Verhältnisse in Gramm umrechnen** (z. B. „Hydration 65 %, Salz 2 %,
   Hefe 1 %“ bei 500 g Mehl → 325 g Wasser, 10 g Salz, 5 g Frischhefe). Portionen aus dem Bericht
   oder Standard 4.
3. **Spannweiten auflösen** („180–200 g“, „20–30 min“): Mittelwert bzw. die untere Garzeit plus
   „Garprobe machen“.
4. **Erklärtext weglassen**, Schritte auf Handlungen reduzieren. Wichtige Begründungen als
   kurzen Hinweis im Schritt („nicht kochen lassen, sonst gerinnt das Eigelb“).
5. **Nicht blind vertrauen**: KI-Rezepte enthalten gelegentlich Fehler (falsche Mengen, fehlende
   Zutat in der Liste, die im Text vorkommt, unsichere Garzeiten bei Geflügel). Immer
   `plausibilitaet.md` anwenden; Zutaten aus dem Text in die Liste übernehmen und umgekehrt.
6. **Quellen** aus dem Bericht nicht in die Datei kopieren; in `notes` nur „Nach: KI-Recherche
   (<Thema>)“.

## 4. Eigene Rezepte erfinden

Wenn der Nutzer nur ein Gericht nennt („ein veganes Linsen-Dal für 4“), ein bewährtes,
klassisches Rezept aus eigenem Wissen schreiben – direkt für den Monsieur Cuisine gedacht (das
Gericht so planen, dass möglichst viel im Mixbehälter passiert). Wünsche (Personen, Diät,
Zutaten, die weg sollen) berücksichtigen. Mengen an `plausibilitaet.md` messen. In `notes`:
„Eigenes Rezept (KI), noch nicht erprobt“; Tag `ki-rezept`.

## 5. Wann und wie recherchieren

Recherchieren, wenn eine Lücke **den Erfolg des Gerichts** betrifft und sie sich nicht sicher
aus dem Rezept selbst schließen lässt. Beispiele:

- Menge fehlt oder ist unleserlich („… g Mehl“), Garzeit fehlt, Temperatur fehlt;
- unbekannte Zutat oder Fachbegriff („Masa Harina“, „Tangzhong“, „à la nage“, „Mehlschwitze“);
- regionale Mengenangabe (Cup, Stick Butter, Gas Mark, „ein Glas“);
- Garzeit eines Stücks, das in der Maschine anders gart (ganze Kartoffeln, Hähnchenkeulen).

**Wie**: Wenn Websuche/Seitenabruf verfügbar ist, kurz suchen (2–3 seriöse Quellen,
übliche Werte nehmen, Mittelwert). Ohne Websuche aus Fachwissen ergänzen. In beiden Fällen
nicht beim Nutzer nachfragen. Nicht recherchieren, was schon eindeutig ist – das kostet nur Zeit.

Einheiten: `scripts/units.py` (Cups, Löffel, Unzen, Pfund, Sticks, °F, Gas Mark, Dichten
gängiger Zutaten).

## 6. Markieren, was nicht aus der Quelle stammt

- Ergänzte oder recherchierte Mengen/Zeiten: in `notes` eine Zeile „Ergänzt: …“ bzw.
  „Recherchiert: …“ (mit kurzer Begründung), damit der Nutzer es nachprüfen kann.
- Unsicheres im Schritttext nur dann kennzeichnen, wenn der Nutzer beim Kochen darauf achten
  muss („Garprobe machen“, „Konsistenz prüfen, ggf. 50 g Wasser mehr“).
- In der Zusammenfassung an den Nutzer unter „Angepasst“ auflisten.
