package poct.device.app.ui.workconfig

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.CardConfigBean
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppSampleUtils
import timber.log.Timber


/**
 * 页面定义
 */
@Composable
fun WorkConfigCardView(
    navController: NavController,
    viewModel: WorkConfigCardViewViewModel = viewModel(),
) {
    val viewState = viewModel.viewState.collectAsState()
    val stepState = viewModel.stepState.collectAsState()
    val bean = viewModel.bean.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.onLoad()
    }
    // 处理下级页面返回到当前页时的逻辑
    if (navController.currentBackStackEntry != null) {
        val updateFlag by navController.currentBackStackEntry!!.savedStateHandle
            .getStateFlow("updateFlag", "false").collectAsState()
        Timber.d("updateFlag=$updateFlag")
        LaunchedEffect(updateFlag) {
            if (updateFlag == "true") {
                viewModel.onLoad()
            }
        }
    }
    val title =
        if (AppParams.varCardConfigViewMode != "preview")
            stringResource(id = R.string.work_config_view_card)
        else
            stringResource(id = R.string.work_config_preview_card)
    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = title,
                    backEnabled = true
                )
            },
            bottomBar = {
                WorkConfigCardViewBottomBar(
                    onExit = {
                        navController.popBackStack()
                    },
                    onModify = {
                        viewModel.viewState.value = ViewState.Loading()
                        if (navController.previousBackStackEntry != null) {
                            navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                                "true"
                        }

                        bean.value.showDetail = true
                        AppParams.varCardConfig = bean.value
                        AppParams.varCardConfigMode = "modify"
                        navController.navigate(RouteConfig.WORK_CONFIG_CARD_ADD)
                        viewModel.viewState.value = ViewState.LoadSuccess()
                    },
                )
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, top = 15.dp, end = 15.dp),
                shape = RoundedCornerShape(4.dp),
                color = bgColor
            ) {
                WorkConfigCardViewBody(
                    stepValue = stepState,
                    bean = bean,
                    onInfo = { viewModel.onInfo() },
                    onTop = { viewModel.onTop() },
                    onVar = { viewModel.onVar() }
                )
            }
        }
    }
}


@Composable
fun WorkConfigCardViewBottomBar(
    onExit: () -> Unit,
    onModify: () -> Unit,
) {
    AppBottomBar {
        AppOutlinedButton(
            modifier = Modifier
                .width(120.dp)
                .height(40.dp),
            onClick = { onExit() },
            text = stringResource(id = R.string.btn_label_exit)
        )
        if (AppParams.varCardConfigViewMode != "preview") {
            Spacer(modifier = Modifier.width(10.dp))
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = { onModify() },
                text = stringResource(id = R.string.btn_label_modify)
            )
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun WorkConfigCardViewBody(
    stepValue: State<String>,
    bean: State<CardConfigBean>,
    onInfo: () -> Unit,
    onTop: () -> Unit,
    onVar: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 15.dp)
        ) {
            if (stepValue.value == WorkConfigCardViewViewModel.STEP_INFO) {
                WorkConfigCardViewInfo(bean.value, onTop = onTop, onVar = onVar)
            } else if (stepValue.value == WorkConfigCardViewViewModel.STEP_TOP) {
                WorkConfigCardViewTop(bean.value, onInfo = onInfo, onVar = onVar)
            } else if (stepValue.value == WorkConfigCardViewViewModel.STEP_VAR) {
                WorkConfigCardViewVar(bean.value, onInfo = onInfo, onTop = onTop)
            }
        }
    }
}


@Preview
@Composable
fun WorkConfigCardViewPreview() {
    val viewModel: WorkConfigCardViewViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.bean.value = AppSampleUtils.genCardInfo(5)
        viewModel.viewState.value = ViewState.LoadSuccess()
        viewModel.stepState.value = WorkConfigCardViewViewModel.STEP_INFO
    }
    WorkConfigCardView(rememberNavController())
}