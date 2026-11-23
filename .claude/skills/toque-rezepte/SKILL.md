---
name: toque-rezepte
description: Rezepte für Toque (die freie App für den Lidl Monsieur Cuisine Connect/Smart) übernehmen – vor allem Thermomix-Rezepte (TM31/TM5/TM6/TM7, Cookidoo, Rezeptwelt, Thermomix-Blogs, Kochbücher), aber auch normale Rezepte (Chefkoch, Blogs, Fotos, Screenshots, PDFs, abgetippter Text) und Rezepte anderer Küchenmaschinen. Erzeugt eine geprüfte Datei im Format toque-recettes-1 mit Maschinenschritten (Zeit, Temperatur, Stufe, Linkslauf, Rühraufsatz, Kneten, Dampfgaren, Anbraten, Turbo), die die Sicherheitsregeln von Toque einhält, und schickt sie auf Wunsch direkt an den Rezept-Server des Roboters. Immer verwenden, wenn jemand ein Rezept „für Toque“, „für den Monsieur Cuisine“, „für meinen MC“ oder „für den Lidl-Thermomix“ umwandeln, übernehmen, importieren, anpassen oder anlegen will, oder ein Thermomix-Rezept auf einer anderen Maschine kochen möchte – auch wenn Toque nicht ausdrücklich genannt wird. Nicht für das Eintragen auf monsieur-cuisine.com (dafür gibt es einen eigenen Skill).
---

# Rezepte für Toque übernehmen

Toque ist eine freie Ersatz-App für den Lidl **Monsieur Cuisine Connect** (SKMC 1200/1100,
Android 6) und den **Monsieur Cuisine Smart** (SKMS 1200). Eigene Rezepte („Mes recettes“, ID
`perso-…`) werden als JSON im Format `toque-recettes-1` importiert. Jeder Schritt kann einen
Maschinenschritt tragen, den der Roboter per Knopfdruck ausführt – deshalb muss jeder Wert die
Sicherheitsregeln von Toque einhalten, sonst lehnt der Roboter das ganze Rezept ab.

Ziel dieses Skills: aus beliebigen Rezeptquellen **ohne Rückfragen** eine fertige, geprüfte
Importdatei machen. Rückfragen kosten den Nutzer mehr als eine gut begründete Annahme, die er
in der Zusammenfassung sieht und am Roboter vor dem ersten Start ohnehin bestätigt.

## Ablauf

1. **Quelle lesen.** Text, Foto/Screenshot (selbst lesen), PDF, URL (abrufen). Mehrere Rezepte
   auf einmal sind normal: alle in dieselbe Datei.
2. **Robotermodell festlegen** (Standard: Connect, siehe *Annahmen*).
3. **Umrechnen** nach den Regeln unten. Für Thermomix-Notation zuerst
   `references/thermomix.md` lesen – dort stehen alle Schreibweisen (DE/EN/FR/IT/ES), die
   Umrechnungstabelle und die Sonderfälle. Für normale Rezepte ohne Maschine
   `references/klassische-rezepte.md`. Für das JSON-Format `references/format.md`.
4. **Datei schreiben** als `toque-<kurzname>.json` (bei mehreren Rezepten
   `toque-rezepte-<datum>.json`), immer im Dateiformat mit `"format": "toque-recettes-1"`.
5. **Prüfen** mit dem Skript – es bildet die Regeln von Toque nach und repariert nichts
   stillschweigend:
   ```bash
   python3 <skill-dir>/scripts/toque_check.py toque-xyz.json --robot connect
   ```
   Fehler beheben und erneut prüfen, bis `OK` kommt. Warnungen lesen und, wo sinnvoll, im
   Schritttext oder in `notes` berücksichtigen.
6. **Ausliefern** (siehe *Ausliefern*): Datei an den Nutzer geben; wenn Adresse und Schlüssel
   des Rezept-Servers bekannt sind und der Roboter erreichbar ist, direkt importieren.
7. **Kurze Zusammenfassung** (Format unten).

## Annahmen statt Rückfragen

Triff diese Entscheidungen selbst und nenne sie in der Zusammenfassung:

