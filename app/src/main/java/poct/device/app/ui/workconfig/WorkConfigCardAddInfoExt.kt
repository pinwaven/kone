package poct.device.app.ui.workconfig


import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppDatePicker
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppTextField
import poct.device.app.state.FieldStateHolder

@Composable
fun WorkConfigCardAddInfoExt(
    bean: State<CardConfigBean>,
    fieldStateHolder: State<FieldStateHolder>,
    onRecordUpdate: (newRecord: CardConfigBean) -> Unit,
) {
    if (!bean.value.showDetail) {
        return
    }

    val labelWidth = 130.dp

    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_type),
        fieldState = fieldStateHolder.value.get("type"),
        labelWidth = labelWidth
    ) { _ ->
        AppTextField(
            value = bean.value.type,
            readOnly = true,
        )
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_prod_date),
        labelWidth = labelWidth
    ) {
        AppDatePicker(
            value = bean.value.prodDate,
            onValueChange = { onRecordUpdate(bean.value.copy(prodDate = it)) }
        )
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_end_date),
        labelWidth = labelWidth
    ) {
        AppDatePicker(
            value = bean.value.endDate,
            onValueChange = { onRecordUpdate(bean.value.copy(endDate = it)) }
        )
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_scan_start),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = bean.value.scanStart,
            onValueChange = { onRecordUpdate(bean.value.copy(scanStart = it)) })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_scan_end),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = bean.value.scanEnd,
            onValueChange = { onRecordUpdate(bean.value.copy(scanEnd = it)) })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_scan_ppmm),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = bean.value.scanPPMM,
            onValueChange = { onRecordUpdate(bean.value.copy(scanPPMM = it)) })
    }
    AppDivider()
    if (bean.value.type == CaseBean.TYPE_CRP || bean.value.type == CaseBean.TYPE_SF
        || bean.value.type == CaseBean.TYPE_2LJ_A
    ) {
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_ft0),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.ft0,
                onValueChange = { onRecordUpdate(bean.value.copy(ft0 = it)) })
        }
        AppDivider()
    }
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_xt1),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = bean.value.xt1,
            onValueChange = { onRecordUpdate(bean.value.copy(xt1 = it)) })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_ft1),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = bean.value.ft1,
            onValueChange = { onRecordUpdate(bean.value.copy(ft1 = it)) })
    }
    if (bean.value.type != CaseBean.TYPE_4LJ && bean.value.type != CaseBean.TYPE_3LJ) {
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_score),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.typeScore,
                onValueChange = { onRecordUpdate(bean.value.copy(typeScore = it)) })
        }
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_scope),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = bean.value.scope,
            onValueChange = { onRecordUpdate(bean.value.copy(scope = it)) })
    }
    AppDivider()
    if (bean.value.type != CaseBean.TYPE_2LJ_A && bean.value.type != CaseBean.TYPE_2LJ_B) {
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_c_avg),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cAvg,
                onValueChange = { onRecordUpdate(bean.value.copy(cAvg = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_c_std),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cStd,
                onValueChange = { onRecordUpdate(bean.value.copy(cStd = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_c_min),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cMin,
                onValueChange = { onRecordUpdate(bean.value.copy(cMin = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_c_max),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cMax,
                onValueChange = { onRecordUpdate(bean.value.copy(cMax = it)) })
        }
        AppDivider()
    }
    if (bean.value.type == CaseBean.TYPE_4LJ || bean.value.type == CaseBean.TYPE_3LJ
        || bean.value.type == CaseBean.TYPE_2LJ_A || bean.value.type != CaseBean.TYPE_2LJ_B
    ) {
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff1),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff1,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff1 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff2),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff2,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff2 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff3),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff3,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff3 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff4),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff4,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff4 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoffMax),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOffMax,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOffMax = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff5),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff5,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff5 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff6),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff6,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff6 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff7),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff7,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff7 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_cutoff8),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.cutOff8,
                onValueChange = { onRecordUpdate(bean.value.copy(cutOff8 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_noise1),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.noise1,
                onValueChange = { onRecordUpdate(bean.value.copy(noise1 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_noise2),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.noise2,
                onValueChange = { onRecordUpdate(bean.value.copy(noise2 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_noise3),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.noise3,
                onValueChange = { onRecordUpdate(bean.value.copy(noise3 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_noise4),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.noise4,
                onValueChange = { onRecordUpdate(bean.value.copy(noise4 = it)) })
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_noise5),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = bean.value.noise5,
                onValueChange = { onRecordUpdate(bean.value.copy(noise5 = it)) })
        }
        AppDivider()
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Preview
@Composable
fun WorkConfigCardAddInfoExtPreview() {
    val viewModel: WorkConfigCardAddViewModel = viewModel()
    val bean = viewModel.bean.collectAsState()
    val fieldStateHolder = viewModel.fieldStateHolder.collectAsState()
    AppPreviewWrapper {
        WorkConfigCardAddInfoExt(
            bean = bean,
            fieldStateHolder = fieldStateHolder,
            onRecordUpdate = {})
    }
}