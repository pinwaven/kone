package poct.device.app.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.R
import poct.device.app.component.AppAlert
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.primaryColor
import java.text.DecimalFormat


/**
 * 页面定义
 */
@Composable
fun HomeWorkPre(
    visible: Boolean = false,
    viewModel: HomeWorkPreViewModel = viewModel(),
    onClose: () -> Unit = {},
    onOk: () -> Unit = {},
) {
    if (!visible) {
        return
    }
    val step by viewModel.step.collectAsState()
    val progress by viewModel.progress.collectAsState()
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .width(280.dp)
                .height(280.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            // 内容
            when (step) {
                1 -> HomeWorkPreStep(
                    step = 1,
                    subTitle = stringResource(id = R.string.home_work_pre_no_card_left),
                    btnText = stringResource(id = R.string.btn_label_next)
                ) {
                    viewModel.onStep1Confirmed()
                }

                2 -> HomeWorkPreStep(
                    step = 2,
                    subTitle = stringResource(id = R.string.home_work_pre_no_card_left),
                    btnText = stringResource(id = R.string.home_work_pre_init_start),
                ) {
                    viewModel.onInitStarted()
                }

                3 -> HomeWorkPreStep(
                    step = 3,
                    subTitle = stringResource(id = R.string.home_work_pre_init_ing),
                    progress = progress
                )

                4 -> HomeWorkPreStep(
                    step = 4,
                    subTitle = stringResource(id = R.string.home_work_pre_init_done),
                    btnText = stringResource(id = R.string.btn_label_ok),
                ) {
                    onOk()
                    viewModel.onReset()
                }

                10 -> HomeWorkPreStep(
                    step = 10,
                    subTitle = stringResource(id = R.string.home_work_pre_init_failed),
                    btnText = stringResource(id = R.string.btn_label_close),
                ) {
                    onClose()
                    viewModel.onReset()
                }
            }
            // 关闭按钮
            HomeWorkPreCloseAction {
                onClose()
                viewModel.onReset()
            }
        }
    }

    val tipVisible by viewModel.tipVisible.collectAsState()
    AppAlert(
        title = stringResource(id = R.string.confirm_title_remind),
        visible = tipVisible,
        content = stringResource(id = R.string.home_work_pre_no_card_left),
        onOk = { viewModel.tipVisible.value = false },
        okText = stringResource(id = R.string.btn_label_i_know),
    )
}

@Composable
private fun HomeWorkPreStep(
    step: Int,
    title: String = stringResource(id = R.string.home_work_pre_title),
    subTitle: String,
    btnText: String = "",
    progress: Int = 0,
    onClick: () -> Unit = {},
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (step == 1) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
                    .height(156.dp),
                painter = painterResource(id = R.mipmap.csh_img1),
                contentDescription = ""
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(34.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = fontColor,
                text = title
            )
            Spacer(modifier = Modifier.height(12.dp))
            when (step) {
                10 -> {
                    Image(
                        modifier = Modifier
                            .padding(0.dp)
                            .height(96.dp),
                        painter = painterResource(id = R.mipmap.wlj_icon),
                        contentDescription = ""
                    )
                }

                4 -> {
                    Image(
                        modifier = Modifier
                            .padding(0.dp)
                            .height(96.dp),
                        painter = painterResource(id = R.mipmap.tips_suc_img),
                        contentDescription = ""
                    )
                }

                3 -> {
                    Box(modifier = Modifier.size(96.dp)) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                /*@FloatRange(from = 0.0, to = 1.0)*/
                                progress = { 1F },
                                modifier = Modifier.size(96.dp),
                                strokeWidth = 16.dp,
                                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                color = primaryColor
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                /*@FloatRange(from = 0.0, to = 1.0)*/
                                progress = { progress.div(100F) ?: 0F },
                                modifier = Modifier.size(96.dp),
                                strokeWidth = 16.dp,
                                strokeCap = ProgressIndicatorDefaults.CircularDeterminateStrokeCap,
                                color = filledFontColor
                            )
                        }
                        val df = remember { DecimalFormat("#") }
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = "${df.format(progress)}%", fontSize = 24.sp)
                        }
                    }

                }

                2 -> {
                    Image(
                        modifier = Modifier.height(96.dp),
                        painter = painterResource(id = R.mipmap.csh_img2),
                        contentDescription = ""
                    )
                }

                else -> {
                    Spacer(modifier = Modifier.height(96.dp))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = fontColor,
                text = subTitle
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (btnText.isNotEmpty()) {
                AppFilledButton(
                    modifier = Modifier
                        .width(160.dp)
                        .height(36.dp),
                    text = btnText,
                    onClick = {
                        onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun HomeWorkPreCloseAction(onClose: () -> Unit = {}) {
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(5.dp),
            horizontalArrangement = Arrangement.End
        ) {
            var exitVisible by remember { mutableStateOf(false) }
            IconButton(
                modifier = Modifier.size(24.dp),
                onClick = {
                    exitVisible = true
                }
            ) {
                Icon(
                    painter = painterResource(id = R.mipmap.pop_icon_guanbi),
                    contentDescription = ""
                )
            }
            AppConfirm(
                visible = exitVisible,
                title = stringResource(id = R.string.confirm_title_remind),
                content = stringResource(id = R.string.home_work_pre_exit),
                confirmText = stringResource(id = R.string.btn_label_exit),
                onCancel = { exitVisible = false },
                onConfirm = {
                    onClose()
                    exitVisible = false
                }
            )
        }
    }
}


@Preview
@Composable
fun HomeWorkPrePreview() {
    val viewModel: HomeWorkPreViewModel = viewModel()
    LaunchedEffect(key1 = Unit) {
        viewModel.progress.value = 80
        viewModel.step.value = 3
    }
    AppPreviewWrapper {
        HomeWorkPre(true, viewModel) {}
    }
}