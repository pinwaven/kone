package poct.device.app.ui.workconfig


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppTextField
import poct.device.app.state.FieldStateHolder
import poct.device.app.theme.primaryColor

@SuppressLint("UnrememberedMutableState")
@Composable
fun WorkConfigCardAddInfo(
    bean: State<CardConfigBean>,
    fieldStateHolder: State<FieldStateHolder>,
    onRecordUpdate: (newRecord: CardConfigBean) -> Unit,
    onCheck: () -> Unit,
) {
    val labelWidth = 130.dp

    Spacer(modifier = Modifier.height(14.dp))
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        text = stringResource(id = R.string.work_config_tab_info)
    )
    Spacer(modifier = Modifier.height(24.dp))
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_name),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.name,
                onValueChange = { onRecordUpdate(bean.value.copy(name = it.trim())) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_code),
            required = true,
            fieldState = fieldStateHolder.value.get("code"),
            labelWidth = labelWidth
        ) { fieldState ->
            AppTextField(
                value = bean.value.code,
                fieldState = fieldState,
                readOnly = AppParams.varCardConfigMode == "modify",
                onValueChange = { onRecordUpdate(bean.value.copy(code = it.trim())) })
        }
        AppDivider()
        if (AppParams.varCardConfigMode != "modify") {
            AppFilledButton(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                text = stringResource(id = R.string.btn_label_check),
                onClick = {
                    onCheck()
                },
            )
            AppDivider()
        }
        WorkConfigCardAddInfoExt(bean, fieldStateHolder, onRecordUpdate)
    }
}

@Preview
@Composable
fun WorkConfigCardAddInfoPreview() {
    val viewModel: WorkConfigCardAddViewModel = viewModel()
    val bean = viewModel.bean.collectAsState()
    val fieldStateHolder = viewModel.fieldStateHolder.collectAsState()
    AppPreviewWrapper {
        WorkConfigCardAddInfo(
            bean = bean,
            fieldStateHolder = fieldStateHolder,
            onRecordUpdate = {},
            onCheck = {})
    }
}