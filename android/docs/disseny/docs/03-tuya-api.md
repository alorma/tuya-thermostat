# 03 · Tuya Cloud (OpenAPI)

> Aquest document recull com funciona l'OpenAPI de Tuya en termes generals. **Els codis de dades (DP) i el format del programa depenen del model de termòstat**: s'han de verificar amb el dispositiu real abans de donar-los per bons. Deixa'ls configurables en un sol lloc (`ThermostatDpMap`).

## Credencials (pantalla de Configuració)

- **Access ID** (client_id) i **Access Secret** del projecte Cloud a iot.tuya.com.
- **Regió** del centre de dades del projecte → host:

| Regió | Host |
|---|---|
| EU | `https://openapi.tuyaeu.com` |
| US | `https://openapi.tuyaus.com` |
| CN | `https://openapi.tuyacn.com` |
| IN | `https://openapi.tuyain.com` |

- **Device ID** del termòstat.

## Autenticació i signatura

1. Token: `GET /v1.0/token?grant_type=1` → `access_token`, `refresh_token`, `expire_time`. Desar-lo en memòria i renovar-lo abans que caduqui (`GET /v1.0/token/{refresh_token}`).
2. Totes les peticions porten capçaleres `client_id`, `sign`, `t` (ms), `sign_method: HMAC-SHA256`, `nonce` (opcional) i, excepte la del token, `access_token`.
3. `sign = HMAC-SHA256(secret, client_id + [access_token] + t + nonce + stringToSign).toUpperCase()`
   on `stringToSign = METHOD + "\n" + SHA256(body) + "\n" + capçaleres signades + "\n" + url_amb_query_ordenada`.
4. Comprova el detall exacte a la documentació oficial de Tuya («Sign requests for Cloud authorization») i fes un test unitari de la signatura amb l'exemple que hi donen.

## Endpoints

| Ús | Petició |
|---|---|
| Estat | `GET /v1.0/devices/{device_id}/status` → llista `{code, value}` |
| Ordres | `POST /v1.0/devices/{device_id}/commands` amb `{"commands":[{"code":"temp_set","value":200}]}` |
| Especificació (per descobrir codis, rangs i escala) | `GET /v1.0/devices/{device_id}/specifications` (o `/v1.0/iot-03/devices/{device_id}/specification`) |
| Informació (en línia, nom) | `GET /v1.0/devices/{device_id}` |

## Codis de dades típics de termòstat (a verificar)

| Camp de l'app | DP habitual | Notes |
|---|---|---|
| `power` | `switch` | bool |
| `setpoint` | `temp_set` | Sovint enter amb escala (p. ex. 205 = 20,5 °C). Llegir `scale`, `min`, `max`, `step` de l'especificació |
| `currentTemp` | `temp_current` | Mateixa escala |
| `mode` | `mode` | Valors com `auto` / `manual` (pot haver-n'hi d'altres: `holiday`, `eco`…) |
| `heating` | `work_state` / `valve_state` | Si no existeix, derivar-lo |
| `battery` | `battery_percentage` | |
| programa | p. ex. `week_program`, `program`… | Sovint `raw`/base64 amb format propi del fabricant |
| temporal | p. ex. mode `temp_override` o similar | Si el dispositiu no en té, gestionar l'override des de l'app |

## Errors

- Mostrar la resposta de Tuya (`success:false`, `code`, `msg`) traduïda al català a la pantalla «Error de l'API de Tuya», amb el botó **Obrir iot.tuya.com** quan el problema és del projecte (servei IoT Core caducat, permisos, dispositiu no vinculat).
- Errors de xarxa → estat «Sense connexió a internet».
- Errors d'una ordre concreta → revertir la UI optimista i Snackbar.
