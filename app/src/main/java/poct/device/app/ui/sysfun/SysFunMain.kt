package poct.device.app.ui.sysfun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import poct.device.app.ui.home.HomeWorkPre


/**
 * 页面定义
 */
@Composable
fun SysFunMain(navController: NavController) {
    AppScaffold(
        topBar = {
            AppTopBar(
                navController = navController,
                title = stringResource(id = R.string.sys_fun),
                backEnabled = true
            )
        }
    ) {
        SysFunMainBody(navController)
    }
}


/**
 * 内容主体
 */
@Composable
fun SysFunMainBody(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp)
    ) {
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_fun_xtpz)
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.sys_fun_xtpz_info),
                    painter = painterResource(id = R.mipmap.xtxx_icon),
                    onClick = { navController.navigate(RouteConfig.SYS_FUN_INFO) }
                )
                // TODO 简化信息
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_fun_xtpz_user),
//                    painter = painterResource(id = R.mipmap.yhgl_icon),
//                    onClick = { navController.navigate(RouteConfig.SYS_FUN_USER) }
//                )
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_fun_xtpz_mode),
//                    painter = painterResource(id = R.mipmap.yxms_icon),
//                    onClick = { AppToastUtil.devShow() }
//                )
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        AppMenuCard(
            navController = navController,
            title = stringResource(id = R.string.sys_fun_sbjz)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_fun_sbjz_wzjz),
//                    painter = painterResource(id = R.mipmap.wzjz_icon),
//                    onClick = { AppToastUtil.devShow() }
//                )
                var workPreVisible by remember { mutableStateOf(false) }
                AppMenuCardItem(
                    navController = navController,
                    label = stringResource(id = R.string.home_work_pre_title),
                    painter = painterResource(id = R.mipmap.sbcsh_icon),
                    onClick = {
                        workPreVisible = true
                    }
                )
                HomeWorkPre(
                    visible = workPreVisible,
                    onClose = { workPreVisible = false },
                    onOk = {
                        workPreVisible = false
                        navController.navigate(RouteConfig.WORK)
                    }
                )
                // TODO 简化信息
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_fun_sbjz),
//                    painter = painterResource(id = R.mipmap.jgjz_icon),
//                    onClick = { navController.navigate(RouteConfig.SYS_FUN_ADJUST) }
//                )
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_fun_sbjz_jdjz),
//                    painter = painterResource(id = R.mipmap.jcjdjz_icon),
//                    onClick = { AppToastUtil.devShow() }
//                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
        // TODO 简化信息
//        Spacer(modifier = Modifier.height(12.dp))
//        AppMenuCard(
//            navController = navController,
//            title = stringResource(id = R.string.sys_fun_jkgl)
//        ) {
//            Spacer(modifier = Modifier.height(24.dp))
//            Row(
//                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
//            ) {
//                AppMenuCardItem(
//                    navController = navController,
//                    label = stringResource(id = R.string.sys_fun_jkgl_lis),
//                    painter = painterResource(id = R.mipmap.lis_icon),
//                    onClick = { AppToastUtil.devShow() }
//                )
//            }
//            Spacer(modifier = Modifier.height(20.dp))
//        }
    }
}


@Preview
@Composable
fun SysFunMainPreview() {
    SysFunMain(rememberNavController())
}