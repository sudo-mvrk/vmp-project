# Demo: Smart Distribution in five minutes

A walk through the main flow on the seed data (Slice 9, Q33 … Q39): an organization gets ranked volunteers with the
score breakdown, invites one, the volunteer accepts, an activity is completed and shows up in the history, and an
admin bans a user. Notifications appear as log lines of the application (Q7e).

## 1. Start with seed data

The `seed` profile loads 3 organizations, 30 volunteers and 12 activities on start (10 upcoming, one that has
started, one completed). It does nothing if the data is already there. Never enable it in production.

1. `.env` needs the usual variables plus the admin ones (`ADMIN_USERNAME`, `ADMIN_EMAIL`, `ADMIN_PASSWORD_HASH`;
   see `.env.example`) and `SPRING_PROFILES_ACTIVE=seed`.
2. Start the database and Redis: `docker compose up -d postgres redis`.
3. Start the application: `cd backend && set -a; . ../.env; set +a; ./mvnw spring-boot:run`
   (or everything in containers: `(cd backend && ./mvnw package -DskipTests) && docker compose up -d --build`).
4. The log shows `Seed data loaded: 3 organizations, 30 volunteers, 10 upcoming activities`.

Accounts: `seed_org1` … `seed_org3` and `seed_vol01` … `seed_vol30`, all with password `password1`; the admin from
`.env`. Swagger UI at http://localhost:8080/swagger-ui.html is an alternative to the commands below.

## 2. Shell helpers

Paste into bash or zsh. Each user gets its own cookie jar; every modifying request first fetches a CSRF token
(`GET /auth/csrf` sets the `XSRF-TOKEN` cookie, the request sends it back as a header).

```bash
BASE=http://localhost:8080/api/v1
token() { curl -s -c "$1" -b "$1" "$BASE/auth/csrf" > /dev/null; awk '$6 == "XSRF-TOKEN" {print $7}' "$1"; }
login() { curl -s -c "$1" -b "$1" -H "X-XSRF-TOKEN: $(token "$1")" -H 'Content-Type: application/json' \
            -d "{\"username\": \"$2\", \"password\": \"$3\"}" -o /dev/null -w "login $2: %{http_code}\n" "$BASE/auth/login"; }
send() { curl -s -c "$1" -b "$1" -H "X-XSRF-TOKEN: $(token "$1")" -H 'Content-Type: application/json' \
            -X POST "$BASE$2" ${3:+-d "$3"} -w "\n%{http_code}\n"; }
show() { curl -s -b "$1" "$BASE$2" | python3 -m json.tool; }
json() { python3 -c "import json, sys; d = json.load(sys.stdin); print($1)"; }
```

To start a run with no one logged in: `rm -f org.jar vol.jar vol5.jar vol8.jar admin.jar`.

## 3. Organization: activities and recommendations

```bash
login org.jar seed_org1 password1
show org.jar /organizations/me/activities
```

The demo activity is the upcoming "Sort food donations" (the lowest id; the other "Sort food donations" is the one
that has already started). Five volunteers are always available on its day.

```bash
ACTIVITY=$(curl -s -b org.jar "$BASE/organizations/me/activities" \
  | json 'min(a["id"] for a in d if a["title"] == "Sort food donations")')
show org.jar "/activities/$ACTIVITY/recommended-volunteers?limit=5"
```

Each entry has the volunteer profile and `score`: `geo`, `skill`, `priority`, `total` and the `weights` used
(FUNCTION.md). Volunteers who already applied, have no free capacity, lack a required certification or are not
available at that time are not in the list (feasibility filter).

```bash
show org.jar "/activities/$ACTIVITY/applications"
```

## 4. Volunteer view

`seed_vol08` … `seed_vol12` are always free on the demo activity's day and reach the whole area, so their
recommendations include it; the score has the same breakdown, with the global weights.

```bash
login vol8.jar seed_vol08 password1
show vol8.jar /volunteers/me/recommended-activities
```

## 5. Invitation

```bash
VOLUNTEER=$(curl -s -b org.jar "$BASE/activities/$ACTIVITY/recommended-volunteers?limit=1" | json 'd[0]["volunteer"]["id"]')
send org.jar "/activities/$ACTIVITY/invitations" "{\"volunteerId\": $VOLUNTEER}"
```

The application log now has `Notification to=seed_volNN@example.com subject="You are invited to an activity"`.
Its address tells which volunteer to log in as; put the number into `VOL`:

```bash
VOL=seed_volNN   # from the log line
login vol.jar $VOL password1
show vol.jar /volunteers/me/applications
INVITATION=$(curl -s -b vol.jar "$BASE/volunteers/me/applications" | json '[a["id"] for a in d if a["origin"] == "INVITATION"][0]')
send vol.jar "/invitations/$INVITATION/accept"
```

The log shows `subject="Invitation accepted"` to the organization. In the organization's application list the
accepted row now carries `volunteerEmail` (Q7d):

```bash
show org.jar "/activities/$ACTIVITY/applications"
```

## 6. Completion and history

The started "Sort food donations" has two accepted volunteers, `seed_vol05` and `seed_vol06`.

```bash
STARTED=$(curl -s -b org.jar "$BASE/organizations/me/activities" \
  | json 'max(a["id"] for a in d if a["title"] == "Sort food donations")')
send org.jar "/activities/$STARTED/complete"
login vol5.jar seed_vol05 password1
show vol5.jar /volunteers/me/history
```

The log has one `subject="Activity completed"` line per accepted volunteer. `seed_vol01` already has a history
from the activity completed a week ago.

## 7. Admin

```bash
login admin.jar "$ADMIN_USERNAME" 'your admin password'
show admin.jar "/admin/users?query=seed_vol&size=5"
send admin.jar "/admin/users/$VOLUNTEER/ban"
curl -s -b vol.jar -o /dev/null -w "%{http_code}\n" "$BASE/volunteers/me"   # 401: the session is gone
login vol.jar $VOL password1                                               # 401: the account is banned
```

The ban withdrew the volunteer's accepted invitation, so the log shows `subject="A volunteer withdrew"` to the
organization, and the spot is free again (Q25). `send admin.jar "/admin/users/$VOLUNTEER/unban"` lets the volunteer
log in again but restores nothing (Q27).

## Starting over

Seed data is loaded only into a database without `seed_org1`. The local database has no volume, so recreating the
container empties it completely, including any other data:
`docker compose rm -sf postgres && docker compose up -d postgres`.
