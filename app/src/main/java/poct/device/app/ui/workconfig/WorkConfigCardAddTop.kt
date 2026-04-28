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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardTopBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppList
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppRadioGroup
import poct.device.app.component.AppTextField
import poct.device.app.state.ActionState
import poct.device.app.state.FieldStateHolder
import poct.device.app.theme.filledFontColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppSampleUtils
import timber.log.Timber

@Composable
fun WorkConfigCardAddTop(
    bean: State<CardConfigBean>,
    actionState: State<ActionState>,
    fieldStateHolder: State<FieldStateHolder>,
    onTopAdd: () -> Unit,
    onTopRemove: (record: CardTopBean) -> Unit,
    onRecordUpdate: (newRecord: CardConfigBean) -> Unit,
) {
    var records by remember { mutableStateOf(bean.value.topList) }
    val yesOrNoOptions = AppDictUtils.yesNoOptions(LocalContext.current)
    if (actionState.value.event == WorkConfigCardAddViewModel.EVT_ADD_TOP_DONE || actionState.value.event == WorkConfigCardAddViewModel.EVT_REMOVE_TOP_DONE) {
        records = bean.value.topList
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
            text = stringResource(id = R.string.work_config_tab_top)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = width - 64.dp)
                .clickable { onTopAdd() },
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
        var top by remember { mutableStateOf(it) }
        Timber.w("====${itIndex}")
        WorkConfigCarAddTopItem(
            top,
            fieldStateHolder,
            itIndex,
            labelWidth,
            yesOrNoOptions,
            onTopRemove,
        ) { index, cardTop ->
            val list = ArrayList(bean.value.topList)
            list.removeAt(index)
            list.add(index, cardTop)
            top = cardTop
            onRecordUpdate(bean.value.copy(topList = list))
        }
    }
}

@Composable
private fun WorkConfigCarAddTopItem(
    cardTop: CardTopBean,
    fieldStateHolder: State<FieldStateHolder>,
    itIndex: Int,
    labelWidth: Dp,
    yesOrNoOptions: Map<String, String>,
    onTopRemove: (record: CardTopBean) -> Unit,
    onItemUpdate: (index: Int, record: CardTopBean) -> Unit,
) {
    var start by remember { mutableStateOf(cardTop.start) }
    var end by remember { mutableStateOf(cardTop.end) }
    var ctrl by remember { mutableStateOf(cardTop.ctrl) }
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            text = stringResource(id = R.string.work_config_tab_top) + (itIndex + 1)
        )
        Spacer(modifier = Modifier.width(18.dp))
        Icon(
            modifier = Modifier
                .size(24.dp)
                .clickable { onTopRemove(cardTop) },
            painter = painterResource(id = R.mipmap.sc_icon),
            tint = filledFontColor,
            contentDescription = ""
        )
    }
    Spacer(modifier = Modifier.width(4.dp))
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_start),
        fieldState = fieldStateHolder.value.get("cardTopStart" + cardTop.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = start,
            onValueChange = {
                start = it
                onItemUpdate(itIndex, cardTop.copy(start = it))
            })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_end),
        fieldState = fieldStateHolder.value.get("cardTopEnd" + cardTop.id),
        labelWidth = labelWidth
    ) {
        AppTextField(
            value = end,
            onValueChange = {
                end = it
                onItemUpdate(itIndex, cardTop.copy(end = it))
            })
    }
    AppDivider()
    AppFieldWrapper(
        text = stringResource(id = R.string.work_config_f_top_ctrl),
        labelWidth = labelWidth
    ) {
        AppRadioGroup(
            value = ctrl, options = yesOrNoOptions,
            gap = 0.dp,
            onValueChange = {
                ctrl = it
                onItemUpdate(itIndex, cardTop.copy(ctrl = it))
            })
    }
    AppDivider()
    Spacer(modifier = Modifier.height(24.dp))
}

@Preview
@Composable
fun WorkConfigCardAddTopPreview() {
    val viewModel: WorkConfigCardAddViewModel = viewModel()
    val bean = viewModel.bean.collectAsState()
    val actionState = viewModel.actionState.collectAsState()
    val fieldStateHolder = viewModel.fieldStateHolder.collectAsState()
    LaunchedEffect(key1 = Unit) {
        viewModel.bean.value = AppSampleUtils.genCardInfo()
    }
    AppPreviewWrapper {
        WorkConfigCardAddTop(
            bean = bean,
            actionState = actionState,
            fieldStateHolder = fieldStateHolder,
            onTopAdd = {},
            onTopRemove = {},
            onRecordUpdate = {})
    }
}