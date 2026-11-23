#!/usr/bin/env python3
"""
Schickt eine Toque-Rezeptdatei an den Rezept-Server des Roboters (POST /api/import).

Aufruf:
  python3 toque_push.py DATEI.json --url "http://192.168.1.20:8080/#k=SCHLUESSEL"
  python3 toque_push.py DATEI.json --url http://192.168.1.20:8080 --key SCHLUESSEL
Die Adresse mit #k=… steht im QR-Code (Toque › Einstellungen › Rezept-Server › Code QR).
Umgebungsvariablen statt Argumenten: TOQUE_URL, TOQUE_KEY.

Exit 0 = importiert, 1 = vom Roboter abgelehnt (Meldung wird gezeigt), 2 = nicht erreichbar.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request


def parse(url, key):
    url = (url or "").strip()
    m = re.search(r"[#&?]k=([a-z0-9]+)", url)
    if m and not key:
        key = m.group(1)
    base = re.sub(r"[#?].*$", "", url).rstrip("/")
    if base and not base.startswith("http"):
        base = "http://" + base
    if base and not re.search(r":\d+$", base):
        base += ":8080"
    return base, key


def call(method, url, key, body=None, timeout=10):
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Authorization", "Bearer " + key)
    if body is not None:
        req.add_header("Content-Type", "application/json; charset=utf-8")
    # Der Roboter ist im Heimnetz: nie über einen Proxy gehen.
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    with opener.open(req, timeout=timeout) as r:
        return r.status, json.loads(r.read().decode("utf-8") or "null")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("file")
    ap.add_argument("--url", default=os.environ.get("TOQUE_URL", ""))
    ap.add_argument("--key", default=os.environ.get("TOQUE_KEY", ""))
    a = ap.parse_args()
    base, key = parse(a.url, a.key)
    if not base or not key:
        print("FEHLER: Adresse und Schlüssel fehlen (--url \"http://IP:8080/#k=…\")")
        sys.exit(2)
    body = open(a.file, "rb").read()
    try:
        _, info = call("GET", base + "/api/info", key)
        print(f"Roboter: {info.get('robot', {}).get('name', '?')} · Toque {info.get('version', '?')}")
        if info.get("busy"):
            print(f"FEHLER: Der Roboter kocht gerade ({info['busy']}) – nach dem Kochen erneut senden")
            sys.exit(1)
        status, r = call("POST", base + "/api/import", key, body, timeout=30)
    except urllib.error.HTTPError as e:
        try:
            r = json.loads(e.read().decode("utf-8"))
        except Exception:
            r = {"error": str(e)}
        if e.code == 401:
            print("FEHLER: Schlüssel abgelehnt – neuen QR-Code fotografieren")
        elif e.code == 429:
            print("FEHLER: Zu viele falsche Schlüssel – eine Minute warten")
        elif e.code == 503:
            print("FEHLER: Der Roboter kocht gerade – nach dem Kochen erneut senden")
        else:
            print(f"FEHLER {e.code}: {r.get('error', '')}")
            for p in r.get("problems", []) or []:
                print("  -", p)
        sys.exit(1)
    except Exception as e:
        print(f"NICHT ERREICHBAR: {base} ({e}). Server an? Selbes WLAN? Kocht der Roboter (dann ist der Server aus)?")
        sys.exit(2)
    print(f"IMPORTIERT: {len(r.get('imported', []))} Rezepte: {', '.join(r.get('imported', []))}")
    if r.get("problems"):
        print("ABGELEHNT:")
        for p in r["problems"]:
            print("  -", p)
    if r.get("unreadable"):
        print(f"UNLESBAR: {r['unreadable']}")
    sys.exit(0 if not r.get("problems") and not r.get("unreadable") else 1)


if __name__ == "__main__":
    main()
