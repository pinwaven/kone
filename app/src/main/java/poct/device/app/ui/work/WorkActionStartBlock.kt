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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.component.AppDatePicker
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppRadioGroup
import poct.device.app.component.AppTextField
import poct.device.app.entity.User
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppFormUtils


@Composable
fun WorkMainActionStartBlock(
    sysConfig: ConfigSysBean,
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
                    text = stringResource(id = R.string.work_info),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(65.dp))
                // TODO 简化信息
//                Image(
//                    modifier = Modifier
//                        .width(102.dp)
//                        .height(70.dp),
//                    painter = painterResource(id = R.mipmap.icon_saoma),
//                    contentDescription = ""
//                )
//                Spacer(modifier = Modifier.height(17.5.dp))
//                Text(
//                    modifier = Modifier.padding(10.dp, 0.dp, 10.dp, 0.dp),
//                    text = stringResource(id = R.string.work_info_tip),
//                    textAlign = TextAlign.Center,
//                    fontSize = 12.sp,
//                    color = tipFontColor
//                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 15.dp, end = 15.dp, top = 10.dp),
                ) {
                    AppFieldWrapper(
                        text = stringResource(id = R.string.work_f_name),
                        height = 40.dp
                    ) {
                        AppTextField(
                            value = bean.value.name,
                            onValueChange = {
                                onBeanUpdate(
                                    bean.value.copy(
                                        name = AppFormUtils.regulateLength(
                                            it,
                                            16
                                        )
                                    )
                                )
                            }
                        )
                    }
                    AppDivider()
                    AppFieldWrapper(
                        text = stringResource(id = R.string.work_f_gender),
                        height = 40.dp
                    ) {
                        val options = AppDictUtils.genderOptions(LocalContext.current)
                        AppRadioGroup(
                            value = bean.value.gender,
                            options = options,
                            onValueChange = { onBeanUpdate(bean.value.copy(gender = it)) }
                        )
                    }
                    AppDivider()
                    AppFieldWrapper(
                        text = stringResource(id = R.string.work_f_birthday),
                        height = 40.dp
                    ) {
                        AppDatePicker(
                            value = bean.value.birthday,
                            onValueChange = { onBeanUpdate(bean.value.copy(birthday = it)) }
                        )
                    }
                    AppDivider()
                    if (sysConfig.scan == "n"
                        && AppParams.curUser.role == User.ROLE_DEV
                    ) {
                        AppFieldWrapper(
                            text = stringResource(id = R.string.work_qrcode),
                            height = 40.dp
                        ) {
                            AppTextField(
                                value = bean.value.qrCode,
                                onValueChange = {
                                    onBeanUpdate(
                                        bean.value.copy(
                                            qrCode = AppFormUtils.regulateLength(it, 19)
                                        )
                                    )
                                }
                            )
                        }
                    }
                    // TODO 简化信息
//                    AppDivider()
//                    AppFieldWrapper(
//                        text = stringResource(id = R.string.work_f_type),
//                        height = 40.dp
//                    ) {
//                        val options = AppDictUtils.bloodTypeOptions(LocalContext.current)
//                        AppRadioGroup(
//                            value = bean.value.caseType,
//                            options = options,
//                            onValueChange = { onBeanUpdate(bean.value.copy(caseType = it)) }
//                        )
//                    }
//                    AppDivider()
                }

            }
        }
    }
}

@Preview
@Composable
fun WorkMainActionStartBlockPreview(viewModel: WorkMainViewModel = viewModel()) {
    val sysConfig = ConfigSysBean(scan = "y")
    val bean = viewModel.bean.collectAsState()
    LaunchedEffect(key1 = null) {
        viewModel.action.value = WorkMainViewModel.ACTION_START
    }
    WorkMainActionStartBlock(sysConfig, bean) { viewModel.onBeanUpdate(it) }
}