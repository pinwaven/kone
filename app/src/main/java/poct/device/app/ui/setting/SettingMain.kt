package poct.device.app.ui.setting

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.component.AppMenuCard
import poct.device.app.component.AppMenuCardItem
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.entity.User
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.ui.home.HomeWorkPre

/**
 * 页面定义
 */
@Composable
fun SettingMain(
    navController: NavController,
    viewModel: SettingMainViewModel = viewModel(),
) {
    val viewState = viewModel.viewState.collectAsState()
    LaunchedEffect(viewState.value) {
        if (viewState.value == ViewState.Default) {
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
                    title = "",
                    homeEnabled = true,
                )
            },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor)
            ) {
                Column {
                    SettingMainBody(navController, viewModel)
                }
            }
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun SettingMainBody(
    navController: NavController,
    viewModel: SettingMainViewModel
) {
    val workPreVisible = viewModel.workPreVisible.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
    ) {
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_fun_menu)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_other_wlan),
                    painter = painterResource(id = R.mipmap.wlan_icon),
                    onClick = {
                        viewModel.sysWifiConfig()
                    }
                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_fun_menu_device),
                    painter = painterResource(id = R.mipmap.xtxx_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_FUN_INFO) }
                )
                // TODO 简化信息
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.report_main),
//                    painter = painterResource(id = R.mipmap.ybjc_ckbg_icon_def),
//                    onClick = { navController.navigate(RouteConfig.REPORT) }
//                )
//
//                if (AppParams.curUser.role != User.ROLE_CHECKER) {
//                    AppMenuCardItem(
//                        navController = navController,
//                        label = stringResource(id = R.string.sys_fun_xtpz_user),
//                        painter = painterResource(id = R.mipmap.yhgl_icon),
//                        onClick = { navController.navigate(RouteConfig.SYS_FUN_USER) }
//                    )
//                }
            }

//            if (AppParams.curUser.role == User.ROLE_DEV) {
//                Spacer(modifier = Modifier.height(24.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceEvenly
//                ) {
//                    AppMenuCardItem(
//                        navController = navController,
//                        label = stringResource(id = R.string.work_config),
//                        painter = painterResource(id = R.mipmap.sjk_icon),
//                        onClick = { viewModel.onWorkConfigCard(navController) }
//                    )
//                }
//            }

            Spacer(modifier = Modifier.height(20.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_fun_system)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_sys),
                    painter = painterResource(id = R.mipmap.nbsz_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_SYS_COMBINE) }
                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.home_work_pre_title),
                    painter = painterResource(id = R.mipmap.sbcsh_icon),
                    onClick = { viewModel.workPreVisible.value = true }
                )

                if (AppParams.curUser.role != User.ROLE_CHECKER) {
                    AppMenuCardItem(
                        navController = navController,
                        label = stringResource(id = R.string.after_sale_temp),
                        painter = painterResource(id = R.mipmap.lscz_icon),
                        onClick = {
                            navController.navigate(RouteConfig.SAMPLE_SERIAL)
                        }
                    )
                }

                HomeWorkPre(
                    visible = workPreVisible.value,
                    onClose = { viewModel.workPreVisible.value = false },
                    onOk = {
                        viewModel.workPreVisible.value = false
                    }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Preview
@Composable
fun SettingMainPreview() {
    AppParams.curUser.role = "dev"
    AppPreviewWrapper {
        SettingMain(rememberNavController())
    }
}