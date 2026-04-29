package poct.device.app.ui.work

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.bean.CaseBean
import poct.device.app.theme.bgColor
import poct.device.app.thirdparty.model.nano.NanoBiomarkersResp
import poct.device.app.thirdparty.model.nano.NanoSubAges

// ── Color palette mirroring nano-miniapp/pages/main/main.js BM_META + SUB_AGE_COLORS

private data class BmMeta(val key: String, val label: String, val unit: String, val color: Color)

private val BM_META = listOf(
    BmMeta("hsCRP",     "hsCRP",            "mg/L",      Color(0xFFEF4444)),
    BmMeta("GDF15",     "GDF-15",           "pg/mL",     Color(0xFFF97316)),
    BmMeta("IL6",       "IL-6",             "pg/mL",     Color(0xFFA855F7)),
    BmMeta("GA",        "Glycated Albumin", "%",         Color(0xFF6375EC)),
    BmMeta("CystatinC", "Cystatin C",       "mg/L",      Color(0xFF0EA5E9)),
    BmMeta("CD38",      "CD38",             "xBaseline", Color(0xFF10B981)),
)

private data class SubAgeMeta(val key: String, val label: String, val color: Color)

private val SUB_AGES = listOf(
    SubAgeMeta("ResilienceAge",    "Resilience Age",    Color(0xFFC084D4)),
    SubAgeMeta("CellularAge",      "Cellular Age",      Color(0xFF10B981)),
    SubAgeMeta("MetabolicAge",     "Metabolic Age",     Color(0xFF6375EC)),
    SubAgeMeta("MicroVascularAge", "Micro-Vascular Age", Color(0xFF0EA5E9)),
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
            .padding(horizontal = 15.dp),
        shape = RoundedCornerShape(12.dp),
        color = bgColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
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
                    BiomarkersTab(report = nanoReport.value!!, chipKeys = chipKeys.value)
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
                Text("BIO AGE", color = Color(0xFF6B7280), fontSize = 11.sp)
                Text(
                    text = "%.1f".format(profile.bioAge),
                    color = bioAgeColor(profile.bioAge, profile.chronoAge),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                if (bean.name.isNotEmpty()) {
                    Text("PATIENT", color = Color(0xFF6B7280), fontSize = 9.sp)
                    Text(bean.name, color = Color(0xFF111827), fontSize = 14.sp)
                }
            } else {
                Text("STATUS", color = Color(0xFF6B7280), fontSize = 11.sp)
                Text(
                    text = "Complete",
                    color = Color(0xFF10B981),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                if (bean.name.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("PATIENT", color = Color(0xFF6B7280), fontSize = 9.sp)
                    Text(bean.name, color = Color(0xFF111827), fontSize = 14.sp)
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
        TabItem("Bio Age",    "bioage",     active, onChange, Modifier.weight(1f))
        TabItem("Biomarkers", "biomarkers", active, onChange, Modifier.weight(1f))
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
        AgeChip(value = "%.1f".format(profile.bioAge),    label = "BIO AGE",
                color = bioAgeColor(profile.bioAge, profile.chronoAge))
        AgeChip(value = "%.0f".format(profile.chronoAge), label = "CHRONO AGE",
                color = Color(0xFFA6C4E5))
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
            Text(meta.label, modifier = Modifier.weight(1f), fontSize = 14.sp,
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
private fun BiomarkersTab(report: NanoBiomarkersResp, chipKeys: List<String>?) {
    val measured = chipKeys?.toSet() ?: emptySet()
    val values = report.biomarkers ?: emptyMap()
    BM_META.forEach { meta ->
        val v = values[meta.key]
        val isMeasured = meta.key in measured
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
                meta.label,
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = if (isMeasured) Color(0xFF111827) else Color(0xFF9CA3AF),
            )
            val text = if (v != null) "%.2f".format(v) else "—"
            Text(
                text = "$text  ${meta.unit}",
                color = if (isMeasured) Color(0xFF111827) else Color(0xFF9CA3AF),
                fontSize = 13.sp,
            )
        }
    }
    if (measured.isNotEmpty() && measured.size < BM_META.size) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Greyed values are estimated from measured biomarkers + age.",
            color = Color(0xFF9CA3AF),
            fontSize = 11.sp,
        )
    }
}
