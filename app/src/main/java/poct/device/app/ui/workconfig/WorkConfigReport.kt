package poct.device.app.ui.workconfig

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.App
import poct.device.app.R
import poct.device.app.RouteConfig
import poct.device.app.bean.ConfigReportBean
import poct.device.app.component.AppAlert
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTextField
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppFormUtils
import poct.device.app.utils.app.AppSystemUtils
import timber.log.Timber
import java.io.File
import java.io.FileInputStream


/**
 * 页面定义
 */
@Composable
fun WorkConfigReport(navController: NavController, viewModel: WorkConfigReportViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val bean by viewModel.bean.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val showTemp by viewModel.showTemp.collectAsState()

    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    val title =
        if (mode == "view") stringResource(id = R.string.work_config_report)
        else stringResource(id = R.string.work_config_report_edit)
    AppViewWrapper(
        viewState = viewState,
        onErrorClick = {
            navController.navigate(RouteConfig.WORK_CONFIG_MAIN)
        }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = title,
                    backEnabled = true,
                    onBack = {
                        if (mode == "view") {
                            navController.navigate(RouteConfig.WORK_CONFIG_MAIN)
                        } else {
                            viewModel.onBackConfirm()
                        }
                    }
                )
            },
            bottomBar = {
                WorkConfigReportBottomBar(
                    mode = mode,
                    onModifyPre = { viewModel.onModifyPre() },
                    onBack = {
                        viewModel.onBackConfirm()
                    },
                    onModify = { viewModel.onSave() }
                )
            }
        ) {
            WorkConfigReportBody(
                bean = bean,
                mode = mode,
                showTemp = showTemp,
                onCopyImageToAssets = { context: Context, imgUri: Uri ->
                    viewModel.onCopyImageToAssets(context, imgUri)
                },
                onShowTempUpdate = {viewModel.onShowTempUpdate(it)},
                onBeanUpdate = { viewModel.onBeanUpdate(it) }
            )
        }
    }

    WorkConfigReportInteraction(
        actionState = actionState,
        onBackPage= {
            viewModel.onClearInteraction()
            viewModel.onBack()
            viewModel.onLoad()
        },
        onSaveSuccess= {viewModel.onSaveSuccess()},
        onClearInteraction = { viewModel.onClearInteraction() }
    )
}


@Composable
fun WorkConfigReportInteraction(
    actionState: ActionState,
    onBackPage: () -> Unit,
    onSaveSuccess: () -> Unit,
    onClearInteraction: () -> Unit,
) {
    if (actionState.event == WorkConfigReportViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.work_report_edit_exit_confirm),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    } else if (actionState.event == WorkConfigReportViewModel.EVT_LOADING) {
        AppViewLoading(actionState.msg)
    } else if (actionState.event == WorkConfigReportViewModel.EVT_SAVE_DONE) {
        AppAlert(
            visible = true,
            content = stringResource(id = R.string.save_ok),
            onOk = { onSaveSuccess() }
        )
    } else if (actionState.event == WorkConfigReportViewModel.EVT_ERROR) {
        actionState.msg?.let {
            AppAlert(
                visible = true,
                content = it,
                onOk = { onClearInteraction() }
            )
        }
    }
}

/**
 * 内容主体
 */
@Composable
fun WorkConfigReportBody(
    bean: ConfigReportBean,
    mode: String,
    showTemp: Boolean,
    onShowTempUpdate: (Boolean) -> Unit,
    onCopyImageToAssets: (Context, Uri) -> Unit,
    onBeanUpdate: (ConfigReportBean) -> Unit,
) {
    val readOnly = mode == "view"
    val context = LocalContext.current
    val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 15.dp, end = 15.dp, top = 15.dp)
    ) {
        val labelWidth = 100.dp
        val resultLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) {
            if (it.resultCode == android.app.Activity.RESULT_OK) {
                // 复制图片到assets目录
                val imageUri = it.data?.data
                imageUri?.let { uri ->
                    Timber.w("====imageUrl${uri}")
                    onCopyImageToAssets(context, uri)
                }
            }
        }
        var logo: Bitmap
        val tempFile = File(AppFileUtils.getLogoUrlTemp())
        Timber.w("====tempFile${tempFile.exists()}")
        if(tempFile.exists()) {
            FileInputStream(AppFileUtils.getLogoUrlTemp()).use {
                logo = BitmapFactory.decodeStream(it)
            }
        } else{
            val logoFile = File(AppFileUtils.getLogoUrl())
            if(logoFile.exists()) {
                FileInputStream(AppFileUtils.getLogoUrl()).use {
                    logo = BitmapFactory.decodeStream(it)
                }
            }else{
                App.getContext().assets.open("report_template/logo.png").use {
                    logo = BitmapFactory.decodeStream(it)
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor)
                .padding(horizontal = 15.dp)
        ) {
            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = stringResource(id = R.string.work_config_report_hos),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.work_config_report_hos_name)
            )
            {
                AppTextField(
                    value = bean.hosName,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(hosName = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                height = 100.dp,
                labelWidth = labelWidth,
                text = stringResource(id = R.string.work_config_report_hos_logo)
            )
            {
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(60.dp)
                ) {
                    Image(
                        modifier = Modifier.fillMaxWidth(),
                        bitmap = logo.asImageBitmap(),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                }
                if(!readOnly) {
                    Spacer(modifier = Modifier.width(24.dp))
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(60.dp)
                            .clickable {
                                onShowTempUpdate(false)
                                // 直接启动图片选择器
                                // 实现选择图片的Intent，这里以调用相册为例
                                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*")
                                resultLauncher.launch(intent)
                                // 创建浮窗
                                AppSystemUtils.createFloatingWindow(context, windowManager)
                            }
                    ) {
                        Image(
                            modifier = Modifier.fillMaxWidth(),
                            painter = painterResource(id = R.mipmap.tjtp_img),
                            contentScale = ContentScale.Crop,
                            contentDescription = null
                        )
                    }
                }
            }
            AppDivider()
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(id = R.string.work_config_report_info),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.work_config_report_info_ige)
            ) {
                AppTextField(
                    value = bean.igeName,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(igeName = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.work_config_report_info_crp)
            ) {
                AppTextField(
                    value = bean.crpName,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(crpName = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            AppFieldWrapper(
                labelWidth = labelWidth,
                text = stringResource(id = R.string.work_config_report_info_4lj)
            ) {
                AppTextField(
                    value = bean.cljName,
                    readOnly = readOnly,
                    onValueChange = {
                        onBeanUpdate(
                            bean.copy(cljName = AppFormUtils.regulateLength(it, 16))
                        )
                    }
                )
            }
            AppDivider()
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
@Composable
fun WorkConfigReportBottomBar(
    mode: String,
    onModifyPre: () -> Unit,
    onBack: () -> Unit,
    onModify: () -> Unit,
) {
    AppBottomBar {
        if (mode == "view") {
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = { onModifyPre() },
                text = stringResource(id = R.string.btn_label_modify)
            )
        } else {
            AppOutlinedButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = {
                    onBack()
                }, text = stringResource(id = R.string.back)
            )
            Spacer(modifier = Modifier.width(10.dp))
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = { onModify() },
                text = stringResource(id = R.string.btn_label_save)
            )
        }
    }
}


@Preview
@Composable
fun WorkConfigReportPreview() {
    val viewModel: WorkConfigReportViewModel = viewModel()
    LaunchedEffect(null) {
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    WorkConfigReport(rememberNavController())
}