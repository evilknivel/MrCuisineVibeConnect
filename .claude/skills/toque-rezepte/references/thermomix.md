# Thermomix → Monsieur Cuisine (Toque)

Inhalt: 1. Notation erkennen · 2. Umrechnungstabelle · 3. Temperaturen · 4. Stufen ·
5. Modi und Programme · 6. Sonderfälle · 7. Begriffe im Text · 8. Durchgerechnete Beispiele

Gilt für TM31, TM5, TM6, TM7 (Cookidoo, Rezeptwelt.de, Thermomix-Kochbücher, Blogs). Ein Teil
gilt auch für andere Maschinen mit ähnlicher Notation (Bosch Cookit, Kenwood, Lidl-Rezepte).

## 1. Notation erkennen

Thermomix schreibt die Einstellung in einen Block aus **Zeit / Temperatur / Richtung / Stufe**,
getrennt durch `/`, oft fett, oft am Satzende. Reihenfolge und Teile variieren:

| Schreibweise (Beispiele) | Bedeutung |
|---|---|
| `10 Sek./Stufe 5` · `10 sec/speed 5` · `10 sec/vitesse 5` · `10 seg/vel 5` · `10 sec/vel. 5` | Zeit, Stufe, ohne Hitze |
| `3 Min./100°C/Stufe 1` · `3 min/100°C/speed 1` · `3 min/100°C/vitesse 1` | mit Temperatur |
| `5 Min./Varoma/Stufe 1` | Varoma-Temperatur (≈ 120 °C) |
| `20 Min./100°C/⟲/Stufe 1` · `/Linkslauf/` · `/reverse/` · `/sens inverse/` · `/giro a sinistra/` · `/giro inverso/` · Symbol `⟲` oder `↺` | Linkslauf |
| `/Sanftrührstufe` · `/Stufe ⫶` · `/Löffelsymbol` · `/gentle stir setting` · `/spoon speed` · `/vitesse mijotage` · `/velocità soft` · `/velocidad cuchara` | langsamste Rührstufe (≈ 40 U/min) |
| `/Stufe 2-3` · `/Stufe 5-10` · `Stufe 5-10 schrittweise ansteigend` | Bereich / langsam hochdrehen |
| `Turbo/0,5 Sek./3-mal` · `Turbo/1 sec/2 times` | Turbo-Impulse |
| `/Teigknetstufe` · `Teig kneten` · `Teigmodus` · Ähren-Symbol · `/dough mode` · `/mode pétrin` · `/spiga` | Kneten |
| `Rühraufsatz einsetzen` · `butterfly` · `fouet` · `farfalla` · `mariposa` | Rühraufsatz |
| `Messbecher einsetzen` · `ohne Messbecher` · `Garkörbchen statt Messbecher auf den Deckel` | Handgriffe (Text) |
| `Modus „Anbraten“`, `160°C`, `Röstfunktion` (TM6/TM7) | Anbraten |
| `Modus „Andicken“`, `„Sous-vide“`, `„Fermentieren“`, `„Langsamgaren“`, `„Zuckerkochen“`, `„Eier kochen“`, `„Reis kochen“`, `„Wasser erhitzen“`, `„Teig kneten“`, `„Turbo“`, `„Warmhalten“`, `„Vorreinigen“` | TM6/TM7-Modi |
| `h` / `Std.` / `hrs`, `Min.` / `min`, `Sek.` / `sec` / `s`, Kommas als Dezimaltrenner | Zeiteinheiten |

Mehrere Blöcke in einem Satz = mehrere Toque-Schritte.

## 2. Umrechnungstabelle