| Offene Frage | Entscheidung |
|---|---|
| Welches Gerät? | **Connect**, außer der Nutzer nennt Smart/SKMS/MC Smart oder es ist aus dem Gespräch bekannt. Bei Smart `--robot smart`. |
| Sprache | Sprache der Unterhaltung (meist Deutsch). Fremdsprachige Quelle übersetzen, Mengen und Einheiten metrisch. |
| Portionen | Wie in der Quelle; fehlt die Angabe, aus Mengen schätzen (Standard 4). Nicht umrechnen, außer der Nutzer will es. |
| Zeiten (prepMinutes/totalMinutes) | Aus der Quelle; sonst aus den Schritten schätzen (Summe der Maschinenzeiten + übliche Handarbeit, aufrunden). |
| Schwierigkeit | 1 einfach, 2 mittel, 3 aufwendig – nach Anzahl Schritte/Techniken schätzen. |
| Kategorie, Tags | Eine passende Kategorie (z. B. „Suppen“, „Hauptgerichte“, „Desserts“, „Brot & Brötchen“, „Saucen & Dips“, „Getränke“, „Grundrezepte“) und 2–5 Tags; immer Tag `thermomix-umgerechnet` bei Thermomix-Quellen. |
| Wert passt nicht zu Toques Regeln | Den nächsten **sicheren** Wert nehmen (meist kleiner/kürzer/kühler), oder den Schritt aufteilen – nie die Regel umgehen. In `notes` festhalten. |
| Mehrdeutige Notation („Stufe 2–3“, „Stufe 5–10 ansteigend“) | Regeln in `references/thermomix.md`; im Zweifel die niedrigere Stufe, Ansteigen als mehrere kurze Schritte. |
| Nicht übertragbare Funktion (z. B. TM-Waage-Tara, „Gerät aufheizen lassen“, Cookidoo-Guided-Funktion) | Als normalen Text-Schritt ohne Maschine, oder mit dem nächsten passenden Programm, und Hinweis in `notes`. |
| Quelle unvollständig/unleserlich | Fehlendes plausibel ergänzen und mit „(ergänzt)“ im Schritttext oder in `notes` markieren. |
| Bild/Foto des Gerichts | Nicht übernehmen (`image` leer lassen). |

**Echte Rückfragen nur**, wenn ohne sie gar nichts Sinnvolles entsteht: das Rezept selbst fehlt
(z. B. nur ein Cookidoo-Link, der ohne Login nicht lesbar ist → um den kopierten Text oder
Screenshots bitten), oder die Quelle ist so unleserlich, dass kein Rezept erkennbar ist. Alles
andere entscheidest du.

## Kernregeln der Umrechnung

Die Grenzwerte stammen aus `core/.../machine/Safety.kt` und `RecipeCodec` in Toque. Warum sie
wichtig sind: Toque prüft beim Import und vor jedem Start genau diese Werte; ein einziger
unzulässiger Schritt lässt den Import des Rezepts scheitern (HTTP 422 bzw. „illisible“).

**Programme** (`machine.program`):

| Schlüssel | Bedeutung | Connect | Smart |
|---|---|---|---|
| `manuel` | Manuelles Garen: Zeit, Temperatur, Stufe, Linkslauf, Zubehör | ✓ | ✓ |
| `petrir` | Kneten (Teigprogramm, max. 120 s je Schritt, keine Hitze) | ✓ | ✓ |
| `vapeur` | Dampfgaren 120 °C, Messer steht still | ✓ | ✓ |
| `rissoler` | Anbraten 130 °C, Intervall Stufe 1 Linkslauf, max. 14 min | ✓ | ✓ |
| `turbo` | Turbo-Impulse (`pulses`), nie heiß, nie mit Rühraufsatz | ✓ | ✓ |
| `mixer`, `smoothie` | Smart-Programme, 30–120 s | – | ✓ |
| `sous-vide` | 40–85 °C, 15 min–12 h | – | ✓ |
| `mijoter` | Schongaren 60–100 °C, 15 min–8 h | – | ✓ |
| `fermenter` | Fermentieren 30–50 °C, 30 min–12 h | – | ✓ |

**Manuelles Garen** (`manuel`) – alle Bedingungen gleichzeitig:

- `seconds` ≥ 1 und ≤ 5940 (99 min); bei `speed` > 6 höchstens 900 (15 min).
- `speed` 0–10. 0 nur **mit** Temperatur (Erhitzen ohne Rühren, max. 120 °C, nur Connect).
- `tempC` 0 (keine Hitze) oder exakt ein Wert der Tabelle:
  Connect `37, 45, 50, 55, …, 125, 130` (5er-Schritte, **kein 40**);
  Smart zusätzlich `40`.
- **Mit Hitze höchstens Stufe 3.**
- **Linkslauf (`reverse: true`) höchstens Stufe 3.**
- **Rühraufsatz (`accessory: "whisk"`) höchstens Stufe 4**, nie Turbo.
- Heißes Mixen ab Stufe 4 ist erlaubt, Toque fragt dann am Roboter nach – deshalb Pürieren
  heißer Suppen als eigenen Schritt **ohne** Temperatur anlegen.

