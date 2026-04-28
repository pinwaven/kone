package poct.device.app.ui.workconfig


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardVarBean
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppList
import poct.device.app.component.AppSelect
import poct.device.app.component.AppTextField
import poct.device.app.state.ActionState
import poct.device.app.state.FieldStateHolder
import poct.device.app.theme.filledFontColor
import poct.device.app.utils.app.AppDictUtils

@Composable
fun WorkConfigCardAddVar(
    cardConfigBean: State<CardConfigBean>,
    actionState: State<ActionState>,
    fieldStateHolder: State<FieldStateHolder>,
    onVarAdd: () -> Unit,
    onVarRemove: (record: CardVarBean) -> Unit,
    onRecordUpdate: (newRecord: CardConfigBean) -> Unit,
) {
    var records by remember { mutableStateOf(cardConfigBean.value.varList) }
    val caseType = cardConfigBean.value.type
    if (actionState.value.event == WorkConfigCardAddViewModel.EVT_ADD_VAR_DONE || actionState.value.event == WorkConfigCardAddViewModel.EVT_REMOVE_VAR_DONE) {
        records = cardConfigBean.value.varList
    }

    val labelWidth = 140.dp
    Spacer(modifier = Modifier.height(14.dp))
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        val width = this.maxWidth
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            text = stringResource(id = R.string.work_config_tab_var)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = width - 64.dp)
                .clickable { onVarAdd() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.mipmap.add_icon),
                tint = filledFontColor,
                contentDescription = ""
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(id = R.string.btn_label_add),
                color = filledFontColor
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))

    AppList(
        modifier = Modifier.fillMaxSize(),
        records = records
    ) { it, itIndex ->
        var var1 by remember { mutableStateOf(it) }
        WorkConfigCarAddVarItem(
            var1,
            fieldStateHolder,
            caseType,
            itIndex,
            labelWidth,
            onVarRemove,
        ) { index: Int, cardVar: CardVarBean ->
            val list = ArrayList(cardConfigBean.value.varList)
            list.removeAt(index)
            list.add(index, cardVar)
            var1 = cardVar
            onRecordUpdate(cardConfigBean.value.copy(varList = list))
        }
    }
}

@Composable
private fun WorkConfigCarAddVarItem(
    cardVar: CardVarBean,
    fieldStateHolder: State<FieldStateHolder>,
    caseType: String,
    itIndex: Int,
    labelWidth: Dp,
    onVarRemove: (record: CardVarBean) -> Unit,
    onItemUpdate: (index: Int, record: CardVarBean) -> Unit,
) {
    var start by remember { mutableStateOf(cardVar.start) }
    var end by remember { mutableStateOf(cardVar.end) }
    var type by remember { mutableStateOf(cardVar.type) }
    var x0 by remember { mutableStateOf(cardVar.x0) }
    var x1 by remember { mutableStateOf(cardVar.x1) }
    var x2 by remember { mutableStateOf(cardVar.x2) }
    var x3 by remember { mutableStateOf(cardVar.x3) }
    var x4 by remember { mutableStateOf(cardVar.x4) }

    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            text = stringResource(id = R.string.work_config_tab_var) + (itIndex + 1)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Icon(
            modifier = Modifier
                .size(24.dp)
                .clickable { onVarRemove(cardVar) },
            painter = painterResource(id = R.mipmap.sc_icon),
            tint = filledFontColor,
            contentDescription = ""
        )
    }
    Spacer(modifier = Modifier.width(4.dp))
    if (caseType == CaseBean.TYPE_CRP || caseType == CaseBean.TYPE_SF) {
        AppFieldWrapper(
            text = stringResource(id = R.string.work_config_f_crp_type),
            required = true,
            labelWidth = labelWidth
        ) { _ ->
            AppSelect(
                value = type,
                options = AppDictUtils.caseCrpTypeOptions(LocalContext.current),
                onValueChange = {
                    type = it
                    onItemUpdate(itIndex, cardVar.copy(type = it))
                }
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
    }
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_start),
        fieldState = fieldStateHolder.value.get("cardVarStart" + cardVar.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = start,
            onValueChange = {
                start = it
                onItemUpdate(itIndex, cardVar.copy(start = it))
            })
    }
    Spacer(modifier = Modifier.width(4.dp))
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_end),
        fieldState = fieldStateHolder.value.get("cardVarEnd" + cardVar.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = end,
            onValueChange = {
                end = it
                onItemUpdate(itIndex, cardVar.copy(end = it))
            })
    }
    Spacer(modifier = Modifier.width(4.dp))
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x0),
        fieldState = fieldStateHolder.value.get("cardVarX0" + cardVar.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = x0,
            onValueChange = {
                x0 = it
                onItemUpdate(itIndex, cardVar.copy(x0 = it))
            })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x1),
        fieldState = fieldStateHolder.value.get("cardVarX1" + cardVar.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = x1,
            onValueChange = {
                x1 = it
                onItemUpdate(itIndex, cardVar.copy(x1 = it))
            })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x2),
        fieldState = fieldStateHolder.value.get("cardVarX2" + cardVar.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = x2,
            onValueChange = {
                x2 = it
                onItemUpdate(itIndex, cardVar.copy(x2 = it))
            })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x3),
        fieldState = fieldStateHolder.value.get("cardVarX3" + cardVar.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = x3,
            onValueChange = {
                x3 = it
                onItemUpdate(itIndex, cardVar.copy(x3 = it))
            })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_var_x4),
        fieldState = fieldStateHolder.value.get("cardVarX4" + cardVar.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = x4,
            onValueChange = {
                x4 = it
                onItemUpdate(itIndex, cardVar.copy(x4 = it))
            })
    }
    AppDivider()
    Spacer(modifier = Modifier.height(24.dp))
}