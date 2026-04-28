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
