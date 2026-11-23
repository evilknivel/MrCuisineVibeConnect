# Recipe server (local network)

Toque can serve a small web page so recipes can be written and edited from a computer or phone on the
same Wi-Fi, and a JSON API for scripts or AI assistants. No cloud: everything stays on the robot.

## On the robot

*Settings › Recipe server*: switch it on, then **QR code**. The code holds
`http://<robot IP>:8080/#k=<key>`. Scanned with a phone, the page opens already connected (the key
is after `#`, so it is never sent over the network in a URL). **New key** invalidates the old one.

Safety:

- The server **never runs while cooking**: it stops as soon as the base turns or heats, a job is
  active (even paused), or guided cooking is open, and starts again afterwards (`ServerGate`).
- It never drives the machine: it only reads and writes recipes.
- Only addresses of the local network are answered (`LocalNet`); every API call needs the key;
  5 wrong keys in a minute lock the API for a minute (`ApiKey`).
- Only *my recipes* (`perso-…`) can be written. Lidl and community recipes are read-only: copy
  them first (`POST /api/recipes/{id}/copy`). Every recipe is checked like on the robot
  (`UserRecipes.problem`: machine limits, whisk speed, temperatures of this robot).

## API

Header `Authorization: Bearer <key>`. JSON bodies use the `toque-recettes-1` recipe format
(`RecipeCodec`, same as the export file).

| Method | Path | |
|---|---|---|
| GET | `/api/info` | robot, language, programs, temperatures, max speed, UI texts |
| GET | `/api/recipes?q=&source=user\|lidl\|community&limit=` | search |
| POST | `/api/recipes` | create one of my recipes (id assigned) |
| GET / PUT / DELETE | `/api/recipes/{id}` | read; edit or delete (my recipes only) |
| POST | `/api/recipes/{id}/copy` | copy any recipe into my recipes |
| GET | `/api/recipes/{id}/shopping?servings=` | shopping list (items and text) |
| POST | `/api/recipes/{id}/bring?servings=` | put the ingredients on the linked Bring! list |
| POST | `/api/import` | a `toque-recettes-1` file; each recipe becomes one of mine |
| GET | `/api/export` | my recipes as a file |
| GET | `/api/bring` · POST `/api/bring/login` · `/api/bring/list` · `/api/bring/logout` | Bring! account |

Writes answer `503` while the robot cooks, `403` for a read-only recipe, `422` with a readable
message for a recipe that is not valid.

Example:

```sh
curl -H "Authorization: Bearer $KEY" -H "Content-Type: application/json" \
  -d '{"title":"Pesto","servings":2,
       "ingredients":[{"group":"","items":[{"qty":"50","unit":"g","name":"Basilikum"}]}],
       "steps":[{"text":"Zerkleinern","machine":{"program":"manuel","seconds":10,"speed":7}}]}' \
  http://192.168.1.20:8080/api/recipes
```

## Bring!

Optional. On the web page, *Settings › Bring! account*: e-mail and password are sent once to
Bring!; the robot keeps only the login tokens and the chosen list. Bring! has no public API for
writing to a list, so Toque talks like the Bring! Android app (as Home Assistant does); it may stop
working if Bring! changes it. Without an account, *Copy shopping list* puts the list on the
clipboard, to paste into Bring!. Water is left out, and duplicate ingredients are merged.
