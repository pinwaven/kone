package poct.device.app.ui.sysconfig

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.component.AppMenuCard
import poct.device.app.component.AppMenuCardItem
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar


/**
 * 页面定义
 */
@Composable
fun SysConfigMain(navController: NavController) {
    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = stringResource(id = R.string.sys_config),
                backEnabled = true
            )
        }
    ) {
        SysConfigMainBody(navController)
    }
}


/**
 * 内容主体
 */
@Composable
fun SysConfigMainBody(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
    ) {
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_config_sys)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_sys),
                    painter = painterResource(id = R.mipmap.xtpz_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_SYS) }
                )
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_config_inner),
//                    painter = painterResource(id = R.mipmap.nbsz_icon),
//                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_INNER) }
//                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_config_other)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_other_wlan),
                    painter = painterResource(id = R.mipmap.wlan_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_WLAN) }
                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_other_datetime),
                    painter = painterResource(id = R.mipmap.rqsj_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_DATETIME) }
                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_other_lang),
                    painter = painterResource(id = R.mipmap.yypz_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_LANG) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
            ) {
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_config_other_printer),
//                    painter = painterResource(id = R.mipmap.dyj_icon),
//                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_PRINTER) }
//                )
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_config_other_scanner),
                    painter = painterResource(id = R.mipmap.smq_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_CONFIG_SCANNER) }
                )
//                AppMenuCardItem(
//                    navController = navController,
//                    label = "",
//                    painter = null,
//                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


@Preview
@Composable
fun SysConfigMainPreview() {
    SysConfigMain(rememberNavController())
}