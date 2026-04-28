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
import poct.device.app.bean.CardTopBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppList
import poct.device.app.component.AppOutlinedButton
import poct.device.app.utils.app.AppDictUtils

@Composable
fun WorkConfigCardViewTop(
    cardConfigBean: CardConfigBean,
    onInfo: () -> Unit = {},
    onTop: () -> Unit = {},
    onVar: () -> Unit = {},
) {
    val labelWidth = 140.dp
    Spacer(modifier = Modifier.height(15.dp))
    Row (
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        AppOutlinedButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = {onInfo()},
            text = stringResource(id = R.string.work_config_tab_info)
        )
        Spacer(modifier = Modifier.width(5.dp))
        AppFilledButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = {onTop()},
            text = stringResource(id = R.string.work_config_tab_top)
        )
        Spacer(modifier = Modifier.width(5.dp))
        AppOutlinedButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = {onVar()},
            text = stringResource(id = R.string.work_config_tab_var)
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
    val records = cardConfigBean.topList
    val yesOrNoOptions = mapOf(
        "y" to stringResource(R.string.yes),
        "n" to stringResource(R.string.no),
    )
    AppList(
        modifier = Modifier.fillMaxSize(),
        records = records
    ) { it ->
        WorkConfigCardViewTopItem(
            it,
            labelWidth,
            yesOrNoOptions
        )
    }
}

@Composable
private fun WorkConfigCardViewTopItem(
    cardTop: CardTopBean,
    labelWidth: Dp,
    yesOrNo: Map<String, String>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            text = stringResource(id = R.string.work_config_tab_top) + (cardTop.index + 1)
        )
    }
    Spacer(modifier = Modifier.width(4.dp))
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_start),
        labelWidth = labelWidth
    ) {
        Text(text = cardTop.start)
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_end),
        labelWidth = labelWidth
    ) {
        Text(text = cardTop.end)
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_ctrl),
        labelWidth = labelWidth
    ) {
        Text(text = AppDictUtils.label(yesOrNo, cardTop.ctrl))
    }
    AppDivider()
    Spacer(modifier = Modifier.height(24.dp))
}