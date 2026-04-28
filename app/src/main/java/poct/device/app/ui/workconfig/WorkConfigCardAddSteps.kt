package poct.device.app.ui.workconfig


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.MutableStateFlow
import poct.device.app.R
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.inactiveColor
import poct.device.app.ui.workconfig.WorkConfigCardAddViewModel.Companion.STEP_INFO

/**
 * 内容主体
 */
@Composable
fun WorkConfigCardAddSteps(stepValue: State<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 15.dp, horizontal = 50.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.tjsjk_sjxx_icon),
                contentDescription = "",
                tint = switchIconColor(STEP_INFO, stepValue.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.work_config_tab_info),
                fontSize = 12.sp,
                color = switchIconColor(STEP_INFO, stepValue.value)
            )
        }

        Icon(
            painter = painterResource(id = R.mipmap.jindu_icon_jiantou_def),
            contentDescription = "",
            tint = switchArrowColor(STEP_INFO, stepValue.value)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.tjsjk_fwz_icon),
                contentDescription = "",
                tint = switchIconColor(WorkConfigCardAddViewModel.STEP_TOP, stepValue.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.work_config_tab_top),
                fontSize = 12.sp,
                color = switchIconColor(WorkConfigCardAddViewModel.STEP_TOP, stepValue.value)
            )
        }
        Icon(
            painter = painterResource(id = R.mipmap.jindu_icon_jiantou_def),
            contentDescription = "",
            tint = switchArrowColor(WorkConfigCardAddViewModel.STEP_TOP, stepValue.value)
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.tjsjk_dyx_icon),
                contentDescription = "",
                tint = switchIconColor(WorkConfigCardAddViewModel.STEP_VAR, stepValue.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.work_config_tab_var),
                fontSize = 12.sp,
                color = switchIconColor(WorkConfigCardAddViewModel.STEP_VAR, stepValue.value)
            )
        }
    }
}

private fun switchIconColor(stepValue: String, curStepValue: String): Color {
    val stepNum = WorkConfigCardAddViewModel.STEPS.indexOf(stepValue)
    val curStepNum = WorkConfigCardAddViewModel.STEPS.indexOf(curStepValue)
    if (stepNum == curStepNum) {
        return filledFontColor
    } else if (stepNum < curStepNum) {
        return filledFontColor
    } else {
        return inactiveColor
    }
}

private fun switchArrowColor(stepValue: String, curStepValue: String): Color {
    val stepNum = WorkConfigCardAddViewModel.STEPS.indexOf(stepValue)
    val curStepNum = WorkConfigCardAddViewModel.STEPS.indexOf(curStepValue)
    if (stepNum < curStepNum) {
        return filledFontColor
    } else {
        return inactiveColor
    }
}


@Preview
@Composable
fun WorkConfigCardAddStepsPreview() {
    val stepState = MutableStateFlow(STEP_INFO)

    WorkConfigCardAddSteps(
        stepValue = stepState.collectAsState()
    )
}