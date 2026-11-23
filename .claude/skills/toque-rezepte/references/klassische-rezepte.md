# Rezepte ohne Küchenmaschine (Chefkoch, Blogs, Kochbücher) für Toque

Ziel: die Arbeitsschritte, die der Monsieur Cuisine gut kann, als Maschinenschritte anlegen; den
Rest (Backofen, Pfanne, Kühlschrank, Formen) als Text-Schritte lassen. Lieber wenige, sichere
Maschinenschritte als ein künstlich in den Roboter gepresstes Rezept. Alle Maschinenschritte mit
`needsReview: true`.

## Was der Roboter übernimmt

| Handarbeit im Rezept | Toque-Schritt |
|---|---|
| Zwiebel/Knoblauch/Kräuter hacken | `manuel` 3–5 s, Stufe 5 (Kräuter: 3 s Stufe 7, trocken) |
| Gemüse grob würfeln | `manuel` 3–4 s, Stufe 4 (in Stücken einfüllen) |
| Karotten/Käse reiben | `manuel` 5–10 s, Stufe 5–6 |
| Parmesan/Nüsse mahlen | `manuel` 10–15 s, Stufe 8–10 |
| Zucker zu Puderzucker | `manuel` 15–20 s, Stufe 10 |
| In Öl/Butter andünsten | `manuel` 3 min, 120 °C, Stufe 1 |
| Anbraten (Fleisch in Stücken) | `rissoler` 5–8 min (Fleisch ≤ 500 g) |
| Köcheln (Sauce, Suppe) | `manuel` Zeit, 100 °C, Stufe 1 (Stücke: Linkslauf) |
| Schmoren (Gulasch, Ragù) | `manuel` 100 °C, Stufe 1, Linkslauf, ≤ 99 min je Schritt |
| Risotto | Andünsten 3 min/120 °C/Stufe 1; Reis 2 min/120 °C/Stufe 1 Linkslauf; mit Wein + Brühe 18 min/100 °C/Stufe 1 Linkslauf |
| Pürieren (Suppe) | `manuel` 60 s, Stufe 8–10, ohne Hitze |
| Rühren ohne Hitze (Teig verrühren, Dressing) | `manuel` 20–30 s, Stufe 3–4 |
| Rührteig (Kuchen) | Butter+Zucker 2 min Stufe 4 (Rühraufsatz) → Eier einzeln 20 s Stufe 4 → Mehl 20 s Stufe 3 |
| Hefeteig kneten | Zutaten 10 s Stufe 4 vermischen → `petrir` 120 s (+ weitere 60–120 s) |
| Mürbeteig | `manuel` 15–20 s, Stufe 5 → kurz `petrir` 30 s |
| Sahne/Eischnee | `manuel`, `whisk`, Stufe 3–4, 2–4 min (Eischnee: sauberer, trockener Topf) |
| Pudding, Vanillesauce | `manuel` 7–8 min, 95 °C, Stufe 3 |
| Béchamel | Butter 3 min/100 °C/Stufe 1 → Mehl 2 min/100 °C/Stufe 2 → Milch 7 min/95 °C/Stufe 3 |
| Marmelade | `manuel` 10–12 min, 100 °C, Stufe 1, ohne Messbecher (Garkorb als Spritzschutz) |
| Dämpfen (Gemüse, Fisch, Klöße) | `vapeur`, Wasser 500–1000 g in `weigh` |
| Eier kochen | `vapeur` 12–16 min (Eier im Dampfgaraufsatz) |
| Schokolade/Butter schmelzen | `manuel` 3 min, 50 °C, Stufe 2 |
| Hefe in Milch auflösen | `manuel` 2 min, 37 °C, Stufe 2 |

## Was Text bleibt

Backofen (vorheizen, backen), Pfanne/Grill, Nudeln in viel Wasser kochen (Topf zu klein für
500 g Nudeln + 4 l Wasser; Ausnahme: kleine Mengen im Garkorb), Frittieren, Ruhen/Gehen lassen,
Formen, Füllen, Anrichten, Kühlen.

## Mengen

Der Mixbehälter fasst 3 l (Flüssigkeit bei Stufe 10 bis ca. 2,5 kg). Rezepte für 8+ Personen mit
viel Flüssigkeit: in `notes` „ggf. in zwei Durchgängen“. Nicht von selbst halbieren.
