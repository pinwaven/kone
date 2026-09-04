# Kino One API Calls

## fros-api: User Report Interfaces (After Chip Scanning)

### Domain Environments
- **Test:** `https://fros-api-dev.gyyyhospital.com`
- **Production:** `https://fros-api.gyyyhospital.com`

### Endpoints

#### Update User Chip Test Results
- **Path:** `api/service/poct/device/uploadCheckData`
- **Request:**
```json
{
  "code": "Chip ID. Format: BatchNumber-CardNumber",
  "status": "Test status. pending: Waiting, completed: Finished",
  "date": "Test time. Format: 2024-04-07 12:00:01",
  "result": [
    {
      "name": "Test Item 1",
      "result": "Test result",
      "radio_value": "Test result (with unit). 0.00 mg/L",
      "refer": "Item 1 reference range",
      "t1_value": "0.00", // Item 1 T1 position area
      "t2_value": "0.00", // Item 2 T2 position area
      "t3_value": "0.00", // Item 3 T3 position area
      "t4_value": "0.00", // Item 4 T4 position area
      "c_value": "0.00",  // C1 position area
      "c2_value": "0.00"  // C2 position area
    },
    ...
  ]
}
```
- **Response:**
```json
{
  "success": true, // Whether execution was successful
  "message": "Upload successful" // Execution result message
}
```

#### Get User Chip Scan Results
- **Path:** `api/service/poct/device/queryByCode`
- **Request:**
```json
{
  "code": "Chip ID. Format: BatchNumber-CardNumber"
}
```
- **Response:**
```json
{
  "success": true,
  "message": "",
  "result": {
    "code": "Chip ID. Format: BatchNumber-CardNumber",
    "type": "Chip type. bioage_crp: Biological Age Assessment (CRP)",
    "status": "Test status. pending: Waiting, completed: Finished",
    "patient": {
      "name_": "Patient Name",
      "gender": "Patient gender. male: Male, female: Female",
      "birthDate": "Patient date of birth",
      "objectId": "Patient ID"
    },
    "date": "Test time. Format: 2024-04-07 12:00:01",
    "result": [
      {
        "name": "Test Item 1",
        "result": "Test result",
        "radio_value": "Test result (with unit). 0.00 mg/L",
        "refer": "Item 1 reference range",
        "t1_value": "0.00",
        ...
      }
    ]
  }
}
```

---

## edge-func: Device and Chip Interfaces

### Domain Environments
- **Test:** `https://sb.fros.cc/functions/v1`
- **Production:** `https://supabase.virtualhealth.cn/functions/v1`

### Endpoints

#### Device Activation (Bound to cloud on first use)
- **Path:** `business/poct/device/activate`
- **Request:**
```json
{
  "device_id": "Local physical ID of the device"
}
```
- **Response:**
```json
{
  "code": "200",
  "msg": "OK",
  "data": {
    "ok": true
  }
}
```

#### Get Device Info (Verify version during info check/update)
- **Path:** `business/poct/device/getConfig`
- **Request:**
```json
{
  "device_id": "Local physical ID of the device"
}
```
- **Response:**
```json
{
  "code": "200",
  "msg": "OK",
  "data": {
    "id": "Logical ID in the cloud",
    "name": "Device Name",
    "type": "Device type. KINO-A1: Maiwei, KINO-A2: Josh, KINO-A3: Chip compatible version",
    "code": "Device Code",
    "apk_version": "Software version",
    "firmware_version": "Firmware version",
    "network_status": "Network status. offline, online",
    "admin_password": "Admin password",
    "created_at": "Creation time",
    "updated_at": "Last modified time"
  }
}
```

#### Verify Device Admin Password
- **Path:** `business/poct/device/checkPwd`
- **Request:**
```json
{
  "device_id": "Local physical ID of the device",
  "pwd": "Password"
}
```
- **Response:**
```json
{
  "code": "200",
  "msg": "OK",
  "data": {
    "ok": true // Whether password is correct
  }
}
```

