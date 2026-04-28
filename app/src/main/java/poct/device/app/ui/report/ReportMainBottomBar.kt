package poct.device.app.ui.report


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppCheckbox
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.theme.bg2Color
import poct.device.app.theme.dangerColor
import poct.device.app.theme.primaryColor

/**
 * 页面底部按钮
 */
@Composable
fun ReportMainBottomBar(
    records: State<List<CaseBean>>,
    selected: State<List<String>>,
    onCheckAll: () -> Unit,
    onDelMoreConfirm: () -> Unit,
    onExportMoreConfirm: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.5.dp)
            .background(bg2Color)
            .padding(start = 3.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        AppCheckbox(
            text = "",
            checked = (records.value.isNotEmpty() && records.value.size == selected.value.size),
            onCheckedChange = { onCheckAll() }
        )
        Spacer(modifier = Modifier.width(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                fontSize = 12.sp,
                textColor = primaryColor,
                colors = ButtonDefaults.buttonColors(
                    containerColor = dangerColor
                ),
                contentPadding = PaddingValues(0.dp),
                onClick = { onDelMoreConfirm() },
                text = stringResource(id = R.string.btn_label_delete)
            )
            Spacer(modifier = Modifier.width(20.dp))
            AppFilledButton(
                modifier = Modifier
                    .width(120.dp)
                    .height(40.dp),
                onClick = { onExportMoreConfirm() },
                text = stringResource(id = R.string.btn_label_export),
            )
        }
    }
}

@Preview
@Composable
fun ReportMainBottomBarPreview() {
    val viewModel: ReportMainViewModel = viewModel()
    val records = viewModel.records.collectAsState()
    val selected = viewModel.selected.collectAsState()
    AppPreviewWrapper {
        ReportMainBottomBar(
            records = records,
            selected = selected,
            onCheckAll = { viewModel.onCheckAll() },
            onDelMoreConfirm = { viewModel.onDelMoreConfirm() },
            onExportMoreConfirm = { viewModel.onExportMoreConfirm() },
        )
    }
}