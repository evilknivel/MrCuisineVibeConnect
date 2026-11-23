#!/usr/bin/env python3
"""
Rechnet Küchen-Einheiten in Gramm/Milliliter und °C um.

  python3 units.py "1 1/2 cups flour"        -> 188 g Mehl
  python3 units.py "2 tbsp butter" "350 F" "Gas Mark 5" "1 stick butter" "8 oz cheese"
  python3 units.py --list                    # bekannte Zutaten und Dichten

Unbekannte feste Zutat bei Volumenangabe: nur ml, mit Hinweis zum Recherchieren.
"""
import re
import sys
from fractions import Fraction

# g pro US-Cup (240 ml); Schlüssel: englische und deutsche Namen (Teilwort genügt)
CUP_G = {
    ("flour", "mehl"): 125, ("bread flour", "brotmehl"): 130, ("whole wheat", "vollkornmehl"): 120,
    ("sugar", "zucker"): 200, ("brown sugar", "brauner zucker"): 210, ("powdered sugar", "icing sugar", "puderzucker"): 120,
    ("butter",): 227, ("oil", "öl", "oel"): 218, ("milk", "milch"): 245, ("water", "wasser"): 240,
    ("cream", "sahne"): 240, ("sour cream", "schmand", "saure sahne"): 240, ("yogurt", "joghurt"): 245,
    ("honey", "honig"): 340, ("maple syrup", "ahornsirup"): 320, ("rice", "reis"): 185,
    ("oats", "haferflocken"): 90, ("cocoa", "kakao"): 85, ("almond flour", "gemahlene mandeln", "mandelmehl"): 96,
    ("nuts", "nüsse", "walnuts", "walnüsse"): 120, ("cheese", "käse", "kaese"): 100, ("parmesan",): 90,
    ("lentils", "linsen"): 190, ("chickpeas", "kichererbsen"): 165, ("cornstarch", "stärke", "speisestärke"): 128,
    ("breadcrumbs", "paniermehl", "semmelbrösel"): 110, ("salt", "salz"): 290, ("chocolate chips", "schokotropfen"): 170,
    ("raisins", "rosinen"): 150, ("coconut", "kokosraspeln"): 85, ("tomato", "tomaten"): 240, ("broth", "stock", "brühe"): 240,
    ("peanut butter", "erdnussbutter"): 258, ("quinoa",): 170, ("couscous",): 175, ("polenta", "cornmeal", "maisgrieß"): 160,
}
VOL_ML = {
    "cup": 240, "cups": 240, "c": 240, "tasse": 150, "tassen": 150,
    "tablespoon": 15, "tablespoons": 15, "tbsp": 15, "tbs": 15, "el": 15, "essl": 15, "esslöffel": 15,
    "teaspoon": 5, "teaspoons": 5, "tsp": 5, "tl": 5, "teel": 5, "teelöffel": 5,
    "fl oz": 29.57, "floz": 29.57, "pint": 473, "pints": 473, "quart": 946, "quarts": 946,
    "ml": 1, "cl": 10, "dl": 100, "l": 1000, "liter": 1000,
}
MASS_G = {"oz": 28.35, "ounce": 28.35, "ounces": 28.35, "lb": 453.6, "lbs": 453.6, "pound": 453.6, "pounds": 453.6,
          "pfund": 500, "pfd": 500, "g": 1, "kg": 1000, "stick": 113, "sticks": 113}
GAS = {1: 140, 2: 150, 3: 170, 4: 180, 5: 190, 6: 200, 7: 220, 8: 230, 9: 240}
GERMAN = {"flour": "Mehl", "sugar": "Zucker", "butter": "Butter", "milk": "Milch", "water": "Wasser", "cheese": "Käse"}


def number(s):
    s = s.replace(",", ".").replace("½", " 1/2").replace("¼", " 1/4").replace("¾", " 3/4").replace("⅓", " 1/3").replace("⅔", " 2/3").strip()
    total = Fraction(0)
    for part in s.split():
        total += Fraction(part)
    return float(total)


def density(name):
    n = name.lower()
    best = None
    for keys, g in CUP_G.items():
        for k in keys:
            if k in n and (best is None or len(k) > best[0]):
                best = (len(k), g, keys[0])
    return (best[1] / 240.0, best[2]) if best else (None, None)


def convert(text):
    t = text.strip()
    m = re.match(r"^(?:gas\s*mark|gas|stufe)\s*(\d)$", t, re.I)
    if m:
        return f"{t} = {GAS[int(m.group(1))]} °C (Umluft ca. 20 °C weniger)"
    m = re.match(r"^(-?\d+(?:[.,]\d+)?)\s*°?\s*F$", t, re.I)
    if m:
        c = (float(m.group(1).replace(",", ".")) - 32) * 5 / 9
        return f"{t} = {round(c / 5) * 5:.0f} °C (genau {c:.1f} °C)"
    m = re.match(r"^((?:\d+\s+)?\d+(?:[.,/]\d+)?|[½¼¾⅓⅔]|\d+\s*[½¼¾⅓⅔])\s*(fl\s?oz|[a-zA-ZäöüÄÖÜß.]+)?\s*(.*)$", t)
    if not m:
        return f"{t}: nicht erkannt"
    qty = number(m.group(1))
    unit = (m.group(2) or "").lower().rstrip(".").replace(" ", "")
    unit = "fl oz" if unit == "floz" else unit
    rest = m.group(3).strip()
    if unit in MASS_G:
        g = qty * MASS_G[unit]
        return f"{t} = {g:.0f} g {rest}".strip()
    if unit in VOL_ML:
        ml = qty * VOL_ML[unit]
        d, key = density(rest)
        if d is None:
            if not rest or re.search(r"wasser|water|saft|juice|wein|wine|brühe|broth|essig|vinegar|sauce|soße", rest, re.I):
                return f"{t} = {ml:.0f} ml ≈ {ml:.0f} g {rest}".strip()
            return f"{t} = {ml:.0f} ml {rest} (Dichte unbekannt: Gewicht recherchieren/schätzen, nicht wie Wasser rechnen – lockeres Blattgemüse wiegt pro Cup nur 20–40 g)".strip()
        return f"{t} = {ml:.0f} ml ≈ {ml * d:.0f} g {rest}".strip()
    return f"{t}: Einheit '{unit}' unbekannt – Stückangabe? Dann so übernehmen."


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    if sys.argv[1] == "--list":
        for keys, g in sorted(CUP_G.items(), key=lambda x: x[0][0]):
            print(f"{', '.join(keys):45s} {g} g/Cup")
        sys.exit(0)
    for a in sys.argv[1:]:
        print(convert(a))
