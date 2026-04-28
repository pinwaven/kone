package poct.device.app.ui.work

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.entity.User


@Composable
fun WorkMainBottomBar(
    actionValue: State<String>,
    checkStep: State<Int>,
    onReset: () -> Unit = {},
    onActionStartNext: () -> Unit = {},
    onActionCaseChipPre: () -> Unit = {},
    onActionCaseChipNext: () -> Unit = {},
    onActionCaseInputPre: () -> Unit = {},
    onActionCaseInputNext: () -> Unit = {},
    onActionCaseSamplePre: () -> Unit = {},
    onActionCaseSampleNext: () -> Unit = {},
    onActionCaseWaitPre: () -> Unit = {},
    onActionCaseWaitNext: () -> Unit = {},
    onActionWorkOutConfirm: () -> Unit = {},
    onActionContinueConfirm: () -> Unit = {},
    onActionWorkPre: () -> Unit = {},
    onActionWorkNext: () -> Unit = {},
    onActionReportGet: () -> Unit = {},
    onActionReportPre: () -> Unit = {},
    onActionReportContinue: () -> Unit = {},
    onActionReportPrint: () -> Unit = {},
) {
    if (actionValue.value == WorkMainViewModel.ACTION_REPORT1) {
        return
    }

    AppBottomBar {
        when (actionValue.value) {
            WorkMainViewModel.ACTION_START -> {
                AppOutlinedButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onReset() },
                    text = stringResource(id = R.string.btn_label_rest)
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionStartNext() },
                    text = stringResource(id = R.string.btn_label_next)
                )
            }

            WorkMainViewModel.ACTION_CASE_CHIP -> {
                // TODO 简化信息
//                AppOutlinedButton(
//                    modifier = Modifier
//                        .width(120.dp)
//                        .height(40.dp),
//                    fontSize = 14.sp,
//                    onClick = { onActionCaseChipPre() },
//                    text = stringResource(id = R.string.btn_label_previous)
//                )
//                Spacer(modifier = Modifier.width(10.dp))
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionCaseChipNext() },
                    text = stringResource(id = R.string.btn_label_next)
                )
            }

            WorkMainViewModel.ACTION_CASE_INPUT -> {
                AppOutlinedButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionCaseInputPre() },
                    text = stringResource(id = R.string.btn_label_previous)
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionCaseInputNext() },
                    text = stringResource(id = R.string.btn_label_next)
                )
            }

            WorkMainViewModel.ACTION_CASE_SAMPLE -> {
                // TODO 简化信息
//                AppOutlinedButton(
//                    modifier = Modifier
//                        .width(120.dp)
//                        .height(40.dp),
//                    fontSize = 14.sp,
//                    onClick = { onActionCaseSamplePre() },
//                    text = stringResource(id = R.string.btn_label_previous)
//                )
//                Spacer(modifier = Modifier.width(10.dp))
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionCaseSampleNext() },
                    text = stringResource(id = R.string.btn_label_next)
                )
            }

            WorkMainViewModel.ACTION_CASE_WAIT -> {
                AppOutlinedButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionCaseWaitPre() },
                    text = stringResource(id = R.string.btn_label_previous)
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionCaseWaitNext() },
                    text = stringResource(id = R.string.btn_label_next)
                )
            }

            WorkMainViewModel.ACTION_WORK_WAIT -> {
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionWorkOutConfirm() },
                    text = stringResource(id = R.string.work_report_exit)
                )

                if (checkStep.value == 100) {
                    Spacer(modifier = Modifier.width(10.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        fontSize = 14.sp,
                        onClick = { onActionContinueConfirm() },
                        text = stringResource(id = R.string.btn_label_next)
                    )
                }
            }

            WorkMainViewModel.ACTION_WORK -> {
                AppOutlinedButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionWorkPre() },
                    text = stringResource(id = R.string.btn_label_previous)
                )
                Spacer(modifier = Modifier.width(10.dp))
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionWorkNext() },
                    text = stringResource(id = R.string.btn_label_next)
                )
            }

            WorkMainViewModel.ACTION_WORK_PROCESS -> {
                AppFilledButton(
                    modifier = Modifier
                        .width(120.dp)
                        .height(40.dp),
                    fontSize = 14.sp,
                    onClick = { onActionWorkOutConfirm() },
                    text = stringResource(id = R.string.work_report_exit)
                )
            }

            WorkMainViewModel.ACTION_REPORT1 -> {
//                AppFilledButton(
//                    modifier = Modifier
//                        .width(120.dp)
//                        .height(40.dp),
//                    fontSize = 14.sp,
//                    onClick = { onActionReportGet() },
//                    text = stringResource(id = R.string.work_report_gen)
//                )
            }

            WorkMainViewModel.ACTION_REPORT2 -> {
                if (AppParams.curUser.role != User.ROLE_CHECKER) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(100.dp)
                            .height(40.dp),
                        fontSize = 12.sp,
                        onClick = { onActionReportPre() },
                        text = stringResource(id = R.string.btn_label_previous)
                    )
//                    Spacer(modifier = Modifier.width(10.dp))
                }
                // TODO 简化信息
//                AppOutlinedButton(
//                    modifier = Modifier
//                        .width(100.dp)
//                        .height(40.dp),
//                    fontSize = 12.sp,
//                    onClick = { onActionReportContinue() },
//                    text = stringResource(id = R.string.work_report_continue)
//                )
//                Spacer(modifier = Modifier.width(10.dp))
//                AppFilledButton(
//                    modifier = Modifier
//                        .width(100.dp)
//                        .height(40.dp),
//                    fontSize = 12.sp,
//                    onClick = { onActionReportPrint() },
//                    text = stringResource(id = R.string.work_report_print)
//                )
            }
        }
    }
}

@Preview
@Composable
fun WorkMainBottomBarPreview() {
    val viewModel: WorkMainViewModel = viewModel()
    val actionValue = viewModel.action.collectAsState()
    val checkStep = viewModel.checkStep.collectAsState()
    LaunchedEffect(key1 = Unit) {
        viewModel.action.value = WorkMainViewModel.ACTION_REPORT1
        viewModel.checkStep.value = 10
    }
    AppPreviewWrapper {
        WorkMainBottomBar(actionValue, checkStep)
    }
}