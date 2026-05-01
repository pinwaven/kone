# Nano Flow — Integration Guide

The **Nano Flow** is the Waven-specific operating mode for Kone. It replaces the legacy clinical pipeline (`fros-api` + `SbEdge`) with a single cloud backend (`NanoApi`) and renders a biological age report instead of a raw concentration result.

Activated by setting `ConfigSysBean.flow = "nano"` in system settings (same screen where the Nano device serial is configured).

---

## 1. Configuration

| Setting | Field | Notes |
|---|---|---|
| Flow type | `ConfigSysBean.flow` | Must equal `"nano"` |
| Device serial | `ConfigSysBean.nanoDeviceId` | Must match a `serial_number` in `kino_devices` table on the backend |
| API base URL | `AppParams.NANO_BASE_URL` | e.g. `https://nano.fros.cc` |
| API token | `AppParams.NANO_API_TOKEN` | Bearer token sent in every request |

`NanoApi.kt` reads `baseUrl` and `apiToken` from `AppParams` on every call, so changes in settings take effect without restart.

---

## 2. End-to-End Sequence

```
QR code scan
    │
    ▼
GET /api/kino-chip?chip_id=…          ← handleNanoChipScan()
    │  chip config, patient info, biomarker_keys
    │
    ▼
Hardware scan (motor + ADC)           ← existing scan pipeline (unchanged)
    │  raw ADC data → concentration values via var_list polynomial
    │
    ▼
POST /api/biomarkers                  ← doReadData() after EVT_DEV_READ_DATA
    │  measured values + openid + kino_device_id
    │  returns: bioage_profile + full biomarker set (< 1 s)
    │
    ▼
Display report                        ← WorkActionNanoReportBlock
    │  nanoReport StateFlow updated → UI recomposes
    │
    ▼
POST /api/kino-result                 ← marks chip as "used" in DB
```

---

## 3. Step-by-Step Detail

### 3.1 Chip scan — `GET /api/kino-chip`

Triggered in `handleNanoChipScan()` (WorkMainViewModel) when a QR code is read and `isNanoFlow()` is true.

**Request**
```
GET /api/kino-chip?chip_id=<chip_code>
Authorization: Bearer <token>
```

**Response (`NanoChipResp`)**
```json
{
  "found": true,
  "used": false,
  "scan_id": 42,
  "user_id": "wx_openid_abc",
  "nickname": "Pin",
  "birth_date": "1982-03-15",
  "chrono_age": 43,
  "gender": "male",
  "scan_status": "pending",
  "model": "K2",
  "biomarker_keys": ["hsCRP"],
  "chip_config": { "scan_ppmm": 60, "top_list": […], "var_list": […], … },
  "guide_video": "https://…",
  "guide_text": "…"
}
```

**What the app does with it**
- `chip_config` → converted to `CardConfig` via `nanoChipConfigToCardConfig()` and used to drive the hardware scan (scan resolution, peak detection windows, polynomial coefficients, cut-off thresholds)
- `biomarker_keys` → stored in `nanoBiomarkerKeys` (private) and `nanoChipKeys` (StateFlow); used later to filter scan results before POSTing
- Patient fields (`nickname`, `birth_date`, `gender`, `user_id`) → populate `CaseBean` so the UI and the `/api/biomarkers` request have the correct identity
- `guide_video` / `guide_text` → shown in the pre-scan guidance screen

**Error handling**
- `found = false` → `EVT_DEV_ERROR` / "chip not registered"
- `used = true` → `EVT_DEV_ERROR` / "chip already used"
- `chip_config = null` → `EVT_DEV_ERROR` / "chip not configured"
- `biomarker_keys` not in known set → `EVT_DEV_ERROR` / "unknown panel"
- Network failure / null response → `EVT_DEV_ERROR_NETWORK`

---

### 3.2 Hardware scan

Unchanged from the clinical flow. The scan driver uses `CardConfig` (populated from `chip_config` above) for:
- `scan_ppmm` — motor step resolution
- `top_list` — peak detection windows (each entry maps to one biomarker)
- `var_list` — polynomial coefficients (x0 + x1·area → concentration)
- Cut-off thresholds and noise floors

Output: `CaseBean.resultList` — a list of `CaseResult(name, result)` where `name` matches keys in `biomarker_keys` and `result` is the concentration as a string.

---

### 3.3 Biomarkers upload — `POST /api/biomarkers`

Triggered in `doReadData()` immediately after the scan result is available.

**Request (`NanoBiomarkersReq`)**
```json
{
  "openid": "wx_openid_abc",
  "test_type": "kino_chip",
  "test_data": { "hsCRP": 1.42 },
  "kino_device_id": "KNA1-0001"
}
```