Zu lange Vorgänge **aufteilen** (z. B. 2 h Schmoren → zwei Schritte à 60 min; 5 min Kneten →
drei `petrir`-Schritte à 120/120/60 s) statt kürzen, und im Text „weiter …“ schreiben.

**`needsReview: true`** auf jeden Maschinenschritt setzen, der aus einer anderen Maschine oder
einem Rezept ohne Maschinenangaben umgerechnet wurde. Toque lässt den Nutzer diese Einstellung
dann vor dem ersten Start bestätigen – das ist die gewollte Sicherheitsstufe für Umrechnungen.
Nur bei Werten, die 1:1 aus einem Monsieur-Cuisine-Rezept stammen, weglassen.

**Wiegen:** Wird in einem Schritt eine Zutat nach Gewicht in den Topf gegeben, `weigh`
setzen (`{"label": "Wasser", "grams": 500}`) – der Roboter wiegt dann mit. Pro Schritt nur eine
Wiegung; bei mehreren Zutaten die größte, oder den Schritt teilen. Flüssigkeiten in ml ≈ g.

**Schritttexte:** in eigenen Worten, kurz, Monsieur-Cuisine-Begriffe statt Thermomix-Begriffe
(Tabelle in `references/thermomix.md`), die Maschineneinstellung nicht nochmal ausschreiben (die
zeigt Toque selbst), aber Handgriffe nennen: „Messbecher einsetzen“, „Spatel zu Hilfe nehmen“,
„Garkorb als Spritzschutz auf den Deckel“. Eine Handlung pro Schritt; ein Schritt ohne Maschine
ist erlaubt (Vorbereiten, Abfüllen, Backofen).

## Ausliefern

1. **Datei** im Arbeitsordner (bzw. dem Ordner, den die Umgebung für Nutzerdateien vorsieht)
   speichern und dem Nutzer geben.
2. **Direkt an den Roboter**, wenn Adresse und Schlüssel bekannt sind – aus einem Foto des
   QR-Codes (Toque › Einstellungen › Rezept-Server › Code QR; die Adresse hat die Form
   `http://192.168.x.y:8080/#k=<schlüssel>`) oder aus dem Gespräch:
   ```bash
   python3 <skill-dir>/scripts/toque_push.py toque-xyz.json --url "http://192.168.1.20:8080/#k=abc…"
   ```
   Der Server läuft nur im Heimnetz und nie während der Roboter kocht. Aus einer Cloud-Umgebung
   ist er nicht erreichbar – dann nicht weiter versuchen, sondern die anderen Wege nennen. Ist ein
   Rechner im Heimnetz an die Sitzung angebunden, das Skript dort ausführen.
3. **Wege für den Nutzer**, kurz nennen:
   - Browser im selben WLAN: Toque-Seite › Einstellungen › *Datei importieren*;
   - oder Datei als `toque-mes-recettes.json` (bzw. beliebiger Name, Endung `.json`) in den
     Ordner `Download` des Roboters (`adb push … /sdcard/Download/`) und am Roboter
     Einstellungen › Meine Rezepte › *Importieren*.

Gleicher Titel = gleiche ID = beim erneuten Import wird das Rezept **aktualisiert**, nicht
doppelt angelegt. Das macht Korrekturen einfach.

## Zusammenfassung an den Nutzer

Kurz und auf Deutsch (bzw. in seiner Sprache):

```
**<Titel>** – <n> Schritte, davon <m> Maschinenschritte · <Portionen> Personen · Gerät: Connect
Angepasst:
- Schritt 3: 98 °C → 95 °C (Connect hat kein 98 °C)
- Schritt 5: Varoma/Stufe 1 → Dampfgaren 30 min (Messer steht still)
- Schritt 7: 1 Min./100 °C/Stufe 5 → in zwei Schritte geteilt (Hitze max. Stufe 3)
Datei: toque-kuerbissuppe.json · geprüft ✓ · [an den Roboter gesendet ✓ / Import: …]
Alle umgerechneten Einstellungen fragt Toque vor dem ersten Start einmal zur Bestätigung ab.
```

Nur echte Änderungen auflisten, nicht jede 1:1-Übernahme.

## Rechtliches, kurz

Umgerechnete Cookidoo- oder Kochbuch-Rezepte sind für den eigenen Roboter gedacht. Wenn der
Nutzer sie veröffentlichen will (z. B. auf toqueapp.org), darauf hinweisen, dass Texte und Bilder
urheberrechtlich geschützt sein können und Cookidoo-Inhalte nicht weitergegeben werden dürfen.
Die Quelle in `notes` nennen („Nach: <Quelle>“), keine Bilder übernehmen.
