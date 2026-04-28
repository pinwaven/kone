package poct.device.app.ui.sysconfig

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.ScannerInfo
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ViewState
import poct.device.app.theme.bg2Color
import poct.device.app.theme.bgColor
import poct.device.app.theme.tipFontColor
import poct.device.app.utils.app.AppEventUtils


/**
 * 页面定义
 */
@Composable
fun SysConfigScanner(navController: NavController, viewModel: SysConfigScannerViewModel = viewModel()) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val scannerList = viewModel.devList.collectAsState()

    DisposableEffect(key1 = Unit) {
        AppEventUtils.register(viewModel)
        onDispose {
            AppEventUtils.unregister(viewModel)
        }
    }

    LaunchedEffect(viewState.value) {
        if (viewState.value == ViewState.Default) {
            viewModel.onLoad()
        }
    }

    // 处理下级页面返回到当前页时的逻辑
    if (navController.currentBackStackEntry != null) {
        val updateFlag by navController.currentBackStackEntry!!.savedStateHandle
            .getStateFlow("updateFlag", "false").collectAsState()
        LaunchedEffect(updateFlag) {
            if (updateFlag == "true") {
                viewModel.onLoad()
            }
        }
    }

    AppViewWrapper(viewState = viewState, onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.sys_config_other_scanner),
                    backEnabled = true
                )
            },
            bottomBar = {
                SysConfigScannerBottomBar(
                    scannerList=scannerList,
                    onAddScanner = {
                        navController.navigate(RouteConfig.SYS_CONFIG_SCANNER_LIST)
                    }
                )
            }
        ) {
            SysConfigScannerBody(
                scannerList=scannerList
            )
        }
    }
}

@Composable
private fun SysConfigScannerBody(
    scannerList: State<List<ScannerInfo>>
) {
    val text =
        if (scannerList.value.isNotEmpty()) stringResource(id = R.string.sys_config_other_scanner_connected)
        else stringResource(id = R.string.sys_config_other_scanner_disconnected)
    val image =
        if (scannerList.value.isNotEmpty()) painterResource(id = R.mipmap.icon_saoma)
        else painterResource(id = R.mipmap.icon_saoma)
    val state =
        if (scannerList.value.isNotEmpty()) painterResource(id = R.mipmap.ylj_img)
        else painterResource(id = R.mipmap.wlj_icon)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp)
            .background(bgColor),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(54.dp))
            BoxWithConstraints(
                modifier = Modifier
                    .width(125.dp)
                    .height(112.dp)
            ) {
                val width = this.minWidth
                val height = this.minHeight
                Box(modifier = Modifier) {
                    Image(painter = image, contentDescription = "")
                }
                Box(
                    modifier = Modifier.padding(
                        start = width - 24.dp - 9.dp, top = height - 24.dp
                    )
                ) {
                    Image(painter = state, contentDescription = "")
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(text = text, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(4.dp))
            if(scannerList.value.isEmpty()) {
                Text(text = stringResource(id = R.string.sys_config_other_scanner_remind), fontSize = 14.sp, color = tipFontColor)
            }else{
                Text(text = scannerList.value[0].name, fontSize = 14.sp, color = tipFontColor)
            }
        }
    }
}

@Composable
fun SysConfigScannerBottomBar(
    scannerList: State<List<ScannerInfo>>,
    onAddScanner: ()-> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.5.dp)
            .background(bg2Color)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 3.dp, end = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val text =
                if (scannerList.value.isNotEmpty()) stringResource(id = R.string.btn_label_edit_scanner)
                else stringResource(id = R.string.btn_label_add_scanner)
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = onAddScanner,
                text = text
            )
        }
    }
}


@Preview
@Composable
fun SysConfigScannerPreview() {
    val viewModel: SysConfigScannerViewModel = viewModel()
    Box(modifier = Modifier.fillMaxSize()) {
        SysConfigScanner(rememberNavController(), viewModel)
    }
}