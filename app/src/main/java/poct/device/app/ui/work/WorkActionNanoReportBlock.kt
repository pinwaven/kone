package poct.device.app.ui.work

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.theme.bgColor
import poct.device.app.thirdparty.model.nano.NanoBiomarkersResp
import poct.device.app.thirdparty.model.nano.NanoSubAges

// ── Color palette mirroring nano-miniapp/pages/main/main.js BM_META + SUB_AGE_COLORS

private data class BmMeta(@StringRes val labelRes: Int, val key: String, val unit: String, val color: Color)

private val BM_META = listOf(
    BmMeta(R.string.nano_bm_hscrp,    "hsCRP",     "mg/L",      Color(0xFFEF4444)),
    BmMeta(R.string.nano_bm_gdf15,    "GDF15",     "pg/mL",     Color(0xFFF97316)),
    BmMeta(R.string.nano_bm_il6,      "IL6",       "pg/mL",     Color(0xFFA855F7)),
    BmMeta(R.string.nano_bm_ga,       "GA",        "%",         Color(0xFF6375EC)),
    BmMeta(R.string.nano_bm_cystatinc,"CystatinC", "mg/L",      Color(0xFF0EA5E9)),
    BmMeta(R.string.nano_bm_cd38,     "CD38",      "xBaseline", Color(0xFF10B981)),
)

private data class SubAgeMeta(val key: String, @StringRes val labelRes: Int, val color: Color)

private val SUB_AGES = listOf(
    SubAgeMeta("ResilienceAge",    R.string.nano_sub_resilience,    Color(0xFFC084D4)),
    SubAgeMeta("CellularAge",      R.string.nano_sub_cellular,      Color(0xFF10B981)),
    SubAgeMeta("MetabolicAge",     R.string.nano_sub_metabolic,     Color(0xFF6375EC)),
    SubAgeMeta("MicroVascularAge", R.string.nano_sub_microvascular, Color(0xFF0EA5E9)),
)

private fun bioAgeColor(bio: Double?, chrono: Double?): Color {
    if (bio == null || chrono == null) return Color(0xFFEEF2FF)
    val diff = bio - chrono
    return when {
        diff >  2 -> Color(0xFFEF4444)
        diff < -2 -> Color(0xFF10B981)
        else      -> Color(0xFFF59E0B)
    }
}

private fun NanoSubAges?.valueFor(key: String): Double? = when (key) {
    "ResilienceAge"    -> this?.resilienceAge
    "CellularAge"      -> this?.cellularAge
    "MetabolicAge"     -> this?.metabolicAge
    "MicroVascularAge" -> this?.microVascularAge
    else               -> null
}

@Composable
fun WorkActionNanoReportBlock(
    bean: State<CaseBean>,
    nanoReport: State<NanoBiomarkersResp?>,
    chipKeys: State<List<String>?>,
) {
    var activeTab by remember { mutableStateOf("bioage") }
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusFace(bean = bean.value, report = nanoReport.value)
            Spacer(Modifier.height(16.dp))
            if (nanoReport.value?.bioageProfile != null) {
                TabRow(active = activeTab, onChange = { activeTab = it })
                Spacer(Modifier.height(12.dp))
                if (activeTab == "bioage") {
                    BioAgeTab(report = nanoReport.value!!)
                } else {
                    BiomarkersTab(report = nanoReport.value!!)
                }
            }
        }
    }
}

@Composable
private fun StatusFace(bean: CaseBean, report: NanoBiomarkersResp?) {
    val profile = report?.bioageProfile
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .border(width = 6.dp, color = Color(0xFF6375EC), shape = CircleShape)
            .background(Color(0xFFF7F9FF), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (profile != null) {
                Text(stringResource(R.string.nano_bio_age), color = Color(0xFF6B7280), fontSize = 11.sp)
                Text(
                    text = "%.1f".format(profile.bioAge),
                    color = bioAgeColor(profile.bioAge, profile.chronoAge),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                if (bean.name.isNotEmpty()) {
                    Text(bean.name, color = Color(0xFF111827), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
            } else {
                Text(stringResource(R.string.nano_status), color = Color(0xFF6B7280), fontSize = 11.sp)
                Text(
                    text = stringResource(R.string.nano_complete),
                    color = Color(0xFF10B981),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (bean.name.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(bean.name, color = Color(0xFF111827), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun TabRow(active: String, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0xFFEEF2FF), RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabItem(stringResource(R.string.nano_tab_bio_age),    "bioage",     active, onChange, Modifier.weight(1f))
        TabItem(stringResource(R.string.nano_tab_biomarkers), "biomarkers", active, onChange, Modifier.weight(1f))
    }
}

@Composable
private fun TabItem(
    label: String,
    key: String,
    active: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = active == key
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) Color.White else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onChange(key) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (isActive) Color(0xFF111827) else Color(0xFF6B7280),
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun BioAgeTab(report: NanoBiomarkersResp) {
    val profile = report.bioageProfile!!
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        AgeChip(value = "%.0f".format(profile.chronoAge), label = stringResource(R.string.nano_chrono_age),
                color = Color(0xFFA6C4E5))
        AgeChip(value = "%.1f".format(profile.bioAge),    label = stringResource(R.string.nano_bio_age),
                color = bioAgeColor(profile.bioAge, profile.chronoAge))
    }
    Spacer(Modifier.height(20.dp))
    SUB_AGES.forEach { meta ->
        val v = profile.subAges.valueFor(meta.key)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(meta.color, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(stringResource(meta.labelRes), modifier = Modifier.weight(1f), fontSize = 14.sp,
                 color = Color(0xFF374151))
            Text(
                text = if (v != null) "%.1f".format(v) else "—",
                color = meta.color,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun AgeChip(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = color, fontSize = 36.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF6B7280), fontSize = 11.sp)
    }
}

@Composable
private fun BiomarkersTab(report: NanoBiomarkersResp) {
    val values = report.biomarkers ?: emptyMap()
    BM_META.forEach { meta ->
        val v = values[meta.key]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(meta.color, CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(meta.labelRes),
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = Color(0xFF111827),
            )
            val text = if (v != null) "%.2f".format(v) else "—"
            Text(
                text = "$text  ${meta.unit}",
                color = Color(0xFF111827),
                fontSize = 13.sp,
            )
        }
    }
}