| Thermomix | Toque (`program` …) | Regel |
|---|---|---|
| Zeit/Stufe ohne Temperatur | `manuel`, `tempC` 0 | Stufe übernehmen. Bei Stufe > 6 höchstens 15 min je Schritt. |
| Zeit/Temperatur/Stufe ≤ 3 | `manuel` | Temperatur nach Abschnitt 3. |
| Zeit/Temperatur/Stufe 4–10 | aufteilen | Erhitzen mit Stufe ≤ 3 (meist 1–2), danach eigener Schritt ohne Temperatur mit der hohen Stufe. Kurz (≤ 30 s) und nur zum Vermischen gedacht → Stufe 3 mit Hitze reicht, Hinweis in `notes`. |
| Linkslauf mit Stufe ≤ 3 | `manuel`, `reverse: true` | |
| Linkslauf mit Stufe > 3 | `reverse: true`, Stufe 3 | sehr selten; Hinweis. |
| Sanftrührstufe (mit oder ohne Linkslauf) | `speed: 1` | MC hat keine eigene Löffelstufe. Linkslauf übernehmen. |
| Stufe 0 / „ohne Rühren“ mit Temperatur | `speed: 0` (nur Connect, ≤ 120 °C) | Smart: `speed: 1` + Linkslauf. |
| Stufe-Bereich „2-3“ | untere Stufe (2) | „5-10“ (Pürieren) → siehe Abschnitt 4. |
| Rühraufsatz | `accessory: "whisk"`, Stufe ≤ 4 | TM erlaubt selbst nur bis 4. Text: „Rühraufsatz einsetzen“. |
| Turbo/x Sek./n-mal | `turbo`, `pulses: n` | Nie direkt nach Hitze (≥ 60 °C): sonst vorher „etwas abkühlen lassen“ in den Text, Toque sperrt Turbo bei heißem Inhalt. Mit Rühraufsatz: Rühraufsatz vorher entfernen. |
| Teigknetstufe / Teig kneten x Min. | `petrir`, `seconds` ≤ 120 | Länger → mehrere `petrir`-Schritte (je ≤ 120 s), Text „weiterkneten“. Nie mit Rühraufsatz. |
| Varoma-Dämpfen, Mixtopf nur Wasser | `vapeur`, `seconds` = Zeit | Messer steht still – passt, weil nur Wasser im Topf. Text: „Dampfgaraufsatz aufsetzen“. |
| Varoma-Dämpfen, im Mixtopf gart etwas mit (Reis, Kartoffeln im Garkorb, Suppe, Sauce) | `manuel`, `tempC` 120, `speed` 1 (Linkslauf übernehmen) | So wird der Topfinhalt weiter gerührt. ≤ 99 min. |
| „Varoma“ als Temperatur zum Reduzieren/Einkochen | `manuel`, `tempC` 120, Stufe ≤ 3 | |
| Anbraten (TM6/TM7, 130–160 °C) | `rissoler`, `seconds` ≤ 840 | Fest 130 °C, Intervall Stufe 1 Linkslauf. Länger → teilen. Alternativ, wenn ständig gerührt werden muss: `manuel` 130 °C Stufe 1 Linkslauf. |
| Andicken | `manuel`, 90–95 °C, Stufe 2–3 | |
| Zuckerkochen / Karamell (> 130 °C) | `manuel`, 130 °C, Stufe ≤ 2 | Hinweis: höhere Temperatur nicht möglich, Zeit beobachten. |
| Sous-vide | Smart: `sous-vide` (40–85 °C) · Connect: `manuel` mit Tabellenwert, Stufe 1, Linkslauf, ≤ 99 min je Schritt, oder als Text-Schritt | Connect: lange Garzeiten in 99-min-Schritte teilen. |
| Langsamgaren | Smart: `mijoter` (60–100 °C, ≤ 8 h) · Connect: `manuel` 90–100 °C, Stufe 1, Linkslauf, 99-min-Schritte | |
| Fermentieren (Joghurt, Teig gehen lassen) | Smart: `fermenter` (30–50 °C) · Connect: `manuel` 37 °C oder 45 °C, Stufe 1, oder Text-Schritt „an einem warmen Ort gehen lassen“ | Teig gehen lassen besser ohne Maschine (Text-Schritt). |
| Eier kochen | `vapeur` mit Garkorb und Wasser, 12–16 min, oder `manuel` 100 °C Stufe 1 | |
| Reis kochen (Garkörbchen) | `manuel` 100 °C, Stufe 3 (TM nimmt oft Stufe 4; mit Hitze geht in Toque höchstens 3) | Garkorb im Topf. |
| Wasser erhitzen | `manuel` bis Zieltemperatur, Stufe 1 | |
| Warmhalten | `manuel` 65 °C, Stufe 1, Linkslauf, oder weglassen | |
| Vorreinigen / Reinigen | weglassen | Toque hat eine eigene Reinigung. |
| „Gerät/Mixtopf auf … aufheizen“ | `manuel`, Temperatur, Stufe 1, 2–3 min | |
| Waage / „Tara drücken“ / „… einwiegen“ | `weigh` am Schritt | |
| Geführtes Kochen, Timer ohne Rühren | Text-Schritt oder `manuel` Stufe 1 | |

