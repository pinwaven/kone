package poct.device.app.ui.report

import android.content.pm.ActivityInfo
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.lang.math.NumberUtils
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.bean.CaseBean
import poct.device.app.chart.ChartData
import poct.device.app.chart.LineChartData
import poct.device.app.entity.CardConfig
import poct.device.app.entity.CasePoint
import poct.device.app.entity.User
import poct.device.app.entity.service.CardConfigService
import poct.device.app.state.ViewState
import poct.device.app.theme.primaryColor
import timber.log.Timber

class ReportDetailViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)

    // 曲线状态
    val lineState = MutableStateFlow(STEP_SHOW)

    // 曲线图数据
    val chartModelProducer: ChartEntryModelProducer = ChartEntryModelProducer()
    val bean = MutableStateFlow(CaseBean.Empty)

    val valueLabelList = MutableStateFlow<List<String>>(emptyList())

    val columnDataList = MutableStateFlow<List<ChartData>>(emptyList())
    val lineDataList = MutableStateFlow<List<LineChartData>>(emptyList())

    // 试剂卡配置
    private var cardConfig: CardConfig? = null

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        if (AppParams.curUser.role != User.ROLE_CHECKER) {
            if (lineState.value == STEP_SHOW) {
                lineState.value = STEP_NORM
            }
        } else {
            lineState.value = STEP_SHOW
        }

        // 横屏
        AppParams.curActivity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        viewModelScope.launch(Dispatchers.IO) {
            bean.value = AppParams.varReport
            // 查询试剂卡配置
            cardConfig = CardConfigService.findByIden(bean.value.type, bean.value.reagentId)
            var pointList: List<CasePoint>
            Timber.w("查询的数据：${App.gson.toJson(bean.value.pointList)}")
            if (lineState.value == STEP_MAJOR) {
                pointList = getDetailPointList(bean.value.pointList, 160)
            } else {
                pointList = getWholePointList(bean.value)
                Timber.w("当前columnList:${App.gson.toJson(columnDataList.value)}")
            }
            val entryList = ArrayList<FloatEntry>()
            for (point in pointList) {
                entryList.add(FloatEntry(point.x.toFloat(), point.y.toFloat()))
            }
            chartModelProducer.setEntries(entryList)
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onUpdateLineState() {
        viewState.value = ViewState.LoadingOver()
        if (lineState.value == STEP_NORM) {
            lineState.value = STEP_MAJOR
        } else {
            lineState.value = STEP_NORM
        }
        onLoad()
    }

    // 专业：线状图-非自定义
    private fun getDetailPointList(pointList: List<CasePoint>, number: Int): List<CasePoint> {
        // 重新排序
        val list = pointList as ArrayList<CasePoint>
        list.sortBy { it.x }
        // 获取点数数量
        val pointCount = pointList.size
        if (pointCount <= number) {
            return list
        }
        // 点数数量超过200
        // 获取当前点数与200的倍数
        val newPointList = ArrayList<CasePoint>()
        val multiple = (pointCount / number)
        val lineList = ArrayList<LineChartData>()
        val dataList = ArrayList<ChartData>()
        if (multiple < 2) {
            for ((_, casePoint) in list.withIndex()) {
                dataList.add(
                    ChartData(
                        NumberUtils.toFloat(casePoint.y.toString()),
                        casePoint.x.toString()
                    )
                )
            }
            lineList.add(LineChartData(dataList, Color.Black))
            return list
        } else {
            for ((index, casePoint) in list.withIndex()) {
                if (index == 0 || index % multiple == 0) {
                    dataList.add(
                        ChartData(
                            NumberUtils.toFloat(casePoint.y.toString()),
                            casePoint.x.toString()
                        )
                    )
                    newPointList.add(CasePoint(casePoint.x - 1, casePoint.y))
                }
            }
            Timber.w("=======${newPointList.size}")
            newPointList.sortBy { it.x }
            lineList.add(LineChartData(dataList, primaryColor))
            lineDataList.value = lineList
            return newPointList
        }
    }

    // 标准：线状图-非自定义
    private fun getWholePointList(curBean: CaseBean): List<CasePoint> {
        val newPointList = ArrayList<CasePoint>()
        val labelList = ArrayList<String>()
        Timber.w("当前curBean${App.gson.toJson(curBean)}")
        if (curBean.type == CaseBean.TYPE_IGE) {
            Timber.w("当前resultList${App.gson.toJson(curBean.resultList)}")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("IGE")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("C")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            for (i in curBean.resultList.indices) {
                val result = curBean.resultList[i]
                var index = 0.0
                Timber.w("当前result${App.gson.toJson(result)}")
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t1Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.cValue)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                break
            }
        }
        if (curBean.type == CaseBean.TYPE_CRP) {
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("CRP")
            labelList.add("")
            labelList.add("")
            labelList.add("SF")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("C")
            labelList.add("")
            for (i in curBean.resultList.indices) {
                val result = curBean.resultList[i]
                var index = 0.0
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t1Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.cValue)))
                newPointList.add(CasePoint(index++, 0.0))
                break
            }
        }
        if (curBean.type == CaseBean.TYPE_SF) {
            labelList.add("")
            labelList.add("CRP")
            labelList.add("")
            labelList.add("")
            labelList.add("C1")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("")
            labelList.add("SF")
            labelList.add("")
            labelList.add("")
            labelList.add("C2")
            labelList.add("")
            for (i in curBean.resultList.indices) {
                var index = 0.0
                val result = curBean.resultList[i]
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t1Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.cValue)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.c2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                break
            }
        }
        if (curBean.type == CaseBean.TYPE_4LJ) {
            labelList.add("")
            labelList.add("RSV")
            labelList.add("")
            labelList.add("")
            labelList.add("FluA")
            labelList.add("")
            labelList.add("")
            labelList.add("FluB")
            labelList.add("")
            labelList.add("")
            labelList.add("SARS")
            labelList.add("")
            labelList.add("")
            labelList.add("C")
            labelList.add("")
            for (i in curBean.resultList.indices) {
                val result = curBean.resultList[i]
                var index = 0.0
                Timber.w("当前result${i}=====${App.gson.toJson(result)}")
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t1Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t3Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t4Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.cValue)))
                newPointList.add(CasePoint(index++, 0.0))
                break
            }
        }
        if (curBean.type == CaseBean.TYPE_3LJ) {
            labelList.add("")
            labelList.add("FluA")
            labelList.add("")
            labelList.add("")
            labelList.add("FluB")
            labelList.add("")
            labelList.add("")
            labelList.add("SARS")
            labelList.add("")
            labelList.add("")
            labelList.add("C")
            labelList.add("")
            for (i in curBean.resultList.indices) {
                val result = curBean.resultList[i]
                var index = 0.0
                Timber.w("当前result${i}=====${App.gson.toJson(result)}")
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t1Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t3Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.cValue)))
                newPointList.add(CasePoint(index++, 0.0))
                break
            }
        }
        if (curBean.type == CaseBean.TYPE_2LJ_A) {
            labelList.add("")
            labelList.add(CaseBean._2LJ_A_T1)
            labelList.add("")
            labelList.add("")
            labelList.add("C1")
            labelList.add("")
            labelList.add("")
            labelList.add(CaseBean._2LJ_A_T2)
            labelList.add("")
            labelList.add("")
            labelList.add("C2")
            labelList.add("")
            for (i in curBean.resultList.indices) {
                val result = curBean.resultList[i]
                var index = 0.0
                Timber.w("当前result${i}=====${App.gson.toJson(result)}")
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t1Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.cValue)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.c2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                break
            }
        }
        if (curBean.type == CaseBean.TYPE_2LJ_B ||
            curBean.type == CaseBean.TYPE_2LJ_B_M ||
            curBean.type == CaseBean.TYPE_2LJ_B_F
        ) {
            labelList.add("")
            labelList.add(CaseBean._2LJ_B_T1)
            labelList.add("")
            labelList.add("")
            labelList.add("C1")
            labelList.add("")
            labelList.add("")
            labelList.add(CaseBean._2LJ_B_T2)
            labelList.add("")
            labelList.add("")
            labelList.add("C2")
            labelList.add("")
            for (i in curBean.resultList.indices) {
                val result = curBean.resultList[i]
                var index = 0.0
                Timber.w("当前result${i}=====${App.gson.toJson(result)}")
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t1Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.cValue)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.t2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, 0.0))
                newPointList.add(CasePoint(index++, NumberUtils.toDouble(result.c2Value)))
                newPointList.add(CasePoint(index++, 0.0))
                break
            }
        }
        if (curBean.type == CaseBean.TYPE_3LJ_BIOAGE_L1) {
            val result = curBean.resultList[0]
            labelList.add("")
            labelList.add(result.t1ValueName)
            labelList.add("")
            labelList.add("")
            labelList.add("C1")
            labelList.add("")
            labelList.add("")
            labelList.add(result.t2ValueName)
            labelList.add("")
            labelList.add("")
            labelList.add("C2")
            labelList.add("")
            labelList.add(result.t3ValueName)
            labelList.add("")
            labelList.add("")
            labelList.add("C3")
            labelList.add("")

            Timber.w("当前result${0}=====${App.gson.toJson(result)}")
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.t1Value)))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.cValue)))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.t2Value)))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.cValue)))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.t3Value)))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.cValue)))
            newPointList.add(CasePoint(0.0, 0.0))
        }
        if (curBean.type == CaseBean.TYPE_BIOAGE_CRP) {
            val result = curBean.resultList[0]
            labelList.add("")
            labelList.add(result.t1ValueName)
            labelList.add("")
            labelList.add("")
            labelList.add("C1")
            labelList.add("")

            Timber.w("当前result${0}=====${App.gson.toJson(result)}")
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.t1Value)))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, 0.0))
            newPointList.add(CasePoint(0.0, NumberUtils.toDouble(result.cValue)))
            newPointList.add(CasePoint(0.0, 0.0))
        }
        valueLabelList.value = labelList
        return newPointList
    }

    companion object {
        const val STEP_MAJOR = "major"
        const val STEP_NORM = "norm"
        const val STEP_SHOW = "-"
    }

}