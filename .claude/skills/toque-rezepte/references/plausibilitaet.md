# Plausibilität: Mengen, Verhältnisse, Garzeiten, Lebensmittelsicherheit

Vor dem Schreiben der Datei jedes Rezept gegen diese Werte halten. Weicht etwas stark ab
(Faktor 2 oder mehr), ist es fast immer ein Lese-, Übersetzungs- oder KI-Fehler: korrigieren und
in `notes` vermerken. Leichte Abweichungen sind Absicht des Rezepts – stehen lassen.

## Grundverhältnisse

| Was | Üblich |
|---|---|
| Brot/Brötchen (Hefe) | Wasser 60–75 % vom Mehl, Salz 1,8–2,2 %, Frischhefe 1–4 % (Trockenhefe ⅓ davon), Zucker 0–5 % |
| Hefezopf/Brioche | Milch 45–55 %, Butter 10–50 %, Zucker 8–15 %, Hefe 3–5 %, Salz 1–1,5 % |
| Pizzateig | Wasser 60–70 %, Salz 2–3 %, Öl 0–5 %, Hefe 0,2–2 % |
| Nudelteig | 100 g Mehl je Ei (Größe M) |
| Rührkuchen | Mehl : Zucker : Butter : Ei ≈ 1 : 1 : 1 : 1 (Pfundkuchen), Backpulver 3–4 % vom Mehl |
| Mürbeteig | Mehl : Fett : Zucker ≈ 3 : 2 : 1 |
| Biskuit | je Ei 25–30 g Zucker, 25 g Mehl/Stärke |
| Crêpes/Pfannkuchen | 250 g Mehl, 500 ml Milch, 3–4 Eier |
| Béchamel | 50 g Butter + 50 g Mehl je 1 l Milch (mittel) |
| Pudding | 40 g Stärke je 500 ml Milch |
| Risotto | Reis : Flüssigkeit ≈ 1 : 3 bis 1 : 3,5 (Gewicht) |
| Reis (Garkorb/quellen) | 1 : 1,5 bis 1 : 2 |
| Suppe | 250–350 ml je Portion |
| Hauptgericht Fleisch | 150–250 g roh je Person |
| Beilagen | Nudeln/Reis 75–100 g roh, Kartoffeln 200–250 g je Person |
| Salz allgemein | ≈ 1 TL (5–6 g) je kg Speise als Startwert |
| Marmelade | Frucht : Gelierzucker 1 : 1 / 2 : 1 / 3 : 1 je nach Sorte |
| Mayonnaise | 1 Eigelb je 150–250 ml Öl |

## Garzeiten, Kerntemperaturen

| Was | Richtwert |
|---|---|
| Geflügel | durchgaren, Kern ≥ 74 °C; Stücke im MC (2–3 cm) 15–20 min bei 100 °C, Linkslauf |
| Hackfleisch (Sauce) | mindestens 10 min bei 100 °C nach dem Anbraten |
| Rindergulasch | 90–150 min bei 95–100 °C, Linkslauf Stufe 1 |
| Schweinegulasch | 60–90 min |
| Hülsenfrüchte, eingeweicht | Kichererbsen 60–90 min, Linsen rot 15–20, braun 30–40 min; Kidneybohnen roh: mindestens 10 min sprudelnd kochen |
| Kartoffeln in Stücken | 20–25 min (100 °C oder Dampf) |
| Gemüse gedämpft | Brokkoli 10–14, Karotten in Scheiben 18–22, grüne Bohnen 18–25 min |
| Fisch gedämpft | 12–18 min (Filet), Lachs 15–20 min |
| Eier im Dampf | weich 11–12, wachsweich 13–14, hart 15–16 min |
| Eigelbmassen (Hollandaise, Vanillesauce) | höchstens 80–85 °C, sonst gerinnt es |
| Zucker karamellisieren | > 160 °C – geht im MC nicht (max. 130 °C), Pfanne als Text-Schritt |

## Einheiten (Kurzfassung; genauer: `scripts/units.py`)

- 1 EL ≈ 15 ml, 1 TL ≈ 5 ml; Mehl 1 EL ≈ 10 g, Zucker 1 EL ≈ 15 g, Öl 1 EL ≈ 13 g, Butter 1 EL ≈ 15 g.
- US-Cup = 240 ml: Mehl ≈ 125 g, Zucker ≈ 200 g, brauner Zucker ≈ 210 g, Puderzucker ≈ 120 g,
  Butter ≈ 225 g, Reis ≈ 185 g, Milch/Wasser ≈ 240 g, Haferflocken ≈ 90 g.
- 1 Stick Butter = 113 g; 1 oz = 28 g; 1 lb = 454 g; 1 fl oz = 30 ml.
- °F → °C: (F − 32) × 5/9; Gas Mark 4 = 180 °C, 5 = 190, 6 = 200, 7 = 220.
- Alte deutsche Angaben: Pfund = 500 g, Tasse ≈ 150 ml, Msp. ≈ 0,5 g, Päckchen Backpulver 15 g,
  Vanillezucker 8 g, Würfel Hefe 42 g, Trockenhefe 7 g (≈ ½ Würfel).

## Mengen im Mixbehälter

Fassungsvermögen 3 l; Flüssigkeit bei Stufe 10 höchstens ca. 2,5 kg; Teig höchstens ca. 1 kg
Mehl; Zerkleinern am besten 100–500 g auf einmal. Darüber: in `notes` „in zwei Durchgängen“ und
die Schritte entsprechend schreiben, nicht still halbieren.
