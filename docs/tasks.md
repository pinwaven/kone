# Kino FC3.0 Device Auth Client Tasks

Source API document: `/home/tim/project/nano/docs/architecture/kino-fc3-device-auth-plan.md`

Scope: only Android client changes in this repository. Backend admin machine-number creation and server migrations are out of scope unless called out separately.

## 1. Device Activation and Token Storage

Goal: add a Kino FC3.0 activation flow that registers this Android board with the hardware identity returned by the analyzer handshake, then stores both the root token and communication token locally.

Functional points:

- Add client models for `POST /kino/activate`.
  - Request fields: `mainboard_id`, `firmware_id`, `model`.
  - Header: `Authorization: Bearer ${KINO_ACTIVATION_TOKEN}`.
  - Success response includes `machine`, `root_token`, `comm_token`, and `comm_token_expires_at`.
  - Error responses include explicit `error` values such as `firmware_id_mismatch`, `mainboard_id_mismatch`, and `no_available_machine_no`.
- Define where each identifier comes from.
  - `mainboard_id`: Android board/device ID from `App.getDeviceId()`.
  - `firmware_id`: hardware/analyzer ID or version returned by serial handshake.
  - `model`: current Kino machine model, defaulting to the configured model required by backend, expected value `KNA1` unless product config says otherwise.
- Update the factory-test activation button.
  - Current entry: `工厂测试 -> 设备激活`.
  - Activation button should call the new Nano/Kino activation API instead of the legacy `SbEdgeFunc.activateDevice`.
  - The UI must show the selected Nano environment, activation progress, and a detailed final result.
- Handle missing IDs before calling the API.
  - If Android board ID is empty, show a detailed error including the failed source: `mainboard_id 获取失败`.
  - If hardware handshake returns empty or unparsable ID, show a detailed error including the raw handshake response when available.
  - If both IDs are present but activation fails, show HTTP status, backend `error`, and a readable Chinese explanation.
- Persist activation state locally.
  - Store `root_token`, `comm_token`, `comm_token_expires_at`, machine number, machine name, model, and activation time.
  - Prefer an existing local config persistence path if suitable; otherwise add a small dedicated local token store.
  - Do not log tokens or include tokens in UI text.
- Preserve existing environment switching.
  - Activation must use `AppParams.runtimeModeState.nanoBaseUrl()` so production/development/test environments work consistently.
  - Activation token source must be configurable and must not be printed in logs.

Acceptance criteria:

- Pressing `工厂测试 -> 设备激活` collects both IDs, calls `POST {nanoBaseUrl}/activate`, and stores both returned tokens.
- Missing Android board ID and missing hardware ID produce different, specific error messages.
- Backend mismatch errors are surfaced as actionable Chinese messages.
- Tokens are not visible in logs or UI.

## 2. Nano API Authentication, Refresh, and Endpoint Update

Goal: update all Nano backend requests to use the FC3.0 communication token and refresh it once with the root token when it expires.

Functional points:

- Replace fixed-token Nano auth for device-facing APIs.
  - Existing `NanoApi` currently uses `AppParams.NANO_API_TOKEN`.
  - Protected calls must use the locally stored `comm_token`.
  - `POST /kino/activate` continues to use the fixed activation token.
  - `POST /kino/token/exchange` uses the locally stored `root_token`.
- Update endpoint paths according to the API document.
  - `GET /kino/kino-chip?chip_id=__ping__`
  - `GET /kino/kino-chip?chip_id={chipId}`
  - `POST /kino/biomarkers`
  - `POST /kino/kino-result`
  - `POST /kino/kino-machines/info`
  - `GET /kino/kino-upgrade`
  - Confirm whether old `/api/...` paths remain temporarily supported before implementation; client should target the new Kino function paths when backend is ready.
- Add token exchange support.
  - Implement `POST /kino/token/exchange` with `Authorization: Bearer ${root_token}`.
  - On success, update stored `comm_token`, `comm_token_expires_at`, machine info, and last refresh time.
  - On `invalid_root_token` or `machine_not_active`, clear or mark auth state as invalid and show a detailed error.
- Add one-time retry behavior for protected Nano calls.
  - If a protected API returns `comm_token_expired`, call `/kino/token/exchange` once, then retry the original request once.
  - Do not retry indefinitely.
  - Do not refresh on unrelated errors unless backend explicitly returns `comm_token_expired`.
  - If refresh succeeds but retry fails, surface the retry failure.
- Improve error reporting for all Nano calls.
  - Include endpoint name, HTTP status, backend `error`, and short readable message.
  - Preserve parse and network errors instead of returning only `null`.
  - Avoid leaking tokens in request/response logs.
- Ensure all current Nano call sites remain covered.
  - Probe/API test page.
  - Chip scan config lookup.
  - Biomarker submission.
  - Kino result submission.
  - APK upgrade check.
  - New device software/firmware info upload.

Acceptance criteria:

- Every protected Nano request sends `Authorization: Bearer ${comm_token}`.
- Expired communication token causes exactly one root-token exchange and one retry.
- Missing token, invalid root token, inactive machine, expired token after refresh, HTTP errors, and JSON parse errors are distinguishable in UI/log-facing result objects.
- Existing Nano flow behavior is preserved after successful authentication.

