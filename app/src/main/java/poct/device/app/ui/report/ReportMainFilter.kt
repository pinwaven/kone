package poct.device.app.ui.report


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import poct.device.app.R
import poct.device.app.bean.CaseQueryBean
import poct.device.app.component.AppDateRangePicker
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppSelect
import poct.device.app.component.AppTextField
import poct.device.app.utils.app.AppDictUtils

/**
 * 查询条件
 */
@Composable
fun ReportMainFilter(
    query: State<CaseQueryBean>,
    onQueryUpdate: (CaseQueryBean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
//            .background(Color.Black)
            .padding(horizontal = 15.dp)
    ) {
        Spacer(modifier = Modifier.height(15.dp))
        AppFieldWrapper(
            labelWidth = 0.dp,
            height = 36.dp
        ) {
            AppDateRangePicker(
                values = listOf(query.value.dateStarted, query.value.dateEnded),
                borderWidth = 1.dp,
                limit = 30,
                onValueChange = {
                    onQueryUpdate(query.value.copy(dateStarted = it[0], dateEnded = it[1]))
                }
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row {
            val options = AppDictUtils.caseTypeOptions(LocalContext.current)
            val newOptions = LinkedHashMap<String, String>()
            newOptions[""] = stringResource(id = R.string.report_select)
            newOptions.putAll(options)
            Box(modifier = Modifier.width(150.dp)) {
                AppFieldWrapper(
                    labelWidth = 0.dp,
                    height = 36.dp
                ) {
                    AppSelect(
                        value = query.value.caseType,
                        borderWidth = 1.dp,
                        options = newOptions,
                        placeHolder = stringResource(id = R.string.report_filter_type),
                        onValueChange = {
                            onQueryUpdate(query.value.copy(caseType = it))
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                AppFieldWrapper(
                    labelWidth = 0.dp,
                    height = 36.dp
                ) {
                    AppTextField(
                        value = query.value.name,
                        borderWidth = 1.dp,
                        placeHolder = stringResource(id = R.string.report_filter_name),
                        onValueChange = {
                            onQueryUpdate(query.value.copy(name = it))
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}