- `test_data` is built by `extractTestData()`: filters `resultList` to only the keys in `biomarker_keys`, drops NaN/Infinity values
- `kino_device_id` is the device serial from `ConfigSysBean.nanoDeviceId`; the backend resolves it to an integer FK via `kino_devices.serial_number`

Uses `httpUtilsSlow` (60 s read timeout) to accommodate variability, though the backend returns in under 1 s in normal operation.

**Response (`NanoBiomarkersResp`)**
```json
{
  "success": true,
  "user_id": "wx_openid_abc",
  "biomarkers": {
    "hsCRP": 1.42,
    "GDF15": 834.1,
    "IL6": 3.7,
    "GA": 14.2,
    "CystatinC": 0.88,
    "CD38": 1.15
  },
  "bioage_profile": {
    "ChronoAge": 43.0,
    "BioAge": 49.8,
    "AgeDifference": 6.8,
    "SubAges": {
      "ResilienceAge": 53.0,
      "CellularAge": 52.8,
      "MetabolicAge": 46.7,
      "MicroVascularAge": 40.1
    },
    "Scores": {
      "Resilience": 3.2,
      "Cellular": 4.1,
      "Metabolic": 6.8,
      "MicroVascular": 8.4
    }
  }
}
```

Note: `bioage_profile` keys are PascalCase (output of `BioAgeCalculator.js`). `NanoBioAgeProfile` and its children use `@SerializedName` to bridge from Gson's global `LOWER_CASE_WITH_UNDERSCORES` policy.

**What the app does with the response**
- `nanoReport.value = biomarkersResp` → triggers report UI recomposition
- If `bioage_profile` is non-null: updates `resultList[0]` with `bioAge`/`chronoAge` values and sets `radioValue` colour flag; writes `baaResult` into `CaseBean` for the legacy report store
- `saveCase()` is called before `/api/kino-result` to persist the case locally

---

### 3.4 Finalisation — `POST /api/kino-result`

**Request (`NanoKinoResultReq`)**
```json
{
  "chip_id": "MVNS0725122201-0087",
  "data": {
    "biomarkers": { "hsCRP": 1.42, … },
    "bioage_profile": { … }
  },
  "bio_age": 49.8,
  "kino_device_id": "KNA1-0001"
}
```

Marks the chip as `used` in the backend database and stores the result payload. The response is fire-and-forget — the app does not block on it or alter UI based on its outcome.

---

## 4. API Client — `NanoApi.kt`

Location: `app/src/main/java/poct/device/app/thirdparty/NanoApi.kt`

| Method | Purpose |
|---|---|
| `getChip(chipId)` | `GET /api/kino-chip` — chip lookup |
| `postBiomarkers(req)` | `POST /api/biomarkers` — upload + BioAge calculation |
| `postKinoResult(req)` | `POST /api/kino-result` — finalise chip as used |
| `probe()` | Health check: hits `/api/kino-chip?chip_id=__ping__`, returns latency and HTTP status |
| `deviceSerial()` | Reads `ConfigSysBean.nanoDeviceId` via `SysConfigService` |

`httpUtils` (15 s timeout) is used for `getChip` and `postKinoResult`.
`httpUtilsSlow` (60 s timeout) is used for `postBiomarkers` as a safety margin.

All requests include `Authorization: Bearer <token>` when `NANO_API_TOKEN` is non-empty.

---

## 5. Data Models

All models live in `thirdparty/model/nano/NanoModels.kt`.

```
NanoBiomarkersReq       POST /api/biomarkers request
NanoKinoResultReq       POST /api/kino-result request
NanoChipResp            GET /api/kino-chip response
  └─ NanoChipConfig     Hardware scan parameters
       ├─ NanoChipTop   Peak detection windows
       └─ NanoChipVar   Polynomial coefficients
NanoBiomarkersResp      POST /api/biomarkers response
  └─ NanoBioAgeProfile  Biological age result
       ├─ NanoSubAges   Four sub-age dimensions
       └─ NanoBioAgeScores  Per-dimension scores (0–10)
NanoKinoResultResp      POST /api/kino-result response
```

---

## 6. Report UI — `WorkActionNanoReportBlock.kt`

Rendered in `WorkMainBody` for action states `ACTION_WORK_DONE` and `ACTION_RESULT` when `sysConfig.flow == "nano"`.

