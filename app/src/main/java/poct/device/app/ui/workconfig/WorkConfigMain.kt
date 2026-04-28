package poct.device.app.ui.workconfig

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
fun WorkConfigMain(navController: NavController) {
    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = stringResource(id = R.string.work_config),
                backEnabled = true
            )
        }
    ) {
        WorkConfigMainBody(navController)
    }
}


/**
 * 内容主体
 */
@Composable
fun WorkConfigMainBody(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.work_config_card)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.work_config_card),
                    painter = painterResource(id = R.mipmap.sjk_icon),
                    onClick = { navController.navigate(RouteConfig.WORK_CONFIG_CARD) }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Spacer(modifier = Modifier.height(12.dp))
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.work_report_result)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.work_config_report),
                    painter = painterResource(id = R.mipmap.bgpz_icon),
                    onClick = { navController.navigate(RouteConfig.WORK_CONFIG_REPORT) }
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}


@Preview
@Composable
fun WorkConfigMainPreview() {
    WorkConfigMain(rememberNavController())
}