package poct.device.app.ui.work

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.component.AppDivider
import poct.device.app.component.AppFieldWrapper
import poct.device.app.component.AppFilledButton
import poct.device.app.theme.bgColor
import poct.device.app.theme.endColor
import poct.device.app.theme.primaryColor
import poct.device.app.theme.startColor
import poct.device.app.thirdparty.FrosApi
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppSampleUtils

// 文字数据类
data class TextGridItem(
    val title: String,
    val content: String
)

// 数据类
data class ImageGridItem(
    val imageRes: Int,
    val title: String
)

@Composable
fun WorkActionReport1Block(
    bean: State<CaseBean>,
    onDataDetail: () -> Unit,
    onReportGet: () -> Unit,
    onBeanUpdate: (newBean: CaseBean) -> Unit,
    onUpload: (CaseBean) -> Unit,
) {
    val labelWidth = 72.dp

    // TODO 简化信息
//    val gridImageItems = ArrayList<ImageGridItem>()
//    gridImageItems.add(ImageGridItem(imageRes = R.drawable.result_1_1, title = "炎性负荷指数"))
//    gridImageItems.add(ImageGridItem(imageRes = R.drawable.result_2_2, title = "线粒体机能指数"))
//    gridImageItems.add(ImageGridItem(imageRes = R.drawable.result_3_1, title = "微血管发展指数"))
//    gridImageItems.add(ImageGridItem(imageRes = R.drawable.result_4_2, title = "代谢韧性指数"))


    Column {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 15.dp, end = 15.dp),
            shape = RoundedCornerShape(8.dp),
            color = bgColor
        ) {
            // 标题
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(15.dp))
                Row(
                    modifier = Modifier
                        .padding(vertical = 0.dp)
                ) {
                    var title3 = stringResource(id = R.string.work_report_edit)
                    if (bean.value.type == CaseBean.TYPE_2LJ_B_M || bean.value.type == CaseBean.TYPE_2LJ_B_F
                        || bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP
                    ) {
                        title3 = stringResource(id = R.string.work_report_edit_2)
                    }
                    Text(
                        text = title3,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(72.dp)
                            .height(24.dp),
                        text = stringResource(id = R.string.report_upload),
                        fontSize = 10.sp,
                        onClick = { onUpload(bean.value) },
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 15.dp, end = 15.dp),
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = R.string.work_report_basic),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.5.dp))
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
                        AppDictUtils.caseTypeOptions(LocalContext.current)[bean.value.type]?.let { text ->
                            Text(fontSize = 12.sp, text = text)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start, // 左对齐
                        verticalAlignment = Alignment.CenterVertically // 垂直居中
                    ) {
                        Text(
                            modifier = Modifier.width(labelWidth),
                            text = stringResource(id = R.string.work_f_name),
                            fontSize = 12.sp
                        )
                        Text(
                            fontSize = 12.sp,
                            text = bean.value.name,
                            modifier = Modifier.width(100.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(modifier = Modifier.width(32.dp))
                        Text(
                            modifier = Modifier.width(labelWidth / 2),
                            text = stringResource(id = R.string.work_f_gender),
                            fontSize = 12.sp
                        )
                        Text(
                            fontSize = 12.sp, text = AppDictUtils.label(
                                AppDictUtils.genderOptions(LocalContext.current),
                                bean.value.gender
                            )
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start, // 左对齐
                        verticalAlignment = Alignment.CenterVertically // 垂直居中
                    ) {
                        Text(
                            modifier = Modifier.width(labelWidth),
                            text = stringResource(id = R.string.work_f_birthday),
                            fontSize = 12.sp
                        )
                        Text(fontSize = 12.sp, text = bean.value.birthday)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start, // 左对齐
                        verticalAlignment = Alignment.CenterVertically // 垂直居中
                    ) {
                        Text(
                            modifier = Modifier.width(labelWidth - 18.dp),
                            text = stringResource(id = R.string.work_f_reagent_id),
                            fontSize = 12.sp
                        )
                        Text(fontSize = 12.sp, text = bean.value.reagentId)
                        Spacer(modifier = Modifier.width(32.dp))
                        Text(
                            modifier = Modifier.width(labelWidth - 18.dp),
                            text = stringResource(id = R.string.work_f_case_id),
                            fontSize = 12.sp
                        )
                        Text(fontSize = 12.sp, text = bean.value.caseId)
                    }
                    Spacer(modifier = Modifier.height(4.5.dp))
                    AppDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    if (bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
//                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = bean.value.baaAssets.titleImg,
                                contentDescription = "顶部标题图片",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth(0.8f) // 根据图片宽度调整
                                    .padding(bottom = 4.dp) // 减少底部间距
                            )
//                            Image(
//                                painter = painterResource(id = R.drawable.result_title),
//                                contentDescription = "顶部标题图片",
//                                contentScale = ContentScale.Fit,
//                                modifier = Modifier
//                                    .fillMaxWidth(0.8f) // 根据图片宽度调整
//                                    .padding(bottom = 4.dp) // 减少底部间距
//                            )

                            AsyncImage(
                                model = bean.value.baaAssets.diagramImg,
                                contentDescription = "完整结果图片",
                                contentScale = ContentScale.FillWidth,
                                modifier = Modifier.fillMaxSize()
                            )
//                            Image(
//                                painter = painterResource(id = R.drawable.result_fill),
//                                contentDescription = "完整结果图片",
//                                contentScale = ContentScale.FillWidth,
//                                modifier = Modifier.fillMaxSize()
//                            )

                            // TODO 全动态效果
//                            // 四宫格 - 紧贴图片下方
//                            Column(
//                                modifier = Modifier
//                                    .fillMaxSize()
//                            ) {
//                                // 第一行 - 顶部标题
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                ) {
//                                    for (i in 0..1) {
//                                        CompactImageCell(
//                                            item = gridImageItems[i],
//                                            modifier = Modifier
//                                                .weight(1f)
//                                                .aspectRatio(1f),
//                                            contentAlignment = Alignment.TopCenter
//                                        )
//                                    }
//                                }
//
//                                // 第二行 - 底部标题
//                                Row(
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                ) {
//                                    for (i in 2..3) {
//                                        CompactImageCell(
//                                            item = gridImageItems[i],
//                                            modifier = Modifier
//                                                .weight(1f)
//                                                .aspectRatio(1f),
//                                            contentAlignment = Alignment.BottomCenter
//                                        )
//                                    }
//                                }
//                            }
                        }

                        if (bean.value.baaResult.bioAgeProfile.scores.isNotEmpty()) {
                            val scores: Map<String, Double> =
                                bean.value.baaResult.bioAgeProfile.scores
                            val gridTextItems = ArrayList<TextGridItem>()
                            for (name in scores.keys) {
                                if (name != "total") {
                                    gridTextItems.add(
                                        TextGridItem(
                                            title = AppDictUtils.label(
                                                AppDictUtils.baaScoreName(),
                                                name
                                            ),
                                            content = String.format(
                                                "%s：%s分",
                                                name,
                                                scores[name].toString()
                                            )
                                        )
                                    )
                                }
                            }


                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                ) {
                                    for (i in 0..1) {
                                        if (i < gridTextItems.size) {
                                            CompactTextCell(
                                                item = gridTextItems[i],
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(primaryColor)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(primaryColor)
                                ) {
                                    for (i in 2..3) {
                                        if (i < gridTextItems.size) {
                                            CompactTextCell(
                                                item = gridTextItems[i],
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(startColor, endColor),
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, Float.POSITIVE_INFINITY)
                                )
                            )
                            .clip(RoundedCornerShape(8.dp))
                            .padding(12.dp),
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(24.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                var title2 = stringResource(id = R.string.work_report_result)
                                if (bean.value.type == CaseBean.TYPE_2LJ_B_M || bean.value.type == CaseBean.TYPE_2LJ_B_F
                                    || bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP
                                ) {
                                    title2 = stringResource(id = R.string.work_report_result_2)
                                }
                                Text(
                                    text = title2,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Spacer(modifier = Modifier.width(160.dp))
                                if (bean.value.type == CaseBean.TYPE_2LJ_B_M || bean.value.type == CaseBean.TYPE_2LJ_B_F
                                    || bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP
                                ) {
                                    AppFilledButton(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .height(24.dp),
                                        text = stringResource(id = R.string.report_get),
                                        fontSize = 13.sp,
                                        onClick = { onReportGet() },
                                    )
                                } else {
                                    AppFilledButton(
                                        modifier = Modifier
                                            .width(72.dp)
                                            .height(24.dp),
                                        text = stringResource(id = R.string.report_item_detail),
                                        fontSize = 13.sp,
                                        onClick = { onDataDetail() },
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.5.dp))
                            var title1 = stringResource(id = R.string.report_edit_title1)
                            if (bean.value.type == CaseBean.TYPE_2LJ_B_M || bean.value.type == CaseBean.TYPE_2LJ_B_F
                                || bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP
                            ) {
                                title1 = stringResource(id = R.string.report_edit_title1_2)
                            }
                            AppFieldWrapper(
                                labelWidth = 80.dp,
                                text = title1,
                                fontSize = 13.sp,
                                background = Color.Unspecified
                            ) {
                                if (bean.value.type == CaseBean.TYPE_4LJ || bean.value.type == CaseBean.TYPE_3LJ) {
                                    Text(
                                        modifier = Modifier.width(100.dp),
                                        fontSize = 13.sp,
                                        text = stringResource(id = R.string.report_edit_title2)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                } else if (bean.value.type == CaseBean.TYPE_2LJ_B_M || bean.value.type == CaseBean.TYPE_2LJ_B_F
                                    || bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP
                                ) {
                                    Text(
                                        modifier = Modifier.width(50.dp),
                                        fontSize = 13.sp,
                                        text = stringResource(id = R.string.report_edit_title6)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        modifier = Modifier.width(50.dp),
                                        fontSize = 13.sp,
                                        text = stringResource(id = R.string.report_edit_title7)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    modifier = Modifier.width(80.dp),
                                    fontSize = 13.sp,
                                    text = stringResource(id = R.string.report_edit_title3)
                                )
                            }
                            AppDivider()
                            val workResult = bean.value.resultList
                            for (i in workResult.indices) {
                                val result = workResult[i]

                                // 判断是否应该显示
                                val shouldShow = when {
                                    bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP ->
                                        result.name == CaseBean._3LJ_BIOAGE_L1_MAIN_Final

                                    else -> true
                                }

                                if (shouldShow) {
                                    AppFieldWrapper(
                                        labelWidth = 80.dp,
                                        text = result.name,
                                        fontSize = 15.sp,
                                        background = Color.Unspecified
                                    ) {
                                        if (bean.value.type == CaseBean.TYPE_4LJ || bean.value.type == CaseBean.TYPE_3LJ) {
                                            Text(
                                                modifier = Modifier.width(100.dp),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                text = result.radioValue
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                modifier = Modifier.width(80.dp),
                                                fontSize = 15.sp,
                                                text = result.result
                                            )
                                        } else if (bean.value.type == CaseBean.TYPE_2LJ_B_M || bean.value.type == CaseBean.TYPE_2LJ_B_F) {
                                            Text(
                                                modifier = Modifier.width(50.dp),
                                                fontSize = 15.sp,
                                                text = result.refer
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                modifier = Modifier.width(50.dp),
                                                fontSize = 15.sp,
                                                text = result.result
                                            )
                                        } else if (
                                            (bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP)
                                            && result.name == CaseBean._3LJ_BIOAGE_L1_MAIN_Final
                                        ) {
                                            Text(
                                                modifier = Modifier.width(50.dp),
                                                fontSize = 15.sp,
                                                text = bean.value.baaResult.bioAgeProfile.chronoAge.toString()
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                modifier = Modifier.width(50.dp),
                                                fontSize = 15.sp,
                                                text = bean.value.baaResult.bioAgeProfile.bioAge.toString()
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            val colorMap = mapOf(
                                                "green" to Color.Green,
                                                "red" to Color.Red,
                                            )
                                            val data = result.radioValue.split("|")
                                            Text(
                                                modifier = Modifier.width(80.dp),
                                                fontSize = 15.sp,
                                                color = colorMap[data[0]] ?: Color.Black,
                                                text = data[1]
                                            )
                                        } else {
                                            Text(
                                                modifier = Modifier.width(80.dp),
                                                fontSize = 15.sp,
                                                text = result.result
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            if (result.flag == 1) {
                                                if (result.name == CaseBean.CRP_T2) {
                                                    Text(
                                                        fontSize = 15.sp,
                                                        text = "↓"
                                                    )
                                                } else {
                                                    Text(
                                                        fontSize = 15.sp,
                                                        text = "↑"
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    AppDivider()
                                }
                            }
                        }
                    }
                    // TODO 全动态效果
//                    if (bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1) {
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Column(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalAlignment = Alignment.CenterHorizontally,
//                        ) {
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth(),
//                            ) {
//                                Column(
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                        .background(primaryColor)
//                                        .padding(12.dp),
//                                    horizontalAlignment = Alignment.Start,
//                                    verticalArrangement = Arrangement.Top,
//                                ) {
//                                    Text(
//                                        text = "结论:",
//                                        fontSize = 13.sp,
//                                        fontWeight = FontWeight.Bold,
//                                        color = Color.Black,
//                                        maxLines = 1,
//                                        overflow = TextOverflow.Ellipsis,
//                                        modifier = Modifier
//                                            .fillMaxWidth()
//                                            .padding(bottom = 6.dp)
//                                    )
//                                    Text(
//                                        text = "该用户的生理机能相当于26.4岁的状态,实现芈了显著的逆龄(-11.6岁)。这得益于其极低的炎性负荷和完美的代谢控制。",
//                                        fontSize = 13.sp,
//                                        color = Color.DarkGray,
//                                        lineHeight = 16.sp,
//                                        maxLines = 5,
//                                        overflow = TextOverflow.Ellipsis,
//                                        modifier = Modifier.fillMaxWidth()
//                                    )
//                                }
//                            }
//                        }
//                    }
                    if (bean.value.type == CaseBean.TYPE_2LJ_B_M || bean.value.type == CaseBean.TYPE_2LJ_B_F) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                modifier = Modifier.fillMaxSize(),
                                painter = painterResource(id = R.drawable.report_result1),
                                contentDescription = "",
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                    // TODO 简化信息
//                    // 获取当前用户角色
//                    if (AppParams.curUser.role != User.ROLE_CHECKER) {
//                        Spacer(modifier = Modifier.height(12.dp))
//                        Box(
//                            modifier = Modifier
//                                .fillMaxSize()
//                                .background(
//                                    Brush.linearGradient(
//                                        listOf(startColor, endColor),
//                                        start = Offset(0f, 0f),
//                                        end = Offset(0f, Float.POSITIVE_INFINITY)
//                                    )
//                                )
//                                .clip(RoundedCornerShape(8.dp))
//                                .padding(12.dp),
//                        ) {
//                            Column {
//                                Text(
//                                    modifier = Modifier.fillMaxWidth(),
//                                    text = stringResource(id = R.string.work_report_result2),
//                                    fontSize = 14.sp,
//                                    fontWeight = FontWeight.Bold,
//                                )
//                                Spacer(modifier = Modifier.height(4.5.dp))
//                                AppFieldWrapper(
//                                    text = stringResource(id = R.string.report_edit_title5),
//                                    fontSize = TextUnit.Unspecified,
//                                    background = Color.Unspecified
//                                ) {
//                                    Spacer(modifier = Modifier.width(10.dp))
//                                    Text(
//                                        modifier = Modifier.width(100.dp),
//                                        text = stringResource(id = R.string.report_edit_title3)
//                                    )
//                                    Spacer(modifier = Modifier.width(10.dp))
//                                    Text(
//                                        modifier = Modifier.width(100.dp),
//                                        text = stringResource(id = R.string.report_edit_title4)
//                                    )
//                                }
//                                AppDivider()
//                                val workResult = bean.value.resultList
//                                for (i in workResult.indices) {
//                                    val result = workResult[i]
//                                    if (bean.value.type == CaseBean.TYPE_IGE) {
//                                        AppFieldWrapper(
//                                            text = "T1",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t1Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t1Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "C",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.cValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        break
//                                    }
//                                    if (bean.value.type == CaseBean.TYPE_CRP) {
//                                        AppFieldWrapper(
//                                            text = "T1",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t1Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t1Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "T2",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t2Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t2Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "C",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.cValue
//                                            )
//                                        }
//                                        AppDivider()
//                                    }
//                                    if (bean.value.type == CaseBean.TYPE_4LJ || bean.value.type == CaseBean.TYPE_3LJ || bean.value.type == CaseBean.TYPE_2LJ_B) {
//                                        AppFieldWrapper(
//                                            text = "T1",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t1Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t1Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "T2",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t2Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t2Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "T3",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t3Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t3Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "T4",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t4Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t4Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "C",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.cValue
//                                            )
//                                        }
//                                        AppDivider()
//                                    }
//                                    if (bean.value.type == CaseBean.TYPE_SF) {
//                                        AppFieldWrapper(
//                                            text = "T1",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t1Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.cValue) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t1Value) / NumberUtils.toDouble(
//                                                        result.cValue
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "C1",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.cValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "T2",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.t2Value
//                                            )
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            var radioValue = "0.0"
//                                            if (NumberUtils.toDouble(result.c2Value) != 0.0) {
//                                                radioValue = "%.4f".format(
//                                                    NumberUtils.toDouble(result.t2Value) / NumberUtils.toDouble(
//                                                        result.c2Value
//                                                    )
//                                                )
//                                            }
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = radioValue
//                                            )
//                                        }
//                                        AppDivider()
//                                        AppFieldWrapper(
//                                            text = "C2",
//                                            background = Color.Unspecified
//                                        ) {
//                                            Spacer(modifier = Modifier.width(10.dp))
//                                            Text(
//                                                modifier = Modifier.width(100.dp),
//                                                text = result.c2Value
//                                            )
//                                        }
//                                        AppDivider()
//                                    }
//                                    break
//                                }
//                            }
//                        }
//                    }
//                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
        }
    }
}

@Composable
fun CompactTextCell(
    item: TextGridItem,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(primaryColor),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Top
        ) {
            // 标题 - 更紧凑
            Text(
                text = item.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )

            // 分隔线
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.LightGray)
                    .padding(bottom = 6.dp)
            )

            // 正文 - 紧凑排版
            Text(
                text = item.content,
                fontSize = 13.sp,
                color = Color.DarkGray,
                lineHeight = 16.sp,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CompactImageCell(
    item: ImageGridItem,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment,
) {
    Box(
        modifier = modifier
            .background(Color.White),
        contentAlignment = contentAlignment
    ) {
        Image(
            painter = painterResource(id = item.imageRes),
            contentDescription = item.title,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize(),
        )

        // 底部标题区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.7f)
                    )
                ),
        ) {
            Text(
                text = item.title,
                fontSize = 14.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 4.dp)
            )
        }
    }
}

@Preview
@Composable
fun HomeWorkActionReport1BlockPreview(viewModel: WorkMainViewModel = viewModel()) {
    val bean = viewModel.bean.collectAsState()
    bean.value.type = CaseBean.TYPE_3LJ_BIOAGE_L1
    // bean.value.type = CaseBean.TYPE_BIOAGE_CRP
    LaunchedEffect(key1 = null) {
        viewModel.action.value = WorkMainViewModel.ACTION_REPORT1
        viewModel.bean.value = AppSampleUtils.genCaseInfo()
    }

    FrosApi.uploadPatientReportDataToServer(bean.value)
    WorkActionReport1Block(
        bean = bean,
        onDataDetail = { viewModel.onDataDetail(bean.value) },
        onReportGet = { viewModel.onActionReportGet() },
        onBeanUpdate = { viewModel.onBeanUpdate(it) },
        onUpload = { viewModel.uploadReport(bean.value) },
    )
}