Driven by two StateFlows from `WorkMainViewModel`:
- `nanoReport: StateFlow<NanoBiomarkersResp?>` — the full API response
- `nanoChipKeys: StateFlow<List<String>?>` — the chip's declared biomarker keys (currently unused in the UI but retained for future use)

### Layout

```
Surface (rounded card, fillMaxSize, 15 dp horizontal margin, 12 dp bottom margin)
  └─ Column (scrollable, 16 dp padding, 4 dp bottom)
       ├─ StatusFace (200 dp circle)
       ├─ [if bioage_profile != null]
       │    ├─ TabRow ("Bio Age" | "Biomarkers")
       │    ├─ [Bio Age tab]
       │    │    ├─ Row: ChronoAge chip | BioAge chip
       │    │    └─ 4× sub-age rows (dot + label + value)
       │    └─ [Biomarkers tab]
       │         └─ 6× biomarker rows (dot + label + value + unit)
```

### StatusFace circle

- 200 dp, 6 dp border in `#6375EC`
- **With `bioage_profile`:** shows "BIO AGE" label + value (coloured by age delta) + patient name
- **Without `bioage_profile`:** shows "STATUS / Complete" + patient name (fallback when backend returns no profile)
- BioAge colour: red (bio > chrono + 2), green (bio < chrono - 2), amber (within ±2)

### Bio Age tab

- **Chrono Age chip** (left) in `#A6C4E5`
- **Bio Age chip** (right) coloured by age delta
- Four sub-age rows in fixed order: Resilience → Cellular → Metabolic → Micro-Vascular

### Biomarkers tab

- Six rows in fixed order: hsCRP → GDF-15 → IL-6 → Glycated Albumin → Cystatin C → CD38
- All values displayed with uniform styling — no distinction between measured and estimated
- Format: `"%.2f  <unit>"`

### Internationalisation

All displayed strings use Android string resources (`stringResource()`). The app uses Lingver to apply the locale set in system settings.

| Resource key | Chinese (`values/`) | English (`values-en/`) |
|---|---|---|
| `nano_bio_age` | 生理年龄 | BIO AGE |
| `nano_status` | 状态 | STATUS |
| `nano_complete` | 完成 | Complete |
| `nano_tab_bio_age` | 生理年龄 | Bio Age |
| `nano_tab_biomarkers` | 生物标志物 | Biomarkers |
| `nano_chrono_age` | 实际年龄 | CHRONO AGE |
| `nano_sub_resilience` | 抗压年龄 | Resilience Age |
| `nano_sub_cellular` | 细胞年龄 | Cellular Age |
| `nano_sub_metabolic` | 代谢年龄 | Metabolic Age |
| `nano_sub_microvascular` | 微血管年龄 | Micro-Vascular Age |
| `nano_bm_hscrp` | hsCRP | hsCRP |
| `nano_bm_gdf15` | GDF-15 | GDF-15 |
| `nano_bm_il6` | IL-6 | IL-6 |
| `nano_bm_ga` | 糖化白蛋白 | Glycated Albumin |
| `nano_bm_cystatinc` | 胱抑素 C | Cystatin C |
| `nano_bm_cd38` | CD38 | CD38 |

---

## 7. Differences from Clinical Flow

| | Clinical flow | Nano flow |
|---|---|---|
| Config source | `SbEdgeFunc.getCardInfo` + fros-api | `NanoApi.getChip` |
| Patient lookup | fros-api patient database | Nano backend (WeChat openid) |
| AI processing | Async polling (`baaResult` loop) | Inline in `POST /api/biomarkers` response |
| Result shown | Raw concentrations + hsCRP reference | BioAge + four sub-ages + full biomarker panel |
| Chip lifecycle | Card reusable | Chip single-use; marked `used` via `/api/kino-result` |
| Report language | Fixed | Follows system language setting (CN/EN) |

---

## 8. Related Files

| File | Purpose |
|---|---|
| `thirdparty/NanoApi.kt` | HTTP client for all three nano endpoints |
| `thirdparty/model/nano/NanoModels.kt` | Request/response data classes |
| `ui/work/WorkMainViewModel.kt` | `handleNanoChipScan()`, `doReadData()` nano branch, `extractTestData()`, `isNanoFlow()` |
| `ui/work/WorkActionNanoReportBlock.kt` | Report composable |
| `ui/work/WorkMain.kt` | Mounts `WorkActionNanoReportBlock` in `ACTION_WORK_DONE` and `ACTION_RESULT` states |
| `bean/ConfigSysBean.kt` | `flow`, `nanoDeviceId` settings fields |
| `res/values/strings.xml` | Chinese string resources for nano report |
| `res/values-en/strings.xml` | English string resources for nano report |
