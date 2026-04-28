package poct.device.app.ui.sysconfig

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.App
import poct.device.app.R
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppList
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.dangerColor
import poct.device.app.theme.primaryColor
import poct.device.app.utils.app.AppEventUtils


/**
 * 页面定义
 */
@Composable
fun SysConfigScannerList(
    navController: NavController,
    viewModel: SysConfigScannerListViewModel = viewModel()
) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val devList = viewModel.devList.collectAsState()

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
    AppViewWrapper(viewState = viewState, onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.sys_config_other_scanner),
                    backEnabled = true,
                    onBack = {
                        if (navController.previousBackStackEntry != null) {
                            navController.previousBackStackEntry!!.savedStateHandle["updateFlag"] =
                                "true"
                        }
                        navController.popBackStack()
                    }
                )
            },
            bottomBar = {
                AppBottomBar {
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(40.dp),
                        onClick = { viewModel.onLoad() },
                        text = stringResource(id = R.string.btn_label_refresh)
                    )
                }
            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, end = 15.dp, top = 15.dp, bottom = 15.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 15.dp)
                ) {
//                    SysConfigWlanHeader(
//                        config,
//                        curWlan,
//                        onConfigUpdated = { viewModel.onConfigUpdated(it) })
                    AppList(
                        modifier = Modifier.fillMaxSize(),
                        records = devList.value
                    ) { it ->
                        if (ActivityCompat.checkSelfPermission(
                                App.getContext(),
                                Manifest.permission.BLUETOOTH_ADMIN
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return@AppList
                        }
                        AppFieldWrapper(text = it.name, labelWidth = 170.dp) { _ ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Spacer(modifier = Modifier.width(15.dp))
                                if (it.connected) {
                                    AppFilledButton(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(30.dp),
                                        onClick = { viewModel.createOrRemoveBond(2, it.device) },
                                        textColor = primaryColor,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = dangerColor
                                        ),
                                        text = stringResource(id = R.string.btn_label_disconnect)
                                    )
                                } else {
                                    AppFilledButton(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .height(30.dp),
                                        onClick = { viewModel.createOrRemoveBond(1, it.device) },
                                        text = stringResource(id = R.string.btn_label_connect)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun SysConfigScannerListPreview() {
    val navController = rememberNavController()
    val viewModel: SysConfigScannerListViewModel = viewModel()

    LaunchedEffect(null) {
        viewModel.viewState.value = ViewState.LoadSuccess()
        viewModel.actionState.value = ActionState.Default
    }

    AppPreviewWrapper {
        SysConfigScannerList(navController, viewModel)
    }
}