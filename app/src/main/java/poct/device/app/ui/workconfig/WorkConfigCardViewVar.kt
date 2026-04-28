package poct.device.app.ui.workconfig


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardVarBean
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppList
import poct.device.app.component.AppOutlinedButton

@Composable
fun WorkConfigCardViewVar(
    cardConfigBean: CardConfigBean,
    onInfo: () -> Unit = {},
    onTop: () -> Unit = {},
    onVar: () -> Unit = {},
) {
    val labelWidth = 140.dp
    Spacer(modifier = Modifier.height(15.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        AppOutlinedButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = { onInfo() },
            text = stringResource(id = R.string.work_config_tab_info)
        )
        Spacer(modifier = Modifier.width(5.dp))
        AppOutlinedButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = { onTop() },
            text = stringResource(id = R.string.work_config_tab_top)
        )
        Spacer(modifier = Modifier.width(5.dp))
        AppFilledButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = { onVar() },
            text = stringResource(id = R.string.work_config_tab_var)
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    val records = cardConfigBean.varList
    val caseType = cardConfigBean.type
    AppList(
        modifier = Modifier.fillMaxSize(),
        records = records
    ) { it ->
        WorkConfigCardViewVarItem(
            it,
            caseType,
            labelWidth,
        )
    }
}

@Composable
private fun WorkConfigCardViewVarItem(
    cardVar: CardVarBean,
    caseType: String,
    labelWidth: Dp,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            text = stringResource(id = R.string.work_config_tab_var) + (cardVar.index + 1)
        )
    }
    if (caseType == CaseBean.TYPE_CRP) {
        Spacer(modifier = Modifier.width(4.dp))
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_crp_type),
            labelWidth = labelWidth
        ) {
            Text(text = cardVar.type)
        }
    }
    Spacer(modifier = Modifier.width(4.dp))
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_start),
        labelWidth = labelWidth
    ) {
        Text(text = cardVar.start)
    }
    Spacer(modifier = Modifier.width(4.dp))
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_end),
        labelWidth = labelWidth
    ) {
        Text(text = cardVar.end)
    }
    Spacer(modifier = Modifier.width(4.dp))
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x0),
        labelWidth = labelWidth
    ) {
        Text(text = cardVar.x0)
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x1),
        labelWidth = labelWidth
    ) {
        Text(text = cardVar.x1)
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x2),
        labelWidth = labelWidth
    ) {
        Text(text = cardVar.x2)
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x3),
        labelWidth = labelWidth
    ) {
        Text(text = cardVar.x3)
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x4),
        labelWidth = labelWidth
    ) {
        Text(text = cardVar.x4)
    }
    AppDivider()
    Spacer(modifier = Modifier.height(24.dp))
}