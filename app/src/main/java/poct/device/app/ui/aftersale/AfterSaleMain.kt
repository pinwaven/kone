package poct.device.app.ui.aftersale

import android.content.Context
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppMenuCard
import poct.device.app.component.AppMenuCardItem
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.state.ActionState
import poct.device.app.utils.app.AppSystemUtils


/**
 * 页面定义
 */
@Composable
fun AfterSaleMain(
    navController: NavController,
    viewModel: AfterSaleViewModel = viewModel(),
) {
    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = stringResource(id = R.string.after_sale),
                backEnabled = true
            )
        }
    ) {
        AfterSaleMainBody(navController, viewModel)
    }

}

/**
 * 内容主体
 */
@Composable
private fun AfterSaleMainBody(navController: NavController, viewModel: AfterSaleViewModel) {
    val viewState = viewModel.viewState.collectAsState()
    val actionState = viewModel.actionState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
    ) {
        // TODO 简化信息
//        AppMenuCard(
//            navController = navController,
//            title = stringResource(id = R.string.after_sale_service)
//        ) {
//            Spacer(modifier = Modifier.height(24.dp))
//            // 2个，两边对齐
//            Row(
//                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
//            ) {
//                // TODO 简化信息
////                AppMenuCardItem(
////                    navController = navController,
////                    label = stringResource(id = R.string.after_sale_version_upgrade),
////                    painter = painterResource(id = R.mipmap.bbgx_icon),
////                    onClick = { navController.navigate(RouteConfig.AFTER_SALE_VERSION_UPGRADE) }
////                )
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.after_sale_space_free),
//                    painter = painterResource(id = R.mipmap.kjyh_icon),
//                    onClick = { AppToastUtil.devShow() }
//                )
//            }
//            Spacer(modifier = Modifier.height(20.dp))
//        }
//        Spacer(modifier = Modifier.height(12.dp))
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.after_sale_my_service)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            // 2个，两边对齐
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.after_sale_sun_login),
//                    painter = painterResource(id = R.mipmap.yccz_icon)
//                ) {
//                    viewModel.onUpgradeSunConfirm()
//                }
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.after_sale_temp),
                    painter = painterResource(id = R.mipmap.lscz_icon)
                ) {
                    navController.navigate(RouteConfig.SAMPLE_SERIAL)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
    AfterSaleInteraction(
        actionState = actionState,
        onUpgradeSunLogin = { viewModel.onUpgradeSunLogin() },
        onClearInteraction = { viewModel.onClearInteraction() }
    )
}

@Composable
private fun AfterSaleInteraction(
    actionState: State<ActionState>,
    onUpgradeSunLogin: () -> Unit,
    onClearInteraction: () -> Unit,
) {
    var context = LocalContext.current
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    when (actionState.value.event) {
        AfterSaleViewModel.SALE_SUN_LOGIN -> {
            AppConfirm(
                visible = true,
                content = stringResource(id = R.string.after_sale_sun_upgrade),
                onCancel = { onClearInteraction() },
                onConfirm = { onUpgradeSunLogin() }
            )
        }

        AfterSaleViewModel.SALE_SUN_BUT -> {
            // 创建浮窗
            AppSystemUtils.createFloatingWindow(context, windowManager)
            onClearInteraction()
        }
    }
}


@Preview
@Composable
fun AfterSaleMainPreview() {
    AfterSaleMain(rememberNavController())
}