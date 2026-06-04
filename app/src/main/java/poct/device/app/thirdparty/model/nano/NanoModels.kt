package poct.device.app.thirdparty.model.nano

import com.google.gson.annotations.SerializedName
import poct.device.app.R

// ── Requests ────────────────────────────────────────────────────────────────

data class NanoBiomarkersReq(
    val openid: String,
    val testType: String = "kino_chip",
    val testData: Map<String, Double>,
    val kinoDeviceId: String? = null,
    val testedAt: String? = null,
)

data class NanoKinoResultReq(
    val chipId: String,
    val data: Map<String, Any?>,
    val bioAge: Double? = null,
    val kinoDeviceId: String? = null,
)

data class NanoActivateReq(
    val mainboardId: String,
    val firmwareId: String,
    val model: String,
)

data class NanoMachine(
    val id: Long? = null,
    val machineNo: String? = null,
    val machineName: String? = null,
    val model: String? = null,
    val status: String? = null,
    val softwareVersion: String? = null,
    val firmwareVersion: String? = null,
)

data class NanoActivateResp(
    val success: Boolean = false,
    val machine: NanoMachine? = null,
    val rootToken: String? = null,
    val commToken: String? = null,
    val commTokenExpiresAt: String? = null,
    val error: String? = null,
)

data class NanoMachineInfoReq(
    val softwareVersion: String? = null,
    val firmwareVersion: String? = null,
)

data class NanoMachineInfoResp(
    val success: Boolean = false,
    val machine: NanoMachine? = null,
    val error: String? = null,
)

data class NanoAuthState(
    val rootToken: String = "",
    val commToken: String = "",
    val commTokenExpiresAt: String = "",
    val machineNo: String = "",
    val machineName: String = "",
    val model: String = "",
    val status: String = "",
    val activatedAt: String = "",
    val refreshedAt: String = "",
) {
    fun isActivated(): Boolean =
        rootToken.isNotBlank() && commToken.isNotBlank() && machineNo.isNotBlank()
}

object NanoAuthSupport {
    fun messageResForError(error: String?): Int {
        return when (error) {
            "firmware_id_mismatch" -> R.string.nano_auth_error_firmware_id_mismatch
            "mainboard_id_mismatch" -> R.string.nano_auth_error_mainboard_id_mismatch
            "no_available_machine_no" -> R.string.nano_auth_error_no_available_machine_no
            "invalid_root_token" -> R.string.nano_auth_error_invalid_root_token
            "machine_not_active" -> R.string.nano_auth_error_machine_not_active
            "comm_token_expired" -> R.string.nano_auth_error_comm_token_expired
            null, "" -> R.string.nano_auth_error_unknown
            else -> R.string.nano_auth_error_backend
        }
    }

    fun redactToken(token: String?): String {
        val value = token.orEmpty()
        if (value.isEmpty()) return "(empty)"
        if (value.length <= 8) return "***"
        return "${value.take(4)}...${value.takeLast(4)}"
    }

    fun extractFirmwareId(rawHandshake: String?): String {
        val raw = rawHandshake.orEmpty().trim()
        if (raw.isEmpty()) return ""
        val value = if (raw.contains("ver:")) raw.substringAfter("ver:") else raw
        val firmware = value.trim().lineSequence().firstOrNull().orEmpty().trim()
        return firmware.substringAfter("~", firmware).trim()
    }

    fun extractFirmwareVersion(rawHandshake: String?): String {
        val raw = rawHandshake.orEmpty().trim()
        if (raw.isEmpty()) return ""
        val value = if (raw.contains("ver:")) raw.substringAfter("ver:") else raw
        val firmware = value.trim().lineSequence().firstOrNull().orEmpty().trim()
        return firmware.substringBefore("~").trim()
    }
}

object NanoMachineInfoSupport {
    private const val MAX_VERSION_LENGTH = 128

    fun buildRequest(
        softwareVersion: String?,
        firmwareVersion: String?,
    ): NanoMachineInfoReq? {
        val software = softwareVersion.normalizedVersion() ?: return null
        val firmware = firmwareVersion.normalizedVersion() ?: return null
        if (software.isEmpty() && firmware.isEmpty()) return null
        return NanoMachineInfoReq(
            softwareVersion = software.ifEmpty { null },
            firmwareVersion = firmware.ifEmpty { null },
        )
    }

