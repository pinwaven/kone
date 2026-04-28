package poct.device.app.ui.sysconfig

import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import poct.device.app.App
import poct.device.app.R
import poct.device.app.bean.ConfigOtherBean
import poct.device.app.component.AppBottomBar
import poct.device.app.component.AppConfirm
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppRadioGroup
import poct.device.app.component.AppScaffold
import poct.device.app.component.AppTopBar
import poct.device.app.component.AppViewLoading
import poct.device.app.component.AppViewWrapper
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.theme.bgColor


/**
 * 页面定义
 */
@Composable
fun SysConfigLang(navController: NavController, viewModel: SysConfigLangViewModel = viewModel()) {
    val viewState by viewModel.viewState.collectAsState()
    val actionState by viewModel.actionState.collectAsState()
    val bean by viewModel.bean.collectAsState()
    val mode by viewModel.mode.collectAsState()
    LaunchedEffect(viewState) {
        if (viewState == ViewState.Default) {
            viewModel.onLoad()
        }
    }
    val title =
        if (mode == "view") stringResource(id = R.string.sys_config_other_lang)
        else stringResource(id = R.string.sys_config_other_lang_modify)
    AppViewWrapper(
        viewState = viewState,
        onErrorClick = { navController.popBackStack() }) {
        AppScaffold(
            topBar = {
                AppTopBar(
                    navController = navController,
                    title = title,
                    backEnabled = true,
                    onBack = {
                        if (mode == "view") {
                            navController.popBackStack()
                        } else {
                            viewModel.onBackConfirm()
                        }
                    }
                )
            },
            bottomBar = {
                SysConfigLangBottomBar(
                    mode = mode,
                    onModifyPre = { viewModel.onModifyPre() },
                    onBack = {
                        viewModel.onBackConfirm()
                    },
                    onSaveConfirm = { viewModel.onSaveConfirm() }
                )
            }
        ) {
            SysConfigLangForm(
                bean = bean,
                mode = mode,
                onBeanUpdate = { viewModel.onBeanUpdate(it) }
            )
        }
    }

    SysConfigLangTimeInteraction(
        actionState = actionState,
        onSave = { viewModel.onSave() },
        onClearInteraction = { viewModel.onClearInteraction() }
    ) {
        viewModel.onClearInteraction()
        viewModel.onBack()
        viewModel.onLoad()
    }
}


@Composable
fun SysConfigLangTimeInteraction(
    actionState: ActionState,
    onSave: () -> Unit,
    onClearInteraction: () -> Unit,
    onBackPage: () -> Unit,
) {
    if (actionState.event == SysConfigLangViewModel.EVT_EXIT) {
        AppConfirm(
            title = stringResource(id = R.string.confirm_title_remind),
            visible = true,
            content = stringResource(id = R.string.sys_config_other_lang_exit),
            onCancel = { onClearInteraction() },
            onConfirm = { onBackPage() }
        )
    } else if (actionState.event == SysConfigLangViewModel.EVT_LOADING) {
        AppViewLoading(actionState.msg)
    } else if (actionState.event == SysConfigLangViewModel.EVT_SAVE_CONFIRM) {
        AppConfirm(
            visible = true,
            content = stringResource(id = R.string.msg_lang_save_confirm),
            onCancel = onClearInteraction,
            onConfirm = onSave
        )
    }
}


val SysConfigLangOptions = mapOf(
    "cn" to App.getContext().getString(R.string.chinese),
    "en" to App.getContext().getString(R.string.english)
)

@Composable
private fun SysConfigLangForm(
    bean: ConfigOtherBean,
    mode: String,
    onBeanUpdate: (ConfigOtherBean) -> Unit,
) {
    val readOnly = mode == "view"
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(15.dp),
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(15.dp)
        ) {
            Spacer(modifier = Modifier.height(17.dp))
            SysConfigLangHeader(SysConfigLangOptions.getValue(bean.lang.ifEmpty { "cn" }))
            Spacer(modifier = Modifier.height(24.dp))
            val labelWidth = 60.dp
            AppFieldWrapper(
                text = stringResource(id = R.string.sys_config_other_lang_label),
                labelWidth = labelWidth
            ) {
                AppRadioGroup(
                    value = bean.lang.ifEmpty { "cn" },
                    readOnly = readOnly,
                    options = SysConfigLangOptions,
                    gap = 0.dp,
                    onValueChange = { onBeanUpdate(bean.copy(lang = it)) }
                )
            }
        }
    }
}

@Composable
fun SysConfigLangHeader(langLabel: String) {
    val format = stringResource(id = R.string.sys_config_other_lang_cur)
    val text = String.format(format, langLabel)
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Normal,
        text = text,
        fontSize = 16.sp
    )
}

@Composable
fun SysConfigLangBottomBar(
    mode: String,
    onModifyPre: () -> Unit,
    onBack: () -> Unit,
    onSaveConfirm: () -> Unit,
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
                onClick = { onSaveConfirm() },
                text = stringResource(id = R.string.btn_label_save)
            )
        }
    }
}


@Preview
@Composable
fun SysConfigLangPreview() {
    val viewModel: SysConfigLangViewModel = viewModel()
    val navController = rememberNavController()
    LaunchedEffect(key1 = Unit) {
        viewModel.viewState.value = ViewState.LoadSuccess()
    }
    AppPreviewWrapper {
        SysConfigLang(navController, viewModel)
    }
}