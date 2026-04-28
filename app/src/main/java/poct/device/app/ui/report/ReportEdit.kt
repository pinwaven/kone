package poct.device.app.ui.report

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.endColor
import poct.device.app.theme.startColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppSampleUtils

/**
 * 页面定义
 */
@Composable
fun ReportEdit(navController: NavController, viewModel: ReportEditViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val bean by viewModel.bean.collectAsState()
    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }
    ) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.report_edit),
                    backEnabled = true,
                    onBack = {
                        viewModel.onExitConfirm()
                    }
                )
            },
            bottomBar = {
//                AppBottomBar {
//                    AppFilledButton(
//                        modifier = Modifier
//                            .width(80.dp)
//                            .height(40.dp),
//                        onClick = { viewModel.onSave() },
//                        text = stringResource(id = R.string.btn_label_save)
//                    )
//                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, top = 15.dp, end = 15.dp),
                color = bgColor,
                shape = RoundedCornerShape(4.dp)
            ) {
                ReportEditBody(
                    bean = bean,
                    navController = navController,
                    viewModel = viewModel,
                    onBeanUpdate = { viewModel.onBeanUpdate(it) }
                )
            }
        }
    }

    ReportEditInteraction(
        actionState = actionState,
        onClearInteraction = { viewModel.onClearInteraction() },
    ) {
        viewModel.onClearInteraction()
        if (navController.previousBackStackEntry != null) {
            navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                "true"
        }
        navController.popBackStack()
    }
}

@Composable
fun ReportEditInteraction(
    actionState: ActionState,
    onClearInteraction: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.event == ReportEditViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.report_edit_exit_confirm),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    } else if (actionState.event == ReportEditViewModel.EVT_LOADING) {
        AppViewLoading()
    } else if (actionState.event == ReportEditViewModel.EVT_SAVE_OK) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.save_ok),
            onOk = { onBackPage() }
        )
    }
}

/**
 * 内容主体
 */