## 3. Temperaturen

Toque akzeptiert **nur Tabellenwerte**:

- Connect: 37, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 130 °C
- Smart: dieselben plus 40 °C

| Thermomix | Connect | Smart |
|---|---|---|
| 37 °C | 37 | 37 |
| 40 °C | **37** (Hefe/Schmelzen: lieber kühler) | 40 |
| 45 … 95 °C | gleich | gleich |
| 98 °C („nicht kochen“, Milch, Pudding) | **95** | 95 |
| 100 °C | 100 | 100 |
| 105–120 °C | gleich | gleich |
| Varoma | 120 (oder `vapeur`, siehe oben) | 120 |
| 125–130 °C | gleich | gleich |
| > 130 °C (TM6/7 bis 160) | 130 oder `rissoler` | 130 oder `rissoler` |

Bei krummen Werten auf den nächsten **niedrigeren** Tabellenwert gehen (sicherer, Garzeit
eventuell +10 %).

## 4. Stufen

- Thermomix und Monsieur Cuisine haben beide Stufe 1–10 mit ähnlicher Abstufung (TM:
  ca. 100 U/min auf 1 bis ca. 10 000 auf 10). Werte 1:1 übernehmen.
- **Zerkleinern** (3–10 s, Stufe 4–8): 1:1. Der MC-Topf ist breiter; bei sehr kleinen Mengen
  (< 50 g, z. B. eine Knoblauchzehe) eine Stufe höher oder 2 s länger, Hinweis im Text: „mit dem
  Spatel nach unten schieben“.
- **Pürieren** „Stufe 5-10 schrittweise ansteigend“: als ein Schritt mit der Endstufe (z. B. 8
  oder 10) und derselben Zeit; Text: „Messbecher einsetzen, langsam auf Stufe … hochdrehen“.
  Toque startet die Stufe ohnehin weich.
- **Heißes Pürieren**: Thermomix püriert oft direkt nach dem Kochen. Toque erlaubt Stufe ≥ 4
  bei heißem Inhalt nur mit Bestätigung am Roboter – den Schritt **ohne Temperatur** anlegen,
  das reicht.
- **Rühren beim Garen**: Stufe 1 (mit Linkslauf, wenn TM Linkslauf hat oder Stücke ganz bleiben
  sollen: Gulasch, Risotto, Gemüse in Stücken).
- **Emulgieren** (Mayonnaise, Hollandaise): Stufe 4 mit Messer, Öl langsam auf den Deckel
  laufen lassen; Hollandaise mit Hitze → Stufe 3.
- **Schlagen** (Sahne, Eischnee): Rühraufsatz, Stufe 3–4, ohne Messbecher; Zeit nach Sicht.

## 5. Modi und Programme

Siehe Tabelle in Abschnitt 2. Zusätzlich:

- TM-Modus „Teig kneten“ ist gleichzeitig Links-/Rechtslauf im Intervall – genau das macht
  `petrir`.
- TM „Turbo“ ohne Zeit (nur „3-mal“) → 0,5–1 s je Impuls; Toque steuert Impulse am Knopf, nur
  `pulses` angeben.
- TM7-Funktionen „Hochgeschwindigkeits-Garen“, „Slow Cook“ gibt es auf dem Connect nicht:
  Abschnitt 2 (Langsamgaren) nutzen.

## 6. Sonderfälle

- **Menge über dem Fassungsvermögen**: TM31 2 l, TM5/TM6/TM7 2,2 l, MC Connect/Smart 3 l (bis
  ca. 2,5 kg Flüssigkeit bei Stufe 10) – meist kein Problem. Bei Rezepten für TM-„Mixtopf voll“
  nichts ändern.
- **Garzeiten**: Der MC heizt etwas anders auf. Bei Fleisch, Reis, Kartoffeln die Zeit
  übernehmen und im Text „Garprobe machen“ ergänzen.
- **Varoma-Etagen** (Einlegeboden + Behälter): MC-Dampfgaraufsatz hat ebenfalls zwei Ebenen.
  Wasser im Topf: TM nennt meist 500 g; für > 30 min Dämpfen 750–1000 g (MC verdampft ähnlich),
  in `weigh` setzen.
- **Garkörbchen als Spritzschutz** → „Garkorb statt Messbecher auf den Deckel stellen“.
- **„Ohne Messbecher“** bei Reduktionen → Text so lassen.
- **Thermomix-Rezepte mit Waage-Schritten** („500 g Wasser in den Mixtopf einwiegen“) →
  `weigh` mit genau dieser Zutat.
