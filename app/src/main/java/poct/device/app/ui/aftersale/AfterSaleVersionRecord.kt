package poct.device.app.ui.aftersale

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.bean.VersionBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppList
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.theme.tipFontColor
import poct.device.app.utils.app.AppSampleUtils


/**
 * 页面定义
 */
@Composable
fun AfterSaleVersionRecord(
    navController: NavController,
    viewModel: AfterSaleVersionRecordViewModel = viewModel(),
) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val records by viewModel.records.collectAsState()
    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {

        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.after_sale_type_upgrade),
                    backEnabled = true
                )
            },
        ) {
            AfterSaleVersionRecordBody(
                records = records,
                onDetail = { viewModel.onViewVersion(it) },
                onHandle = { viewModel.onHandleVersion(it) }
            )
        }
    }
    AfterSaleVersionRecordInteraction(
        actionState = actionState,
        onClearInteraction = { viewModel.onClearInteraction() },
        onUpgradeNow = { viewModel.onUpgrade(it) },
    )
}

@Composable
fun AfterSaleVersionRecordInteraction(
    actionState: ActionState,
    onClearInteraction: () -> Unit,
    onUpgradeNow: (VersionBean) -> Unit,
) {
    if (actionState.event == AfterSaleVersionRecordViewModel.EVT_HANDLE) {
        val version = actionState.payload as VersionBean
        AfterSaleVersionHandleDialog(
            mode = "upgrade",
            version = version,
            onUpgradeCancel = onClearInteraction,
            onUpgradeNow = { onUpgradeNow(version) }
        )
    } else if (actionState.event == AfterSaleVersionRecordViewModel.EVT_DETAIL) {
        val version = actionState.payload as VersionBean
        AfterSaleVersionHandleDialog(
            mode = "view",
            version = version,
            onUpgradeCancel = onClearInteraction,
            onUpgradeNow = {}
        )
    } else if (actionState.event == AfterSaleVersionRecordViewModel.EVT_UPGRADE_ING) {
        val msg = actionState.msg!!
        AppViewLoading(msg = msg, width = 180.dp, height = 132.dp)
    } else if (actionState.event == AfterSaleVersionRecordViewModel.EVT_UPGRADE_DONE) {
        val msg = actionState.msg ?: "Pls set msg for event"
        AppAlert(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = msg,
            onOk = { onClearInteraction() }
        )
    }
}

/**
 * 内容主体
 */
@Composable
fun AfterSaleVersionRecordBody(
    records: List<VersionBean>,
    onDetail: (VersionBean) -> Unit,
    onHandle: (VersionBean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        AppList(
            modifier = Modifier.fillMaxWidth(),
            records = records
        ) { it ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .padding(15.dp)
                ) {
                    Image(
                        modifier = Modifier.size(36.dp),
                        painter = painterResource(id = R.mipmap.tips_update_abnor_img),
                        contentDescription = ""
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 3.5.dp)
                    ) {
                        Text(
                            text = genTitle(it),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = genDate(it),
                            fontSize = 12.sp,
                            color = tipFontColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        val handleLabel =
                            when (it.state) {
                                0 -> stringResource(id = R.string.btn_label_handle)
                                2 -> stringResource(id = R.string.btn_label_handled)
                                else -> stringResource(id = R.string.btn_label_expired)
                            }
                        val enabled = it.state == 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            AppOutlinedButton(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(30.dp),
                                onClick = { onDetail(it) },
                                text = stringResource(id = R.string.btn_label_detail)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            AppFilledButton(
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(30.dp),
                                onClick = { onHandle(it) },
                                enabled = enabled,
                                text = handleLabel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun genDate(bean: VersionBean): String {
    val label = when (bean.state) {
        0 -> stringResource(id = R.string.after_sale_f_effect_date)
        2 -> stringResource(id = R.string.after_sale_f_handle_date)
        else -> stringResource(id = R.string.after_sale_f_expired_date)
    }
    val value = when (bean.state) {
        0 -> bean.lapseTime
        1 -> bean.handleTime
        2 -> bean.handleTime
        else -> bean.lapseTime
    }
    return "${label}${value}"
}

@Composable
private fun genTitle(bean: VersionBean): String {
    val type = stringResource(id = R.string.after_sale_type_upgrade)
    return when (bean.state) {
        0 -> stringResource(id = R.string.after_sale_task_has).format(type)
        2 -> stringResource(id = R.string.after_sale_task_handled).format(type)
        else -> stringResource(id = R.string.after_sale_task_expired).format(type)
    }
}

@Preview
@Composable
fun AfterSaleVersionRecordPreview() {
    val viewModel: AfterSaleVersionRecordViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.records.value = AppSampleUtils.genVersionInfos()
        viewModel.viewState.value = ViewState.LoadSuccess()

//        viewModel.actionState.value =
//            ActionState(
//                event = AfterSaleVersionRecordViewModel.EVT_DETAIL,
//                payload = AppSampleUtils.genVersionInfo()
//            )
        viewModel.actionState.value =
            ActionState(
                event = AfterSaleVersionRecordViewModel.EVT_UPGRADE_ING,
                msg = "正在升级中，请稍后"
            )
    }
    val navController = rememberNavController()
    AppPreviewWrapper {
        AfterSaleVersionRecord(navController, viewModel)
    }
}