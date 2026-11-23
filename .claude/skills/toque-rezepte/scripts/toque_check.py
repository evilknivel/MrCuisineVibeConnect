#!/usr/bin/env python3
"""
Prüft eine Toque-Rezeptdatei (Format toque-recettes-1) gegen die Regeln, die Toque beim Import
und vor jedem Start anwendet (RecipeCodec, UserRecipes.problem, StepMachine.violations,
SafetyPolicy in core/.../machine/Safety.kt). Repariert nichts.

Aufruf:  python3 toque_check.py DATEI.json [--robot connect|smart] [--json]
Exit 0 = OK (Warnungen möglich), 1 = Fehler, 2 = Datei unlesbar.
"""
import argparse
import json
import re
import sys
import unicodedata

CONNECT_TEMPS = [37, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100, 105, 110, 115, 120, 125, 130]
SMART_TEMPS = sorted(CONNECT_TEMPS + [40])

BASE_PROGRAMS = {"manuel", "petrir", "vapeur", "rissoler", "turbo"}
SMART_ONLY = {
    # Schlüssel: (min s, max s, min °C, max °C, Standard °C)
    "mixer": (30, 120, None, None, 0),
    "smoothie": (30, 120, None, None, 0),
    "sous-vide": (900, 12 * 3600, 40, 85, 60),
    "mijoter": (900, 8 * 3600, 60, 100, 90),
    "fermenter": (1800, 12 * 3600, 30, 50, 40),
}

MAX_MS_DEFAULT = 99 * 60
MAX_HIGH_SPEED = 15 * 60
MAX_KNEAD = 120
MAX_TOAST = 840
MAX_SPEED_HEATED = 3
MAX_SPEED_REVERSE = 3
MAX_SPEED_WHISK = 4
MAX_NO_STIR_C = 120
KNOWN_RECIPE_KEYS = {
    "id", "title", "category", "tags", "servings", "prepMinutes", "totalMinutes", "difficulty", "source",
    "image", "favorite", "notes", "author", "rating", "ratingCount", "language", "servingUnit", "devices",
    "ingredients", "steps",
}
KNOWN_MACHINE_KEYS = {"program", "seconds", "speed", "tempC", "reverse", "accessory", "pulses"}


def slug(text):
    t = unicodedata.normalize("NFD", text.lower())
    t = "".join(c for c in t if unicodedata.category(c) != "Mn")
    t = re.sub(r"[^a-z0-9]+", "-", t).strip("-")
    return t[:48]


def is_int(v):
    return isinstance(v, int) and not isinstance(v, bool)


