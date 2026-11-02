# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Personal tooling for one Tuya Wi-Fi thermostat (R11, category `wk`, device id `bf951d1682f4350b08omnp`, EU region), controlled through the Tuya Cloud OpenAPI. The user communicates in Catalan; UI strings and code comments are in Catalan too.

## Layout

- Root = shared across platforms: Python Tuya tooling (`list_devices.py`), `.venv`, and the cloud credentials.
- `macOS/` = SwiftUI menu bar app (`TermostatBar`), a Swift package with no Xcode project.
- `android/` = Android app **Caliu** (`com.alorma.caliu`), a standalone Gradle project (Kotlin, Jetpack Compose, Material 3 Expressive from `material3` 1.5 beta). Modules: `:app` and `:designsystem` (theme `CaliuTheme`; the only place for `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`). It follows the conventions of the sibling projects `../converses` and `../caducity`: version catalog in `gradle/libs.versions.toml`, ktlint with rules in `.editorconfig`, UI strings in `strings.xml` (English default, `values-ca`, `values-es`). The work plan lives in `android/docs/` (`README.md` with scope, decisions and handoff-vs-device differences; `fases.md` with the phases), and the Claude Design handoff (spec, colors, type, strings, captures) is in `android/docs/disseny/`, which is the design source of truth and is not edited.

## Commands

```bash
# macOS app: release build, ad-hoc sign, install to ~/Applications (kills the running instance)
macOS/build.sh
open ~/Applications/TermostatBar.app        # relaunch after building

# Quick compile check only
cd macOS && swift build -c release

# Android app (run from android/)
./gradlew :app:assembleDebug
./gradlew ktlintCheck            # ktlintFormat to fix

# Python: list cloud devices + their status (rewrites devices.json)
.venv/bin/python list_devices.py
```

There are no tests. The only linter is ktlint in the Android project.

## Credentials (gitignored, never commit)

- `tinytuya.json` (`apiKey`, `apiSecret`, `apiRegion`) is used by the Python tooling.
- The macOS app reads `~/Library/Application Support/TermostatBar/config.json` (`accessId`, `accessSecret`, `region`, `deviceId`). This is a separate copy of the same Tuya IoT project credentials.

## macOS app architecture

- `TuyaClient` (actor): hand-rolled Tuya OpenAPI client. It handles HMAC-SHA256 request signing, token caching/refresh, and maps Tuya error codes to `TuyaError`, which carries recovery instructions such as an expired IoT Core trial.
- `ThermostatModel` (`@MainActor` ObservableObject): polls `status()` every 60 s, applies changes optimistically and rolls them back on failure. Setpoint changes from +/- are debounced (800 ms). "Pause" lowers the setpoint below the current temperature, and its state is stored in UserDefaults.
- `TermostatBarApp.swift`: `MenuBarExtra` window UI (`PanelView`, `ScheduleView`).

## Tuya API quirks for this device (learned the hard way)

- **Two APIs with different capabilities:**
  - `/v1.0/devices/{id}/commands` (standard instruction set) only exposes a subset of the DPs, and `mode` there only accepts `"auto"`; sending `"home"` fails with `params range invalid`.
  - `/v2.0/cloud/thing/{id}/shadow/properties` (read) and `.../shadow/properties/issue` (write, body `{"properties": "<JSON string>"}`) expose the full device model: `mode` ∈ `home|auto|temporary`, `work_state`, `week_program_13_*`, `dormant_*`. Use v2 for anything outside the basic set.
- `work_state` (`heating`/`stop`) and the weekly program only appear through the v2 properties endpoint, not `/v1.0/.../status`.
- **Mode:** `home` is manual and `auto` follows the weekly program. `temporary` means the user changed the setpoint while in auto; it is expected to last until the next program slot. The app treats `temporary` as auto.
- **Weekly program:** `week_program_13_1` … `_7` (assumed 1 = Monday). Each is a raw base64 value of exactly 25 bytes: `[day, (hour, minute, temp×10 as uint16 BE) × 6]`. There is **no slot-count byte**; adding one shifts every field and the Smart Life app shows garbage like `220:13 | 0.0°C`. Decoding is in `ScheduleSlot.decode`. After writing, check in the Smart Life app, not only by reading the value back. Writing a `week_program_13_*` value drops a `temporary` override: the device goes back to `auto` and to the programmed setpoint (seen 2026-11-02). Right after the write, the cloud can still return the old value for a moment, so read it again a couple of seconds later.
- Temperatures are integers ×10 (`temp_set` 220 = 22.0 °C).
- `temp_correction` is intentionally set to -20 (-2 °C), because the thermostat sits near the router and TV. Don't reset it.
- **Device logs:** `/v1.0/devices/{id}/logs`, or `tinytuya.Cloud.getdevicelog`, which handles paging. Log queries are rate-limited (`40000309`), so space them out.
  - Event types: 1 = online, 5 = command, 7 = DP report, 8 = signal.
  - `event_from` values: `1` = device (physical buttons or LAN), `2` = Smart Life app, `3` = third-party integration, `4` = cloud scene/automation/timer, `cloud2cloud` = this project's v1 command calls, `caesar` = this project's v2 property writes.
- Timer/schedule APIs (`/v1.0/devices/{id}/timers`) are not subscribed for this cloud project.

Writing device state (setpoint, mode, program) affects a real home's heating, so confirm with the user before doing it.
