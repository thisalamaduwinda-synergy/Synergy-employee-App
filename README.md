# Synergy Employee

Employee mobile app for **Synergy Pharmaceuticals Corporation (Pvt) Ltd**.

It mirrors what the company's face-recognition gate system already recorded —
daily gate IN/OUT, working days, overtime, leave balance — and gives employees
the one thing the gate cannot: a way to explain a day it has no record of.

- **Stack:** Java 17 · Android Gradle Plugin 8.13.2 · minSdk 24 · targetSdk 36 ·
  Material 3 · MVVM · Navigation Component · ViewBinding
- **Application id:** `lk.synergypharma.employee`
- **Backend:** the face-recognition system's FastAPI server, `/api/v1/me/…`
  (`backend/app/api/v1/me.py`). Login, profile and attendance are real; the
  rest still runs off generated data — see
  [Phase 9](#phase-9--swapping-in-the-real-api).

---

## The one design decision that shapes everything

**Attendance is read-only in this app, and there is no "Check in" button.**

The gate already identifies the employee by face. A button here would create a
second, conflicting source of truth, and the first time the two disagree the
attendance record stops being evidence of anything. The app therefore *mirrors*
the gate: every IN/OUT event, the gate name, the camera, the match score.

There are exactly **two** things an employee can write:

1. an **absence reason** for a day the gate has no record of, and
2. a **leave request**.

Everything else is read-only. That is what keeps the face-recognition system the
single source of truth and the whole thing audit-safe.

---

## Running it

Everything below works with no backend and no network.

```bash
# from the project root
./gradlew assembleDebug        # builds app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest    # 40 unit tests over the business rules
./gradlew installDebug         # with a device or emulator attached
```

Or open the folder in Android Studio and press Run. The Gradle wrapper, the SDK
path in `local.properties` and the version catalog are all committed except
`local.properties`, which is machine-specific and git-ignored.

### Verified on device

Run end to end on a **Pixel 7 emulator, Android 16 (API 36)** — deliberately the
same API level as `targetSdk`, so the edge-to-edge handling is exercised rather
than assumed. Login, the month calendar, the gate-log timeline, the absence
reason flow (including its validation and the home alert clearing afterwards),
and the Sinhala switch were all walked through with no crashes. Screenshots of
each screen are in `build-screenshots/`.

Four things only a real device showed, all since fixed:

| What looked wrong | Cause |
|---|---|
| Every card rendered pale blue instead of white | Material 3 tints elevated surfaces toward the primary colour — `elevationOverlayEnabled` is now off |
| Dialogs rendered pale lilac | Dialogs read `colorSurfaceContainer*`, not `colorSurface`; those were left at the M3 tonal defaults |
| A stray dot at the end of every leave bar | Material 1.12 draws a track stop indicator by default |
| "Inside 8h 57m" in the gate-log header above "Total inside 9h 57m" in the card | Two different figures both labelled "inside" — the header is net of the break, so it now says "Worked" |

### Demo logins

Any password of four characters or more is accepted.

| Employee number | Who | Role |
|---|---|---|
| `1213` | Thisala Maduwinda, Junior Executive | `employee` |
| `1001` | Ruwani Silva, QA Manager | `supervisor` — the extra Approvals row appears under **More** |

These are real `employee_code` values from the recognition database. Anything
else logs in as `1213`, so the app can be demonstrated without handing out a
list of valid codes first.

### What the mock data does

`MockSynergyApi` generates gate events **relative to today**, seeded off the
date — so the same day always produces the same times, and the calendar is never
empty in a demo. It deliberately leaves one recent working day with **no gate
record**, because the "missing record → submit a reason" flow is the most
important thing in the release.

Writes are held in memory for the session, so submitting an absence reason
really does clear the home-screen alert and really does change the calendar day.
The demo behaves like the app, not like a slideshow.

Crucially, the mock returns data shaped **exactly** like the live recognition
database — lowercase status values, naive space-separated timestamps, cosine
similarities in the 0.5–0.7 band. A mock that returned prettier data than
reality would hide the very bugs it exists to surface; two of them are described
under [Integration](#integration-with-the-recognition-system).

---

## Screens in this release

| # | Screen | Notes |
|---|---|---|
| 00 | Register | Three steps: identity → OTP → password. See [Registration](#registration-is-activation-not-sign-up) |
| 01 | Log in | EPF/employee number + password, biometric unlock |
| 02 | Home | Live gate status, missing-record alert, announcements |
| 03 | Gate log | Full IN/OUT timeline with gate, camera and match score |
| 04 | Attendance | Month calendar, four counters, daily record |
| 05 | Absence reason | Camera-first certificate upload |
| 06 | Overtime | Derived from gate OUT vs shift end |
| 07 | Leave | Balance first, history second |
| 08 | Apply for leave | Live day count, covering officer, approver |
| 13 | Notifications | Every row deep-links to the screen that resolves it |
| 14 | More / Profile | Language, help, logout, role-gated Approvals |

**Shipping in v1.1:** payslips, announcements detail, staff directory, and the
supervisor approvals queue. The bottom bar has four tabs rather than the design's
five for that reason — a "Team" tab that opens *coming soon* is worse than no
tab. Adding it later is one `<item>` in `menu/bottom_nav_menu.xml` plus one
destination in `navigation/nav_graph.xml`.

---

## Layout

```
app/src/main/
├── assets/mock/                 Fixtures. Delete with the mock package.
├── java/lk/synergypharma/employee/
│   ├── SynergyApp.java          Application entry: DI, language, night mode
│   ├── ServiceLocator.java      ← the whole dependency graph, and the API switch
│   ├── data/
│   │   ├── api/                 SynergyApi (the seam) + MockSynergyApi
│   │   ├── remote/              ApiService (Retrofit contract) + dto/
│   │   ├── mapper/              DTO → domain. The mock uses these too.
│   │   ├── local/               PrefsManager (EncryptedSharedPreferences)
│   │   └── repository/          One per feature; returns LiveData<Result<T>>
│   ├── domain/
│   │   ├── model/               Clean objects the UI uses, + enums/
│   │   └── usecase/             Business rules that must not live in a Fragment
│   ├── ui/                      One package per screen: Fragment + ViewModel
│   │   └── common/              BaseFragment, Relay
│   └── util/                    Dates, currency, files, locale, view helpers
└── res/
    ├── values/                  colors · themes · styles · dimens · strings
    ├── values-si/strings.xml    සිංහල
    ├── drawable/                30 vector icons converted from the UI mock-up
    ├── mipmap-*/                Launcher icons generated from the logo
    └── navigation/nav_graph.xml Every screen transition
```

Screens never know where data comes from. Fragments talk to ViewModels,
ViewModels talk to Repositories, and only `ServiceLocator` knows whether the
answer came from generated data or the HR server.

---

## Registration is activation, not sign-up

Every employee is **already in the recognition system** — HR enrols their face
before their first day, and the `employees` table is the master record. The only
thing missing is a password. So first-time registration does not create anything;
it activates a row that already exists.

That single decision shapes the whole flow:

| Step | What it asks | Why |
|---|---|---|
| 1 · Identity | Employee number **and NIC** | Proves the record belongs to the person claiming it. The NIC is something HR already holds, not something the employee chooses |
| 2 · Verify | 6-digit OTP | Sent only to the `phone` column on that record — never to a number the client supplies |
| 3 · Password | New password, twice | Ends signed in, so nobody who has just proved who they are has to type it all again |

An unknown employee number is **not** a new joiner — it is somebody guessing.
Self-service sign-up would let anyone create an employee record and would collide
with the face enrolment HR has already done.

**What the server has to enforce** (spelled out in `RegistrationDto`):

- match on `employee_code` **and** `nic`, and only for a record whose status is
  `active` — a resigned employee must not be able to activate an account;
- refuse if a password already exists, rather than letting anyone overwrite a
  live account;
- rate-limit by employee code **and** IP, and return the *same* generic failure
  whether the code was unknown or the NIC was wrong. Two different messages turn
  this screen into an employee-number enumerator;
- expire the challenge token in minutes, cap OTP attempts, invalidate on success.

Password rules are deliberately modest — eight characters with letters and
numbers — because anything stricter gets written on the back of an ID badge.
The one hard rule is that it may not contain the employee number or the NIC,
which are the two things a colleague standing nearby already knows.

`employees.nic` is nullable on the backend, so **HR must fill it in before an
employee can register.** Worth checking that column before rollout.

Not built, and the obvious next thing: **forgot password**, which is the same
three steps with the first one already satisfied.

---

## Integration with the recognition system

The app was aligned against the live **Synergy Face Recognition System**
(FastAPI + SQLAlchemy + SQLite, `backend/app/`). Every DTO in
`data/remote/dto/` is named after that schema's columns, so the employee API is
a *filter* over existing tables rather than a reshaping of them.

### What already exists there

| App concept | Backend table | Notes |
|---|---|---|
| Gate event | `attendance_scans` | `scanned_at`, `direction`, `confidence`, `camera_id`, `image_path`, `is_duplicate` |
| Attendance day | `attendance` | one row per employee per `work_date`, with `worked_minutes`, `overtime_minutes`, `late_minutes`, `missing_checkout` |
| Employee | `employees` | `employee_code` is the login key and, at Synergy, the EPF number |
| Leave | `leave_requests`, `leave_balances` | including `carried_forward` |
| Holidays | `holidays` | with a `poya` / `mercantile` type |
| Shift | `shifts` | seeded GEN 08:00–17:00, 15 min grace, 60 min break |

### Two bugs this comparison caught

Both would have shipped, and both were in code that looked perfectly reasonable:

1. **Timestamps would not have parsed at all.** The backend writes
   `2026-08-11 13:17:43.957009` — a space separator, no offset, microseconds.
   The mapper only handled ISO-8601, so every scan would have been dropped and
   the employee would have seen an empty gate log for a day they worked. Fixed
   in `Wire.dateTime`, now covered by `WireTest`.

2. **Every genuine scan was being flagged as suspicious.** `confidence` is a
   *cosine similarity*, not a percentage. The gate accepts at
   `FACE_MATCH_THRESHOLD = 0.45`, and real scans read 0.53–0.68. The app was
   rendering that as "match 53%" and marking anything under 0.90 as low
   confidence — which is all of them. It now shows the raw similarity and flags
   only what is genuinely close to the threshold.

### What still has to be built

The recognition backend is HR-facing. Nothing under `/api/v1` is safe to point
the app at directly:

1. **Employee authentication.** Its `users` table holds `admin` / `hr` /
   `security` operator accounts. Employees have no credentials at all today —
   `employees` has no password column. That is what the three
   `me/auth/register/*` endpoints and a `password_hash` column are for; see
   [Registration](#registration-is-activation-not-sign-up).
2. **Employee-scoped endpoints** (`/me/…`). Every existing route returns the
   whole workforce. These must resolve the employee from the token and never
   accept an employee id from the client — otherwise anyone could read a
   colleague's attendance by changing a number in the URL.
3. **An absence-reasons table.** None exists. The backend records *that*
   somebody was absent but has nowhere to put *why* — and that is this app's
   headline feature. Shape proposed in `AbsenceReasonDto`.
4. **Announcements and notifications tables.** Neither exists.
5. **Two leave columns:** `covering_officer` and `half_day_period`. Both are
   standard on a Sri Lankan leave form and both are already on the app's screen.
6. **`camera_name` on the scan payload.** The table stores only `camera_id`; a
   bare number means nothing to the person reading their own timeline.
7. The `overtime_counted_minutes` migration has not been applied to the live
   database yet — the model has the column, the DB does not.

Also worth knowing: `employees.is_vip` hides certain people's movements from
shared dashboards. It does not affect their own view of their own attendance,
but any future team screen must honour it.

---

## Phase 9 — swapping in the real API

### Status

Done, step one: `RemoteSynergyApi` + `ApiClient` (Retrofit, bearer token,
silent refresh on 401). `USE_MOCK_DATA` is **false in debug** and the base URL
is `http://10.0.2.2:8000/api/v1/` — the emulator's alias for the machine
running the backend. `src/debug/res/xml/network_security_config.xml` allows
cleartext for that build only.

Real today: `login`, `logout`, `attendanceMonth`, `attendanceDay`. Everything
else in `RemoteSynergyApi` is marked `// MOCK` and delegates to
`MockSynergyApi` until its endpoint exists.

To run it:

1. Start the recognition backend (`uvicorn app.main:app --host 0.0.0.0 --port 8000`
   from `backend/`). It creates the `employee_credentials` table on start.
2. Issue an app password once:
   `python scripts/set_employee_app_password.py 1213 "Temp@1234"`
   (or `PUT /api/v1/employees/{id}/app-access` with an HR token).
3. `./gradlew installDebug`, log in with `1213` / `Temp@1234`.

On a physical phone replace `10.0.2.2` with the machine's LAN IP in
`app/build.gradle.kts` and keep both on the same Wi-Fi.

The contract is already written and compiled: `data/remote/ApiService.java` and
`data/remote/dto/`. Hand that to whoever builds the employee API.

When it exists:

1. Write `RemoteSynergyApi implements SynergyApi`, calling `ApiService` and
   converting with the **existing** mappers in `data/mapper/` — they are already
   exercised by the mock, so they are known to work.
2. Change one method in `ServiceLocator`:

   ```java
   private static SynergyApi createApi(Context context) {
       return BuildConfig.USE_MOCK_DATA
               ? new MockSynergyApi(context)
               : new RemoteSynergyApi(ApiClient.create(prefs), context);
   }
   ```

3. Flip `USE_MOCK_DATA` to `false` in `app/build.gradle.kts`.
4. Delete `data/api/MockSynergyApi.java`, `MockDataFactory.java` and
   `assets/mock/`.

No Fragment and no ViewModel changes. That is the entire reason for the
repository layer.

### Rules for the server side

- **Never let the app talk to the HR database directly.** Credentials inside an
  APK can be extracted in minutes and every employee would effectively hold them.
  Put a small REST service (Spring Boot or Node) in front of it.
- **No endpoint may create a gate event.** There is deliberately none in
  `ApiService`.
- The server must re-check the supervisor role on every approval call.
  `UserRole` on the client only decides what is on screen.
- Add certificate pinning in `res/xml/network_security_config.xml` once the
  domain is fixed — payslip data flows over that connection.

---

## Before the Play Store

- [ ] Replace the launcher icon and `drawable-xxhdpi/logo_*.png` with the
      high-resolution artwork (see [Logo](#logo)).
- [ ] Release signing config + `keystore.properties` (already git-ignored).
- [ ] **Data Safety declaration.** The app handles biometric-derived attendance
      data — declare it, and add a privacy policy URL before uploading.
- [ ] Internal testing track first, with two or three plant staff.
- [ ] `assets/play-store/ic_playstore_512.png` is ready for the listing.

---

## Logo

Taken from the recognition system's own frontend (`frontend/dist/favicon.png`,
512×512, and `logo.png`, 512×215) — the same artwork the gate dashboard uses, so
the two systems match. Everything in `res/mipmap-*` and
`res/drawable-xxhdpi/logo_*.png` is generated from those two files.

**To drop in higher-resolution artwork:** replace `logo_mark.png` and
`logo_full.png`, then regenerate the launcher icons with Android Studio's
*Image Asset* tool (white background, the mark at about 52% of the canvas so it
sits inside the adaptive-icon safe zone).

The mark contains black and orange, so it always sits on **white** — on the blue
header it goes inside a white tile rather than being knocked out to one colour.

---

## Notes for whoever picks this up

- **`strings.xml` from line one.** No layout contains hard-coded text, which is
  why the Sinhala switch under **More → Language** is a translation file rather
  than a rewrite.
- **Dark theme is not in v1.** The colour tokens are already split so adding
  `res/values-night/` is a resource change and nothing else. Until then
  `SynergyApp` pins light mode, so the app never renders half-inverted.
- **Inter is the design font.** Drop `inter_*.ttf` into `res/font` and add
  `android:fontFamily` to the `TextAppearance.Synergy.*` styles in `styles.xml`;
  no layout needs to change.
- **Deliberate deviations** from the original structure document:
  - a hand-written `ServiceLocator` instead of Hilt — same amount of code at this
    size, no annotation processor, and the whole graph is readable in one file;
  - no Room yet — there is nothing to cache while the data is generated on
    device. It slots in behind the repositories when the API lands;
  - Navigation 2.7.7 rather than 2.8.x, because 2.8 moved `NavGraph` onto
    kotlinx-serialization, which javac cannot resolve in a Java-only module.
- **The tests are in `app/src/test/`** and cover the two things worth guarding:
  the business rules (worked time, overtime rounding, leave day counting, the
  two validators) and the wire contract (timestamp parsing, and every enum
  against the backend's actual string vocabulary). **57 tests, all passing** —
  `./gradlew testDebugUnitTest`.
- **The wire vocabulary differs from the display language in two places**, and
  both are deliberate: leave type `sick` is labelled "Medical", because that is
  what staff and the certificate call it; and backend statuses `present`,
  `late` and `half_day` all render as one calendar colour, because lateness is a
  payroll distinction, not a reason to paint someone's day red.