## 3. Upload Software/Firmware Version Info

Goal: send local software and hardware firmware versions to the backend after activation or during device info refresh.

Functional points:

- Add client models for `POST /kino/kino-machines/info`.
  - Request fields: optional `software_version`, optional `firmware_version`; at least one must be sent.
  - Header: `Authorization: Bearer ${comm_token}` with the same refresh-on-expiry behavior.
  - Response returns authenticated `machine` info.
- Determine version sources.
  - `software_version`: app version from existing local config/version source.
  - `firmware_version`: hardware version from serial handshake or current `ConfigInfoV2Bean.hardware`.
- Decide trigger points.
  - After successful activation.
  - When opening the device information page.
  - Optionally after hardware handshake updates the local firmware version.
- Validate before sending.
  - Trim version strings.
  - Do not send the request if both values are blank.
  - Backend rejects strings longer than 128 chars; client should avoid sending obviously invalid values.

Acceptance criteria:

- Version upload uses the token-authenticated machine identity and does not send a machine ID in the request body.
- Successful upload updates local cached machine/version fields.
- Upload failures show detailed messages without blocking normal viewing of local fallback data unless the page requires remote data.

## 4. Device Information Page Update

Goal: change the device information page to load authoritative device details from `GET /kino/device/me`.

Functional points:

- Add client models for `GET /kino/device/me`.
  - Header: `Authorization: Bearer ${comm_token}`.
  - Use the same one-time refresh-on-expiry behavior.
  - Expected fields should include machine identity and version/status fields; confirm exact backend response before implementation.
- Update `SysFunInfo` / `SysFunInfoViewModel` loading behavior.
  - In Nano flow, load device info from `/kino/device/me`.
  - In non-Nano flow, preserve existing legacy device info behavior.
  - Map remote fields into existing UI fields where possible:
    - `machine_no` -> device code.
    - `model` -> device type.
    - `machine_name` -> device name.
    - `software_version` -> software.
    - `firmware_version` -> hardware.
    - `status` -> display or diagnostic text if there is no existing field.
- Add fallback and errors.
  - If not activated, show a clear message telling the user to run `工厂测试 -> 设备激活`.
  - If token refresh fails, show the root-token/auth error.
  - If backend response is missing expected fields, show a structured parse/detail error.
- Keep upgrade navigation working.
  - The version upgrade button must continue to navigate to `AFTER_SALE_VERSION_UPGRADE`.

Acceptance criteria:

- Opening the device information page in Nano flow calls `GET {nanoBaseUrl}/kino/device/me`.
- Expired communication token is refreshed once before retrying this call.
- The displayed device information comes from backend machine data when the call succeeds.
- Non-Nano device information behavior is unchanged.

## 5. Local Data and Configuration Tasks

Goal: make activation and token state durable, testable, and safe.

Functional points:

- Decide local persistence target for auth state.
  - Existing system config can be extended if it is the established persistence layer.
  - A dedicated auth config/entity is preferable if token fields would pollute user-editable settings.
- Store at minimum:
  - `root_token`
  - `comm_token`
  - `comm_token_expires_at`
  - `machine_no`
  - `machine_name`
  - `model`
  - `status`
  - last activation/refresh timestamp
- Add a small in-memory wrapper/cache only if needed for request performance.
- Add token clearing/reset support for factory diagnostics.
  - This can be a later button or hidden diagnostic action, but implementation should allow invalidating local auth state.
- Redact sensitive values in all logs.

Acceptance criteria:

- Restarting the app preserves activation and token state.
- Token updates are written atomically enough that a failed refresh does not erase the old root token.
- Logs never contain full root or communication tokens.

## 6. Testing and Verification Tasks

Goal: cover the auth lifecycle without requiring a real backend for every test.

Unit tests:

- Token store save/load/update behavior.
- Error mapping for backend errors:
  - `firmware_id_mismatch`
  - `mainboard_id_mismatch`
  - `no_available_machine_no`
  - `invalid_root_token`
  - `machine_not_active`
  - `comm_token_expired`
- One-time refresh retry logic.
  - Success after refresh.
  - Refresh fails.
  - Retry fails after refresh.
  - Non-expiry errors do not refresh.
- Request path/header construction for protected calls.

Manual/integration verification:

- Activate with valid board ID and hardware handshake ID.
- Activate with missing hardware ID.
- Activate when backend returns mismatch.
- Run chip lookup, biomarker submit, result submit, and upgrade check with valid comm token.
- Force expired token response and confirm one refresh plus retry.
- Open device info page and confirm `/kino/device/me` data renders.

Recommended command before implementation PR:

```bash
./gradlew test
```

## 7. Open Questions Before Coding

- What is the exact hardware identifier to use as `firmware_id`: the full handshake result, the substring after `ver:`, or another hardware command response?
- What is the exact activation token source for Android builds: build config, environment-specific config, or existing `AppParams` constant?
- Is the new Kino function mounted at the base URL root (`/activate`, `/kino-chip`) for all environments, or is there still an `/api` prefix during rollout?
- What is the exact response schema for `GET /kino/device/me`?
- Should successful activation overwrite `ConfigSysBean.nanoDeviceId` with returned `machine_no`, or should machine number live only in the auth store?
