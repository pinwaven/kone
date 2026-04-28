package poct.device.app.ui.report


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppCheckbox
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppOutlinedButton
import poct.device.app.theme.bgColor
import poct.device.app.theme.dangerColor
import poct.device.app.theme.fontColor
import poct.device.app.theme.sepColor
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppLocalDateUtils
import java.time.LocalDate


/**
 * 每条记录显示内容
 */
@Composable
fun ReportMainRecordItem(
    bean: CaseBean,
    reportExist: Boolean,
    selected: State<List<String>>,
    onItemChecked: (caseId: String) -> Unit,
    onItemDelConfirm: (CaseBean) -> Unit,
    onItemExportConfirm: (CaseBean) -> Unit,
    onItemEdit: (CaseBean) -> Unit,
    onItemDetail: (CaseBean) -> Unit,
    onItemPDF: (CaseBean) -> Unit,
    onGenPDF: (CaseBean) -> Unit,
    onUpload: (CaseBean) -> Unit,
) {
    val labelWidth = 72.dp
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppCheckbox(
            text = "",
            checked = selected.value.contains(bean.id),
            onCheckedChange = { onItemChecked(bean.id) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .background(bgColor)
                .fillMaxWidth()
                .height(168.dp),
            shape = RoundedCornerShape(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 头部信息
                    Text(
                        text = "${stringResource(id = R.string.work_case_id)} ${bean.caseId}",
                        fontSize = 12.sp,
                        color = fontColor
                    )
                    Row {
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = { onItemExportConfirm(bean) }
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.mipmap.dc_b_icon),
                                tint = Color.Unspecified,
                                contentDescription = ""
                            )
                        }
                        Spacer(modifier = Modifier.width(15.dp))
                        IconButton(
                            modifier = Modifier.size(24.dp),
                            onClick = { onItemDelConfirm(bean) }
                        ) {
                            Icon(
                                modifier = Modifier.size(24.dp),
                                painter = painterResource(id = R.mipmap.sc_icon),
                                tint = dangerColor,
                                contentDescription = ""
                            )
                        }
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.5.dp)
                        .background(sepColor)
                )
                // 记录
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, start = 10.dp, end = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                modifier = Modifier.width(labelWidth),
                                text = stringResource(id = R.string.work_f_name),
                                fontSize = 12.sp
                            )
                            Text(
                                modifier = Modifier.width(labelWidth),
                                overflow = TextOverflow.Ellipsis,
                                text = bean.name,
                                fontSize = 12.sp
                            )
                            Text(
                                modifier = Modifier.width(labelWidth / 2),
                                text = stringResource(id = R.string.work_f_gender),
                                fontSize = 12.sp
                            )
                            Text(
                                modifier = Modifier.width(labelWidth / 4),
                                text = AppDictUtils.label(
                                    AppDictUtils.genderOptions(LocalContext.current),
                                    bean.gender
                                ),
                                fontSize = 12.sp
                            )
                            Text(
                                modifier = Modifier.width(labelWidth / 2),
                                text = stringResource(id = R.string.work_f_age),
                                fontSize = 12.sp
                            )
                            Text(
                                modifier = Modifier.width(labelWidth / 2),
                                text = AppLocalDateUtils.calcAge(
                                    AppLocalDateUtils.parseDate(bean.birthday),
                                    LocalDate.now()
                                ).toString(),
                                fontSize = 12.sp
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.width(labelWidth),
                            text = stringResource(id = R.string.work_f_case_type),
                            fontSize = 12.sp
                        )
                        Text(
                            text = AppDictUtils.label(
                                AppDictUtils.caseTypeOptions(LocalContext.current),
                                bean.type
                            ), fontSize = 12.sp
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            modifier = Modifier.width(labelWidth),
                            text = stringResource(id = R.string.work_f_case_time),
                            fontSize = 12.sp
                        )
                        Text(text = bean.workTime, fontSize = 12.sp)
                    }
                }
                // 操作
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, end = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(90.dp)
                            .height(30.dp),
                        text = stringResource(id = R.string.report_upload),
                        onClick = { onUpload(bean) },
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(90.dp)
                            .height(30.dp),
                        text = stringResource(id = R.string.work_report),
                        onClick = { onItemEdit(bean) },
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    AppOutlinedButton(
                        modifier = Modifier
                            .width(90.dp)
                            .height(30.dp),
                        text = stringResource(id = R.string.report_item_detail),
                        onClick = { onItemDetail(bean) },
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    if (reportExist) {
                        AppFilledButton(
                            modifier = Modifier
                                .width(90.dp)
                                .height(30.dp),
                            text = stringResource(id = R.string.report_pdf),
                            onClick = { onItemPDF(bean) },
                        )
                    } else {
//                        AppFilledButton(
//                            modifier = Modifier
//                                .width(90.dp)
//                                .height(30.dp),
//                            text = stringResource(id = R.string.work_report_gen),
//                            onClick = { onGenPDF(bean) },
//                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun ReportMainRecordItemPreview() {
    val viewModel: ReportMainViewModel = viewModel()
    val records = viewModel.records.collectAsState()
    val bean = records.value[0]
    val selected = viewModel.selected.collectAsState()

    ReportMainRecordItem(
        bean, viewModel.pdfReportExist(bean), selected,
        onItemChecked = { },
        onItemExportConfirm = { },
        onItemDelConfirm = { },
        onItemEdit = { },
        onItemDetail = { },
        onItemPDF = { },
        onGenPDF = { },
        onUpload = { },
    )
}