#### Get Chip Info
- **Path:** `business/poct/card/getInfo`
- **Request:**
```json
{
  "card_batch_code": "Batch Number",
  "card_code": "Chip ID"
}
```
- **Response:**
```json
{
  "code": "200",
  "msg": "OK",
  "data": {
    "card": {
      "id": "Chip logical ID",
      "card_batch_id": "Batch ID",
      "code": "Chip ID",
      "used_date": "Usage time",
      "status": "Chip status. inactive, active, checking, success",
      "created_at": "Creation time",
      "updated_at": "Last modified time"
    },
    "card_batch": {
      "id": "Batch logical ID",
      "name": "Batch Name",
      "type": "Batch type. bioage_crp: Biological Age Assessment (CRP)",
      "code": "Batch Number",
      "prod_date": "Production date",
      "exp_date": "Expiry date",
      "status": "Batch status. inactive, active",
      "guide_video": "Guide video URL",
      "guide_text": "Guide text content",
      "created_at": "Creation time",
      "updated_at": "Last modified time"
    },
    "card_config": {
      "scan_start": "Scan start position",
      "scan_end": "Scan end position",
      "scan_ppmm": 0, // Scan density
      "top_list": [
        {
          "id": "Item ID",
          "start": 0.0, // Item T point start position
          "end": 0.0,   // Item T point end position
          "ctrl": "Is C-value? n: No, y: Yes",
          "name": "Item Name"
        }
      ],
      "var_list": [
        {
          "id": "Formula ID",
          "start": 0.0, // Formula judgment start value
          "end": 0.0,   // Formula judgment end value
          "x0": 0.0,    // Variable 1
          "x1": 0.0     // Variable 2
        }
      ],
      "ft0": 0, // Initial reaction time (seconds)
      "xt1": 0, // Water absorption time (seconds)
      "ft1": 0, // Reaction time (seconds)
      "scope": 0.0, // Slope
      "type_score": 0.0, // Blood type coefficient (default 1.0 for whole blood)
      "c_min": 0.0, // Min reference value
      "c_max": 0.0,
      "cut_off1": 0.0, // Laser power
      "cut_off2": 0.0, // External reaction time (seconds)
      ...
    }
  }
}
```

#### Update Chip Status
- **Path:** `business/poct/card/updateStatus`
- **Request:**
```json
{
  "card_code": "Chip ID",
  "status": "Chip status. inactive, active, checking, success"
}
```
- **Response:**
```json
{
  "code": "200",
  "msg": "OK",
  "data": {
    "ok": true
  }
}
```

#### Get Biological Age Assessment Report (AI-assisted if missing)
- **Path:** `business/poct/baa/result`
- **Request:**
```json
{
  "code": "Chip ID",
  "dms_patient_id": "Patient ID"
}
```
- **Response:**
```json
{
  "code": "200",
  "msg": "OK",
  "data": {
    "detail": {
      "bio_age_profile": {
        "chrono_age": 0.0, // Actual age
        "bio_age": 0.0,    // Assessed age
        "age_difference": 0.0,
        "scores" : {}      // Dynamic key-value pairs
      }
    },
    "assets": {
      "title_img": "Title image URL",
      "diagram_img": "Result diagram URL"
    }
  }
}
```

### Chip Interface Calling Sequence
1. **Chip Verification:** `Get Chip Info` -> `Get User Chip Scan Results`
2. **Branching based on scan results:**
   - **New Card:** `Get User Chip Scan Results` -> `Update Chip Status (checking)` -> `Update User Chip Test Results` -> `Get Biological Age Assessment Report (AI-assisted)` -> `Update Chip Status (success)`
   - **Already Tested Card:** `Get User Chip Scan Results` -> `Get Biological Age Assessment Report (AI-assisted)`

---

## nano-api: Waven Nano AI Backend Interfaces

Thin client interface for the Waven Nano AI backend (running on Aliyun FC 3.0). It is activated when `ConfigSysBean.flow == "nano"`.

### Domain Environments
- **Production (Default):** `https://nano.fros.cc`
- **Development:** `https://nano-dev.fros.cc`
- **Test:** `https://nano-test.fros.cc`
- *Note: Base URL is dynamic and can be customized by售后/administrators in Settings.*

### Authentication Headers
- `Authorization: Bearer <NANO_API_TOKEN>`

### Endpoints

#### 1) Worker Node Connectivity Probe (Ping)
- **Method / Function:** `GET` / `probe()`
- **Path:** `/kino/kino-chip?chip_id=__ping__`
- **Description:** Hit by client to verify connectivity, latency, and Bearer Token validity.
- **Request:**
  - Query parameter: `chip_id=__ping__`
- **Response:**
  - HTTP 200 response indicating a successful parse by the worker node:
  ```json
  {
    "found": false
  }
  ```

#### 2) Get Reagent Chip Config Info
- **Method / Function:** `GET` / `getChip(chipId: String)`
- **Path:** `/kino/kino-chip?chip_id={chipId}`
- **Description:** Pulls reagent chip physical channel scanning metadata, limits, validity, and patient bindings by card code.
- **Request:**
  - Query parameter: `chip_id=BatchNumber-CardNumber` (URL encoded)