def check_machine(m, where, robot, err, warn, prev_hot):
    """Gibt zurück, ob der Inhalt nach dem Schritt heiß sein kann (für Turbo-Hinweis)."""
    if not isinstance(m, dict):
        err(f"{where}: 'machine' muss ein Objekt sein")
        return prev_hot
    for k in m:
        if k not in KNOWN_MACHINE_KEYS:
            warn(f"{where}: unbekanntes Feld machine.{k} (wird ignoriert)")
    prog = str(m.get("program", "")).strip().lower()
    allowed = BASE_PROGRAMS | (set(SMART_ONLY) if robot == "smart" else set())
    if prog not in BASE_PROGRAMS | set(SMART_ONLY):
        err(f"{where}: Programm '{prog}' unbekannt – Toque würde den Maschinenschritt still verwerfen. Erlaubt: {', '.join(sorted(allowed))}")
        return prev_hot
    if prog not in allowed:
        err(f"{where}: Programm '{prog}' gibt es nur auf dem Smart")
        return prev_hot
    for k in ("seconds", "speed", "tempC", "pulses"):
        if k in m and not is_int(m[k]):
            err(f"{where}: machine.{k} muss eine ganze Zahl sein (ist {m[k]!r})")
            return prev_hot
    for k in ("reverse",):
        if k in m and not isinstance(m[k], bool):
            err(f"{where}: machine.{k} muss true/false sein")
    acc = str(m.get("accessory", "blade")).lower()
    if acc not in ("blade", "whisk"):
        err(f"{where}: accessory '{acc}' unbekannt (blade oder whisk)")
        acc = "blade"
    s = m.get("seconds", 0) or 0
    speed = m.get("speed", 0) or 0
    temp = m.get("tempC", 0) or 0
    rev = bool(m.get("reverse", False))
    pulses = m.get("pulses", 0) or 0
    if s < 0 or pulses < 0:
        err(f"{where}: negative Werte")

    if prog == "turbo":
        if acc == "whisk":
            err(f"{where}: kein Turbo mit Rühraufsatz")
        if pulses == 0:
            warn(f"{where}: Turbo ohne 'pulses' – Anzahl Impulse angeben")
        if prev_hot:
            warn(f"{where}: Turbo direkt nach einem heißen Schritt – Toque sperrt Turbo ab 60 °C; im Text abkühlen lassen")
        return prev_hot

    if prog == "manuel":
        if speed < 0 or speed > 10:
            err(f"{where}: Stufe {speed} außerhalb 0–10")
        temps = SMART_TEMPS if robot == "smart" else CONNECT_TEMPS
        if temp != 0 and temp not in temps:
            lower = [t for t in temps if t <= temp]
            hint = f" – nächster niedrigerer Wert: {lower[-1]}" if lower else ""
            err(f"{where}: {temp} °C gibt es auf dem {robot.capitalize()} nicht{hint}")
        if s < 1:
            err(f"{where}: Dauer fehlt (seconds ≥ 1)")
        if speed == 0 and temp == 0:
            err(f"{where}: Stufe 0 ohne Temperatur – nichts zu tun (Stufe ≥ 1)")
        if speed == 0 and temp > 0:
            if robot == "smart":
                err(f"{where}: Erhitzen ohne Rühren (Stufe 0) bietet Toque auf dem Smart nicht an – Stufe 1 nehmen")
            if temp > MAX_NO_STIR_C:
                err(f"{where}: ohne Rühren höchstens {MAX_NO_STIR_C} °C")
            if rev:
                warn(f"{where}: Linkslauf bei Stufe 0 ist wirkungslos")
        if temp > 0 and speed > MAX_SPEED_HEATED:
            err(f"{where}: mit Hitze höchstens Stufe {MAX_SPEED_HEATED} (ist {speed}) – Schritt teilen: erst erhitzen, dann ohne Temperatur mixen")
        if rev and speed > MAX_SPEED_REVERSE:
            err(f"{where}: Linkslauf höchstens Stufe {MAX_SPEED_REVERSE} (ist {speed})")
        if acc == "whisk" and speed > MAX_SPEED_WHISK:
            err(f"{where}: Rühraufsatz höchstens Stufe {MAX_SPEED_WHISK} (ist {speed})")
        limit = MAX_HIGH_SPEED if speed > 6 else MAX_MS_DEFAULT
        if s > limit:
            err(f"{where}: {s} s zu lang (max. {limit} s bei Stufe {speed}) – in mehrere Schritte teilen")
        if temp == 0 and speed >= 4 and prev_hot:
            warn(f"{where}: Mixen ab Stufe 4 nach heißem Schritt – Toque fragt am Roboter nach (gewollt)")
        return temp >= 60 or (prev_hot and temp == 0 and s < 600)

    if prog == "petrir":
        if s < 1 or s > MAX_KNEAD:
            err(f"{where}: Kneten 1–{MAX_KNEAD} s (ist {s}) – längeres Kneten in mehrere Schritte teilen")
        if acc == "whisk":
            err(f"{where}: Kneten nicht mit Rühraufsatz")
        if temp:
            warn(f"{where}: tempC beim Kneten wird ignoriert (keine Hitze)")
        return False
    if prog == "vapeur":
        if s < 1 or s > MAX_MS_DEFAULT:
            err(f"{where}: Dampfgaren 1–{MAX_MS_DEFAULT} s (ist {s})")
        if speed or temp:
            warn(f"{where}: Stufe/Temperatur beim Dampfgaren werden ignoriert (fest 120 °C, Messer steht)")
        return True
    if prog == "rissoler":
        if s < 1 or s > MAX_TOAST:
            err(f"{where}: Anbraten 1–{MAX_TOAST} s (ist {s}) – teilen")
        if acc == "whisk":
            warn(f"{where}: Anbraten mit Rühraufsatz ist unüblich")
        return True
    # Smart-Programme
    mn, mx, cmin, cmax, cdef = SMART_ONLY[prog]
    if s < mn or s > mx:
        err(f"{where}: {prog} {mn}–{mx} s (ist {s})")
    if temp and cmin is None:
        warn(f"{where}: {prog} hat keine wählbare Temperatur")
    if temp and cmin is not None and not (cmin <= temp <= cmax):
        err(f"{where}: {prog} {cmin}–{cmax} °C (ist {temp})")
    return (temp or cdef) >= 60


