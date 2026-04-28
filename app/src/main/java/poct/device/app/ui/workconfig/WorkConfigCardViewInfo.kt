package poct.device.app.ui.workconfig


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.component.AppTextField


@Composable
fun WorkConfigCardViewInfo(
    record: CardConfigBean,
    onInfo: () -> Unit = {},
    onTop: () -> Unit = {},
    onVar: () -> Unit = {},
) {
    val labelWidth = 140.dp
    Spacer(modifier = Modifier.height(15.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        AppFilledButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = { onInfo() },
            text = stringResource(id = R.string.work_config_tab_info)
        )
        Spacer(modifier = Modifier.width(5.dp))
        AppOutlinedButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = { onTop() },
            text = stringResource(id = R.string.work_config_tab_top)
        )
        Spacer(modifier = Modifier.width(5.dp))
        AppOutlinedButton(
            modifier = Modifier
                .width(90.dp)
                .height(30.dp),
            onClick = { onVar() },
            text = stringResource(id = R.string.work_config_tab_var)
        )
    }
    Spacer(modifier = Modifier.height(20.dp))
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
                value = record.name,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_code),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.code,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_type),
            labelWidth = labelWidth
        ) { _ ->
            AppTextField(
                value = record.type,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_prod_date),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.prodDate,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_end_date),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.endDate,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_scan_start),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.scanStart,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_scan_end),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.scanEnd,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_scan_ppmm),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.scanPPMM,
                readOnly = true,
            )
        }
        AppDivider()
        if (record.type == CaseBean.TYPE_CRP || record.type == CaseBean.TYPE_SF || record.type == CaseBean.TYPE_2LJ_A) {
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_ft0),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.ft0,
                    readOnly = true,
                )
            }
            AppDivider()
        }
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_xt1),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.xt1,
                readOnly = true,
            )
        }
        AppDivider()
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_ft1),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.ft1,
                readOnly = true,
            )
        }
        AppDivider()
        if (record.type != CaseBean.TYPE_4LJ && record.type != CaseBean.TYPE_3LJ) {
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_score),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.typeScore,
                    readOnly = true,
                )
            }
            AppDivider()
        }
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_scope),
            labelWidth = labelWidth
        ) {
            AppTextField(
                value = record.scope,
                readOnly = true,
            )
        }
        AppDivider()
        if (record.type != CaseBean.TYPE_2LJ_A && record.type != CaseBean.TYPE_2LJ_B) {
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_c_avg),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cAvg,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_c_std),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cStd,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_c_min),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cMin,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_c_max),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cMax,
                    readOnly = true,
                )
            }
            AppDivider()
        }
        if (record.type == CaseBean.TYPE_4LJ || record.type == CaseBean.TYPE_3LJ
            || record.type == CaseBean.TYPE_2LJ_A || record.type == CaseBean.TYPE_2LJ_B
        ) {
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff1),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff1,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff2),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff2,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff3),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff3,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff4),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff4,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoffMax),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOffMax,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff5),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff5,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff6),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff6,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff7),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff7,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_cutoff8),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.cutOff8,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_noise1),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.noise1,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_noise2),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.noise2,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_noise3),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.noise3,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_noise4),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.noise4,
                    readOnly = true,
                )
            }
            AppDivider()
            AppFieldWrapper(
                text = stringResource(id = R.string.work_config_f_noise5),
                labelWidth = labelWidth
            ) {
                AppTextField(
                    value = record.noise5,
                    readOnly = true,
                )
            }
            AppDivider()
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}