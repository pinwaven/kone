# Core Business Logic

## Biomarker Analysis (BAA)
The primary intelligence of the system resides in `BAAService.kt`.
- **Mapping:** Maps biological features (e.g., Albumin, ALP, Urea, Glucose) to health indices.
- **Logic:** Calculates physiological metrics based on scanned reagent card data.

## Reagent Card Management
- **`CardConfig` Entity:** Defines the scanning parameters for different reagent cards.
- **Parameters:** Includes start/end positions, laser power, and specific reaction times.

## Data Persistence
- **`AppDatabase.kt`**: Managed via Room.
- **Entities:**
  - `Case`: Individual test records.
  - `User`: System users and technicians.
  - `CardConfig`: Reagent card specifications.
  - `SysConfig`: Device system settings.