@SuppressLint("DefaultLocale")
@Composable
fun ReportEditBody(
    bean: CaseBean,
    navController: NavController,
    viewModel: ReportEditViewModel,
    onBeanUpdate: (newBean: CaseBean) -> Unit,
) {
    val labelWidth = 72.dp
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 15.dp, end = 15.dp),
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(id = R.string.work_report_basic),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.5.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.width(labelWidth),
                text = stringResource(id = R.string.work_f_case_type),
                fontSize = 12.sp
            )
            AppDictUtils.caseTypeOptions(LocalContext.current)[bean.type]?.let { text ->
                Text(fontSize = 12.sp, text = text)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start, // 左对齐
            verticalAlignment = Alignment.CenterVertically // 垂直居中
        ) {
            Text(
                modifier = Modifier.width(labelWidth),
                text = stringResource(id = R.string.work_f_name),
                fontSize = 12.sp
            )
            Text(
                fontSize = 12.sp,
                text = bean.name,
                modifier = Modifier.width(100.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.width(32.dp))
            Text(
                modifier = Modifier.width(labelWidth / 2),
                text = stringResource(id = R.string.work_f_gender),
                fontSize = 12.sp
            )
            Text(
                fontSize = 12.sp, text = AppDictUtils.label(
                    AppDictUtils.genderOptions(LocalContext.current),
                    bean.gender
                )
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start, // 左对齐
            verticalAlignment = Alignment.CenterVertically // 垂直居中
        ) {
            Text(
                modifier = Modifier.width(labelWidth),
                text = stringResource(id = R.string.work_f_birthday),
                fontSize = 12.sp
            )
            Text(fontSize = 12.sp, text = bean.birthday)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start, // 左对齐
            verticalAlignment = Alignment.CenterVertically // 垂直居中
        ) {
            Text(
                modifier = Modifier.width(labelWidth - 18.dp),
                text = stringResource(id = R.string.work_f_reagent_id),
                fontSize = 12.sp
            )
            Text(fontSize = 12.sp, text = bean.reagentId)
            Spacer(modifier = Modifier.width(32.dp))
            Text(
                modifier = Modifier.width(labelWidth - 18.dp),
                text = stringResource(id = R.string.work_f_case_id),
                fontSize = 12.sp
            )
            Text(fontSize = 12.sp, text = bean.caseId)
        }
        Spacer(modifier = Modifier.height(4.5.dp))
        AppDivider()
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(startColor, endColor),
                        start = Offset(0f, 0f),
                        end = Offset(0f, Float.POSITIVE_INFINITY)
                    )
                )
                .clip(RoundedCornerShape(8.dp))
                .padding(12.dp),
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var title2 = stringResource(id = R.string.work_report_result)
                    if (bean.type == CaseBean.TYPE_2LJ_B_M || bean.type == CaseBean.TYPE_2LJ_B_F
                        || bean.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.type == CaseBean.TYPE_BIOAGE_CRP
                    ) {
                        title2 = stringResource(id = R.string.work_report_result_2)
                    }
                    Text(
                        text = title2,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.width(160.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(72.dp)
                            .height(24.dp),
                        text = stringResource(id = R.string.report_item_detail),
                        fontSize = 13.sp,
                        onClick = {
                            viewModel.onDataDetail(bean) {
                                navController.navigate(RouteConfig.REPORT_DETAIL)
                            }
                        },
                    )
                }
                Spacer(modifier = Modifier.height(4.5.dp))
                var title1 = stringResource(id = R.string.report_edit_title1)
                if (bean.type == CaseBean.TYPE_2LJ_B_M || bean.type == CaseBean.TYPE_2LJ_B_F
                    || bean.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.type == CaseBean.TYPE_BIOAGE_CRP
                ) {
                    title1 = stringResource(id = R.string.report_edit_title1_2)
                }
                AppFieldWrapper(
                    labelWidth = 80.dp,
                    text = title1,
                    fontSize = 13.sp,
                    background = Color.Unspecified
                ) {
                    if (bean.type == CaseBean.TYPE_4LJ || bean.type == CaseBean.TYPE_3LJ) {
                        Text(
                            modifier = Modifier.width(100.dp),
                            fontSize = 13.sp,
                            text = stringResource(id = R.string.report_edit_title2)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    } else if (bean.type == CaseBean.TYPE_2LJ_B_M || bean.type == CaseBean.TYPE_2LJ_B_F
                        || bean.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.type == CaseBean.TYPE_BIOAGE_CRP
                    ) {
                        Text(
                            modifier = Modifier.width(50.dp),
                            fontSize = 13.sp,
                            text = stringResource(id = R.string.report_edit_title6)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            modifier = Modifier.width(50.dp),
                            fontSize = 13.sp,
                            text = stringResource(id = R.string.report_edit_title7)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                    }
                    Text(
                        modifier = Modifier.width(80.dp),
                        fontSize = 13.sp,
                        text = stringResource(id = R.string.report_edit_title3)
                    )
                }
                AppDivider()
                val workResult = bean.resultList
                for (i in workResult.indices) {
                    val result = workResult[i]
                    AppFieldWrapper(
                        labelWidth = 80.dp,
                        text = result.name,
                        fontSize = 15.sp,
                        background = Color.Unspecified
                    ) {
                        if (bean.type == CaseBean.TYPE_4LJ || bean.type == CaseBean.TYPE_3LJ) {
                            Text(
                                modifier = Modifier.width(100.dp),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                text = result.radioValue
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                modifier = Modifier.width(80.dp),
                                fontSize = 15.sp,
                                text = result.result
                            )
                        } else if (bean.type == CaseBean.TYPE_2LJ_B_M || bean.type == CaseBean.TYPE_2LJ_B_F
                            || bean.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.type == CaseBean.TYPE_BIOAGE_CRP
                        ) {
                            Text(
                                modifier = Modifier.width(50.dp),
                                fontSize = 15.sp,
                                text = result.refer
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                modifier = Modifier.width(50.dp),
                                fontSize = 15.sp,
                                text = result.result
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            val colorMap = mapOf(
                                "green" to Color.Green,
                                "red" to Color.Red,
                            )
                            val data = result.radioValue.split("|")
                            Text(
                                modifier = Modifier.width(80.dp),
                                fontSize = 15.sp,
                                color = colorMap[data[0]] ?: Color.Black,
                                text = data[1]
                            )
                        } else {
                            Text(
                                modifier = Modifier.width(80.dp),
                                fontSize = 15.sp,
                                text = result.result
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            if (result.flag == 1) {
                                if (result.name == CaseBean.CRP_T2) {
                                    Text(
                                        fontSize = 15.sp,
                                        text = "↓"
                                    )
                                } else {
                                    Text(
                                        fontSize = 15.sp,
                                        text = "↑"
                                    )
                                }
                            }
                        }
                    }
                    AppDivider()
                }
            }
        }
        if (bean.type == CaseBean.TYPE_2LJ_B_M || bean.type == CaseBean.TYPE_2LJ_B_F
            || bean.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.type == CaseBean.TYPE_BIOAGE_CRP
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    painter = painterResource(id = R.drawable.report_result1),
                    contentDescription = "",
                )
            }
        }
    }
}


@Preview
@Composable
fun ReportEditPreview(
    navController: NavController = rememberNavController(),
    viewModel: ReportEditViewModel = viewModel(),
) {
    LaunchedEffect(null) {
        viewModel.bean.value = AppSampleUtils.genCaseInfo()
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        ReportEdit(navController)
    }
}