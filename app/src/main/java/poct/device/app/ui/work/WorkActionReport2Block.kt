package poct.device.app.ui.work

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppPDFView
import poct.device.app.theme.bgColor
import poct.device.app.utils.app.AppSampleUtils


@Composable
fun WorkMainActionReport2Block(bean: State<CaseBean>) {
    Column {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(400.dp)
                .padding(horizontal = 15.dp),
            shape = RoundedCornerShape(8.dp),
            color = bgColor
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    AppPDFView(
                        modifier = Modifier.fillMaxSize(), pdfPath = bean.value.pdfPath
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun WorkMainActionReport2BlockPreview(viewModel: WorkMainViewModel = viewModel()) {
    val bean = viewModel.bean.collectAsState()
    LaunchedEffect(key1 = null) {
        viewModel.action.value = WorkMainViewModel.ACTION_REPORT1
        viewModel.bean.value = AppSampleUtils.genCaseInfo()
    }
    WorkMainActionReport2Block(bean)
}