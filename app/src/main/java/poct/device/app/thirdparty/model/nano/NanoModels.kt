package poct.device.app.thirdparty.model.nano

import com.google.gson.annotations.SerializedName

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
