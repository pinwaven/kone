package poct.device.app.ui.work

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppSelect
import poct.device.app.component.AppTextField
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppFormUtils

@Composable
fun WorkMainActionCase4Block(
    bean: State<CaseBean>,
    onBeanUpdate: (newBean: CaseBean) -> Unit,
) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(horizontal = 15.dp),
            shape = RoundedCornerShape(8.dp),
            color = bgColor
        ) {
            // 标题
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = stringResource(id = R.string.work_case_input_batch),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(15.dp))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 15.dp, end = 15.dp, top = 17.5.dp),
                ) {
                    AppFieldWrapper(
                        text = stringResource(id = R.string.work_config_f_type),
                        required = true,
                    ) { _ ->
                        AppSelect(
                            value = bean.value.type,
                            options = AppDictUtils.caseTypeOptions(LocalContext.current),
                            onValueChange = {
                                onBeanUpdate(
                                    bean.value.copy(
                                        type = it
                                    )
                                )
                            }
                        )
                    }
                    AppDivider()
                    AppFieldWrapper(
                        text = stringResource(id = R.string.work_config_f_code)
                    ) {
                        AppTextField(
                            value = bean.value.reagentId,
                            onValueChange = {
                                onBeanUpdate(
                                    bean.value.copy(
                                        reagentId = AppFormUtils.regulateLength(it, 16)
                                    )
                                )
                            }
                        )
                    }
                    AppDivider()
                }
            }
        }
    }
}