- **Rezept nennt keine Maschine bei einem Schritt** (Backofen, Kühlschrank, Teig formen) →
  Text-Schritt ohne `machine`.
- **Teig gehen lassen im Mixtopf** (TM: „1 Std. im Mixtopf gehen lassen“) → Text-Schritt, oder
  Connect `manuel` 37 °C Stufe 1 ist nicht sinnvoll (rührt den Teig zusammen) – Text-Schritt.

## 7. Begriffe im Text

| Thermomix | Monsieur Cuisine / Toque |
|---|---|
| Mixtopf | Mixbehälter (oder „Topf“) |
| Mixtopfdeckel | Deckel |
| Messbecher | Messbecher |
| Garkörbchen | Garkorb |
| Varoma, Varoma-Behälter, Varoma-Einlegeboden | Dampfgaraufsatz, Dampfgarbehälter, Einlegeboden |
| Rühraufsatz / Schmetterling | Rühraufsatz |
| Spatel | Spatel |
| Thermomix, TM, „der Thermi“ | der Roboter / Monsieur Cuisine |
| Cookidoo | (weglassen) |
| Waage / Tara | Waage (Toque wiegt mit) |
| Linkslauf | Linkslauf (in Toque: „sens inverse“) |
| Teigknetstufe | Knetprogramm |

## 8. Durchgerechnete Beispiele

**A.** „Zwiebel halbiert in den Mixtopf geben, **5 Sek./Stufe 5** zerkleinern und mit dem Spatel
nach unten schieben. Öl zugeben und **3 Min./120°C/Stufe 1** dünsten.“
→ Schritt 1: `manuel` 5 s, Stufe 5, 0 °C. Schritt 2: `manuel` 180 s, Stufe 1, 120 °C,
`weigh` Öl.

**B.** „Milch und Zucker **7 Min./98°C/Stufe 2** erhitzen.“
→ Connect: `manuel` 420 s, Stufe 2, **95 °C**; Notiz „98 → 95 °C“.

**C.** „**20 Min./100°C/⟲/Sanftrührstufe** garen.“
→ `manuel` 1200 s, Stufe 1, 100 °C, `reverse: true`.

**D.** „Suppe **1 Min./Stufe 5-10** schrittweise ansteigend pürieren.“ (direkt nach dem Kochen)
→ `manuel` 60 s, Stufe 10, 0 °C, Text „Messbecher einsetzen …“. Toque fragt wegen des heißen
Inhalts am Roboter nach.

**E.** „**3 Min./Teigknetstufe** kneten.“ → `petrir` 120 s + `petrir` 60 s („weiterkneten“).

**F.** „500 g Wasser in den Mixtopf, Varoma aufsetzen, **25 Min./Varoma/Stufe 1** garen.“
(nur Wasser im Topf) → `vapeur` 1500 s, `weigh` Wasser 500 g.

**G.** „Reis im Garkörbchen, Lachs im Varoma, **20 Min./Varoma/Stufe 4**.“
→ Hitze max. Stufe 3 und der Reis braucht Bewegung: `manuel` 1200 s, 120 °C, Stufe 3
(Hinweis „TM Stufe 4 → 3“).

**H.** „**4 Min./Stufe 3,5** mit Rühraufsatz Sahne schlagen.“ → `manuel` 240 s, Stufe **3**
(nur ganze Stufen; halbe abrunden), `whisk`.

**I.** „**Turbo/1 Sek./3-mal**“ direkt nach „5 Min./100 °C“ → `turbo`, `pulses: 3`, Text
„Etwas abkühlen lassen (unter 60 °C), dann …“.

**J.** „Gulasch **90 Min./100°C/⟲/Sanftrührstufe**, dann **30 Min./100°C/⟲/Sanftrührstufe** ohne
Messbecher“ → zwei Schritte `manuel` 5400 s und 1800 s, 100 °C, Stufe 1, Linkslauf.

**K.** „**2 Std./80°C/⟲/Stufe 1** (Langsamgaren)“ → Connect: `manuel` 5940 s + `manuel` 1260 s,
80 °C, Stufe 1, Linkslauf; Smart: `mijoter` 7200 s, 80 °C.

Halbe Stufen (TM: 0,5 Schritte, z. B. „Stufe 2,5“) gibt es in Toque nicht: abrunden.
