package poct.device.app.ui.work


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.inactiveColor

@Composable
fun WorkStepBlock(stepValue: State<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(73.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.ybjc_lrxx_icon_def),
                contentDescription = "",
                tint = switchIconColor(WorkMainViewModel.STEP_START, stepValue.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.work_info),
                fontSize = 12.sp,
                color = switchIconColor(WorkMainViewModel.STEP_START, stepValue.value)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
//        Icon(
//            painter = painterResource(id = R.mipmap.jindu_icon_jiantou_def),
//            contentDescription = "",
//            tint = switchArrowColor(WorkMainViewModel.STEP_START, stepValue.value)
//        )
//        Spacer(modifier = Modifier.width(8.dp))
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Icon(
//                painter = painterResource(id = R.mipmap.ybjc_fryb_icon_def),
//                contentDescription = "",
//                tint = switchIconColor(WorkMainViewModel.STEP_CASE, stepValue.value)
//            )
//            Spacer(modifier = Modifier.height(6.dp))
//            Text(
//                text = stringResource(id = R.string.work_case),
//                fontSize = 12.sp,
//                color = switchIconColor(WorkMainViewModel.STEP_CASE, stepValue.value)
//            )
//        }
//        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.mipmap.jindu_icon_jiantou_def),
            contentDescription = "",
            tint = switchArrowColor(WorkMainViewModel.STEP_CASE, stepValue.value)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.ybjc_ybjc_icon_def),
                contentDescription = "",
                tint = switchIconColor(WorkMainViewModel.STEP_WORK, stepValue.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.work_ing),
                fontSize = 12.sp,
                color = switchIconColor(WorkMainViewModel.STEP_WORK, stepValue.value)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
            painter = painterResource(id = R.mipmap.jindu_icon_jiantou_def),
            contentDescription = "",
            tint = switchArrowColor(WorkMainViewModel.STEP_WORK, stepValue.value)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.ybjc_ckbg_icon_def),
                contentDescription = "",
                tint = switchIconColor(WorkMainViewModel.STEP_REPORT, stepValue.value)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(id = R.string.work_report),
                fontSize = 12.sp,
                color = switchIconColor(WorkMainViewModel.STEP_REPORT, stepValue.value)
            )
        }
    }
}


private fun switchIconColor(stepValue: String, curStepValue: String): Color {
    val stepNum = WorkMainViewModel.STEPS.indexOf(stepValue)
    val curStepNum = WorkMainViewModel.STEPS.indexOf(curStepValue)
    if (stepNum == curStepNum) {
        return filledFontColor
    } else if (stepNum < curStepNum) {
        return filledFontColor
    } else {
        return inactiveColor
    }
}

private fun switchArrowColor(stepValue: String, curStepValue: String): Color {
    val stepNum = WorkMainViewModel.STEPS.indexOf(stepValue)
    val curStepNum = WorkMainViewModel.STEPS.indexOf(curStepValue)
    if (stepNum < curStepNum) {
        return filledFontColor
    } else {
        return inactiveColor
    }
}