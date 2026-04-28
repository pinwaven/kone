package poct.device.app.ui.workconfig

import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppQRCodeUtils
import poct.device.app.utils.app.AppSampleUtils


/**
 * 页面定义
 */
@Composable
fun WorkConfigCardQrCode(
    navController: NavController,
    viewModel: WorkConfigCardQrCodeViewModel = viewModel(),
) {
    val viewState = viewModel.viewState.collectAsState()
    val curContent = viewModel.curContent.collectAsState()

    val bitmap = AppQRCodeUtils.createQRCodeBitmap(curContent.value, 260, 260, "UTF-8", "H", "0", Color.BLACK, Color.WHITE)

    LaunchedEffect(Unit) {
        viewModel.onLoad()
    }

    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = stringResource(id = R.string.work_config_view_qr),
                    backEnabled = true
                )
            },
//            bottomBar = {
//                WorkConfigCardQrCodeBottomBar(
//                    onExit = {
//                        navController.popBackStack()
//                    },
//                    onModify = {
//                        viewModel.onLoad()
//                    },
//                )
//            }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize(),
                shape = RoundedCornerShape(4.dp),
                color = bgColor
            ) {
                Image(
                    modifier = Modifier
                        .width(260.dp)
                        .height(260.dp),
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = ""
                )
            }
        }
    }
}

@Composable
fun WorkConfigCardQrCodeBottomBar(
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


@Preview
@Composable
fun WorkConfigCardQrCodePreview() {
    val viewModel: WorkConfigCardQrCodeViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.bean.value = AppSampleUtils.genCardInfo(5)
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    WorkConfigCardQrCode(rememberNavController())
}