    private fun String?.normalizedVersion(): String? {
        val value = orEmpty().trim()
        return if (value.length > MAX_VERSION_LENGTH) null else value
    }
}

// ── GET /api/kino-chip response ─────────────────────────────────────────────
//
// Worker `handleGetKinoChip` returns this object directly as the JSON body.
// Field naming follows the global LOWER_CASE_WITH_UNDERSCORES policy.

data class NanoChipResp(
    val found: Boolean = false,
    val used: Boolean = false,
    val scanId: Long? = null,
    val userId: String? = null,
    val nickname: String? = null,
    val birthDate: String? = null,
    val chronoAge: Int? = null,
    val gender: String? = null,
    val scanStatus: String? = null,
    val model: String? = null,
    val biomarkerKeys: List<String>? = null,
    val chipConfig: NanoChipConfig? = null,
    val guideVideo: String? = null,
    val guideText: String? = null,
)

data class NanoChipConfig(
    val scanPpmm: Int = 0,
    val topList: List<NanoChipTop> = emptyList(),
    val varList: List<NanoChipVar> = emptyList(),
    val ft0: Int = 0,
    val xt1: Int = 0,
    val ft1: Int = 0,
    val scope: Double = 0.0,
    val typeScore: Double = 0.0,
    val cAvg: Double = 0.0,
    val cStd: Double = 0.0,
    val cMin: Double = 0.0,
    val cMax: Double = 0.0,
    val cutOff1: Double = 0.0,
    val cutOff2: Double = 0.0,
    val cutOff3: Double = 0.0,
    val cutOff4: Double = 0.0,
    val cutOff5: Double = 0.0,
    val cutOff6: Double = 0.0,
    val cutOff7: Double = 0.0,
    val cutOff8: Double = 0.0,
    val cutOffMax: Double = 0.0,
    val noise1: Double = 0.0,
    val noise2: Double = 0.0,
    val noise3: Double = 0.0,
    val noise4: Double = 0.0,
    val noise5: Double = 0.0,
)

data class NanoChipTop(
    val id: String? = null,
    val index: Int = 0,
    val start: Double = 0.0,
    val end: Double = 0.0,
    val ctrl: String? = null,
    val name: String? = null,
)

data class NanoChipVar(
    val id: String? = null,
    val index: Int = 0,
    val start: Double = 0.0,
    val end: Double = 0.0,
    val x0: Double = 0.0,
    val x1: Double = 0.0,
)

// ── POST /api/biomarkers response ───────────────────────────────────────────
//
// `bioage_profile` keeps PascalCase keys (set by BioAgeCalculator on the server)
// — they require explicit @SerializedName since the global Gson policy is
// snake_case.

data class NanoBiomarkersResp(
    val success: Boolean = false,
    val userId: String? = null,
    val biomarkers: Map<String, Double>? = null,
    val bioageProfile: NanoBioAgeProfile? = null,
)

data class NanoBioAgeProfile(
    @SerializedName("ChronoAge")     val chronoAge: Double = 0.0,
    @SerializedName("BioAge")        val bioAge: Double = 0.0,
    @SerializedName("AgeDifference") val ageDifference: Double = 0.0,
    @SerializedName("SubAges")       val subAges: NanoSubAges? = null,
    @SerializedName("Scores")        val scores: NanoBioAgeScores? = null,
)

data class NanoSubAges(
    @SerializedName("ResilienceAge")    val resilienceAge: Double? = null,
    @SerializedName("CellularAge")      val cellularAge: Double? = null,
    @SerializedName("MetabolicAge")     val metabolicAge: Double? = null,
    @SerializedName("MicroVascularAge") val microVascularAge: Double? = null,
)

data class NanoBioAgeScores(
    val total: Double? = null,
    @SerializedName("Resilience")    val resilience: Double? = null,
    @SerializedName("Cellular")      val cellular: Double? = null,
    @SerializedName("Metabolic")     val metabolic: Double? = null,
    @SerializedName("MicroVascular") val microVascular: Double? = null,
)

// ── POST /api/kino-result response ──────────────────────────────────────────

data class NanoKinoResultResp(
    val success: Boolean = false,
    val biomarkerId: Long? = null,
)

// ── GET /api/kino-upgrade response ─────────────────────────────────────────

data class NanoUpgradeResp(
    val version: String,
    val url: String,
)