- **Response:**
  ```json
  {
    "found": true,
    "used": false,
    "scan_id": 12345,
    "user_id": "User OpenID",
    "nickname": "User Nickname",
    "birth_date": "1990-01-01",
    "chrono_age": 36,
    "gender": "male",
    "scan_status": "success",
    "model": "Kino-Model-V1",
    "biomarker_keys": ["CRP", "HbA1c"],
    "guide_video": "https://example.com/video.mp4",
    "guide_text": "Insert chip carefully",
    "chip_config": {
      "scan_ppmm": 10,
      "top_list": [
        {
          "id": "item-1",
          "index": 0,
          "start": 0.0,
          "end": 0.0,
          "ctrl": "n",
          "name": "CRP"
        }
      ],
      "var_list": [
        {
          "id": "formula-1",
          "index": 0,
          "start": 0.0,
          "end": 0.0,
          "x0": 0.0,
          "x1": 0.0
        }
      ],
      "ft0": 0,
      "xt1": 0,
      "ft1": 0,
      "scope": 0.0,
      "type_score": 1.0,
      "c_avg": 0.0,
      "c_std": 0.0,
      "c_min": 0.0,
      "c_max": 0.0,
      "cut_off1": 0.0,
      "cut_off2": 0.0,
      "cut_off3": 0.0,
      "cut_off4": 0.0,
      "cut_off5": 0.0,
      "cut_off6": 0.0,
      "cut_off7": 0.0,
      "cut_off8": 0.0,
      "cut_off_max": 0.0,
      "noise1": 0.0,
      "noise2": 0.0,
      "noise3": 0.0,
      "noise4": 0.0,
      "noise5": 0.0
    }
  }
  ```

#### 3) Post Biomarker Data for AI Evaluation
- **Method / Function:** `POST` / `postBiomarkers(req: NanoBiomarkersReq)`
- **Path:** `/kino/biomarkers`
- **Description:** Submit raw multi-channel scan signals to have AI model evaluate actual biomarker concentrations and compute biological age profiles.
- **Request:**
  ```json
  {
    "openid": "User OpenID",
    "test_type": "kino_chip",
    "test_data": {
      "CRP": 1.25,
      "HbA1c": 5.4
    },
    "kino_device_id": "Device Physical ID",
    "tested_at": "2024-04-07 12:00:01"
  }
  ```
- **Response:**
  ```json
  {
    "success": true,
    "user_id": "User OpenID",
    "biomarkers": {
      "CRP": 1.25,
      "HbA1c": 5.4
    },
    "bioage_profile": {
      "ChronoAge": 36.0,
      "BioAge": 32.5,
      "AgeDifference": -3.5,
      "SubAges": {
        "ResilienceAge": 31.2,
        "CellularAge": 33.1,
        "MetabolicAge": 32.0,
        "MicroVascularAge": 33.5
      },
      "Scores": {
        "total": 85.0,
        "Resilience": 88.0,
        "Cellular": 83.0,
        "Metabolic": 86.0,
        "MicroVascular": 84.0
      }
    }
  }
  ```

#### 4) Post Kino Diagnostic Result
- **Method / Function:** `POST` / `postKinoResult(req: NanoKinoResultReq)`
- **Path:** `/kino/kino-result`
- **Description:** Submits final test summary records to cloud for patient H5/PDF report generation.
- **Request:**
  ```json
  {
    "chip_id": "BatchNumber-CardNumber",
    "data": {
      "CRP": 1.25,
      "HbA1c": 5.4
    },
    "bio_age": 32.5,
    "kino_device_id": "Device Physical ID"
  }
  ```
- **Response:**
  ```json
  {
    "success": true,
    "biomarker_id": 123456789
  }
  ```

#### 5) Check for Software/Firmware Upgrade
- **Method / Function:** `GET` / `checkUpgrade()`
- **Path:** `/kino/kino-upgrade`
- **Description:** Checks latest available client APK version and firmware URL.
- **Request:**
  - None (identifies using authorization token in header)
- **Response:**
  ```json
  {
    "version": "0.3.5",
    "url": "https://poct-upgrade.virtualhealth.cn/apk/kone-0.3.5.apk"
  }
  ```

#### 6) Set Device Laser Intensity
- **Method / Function:** `POST` / `postDeviceConfig(laserIntensity: Int)`
- **Path:** `/kino/device-config`
- **Description:** Persists this device's calibrated laser power to the server (`kino_devices.device_config`, recursively merged so other future device-config keys aren't clobbered). Called from the "设置激光强度" factory-test dialog in `SampleSerial.kt`, alongside a local save to `LaserConfigService`. The server then applies the stored `laser_intensity` on top of the chip model's config for this device's future `Get Reagent Chip Config Info` responses — it overwrites `chip_config.cut_off1` (see endpoint #2), so a per-device laser calibration always takes effect on subsequent scans without needing a chip-model-level change.
- **Request:**
  ```json
  {
    "device_config": {
      "laser_intensity": -25
    }
  }
  ```
- **Response (success):**
  ```json
  {
    "success": true,
    "device_config": {
      "laser_intensity": -25
    }
  }
  ```
- **Response (failure):**
  ```json
  {
    "success": false,
    "error": "laser_intensity_must_be_a_number"
  }
  ```
  Also returns `400 device_config_required` if the `device_config` object is missing, and `404 machine_not_found` if the authenticated machine record doesn't exist.