def check_recipe(r, idx, robot, err, warn, info):
    name = f"Rezept {idx + 1}"
    if not isinstance(r, dict):
        err(f"{name}: kein Objekt")
        return
    title = str(r.get("title", "") or "").strip()
    if not title:
        err(f"{name}: Titel fehlt – Toque verwirft das Rezept")
        return
    name = f"«{title}»"
    for k in r:
        if k not in KNOWN_RECIPE_KEYS:
            warn(f"{name}: unbekanntes Feld '{k}' (wird ignoriert)")
    rid = str(r.get("id", "") or "").strip() or slug(title)
    final_id = rid if rid.startswith("perso-") else "perso-" + slug(rid)[:40]
    info(f"{name}: ID nach Import {final_id}")
    for k in ("servings", "prepMinutes", "totalMinutes", "difficulty"):
        if k in r and not is_int(r[k]):
            err(f"{name}: {k} muss eine ganze Zahl sein")
    d = r.get("difficulty", 1)
    if is_int(d) and not 1 <= d <= 3:
        warn(f"{name}: difficulty {d} wird auf 1–3 begrenzt")
    if "tags" in r and not (isinstance(r["tags"], list) and all(isinstance(t, str) for t in r["tags"])):
        err(f"{name}: tags muss eine Liste von Texten sein")

    n_ing = 0
    groups = r.get("ingredients", []) or []
    if not isinstance(groups, list):
        err(f"{name}: ingredients muss eine Liste von Gruppen sein")
        groups = []
    for gi, g in enumerate(groups):
        if not isinstance(g, dict) or not isinstance(g.get("items", []), list):
            err(f"{name}: Zutatengruppe {gi + 1} braucht {{\"group\": …, \"items\": [...]}}")
            continue
        items = g.get("items", [])
        if not items:
            warn(f"{name}: Zutatengruppe {gi + 1} ist leer (wird verworfen)")
        for ii, it in enumerate(items):
            if isinstance(it, str):
                n_ing += 1
                warn(f"{name}: Zutat '{it}' als reiner Text – besser qty/unit/name trennen")
                continue
            nm = str(it.get("name", "") or "").strip()
            if not nm:
                err(f"{name}: Zutat {ii + 1} in Gruppe {gi + 1} ohne Namen (wird verworfen)")
                continue
            n_ing += 1
            q = str(it.get("qty", "") or "")
            if re.search(r"\d\s*(g|kg|ml|l|EL|TL)\b", q):
                warn(f"{name}: Zutat '{nm}': Einheit steckt in qty ('{q}') – in 'unit' schreiben")
            for k in ("qty", "unit", "name", "note"):
                if k in it and not isinstance(it[k], str):
                    err(f"{name}: Zutat '{nm}': {k} muss Text sein (auch Zahlen in Anführungszeichen)")

    steps = r.get("steps", []) or []
    if not isinstance(steps, list):
        err(f"{name}: steps muss eine Liste sein")
        steps = []
    n_steps = 0
    n_machine = 0
    hot = False
    for si, s in enumerate(steps):
        where = f"{name} Schritt {si + 1}"
        if not isinstance(s, dict):
            err(f"{where}: kein Objekt")
            continue
        text = str(s.get("text", "") or "").strip()
        m = s.get("machine")
        if not text and not m:
            warn(f"{where}: leer (wird verworfen)")
            continue
        n_steps += 1
        if m:
            n_machine += 1
            hot = check_machine(m, where, robot, err, warn, hot)
            if not s.get("needsReview"):
                warn(f"{where}: needsReview fehlt – bei umgerechneten Einstellungen auf true setzen")
        w = s.get("weigh")
        if w is not None:
            if not isinstance(w, dict) or not is_int(w.get("grams", 0)) or w.get("grams", 0) <= 0:
                err(f"{where}: weigh braucht {{\"label\": …, \"grams\": ganze Zahl > 0}}")
        if re.search(r"\bthermomix\b|\bmixtopf\b|\bvaroma\b|\bgarkörbchen\b|\bcookidoo\b", text, re.I):
            warn(f"{where}: Thermomix-Begriff im Text – MC-Begriff verwenden (Mixbehälter, Dampfgaraufsatz, Garkorb)")
    if n_ing == 0 and n_steps == 0:
        err(f"{name}: mindestens eine Zutat oder ein Schritt nötig")
    info(f"{name}: {n_ing} Zutaten, {n_steps} Schritte, davon {n_machine} Maschinenschritte")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("file")
    ap.add_argument("--robot", choices=["connect", "smart"], default="connect")
    ap.add_argument("--json", action="store_true", help="Ergebnis als JSON")
    a = ap.parse_args()
    try:
        text = open(a.file, encoding="utf-8").read()
        data = json.loads(text)
    except Exception as e:
        print(f"FEHLER: Datei unlesbar: {e}")
        sys.exit(2)
    errors, warnings, infos = [], [], []
    if isinstance(data, dict) and "recipes" in data:
        if data.get("format") != "toque-recettes-1":
            errors.append('Datei: "format": "toque-recettes-1" fehlt – der Roboter ignoriert die Datei sonst')
        recipes = data["recipes"]
    elif isinstance(data, list):
        errors.append('Datei: reine Liste – in {"format": "toque-recettes-1", "recipes": [...]} einpacken')
        recipes = data
    elif isinstance(data, dict) and "title" in data:
        errors.append('Datei: einzelnes Rezept – in {"format": "toque-recettes-1", "recipes": [...]} einpacken (nur das JSON-Feld der Webseite nimmt ein einzelnes Rezept)')
        recipes = [data]
    else:
        print("FEHLER: kein Toque-Rezept erkannt")
        sys.exit(2)
    if not isinstance(recipes, list) or not recipes:
        errors.append("Datei: keine Rezepte")
        recipes = []
    titles = {}
    for i, r in enumerate(recipes):
        check_recipe(r, i, a.robot, errors.append, warnings.append, infos.append)
        t = slug(str(r.get("title", ""))) if isinstance(r, dict) else ""
        if t and t in titles and not (isinstance(r, dict) and r.get("id")):
            warnings.append(f"Rezept {i + 1}: gleicher Titel wie Rezept {titles[t] + 1} – gleiche ID, eines überschreibt das andere")
        titles.setdefault(t, i)
    if a.json:
        print(json.dumps({"ok": not errors, "robot": a.robot, "errors": errors, "warnings": warnings, "info": infos}, ensure_ascii=False, indent=2))
    else:
        for x in infos:
            print("·", x)
        for x in warnings:
            print("WARNUNG:", x)
        for x in errors:
            print("FEHLER:", x)
        print("OK" if not errors else f"{len(errors)} Fehler", f"({len(warnings)} Warnungen, Gerät: {a.robot})")
    sys.exit(0 if not errors else 1)


if __name__ == "__main__":
    main()
