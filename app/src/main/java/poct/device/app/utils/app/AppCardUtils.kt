package poct.device.app.utils.app

import android.annotation.SuppressLint
import info.szyh.common4.json.JsonUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.bean.ResultReferBean
import poct.device.app.bean.card.CardConfig
import poct.device.app.entity.CasePoint
import poct.device.app.entity.CaseResult
import poct.device.app.entity.User
import poct.device.app.service.BAAService
import poct.device.app.service.BioFeature
import poct.device.app.state.ActionState
import poct.device.app.thirdparty.model.sbedge.resp.CardConfigTop
import poct.device.app.thirdparty.model.sbedge.resp.CardConfigVar
import poct.device.app.ui.work.WorkMainViewModel.Companion.EVT_DEV_ERROR
import timber.log.Timber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.log
import kotlin.math.sqrt

object AppCardUtils {
    const val MIN_DIFF_VAL = 2
    const val MAX_DIFF_VAL = 2000

    data class ScanData(
        val dataLen: Int,           // uint16_t (2 bytes)
        val laserCurr: Int,         // 12-bit ADC in uint16_t (2 bytes)
        val fixed5a: Byte,          // 0x5a (1 byte)
        val dataBias: Int,          // uint16_t (2 bytes)
        val laserCurrBias: Int,     // uint16_t (2 bytes)
        val fixedA5: Byte,          // 0xa5 (1 byte)
        val rawData: List<Int>,      // 13-bit ADC values (each 2 bytes)
        var dataBiased: List<Int>,
        var laserCurrBiased: Int,
    ) {
        fun processData(): Boolean {
            // 处理首部太小的数据
            val adcValues = this.rawData.toIntArray()
            val validIndices = adcValues.indices.filter { adcValues[it] >= this.dataBias }
            if (validIndices.isNotEmpty()) {
                val firstValid = validIndices.first()
                if (firstValid > 0) {
                    Timber.w("trimming first $firstValid values below bias")
                    this.dataBiased = adcValues.copyOfRange(firstValid, adcValues.size)
                        .map { it - this.dataBias }
                } else {
                    this.dataBiased = adcValues.map { it - this.dataBias }
                }
            } else {
                Timber.w("All values are below bias!")
                return false
            }

            try {
                this.laserCurrBiased = this.laserCurr - this.laserCurrBias
                return true
            } catch (e: Exception) {
                Timber.w("error processing data: ${e.message}")
                return false
            }
        }

        fun getStatistics(): Map<String, Any> {
            val dataBiased = this.dataBiased

            val mean = dataBiased.average()
            val std = calculateStdDev(dataBiased.toIntArray(), mean)
            val min = dataBiased.minOrNull() ?: 0
            val max = dataBiased.maxOrNull() ?: 0
            val median = calculateMedian(dataBiased.toIntArray())

            return mapOf(
                "count" to dataBiased.size,
                "mean" to mean,
                "std" to std,
                "min" to min,
                "max" to max,
                "median" to median,
                "laser_current_corrected" to laserCurrBiased,
                "data_adc_bias" to this.dataBias,
                "laser_curr_bias" to this.laserCurrBias
            )
        }

        private fun calculateStdDev(values: IntArray, mean: Double): Double {
            if (values.size <= 1) return 0.0

            var sum = 0.0
            for (value in values) {
                sum += (value - mean) * (value - mean)
            }

            return sqrt(sum / (values.size - 1))
        }

        private fun calculateMedian(values: IntArray): Double {
            if (values.isEmpty()) return 0.0

            val sorted = values.sorted()
            return if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2].toDouble()
            }
        }
    }

    fun parseData(byteArray: ByteArray): ScanData? {
        try {
            if (byteArray.size < 10) {
                Timber.w("ByteArray too small, less than 10 bytes")
                return null
            }

            // 使用 ByteBuffer 解析头部
            val buffer = ByteBuffer.wrap(byteArray, 0, 10).order(ByteOrder.LITTLE_ENDIAN)

            val dataLen = buffer.short.toUShort().toInt()
            val laserCurr = buffer.short.toUShort().toInt()    // 取低12位
            val fixed5a = buffer.get()
            val dataBias = buffer.short.toUShort().toInt()
            val laserCurrBias = buffer.short.toUShort().toInt()
            val fixedA5 = buffer.get()

            // 检查固定字节
            if (fixed5a != 0x5A.toByte()) {
                Timber.w("fixed byte 0x5a check failed, actual: 0x${fixed5a.toString(16)}")
                return null
            }

            if (fixedA5 != 0xA5.toByte()) {
                Timber.w("fixed byte 0xa5 check failed, actual: 0x${fixedA5.toString(16)}")
                return null
            }

            Timber.w("head check passed")

            // 解析原始数据部分
            val remainingData = byteArray.copyOfRange(10, byteArray.size)
            if (remainingData.size / 2 != dataLen) {
                Timber.w("data length mismatch: expected $dataLen bytes, actual ${remainingData.size} bytes")
                return null
            }

            val rawData = retrieve13BitAdcData(remainingData)
            if (rawData == null) {
                return null
            }

            Timber.w("data length: $dataLen")
            Timber.w("laser current (raw): $laserCurr")
            Timber.w("data ADC bias: $dataBias")
            Timber.w("laser current bias: $laserCurrBias")

            val result = ScanData(
                dataLen,
                laserCurr,
                fixed5a,
                dataBias,
                laserCurrBias,
                fixedA5,
                rawData.toList(),
                emptyList(),
                0,
            )

//            if (!result.processData()) {
//                return null
//            }

            return result
        } catch (e: Exception) {
            Timber.w("error parsing byte array: ${e.message}")
            return null
        }
    }

    /**
     * 生成四联检结果-判断阳性方式
     * 判断使用规则方式： T1 - P1T1 > cutOffMax比较，存在使用临界值2，不存在使用临界值1
     */
    fun genResultFor4LJ(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // TODO 串口失联时，补救逻辑
//        if (AppParams.devMock) {
//            bean.value.name = "测试"
//            bean.value.gender = 1
//            bean.value.birthday = "1999-01-01"
//
//            val resultList = ArrayList<CaseResult>()
//            resultList.add(
//                CaseResult(
//                    name = CaseBean._4LJ_T1,
//                    result = "-",
//                    refer = "-",
//                    t1Value = "0.00",
//                    t2Value = "0.00",
//                    t3Value = "0.00",
//                    t4Value = "0.00",
//                    radioValue = "0.00",
//                    cValue = "0.0",
//                    c2Value = "",
//                    flag = 0
//                )
//            )
//            resultList.add(
//                CaseResult(
//                    name = CaseBean._4LJ_T2,
//                    result = "-",
//                    refer = "-",
//                    t1Value = "0.00",
//                    t2Value = "0.00",
//                    t3Value = "0.00",
//                    t4Value = "0.00",
//                    radioValue = "0.00",
//                    cValue = "0.0",
//                    c2Value = "",
//                    flag = 0
//                )
//            )
//            resultList.add(
//                CaseResult(
//                    name = CaseBean._4LJ_T3,
//                    result = "-",
//                    refer = "-",
//                    t1Value = "0.00",
//                    t2Value = "0.00",
//                    t3Value = "0.00",
//                    t4Value = "0.00",
//                    radioValue = "0.00",
//                    cValue = "0.0",
//                    c2Value = "",
//                    flag = 0
//                )
//            )
//            resultList.add(
//                CaseResult(
//                    name = CaseBean._4LJ_T4,
//                    result = "-",
//                    refer = "-",
//                    t1Value = "0.00",
//                    t2Value = "0.00",
//                    t3Value = "0.00",
//                    t4Value = "0.00",
//                    radioValue = "0.00",
//                    cValue = "0.0",
//                    c2Value = "",
//                    flag = 0
//                )
//            )
//            return Json.encodeToString(resultList)
//        }

        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val t2Top = topList[1]
        val t3Top = topList[2]
        val t4Top = topList[3]
        val cTop = topList[4]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val t3v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t3Top.start,
            t3Top.end,
            cardConfig.scanPPMM
        )
        val t4v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t4Top.start,
            t4Top.end,
            cardConfig.scanPPMM
        )
        val cv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            cTop.start,
            cTop.end,
            cardConfig.scanPPMM
        )

        // C值校验
        if (cv < cardConfig.cMin) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_min_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        } else if (cv > cardConfig.cMax) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_max_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 2
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val t3Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t3Top.start,
            t3Top.end
        )
        val t4Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t4Top.start,
            t4Top.end
        )
        val cScope = AppWorkCalcUtils.calculateSlope(
            pointList,
            cTop.start,
            cTop.end
        )

        Timber.w("t1Scope $t1Scope")
        Timber.w("t2Scope $t2Scope")
        Timber.w("t3Scope $t3Scope")
        Timber.w("t4Scope $t4Scope")
        Timber.w("cScope $cScope")
        if (t1Scope > cardConfig.scope || t2Scope > cardConfig.scope || t3Scope > cardConfig.scope || t4Scope > cardConfig.scope || cScope > cardConfig.scope) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_scope)
            )
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0
        var result3: Double = -1.0
        var result4: Double = -1.0

        var t1RadioValue = 0.0
        var t2RadioValue = 0.0
        var t3RadioValue = 0.0
        var t4RadioValue = 0.0

        // 获取临界值极限值
        if (bean.value.state == 0) {
            val t1V100 = t1v / 100
            val t2V100 = t2v / 100
            val t3V100 = t3v / 100
            val t4V100 = t4v / 100
            // 判断T值是否大于1000（配置获取）
            val resultMap =
                AppWorkCalcUtils.calc4LjRule(cardConfig, t1V100, t2V100, t3V100, t4V100)
            result1 = resultMap["result1"]!!
            result2 = resultMap["result2"]!!
            result3 = resultMap["result3"]!!
            result4 = resultMap["result4"]!!
            t1RadioValue = resultMap["t1RadioValue"]!!
            t2RadioValue = resultMap["t2RadioValue"]!!
            t3RadioValue = resultMap["t3RadioValue"]!!
            t4RadioValue = resultMap["t4RadioValue"]!!
        }
        Timber.w("result: T1: $t1v T2: $t2v T3: $t3v T4: $t4v C: $cv")
        Timber.w("T1: $t1v")
        Timber.w("T2: $t2v")
        Timber.w("T3: $t3v")
        Timber.w("T4: $t4v")
        Timber.w("C: $cv")

        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean._4LJ_T1,
                result = if (result1 > 0) "+" else "-",
                refer = "-",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "%.2f".format(t3v),
                t4Value = "%.2f".format(t4v),
                radioValue = "%.2f".format(t1RadioValue),
                cValue = cv.toString(),
                c2Value = "",
                flag = 0
            )
        )
        resultList.add(
            CaseResult(
                name = CaseBean._4LJ_T2,
                result = if (result2 > 0) "+" else "-",
                refer = "-",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "%.2f".format(t3v),
                t4Value = "%.2f".format(t4v),
                radioValue = "%.2f".format(t2RadioValue),
                cValue = cv.toString(),
                c2Value = "",
                flag = 0
            )
        )
        resultList.add(
            CaseResult(
                name = CaseBean._4LJ_T3,
                result = if (result3 > 0) "+" else "-",
                refer = "-",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "%.2f".format(t3v),
                t4Value = "%.2f".format(t4v),
                radioValue = "%.2f".format(t3RadioValue),
                cValue = cv.toString(),
                c2Value = "",
                flag = 0
            )
        )
        resultList.add(
            CaseResult(
                name = CaseBean._4LJ_T4,
                result = if (result4 > 0) "+" else "-",
                refer = "-",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "%.2f".format(t3v),
                t4Value = "%.2f".format(t4v),
                radioValue = "%.2f".format(t4RadioValue),
                cValue = cv.toString(),
                c2Value = "",
                flag = 0
            )
        )
        return Json.encodeToString(resultList)
    }

    fun genResultForIge(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // TODO 串口失联时，补救逻辑
//        if (AppParams.devMock) {
//            bean.value.name = "测试"
//            bean.value.gender = 1
//            bean.value.birthday = "1999-01-01"
//
//            val resultList = ArrayList<CaseResult>()
//            val months = AppLocalDateUtils.calcMonth(
//                AppLocalDateUtils.parseDate(bean.value.birthday),
//                LocalDate.now()
//            )
//            val realShow = "<3"
//            val referBean = ResultReferBean.getConfigMap()[CaseBean.TYPE_IGE] ?: return "No Refer"
//            val igeItem = referBean.findItem(CaseBean.IGE_T1, months)
//            resultList.add(
//                CaseResult(
//                    name = CaseBean.IGE_T1,
//                    result = "$realShow ${igeItem.unit}",
//                    refer = igeItem.remark,
//                    t1Value = "0.00",
//                    t2Value = "",
//                    t3Value = "",
//                    t4Value = "",
//                    radioValue = "",
//                    cValue = "0.0",
//                    c2Value = ""
//                )
//            )
//            return Json.encodeToString(resultList)
//        }

        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val tTop = topList[0]
        val cTop = topList[1]

        // 计算T、C值, baseValue是根据配置配置的底噪，目前ige不存在
        val tv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            tTop.start,
            tTop.end,
            cardConfig.scanPPMM
        )
        val cv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            cTop.start,
            cTop.end,
            cardConfig.scanPPMM
        )

        // C值校验
        if (cv < cardConfig.cMin) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_min_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        } else if (cv > cardConfig.cMax) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_max_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 2
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            tTop.start,
            tTop.end
        )
        val cScope = AppWorkCalcUtils.calculateSlope(
            pointList,
            cTop.start,
            cTop.end
        )
        if (t1Scope > cardConfig.scope || cScope > cardConfig.scope) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_scope)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        }

        // 结果计算
        var result: Double = -1.0
        if (bean.value.state == 0 && cv != 0.0) {
            // 根据T、C值计算内参
            val value = tv / cv * 1000
            Timber.w("===计算值IGE内参：${value}")
            // 查询多项式
            val varList: List<CardConfigVar> = cardConfig.varList
            Timber.w("===多项式的数量：${varList.size}")
            // 根据内参与多项式计算结果
            result = AppWorkCalcUtils.calIgeValue(value, varList)
        }

        // 组装返回结果
        val referBean = ResultReferBean.getConfigMap()[CaseBean.TYPE_IGE] ?: return "No Refer"
        val resultList = ArrayList<CaseResult>()
        val months = AppLocalDateUtils.calcMonth(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        )

        // 根据样本类型判断结果是否需要乘以全血系数
        if (bean.value.caseType == 2) {
            result *= cardConfig.typeScore
        }

        // 根据年龄判断ige结果是否超出范围
        val flag = referBean.findIgeItem(result, months)
        var realShow = "%.2f".format(result)
        if (result < 3) {
            realShow = "<3"
        } else if (result > 2000) {
            realShow = ">2000"
        }

        val igeItem = referBean.findItem(CaseBean.IGE_T1, months)
        //refer = "${igeItem.valueMin}-${igeItem.valueMax} ${referBean.unit}",
        resultList.add(
            CaseResult(
                name = CaseBean.IGE_T1,
                result = "$realShow ${igeItem.unit}",
                refer = igeItem.remark,
                t1Value = "%.2f".format(tv),
                t2Value = "",
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = cv.toString(),
                c2Value = "",
                flag = if (flag) 1 else 0
            )
        )
        return Json.encodeToString(resultList)
    }

    // 生成CRP结果-LOG表达式
    fun genResultForCrp1(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val t2Top = topList[1]
        val cTop = topList[2]

        // 计算T、C值, baseValue是根据配置配置的底噪，目前crp不存在
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val cv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            cTop.start,
            cTop.end,
            cardConfig.scanPPMM
        )

        // C值校验
        if (cv < cardConfig.cMin) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_min_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        } else if (cv > cardConfig.cMax) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_max_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 2
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val cScope = AppWorkCalcUtils.calculateSlope(
            pointList,
            cTop.start,
            cTop.end
        )
        if (t1Scope > cardConfig.scope || t2Scope > cardConfig.scope || cScope > cardConfig.scope) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_scope)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0
        if (bean.value.state == 0 && cv != 0.0) {
            val varList: List<CardConfigVar> = cardConfig.varList
            val value1 = t1v * (1 + log(cardConfig.cAvg / cv, 10.0))
            Timber.w("crpValue${value1}")
            result1 = AppWorkCalcUtils.calCrpValue(value1, varList, "crp")
            Timber.w("crpValue${result1}")
            Timber.w("crpValue${result1}")
            val value2 = t2v * (1 + log(cardConfig.cAvg / cv, 10.0))
            Timber.w("crpValue${value2}")
            result2 = AppWorkCalcUtils.calCrpValue(value2, varList, "sf")
            Timber.w("result2${result2}")
        }
        val referBean = ResultReferBean.getConfigMap()[CaseBean.TYPE_CRP] ?: return "No Refer"
        val months = AppLocalDateUtils.calcMonth(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        )
        val crpItem = referBean.findItem(CaseBean.CRP_T1, months)
        val sfItem = referBean.findItem(CaseBean.CRP_T2, months)

        // 根据样本类型判断结果是否需要乘以全血系数
        if (bean.value.caseType == 2) {
            result1 *= cardConfig.typeScore
            result2 *= cardConfig.typeScore
        }

        // 根据年龄判断ige结果是否超出范围
        val flagCrp = referBean.findCrpItem(result1)
        val flagSf = referBean.findSfItem(result2, months, result1)

        // CRP显示
        var real1Show = "%.2f".format(result1)
        if (result1 > 5) {
            real1Show = ">5"
        }

        // SF显示
        var real2Show = "%.2f".format(result2)
        if (result2 > 200) {
            real2Show = ">200"
        }

        // 组装返回结果
        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T1,
                result = "$real1Show${crpItem.unit}",
                refer = crpItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = cv.toString(),
                c2Value = "",
                flag = if (flagCrp) 1 else 0
            ),
        )
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T2,
                result = "$real2Show${sfItem.unit}",
                refer = sfItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = cv.toString(),
                c2Value = "",
                flag = if (flagSf) 1 else 0
            ),
        )
        return Json.encodeToString(resultList)
    }

    // 生成CRP结果-T/C*1000
    fun genResultForCrp2(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val t2Top = topList[1]
        val cTop = topList[2]

        // 计算T、C值, baseValue是根据配置配置的底噪，目前crp不存在
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val cv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            cTop.start,
            cTop.end,
            cardConfig.scanPPMM
        )

        // C值校验
        if (cv < cardConfig.cMin) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_min_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        } else if (cv > cardConfig.cMax) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_max_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 2
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val cScope = AppWorkCalcUtils.calculateSlope(
            pointList,
            cTop.start,
            cTop.end
        )
        if (t1Scope > cardConfig.scope || t2Scope > cardConfig.scope || cScope > cardConfig.scope) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_scope)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0
        if (bean.value.state == 0 && cv != 0.0) {
            val varList: List<CardConfigVar> = cardConfig.varList
            val value1 = t1v / cv * 1000
            Timber.w("crpValue${value1}")
            result1 = AppWorkCalcUtils.calCrpValue(value1, varList, "crp")
            Timber.w("crpValue${result1}")
            Timber.w("crpValue${result1}")
            val value2 = t2v / cv * 1000
            Timber.w("crpValue${value2}")
            result2 = AppWorkCalcUtils.calCrpValue(value2, varList, "sf")
            Timber.w("result2${result2}")
        }
        val referBean = ResultReferBean.getConfigMap()[CaseBean.TYPE_CRP] ?: return "No Refer"
        val months = AppLocalDateUtils.calcMonth(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        )
        Timber.w("当前结果CRP${result1}")
        Timber.w("当前结果SF${result2}")

        // 根据样本类型判断结果是否需要乘以全血系数
        if (bean.value.caseType == 2) {
            result1 *= cardConfig.typeScore
            result2 *= cardConfig.typeScore
        }
        val crpItem = referBean.findItem(CaseBean.CRP_T1, months)
        val sfItem = referBean.findItem(CaseBean.CRP_T2, months)

        // 根据年龄判断ige结果是否超出范围
        val flagCrp = referBean.findCrpItem(result1)
        val flagSf = referBean.findSfItem(result2, months, result1)

        // CRP显示
        var real1Show = "%.2f".format(result1)
        if (result1 > 5) {
            real1Show = ">5"
        }
        Timber.w("当前结果CRP${result1}")
        Timber.w("当前结果SF${result2}")

        // SF显示
        var real2Show = "%.2f".format(result2)
        if (result2 > 200) {
            real2Show = ">200"
        }

        // 组装返回结果
        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T1,
                result = "$real1Show${crpItem.unit}",
                refer = crpItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = cv.toString(),
                c2Value = "",
                flag = if (flagCrp) 1 else 0
            ),
        )
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T2,
                result = "$real2Show${sfItem.unit}",
                refer = sfItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = cv.toString(),
                c2Value = "",
                flag = if (flagSf) 1 else 0
            ),
        )
        return Json.encodeToString(resultList)
    }

    // 生成SF/CRP结果-LOG表达式
    fun genResultForSfCrp1(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val c1Top = topList[1]
        val t2Top = topList[2]
        val c2Top = topList[3]

        // 计算T、C值, baseValue是根据配置配置的底噪，目前crp不存在
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val c1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c1Top.start,
            c1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val c2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c2Top.start,
            c2Top.end,
            cardConfig.scanPPMM
        )

        // C值校验
        if (c1v < cardConfig.cMin || c2v < cardConfig.cMin) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_min_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        } else if (c1v > cardConfig.cMax || c2v > cardConfig.cMax) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_max_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 2
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val c1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c1Top.start,
            c1Top.end
        )
        val c2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c2Top.start,
            c2Top.end
        )
        if (t1Scope > cardConfig.scope || t2Scope > cardConfig.scope || c1Scope > cardConfig.scope || c2Scope > cardConfig.scope) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_scope)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0
        if (bean.value.state == 0 && c1v != 0.0) {
            val varList: List<CardConfigVar> = cardConfig.varList
            val value1 = t1v * (1 + log(cardConfig.cAvg / c1v, 10.0))
            Timber.w("crpValue1${value1}")
            result1 = AppWorkCalcUtils.calCrpValue(value1, varList, "crp")
            Timber.w("result1${result1}")
        }
        if (bean.value.state == 0 && c2v != 0.0) {
            val varList: List<CardConfigVar> = cardConfig.varList
            val value2 = t2v * (1 + log(cardConfig.cAvg / c2v, 10.0))
            Timber.w("crpValue2${value2}")
            result2 = AppWorkCalcUtils.calCrpValue(value2, varList, "sf")
            Timber.w("result2${result2}")
        }
        val referBean = ResultReferBean.getConfigMap()[CaseBean.TYPE_CRP] ?: return "No Refer"
        val months = AppLocalDateUtils.calcMonth(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        )
        val crpItem = referBean.findItem(CaseBean.CRP_T1, months)
        val sfItem = referBean.findItem(CaseBean.CRP_T2, months)

        // 根据样本类型判断结果是否需要乘以全血系数
        if (bean.value.caseType == 2) {
            result1 *= cardConfig.typeScore
            result2 *= cardConfig.typeScore
        }

        // 根据年龄判断ige结果是否超出范围
        val flagCrp = referBean.findCrpItem(result1)
        val flagSf = referBean.findSfItem(result2, months, result1)
        // CRP显示
        var real1Show = "%.2f".format(result1)
        if (result1 > 5) {
            real1Show = ">5"
        }

        // SF显示
        var real2Show = "%.2f".format(result2)
        if (result2 > 200) {
            real2Show = ">200"
        }

        // 组装返回结果
        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T1,
                result = "$real1Show${crpItem.unit}",
                refer = crpItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = if (flagCrp) 1 else 0
            ),
        )
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T2,
                result = "$real2Show${sfItem.unit}",
                refer = sfItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = if (flagSf) 1 else 0
            ),
        )
        return Json.encodeToString(resultList)
    }

    // 生成SF/CRP结果-T/C*1000
    fun genResultForSfCrp2(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val c1Top = topList[1]
        val t2Top = topList[2]
        val c2Top = topList[3]

        // 计算T、C值, baseValue是根据配置配置的底噪，目前crp不存在
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val c1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c1Top.start,
            c1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val c2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c2Top.start,
            c2Top.end,
            cardConfig.scanPPMM
        )

        // C值校验
        if (c1v < cardConfig.cMin || c2v < cardConfig.cMin) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_min_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        } else if (c1v > cardConfig.cMax || c2v > cardConfig.cMax) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_max_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 2
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val c1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c1Top.start,
            c1Top.end
        )
        val c2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c2Top.start,
            c2Top.end
        )
        if (t1Scope > cardConfig.scope || t2Scope > cardConfig.scope || c1Scope > cardConfig.scope || c2Scope > cardConfig.scope) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_scope)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0
        if (bean.value.state == 0 && c1v != 0.0) {
            val varList: List<CardConfigVar> = cardConfig.varList
            val value1 = t1v / c1v * 1000
            Timber.w("crpValue1${value1}")
            result1 = AppWorkCalcUtils.calCrpValue(value1, varList, "crp")
            Timber.w("result1${result1}")
        }
        if (bean.value.state == 0 && c2v != 0.0) {
            val varList: List<CardConfigVar> = cardConfig.varList
            val value2 = t2v / c2v * 1000
            Timber.w("crpValue2${value2}")
            result2 = AppWorkCalcUtils.calCrpValue(value2, varList, "sf")
            Timber.w("result2${result2}")
        }
        val referBean = ResultReferBean.getConfigMap()[CaseBean.TYPE_CRP] ?: return "No Refer"
        val months = AppLocalDateUtils.calcMonth(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        )
        val crpItem = referBean.findItem(CaseBean.CRP_T1, months)
        val sfItem = referBean.findItem(CaseBean.CRP_T2, months)

        // 根据样本类型判断结果是否需要乘以全血系数
        if (bean.value.caseType == 2) {
            result1 *= cardConfig.typeScore
            result2 *= cardConfig.typeScore
        }

        // 根据年龄判断ige结果是否超出范围
        val flagCrp = referBean.findCrpItem(result1)
        val flagSf = referBean.findSfItem(result2, months, result1)

        // CRP显示
        var real1Show = "%.2f".format(result1)
        if (result1 > 5) {
            real1Show = ">5"
        }

        // SF显示
        var real2Show = "%.2f".format(result2)
        if (result2 > 200) {
            real2Show = ">200"
        }

        // 组装返回结果
        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T1,
                result = "$real1Show${crpItem.unit}",
                refer = crpItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = if (flagCrp) 1 else 0
            ),
        )
        resultList.add(
            CaseResult(
                name = CaseBean.CRP_T2,
                result = "$real2Show${sfItem.unit}",
                refer = sfItem.remark,
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = if (flagSf) 1 else 0
            ),
        )
        return Json.encodeToString(resultList)
    }

    fun genResultFor3LJ(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val t2Top = topList[1]
        val t3Top = topList[2]
        val cTop = topList[3]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val t3v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t3Top.start,
            t3Top.end,
            cardConfig.scanPPMM
        )
        val cv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            cTop.start,
            cTop.end,
            cardConfig.scanPPMM
        )

        // C值校验
        if (cv < cardConfig.cMin) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_min_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 1
        } else if (cv > cardConfig.cMax) {
            if (AppParams.curUser.role != User.ROLE_CHECKER) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_max_c)
                )
            } else {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_report_check_error)
                )
            }
            bean.value.state = 2
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val t3Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t3Top.start,
            t3Top.end
        )
        val cScope = AppWorkCalcUtils.calculateSlope(
            pointList,
            cTop.start,
            cTop.end
        )
        if (t1Scope > cardConfig.scope || t2Scope > cardConfig.scope || t3Scope > cardConfig.scope || cScope > cardConfig.scope) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_scope)
            )
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0
        var result3: Double = -1.0

        var t1RadioValue = 0.0
        var t2RadioValue = 0.0
        var t3RadioValue = 0.0

        // 获取临界值极限值
        if (bean.value.state == 0) {
            val t1V100 = t1v / 100
            val t2V100 = t2v / 100
            val t3V100 = t3v / 100
            // 判断T值是否大于1000（配置获取）
            val resultMap =
                AppWorkCalcUtils.calc3LjRule(cardConfig, t1V100, t2V100, t3V100)
            result1 = resultMap["result1"]!!
            result2 = resultMap["result2"]!!
            result3 = resultMap["result3"]!!
            t1RadioValue = resultMap["t1RadioValue"]!!
            t2RadioValue = resultMap["t2RadioValue"]!!
            t3RadioValue = resultMap["t3RadioValue"]!!
        }
        Timber.w("result: T1: $t1v T2: $t2v T3: $t3v C: $cv")

        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean._3LJ_T1,
                result = if (result1 > 0) "+" else "-",
                refer = "-",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "%.2f".format(t3v),
                t4Value = "",
                radioValue = "%.2f".format(t1RadioValue),
                cValue = cv.toString(),
                c2Value = "",
                flag = 0
            )
        )
        resultList.add(
            CaseResult(
                name = CaseBean._3LJ_T2,
                result = if (result2 > 0) "+" else "-",
                refer = "-",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "%.2f".format(t3v),
                t4Value = "",
                radioValue = "%.2f".format(t2RadioValue),
                cValue = cv.toString(),
                c2Value = "",
                flag = 0
            )
        )
        resultList.add(
            CaseResult(
                name = CaseBean._3LJ_T3,
                result = if (result3 > 0) "+" else "-",
                refer = "-",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "%.2f".format(t3v),
                t4Value = "",
                radioValue = "%.2f".format(t3RadioValue),
                cValue = cv.toString(),
                c2Value = "",
                flag = 0
            )
        )
        return Json.encodeToString(resultList)
    }

    fun genResultFor2LJA(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val c1Top = topList[1]
        val t2Top = topList[2]
        val c2Top = topList[3]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val c1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c1Top.start,
            c1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val c2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c2Top.start,
            c2Top.end,
            cardConfig.scanPPMM
        )

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val c1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c1Top.start,
            c1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val c2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c2Top.start,
            c2Top.end
        )
        if (t1Scope > cardConfig.scope || c1Scope > cardConfig.scope || t2Scope > cardConfig.scope || c2Scope > cardConfig.scope) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_scope)
            )
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0

        // 获取临界值极限值
        if (bean.value.state == 0) {
            // 查询多项式
            val varList: List<CardConfigVar> = cardConfig.varList
            Timber.w("===多项式的数量：${varList.size}")

            // 根据T、C值计算内参
            val value1 = t1v / c1v
            val value2 = t2v / c2v
            Timber.w("===计算值2LJ内参1：${value1}")
            Timber.w("===计算值2LJ内参2：${value2}")

            if (varList.isNotEmpty() && varList.size >= 6) {
                // 根据内参与多项式计算结果
                // CRP
                var resultVar1 = AppWorkCalcUtils.calc2LJValue(value1, varList.subList(0, 3))
                // 铁蛋白
                var resultVar2 = AppWorkCalcUtils.calc2LJValue(value2, varList.subList(3, 6))

                // 根据样本类型判断结果是否需要乘以全血系数
                if (bean.value.caseType == 2) {
                    resultVar1 *= cardConfig.typeScore
                    resultVar2 *= cardConfig.typeScore
                }

                result1 = resultVar1
                result2 = resultVar2
            } else {
                result1 = value1
                result2 = value2
            }
        }
        Timber.w("result: T1: $t1v C1: $c1v T2: $t2v C2: $c2v")

        val result1Str = if (result1 > 0.0) {
            "%.2f".format(result1) + "mg/L"
        } else if (result1 == 0.0) {
            ">10mg/L"
        } else {
            "<0.5mg/L"
        }

        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean._2LJ_A_T1,
                result = result1Str,
                refer = "",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = 0
            )
        )

        val result2Str = if (result2 > 0.0) {
            "%.2f".format(result2) + "ng/mL"
        } else if (result2 == 0.0) {
            ">200ng/mL"
        } else {
            "<5ng/mL"
        }

        resultList.add(
            CaseResult(
                name = CaseBean._2LJ_A_T2,
                result = result2Str,
                refer = "",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = 0
            )
        )
        return Json.encodeToString(resultList)
    }

    fun genResultFor2LJB(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val c1Top = topList[1]
        val t2Top = topList[2]
        val c2Top = topList[3]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val c1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c1Top.start,
            c1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val c2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c2Top.start,
            c2Top.end,
            cardConfig.scanPPMM
        )

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val c1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c1Top.start,
            c1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val c2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c2Top.start,
            c2Top.end
        )
        if (t1Scope > cardConfig.scope || c1Scope > cardConfig.scope || t2Scope > cardConfig.scope || c2Scope > cardConfig.scope) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_scope)
            )
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0

        // 获取临界值极限值
        if (bean.value.state == 0) {
            // 查询多项式
            val varList: List<CardConfigVar> = cardConfig.varList
            Timber.w("===多项式的数量：${varList.size}")
            Timber.w("===多项式：${JsonUtils.toJson(varList)}")

            // 根据T、C值计算内参
            val value1 = t1v / c1v
            val value2 = t2v / c2v
            Timber.w("===计算值2LJ内参1：${value1}")
            Timber.w("===计算值2LJ内参2：${value2}")

            if (varList.isNotEmpty() && varList.size >= 4) {
                // 根据内参与多项式计算结果
                // 糖化血红蛋白
                var resultVar1 = AppWorkCalcUtils.calc2LJValue(value1, varList.subList(0, 2))
                // 胱抑素C
                var resultVar2 = AppWorkCalcUtils.calc2LJValue(value2, varList.subList(2, 4))
                Timber.w("result: resultVar1: $resultVar1 resultVar2: $resultVar2")

                // 根据样本类型判断结果是否需要乘以全血系数
                if (bean.value.caseType == 2) {
                    resultVar1 *= cardConfig.typeScore
                    resultVar2 *= cardConfig.typeScore
                }

                result1 = resultVar1
                result2 = resultVar2
            } else {
                result1 = value1
                result2 = value2
            }
        }
        Timber.w("result: T1: $t1v C1: $c1v T2: $t2v C2: $c2v")

        val result1Str = if (result1 > 0.0) {
            "%.2f".format(result1) + "%"
        } else if (result1 == 0.0) {
            ">6%"
        } else {
            "<3%"
        }

        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean._2LJ_B_T1,
                result = result1Str,
                refer = "",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = 0
            )
        )

        val result2Str = if (result2 > 0.0) {
            "%.2f".format(result2) + "mg/L"
        } else if (result2 == 0.0) {
            ">2mg/L"
        } else {
            "<1mg/L"
        }

        resultList.add(
            CaseResult(
                name = CaseBean._2LJ_B_T2,
                result = result2Str,
                refer = "",
                t1Value = "%.2f".format(t1v),
                t2Value = "%.2f".format(t2v),
                t3Value = "",
                t4Value = "",
                radioValue = "",
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = 0
            )
        )
        return Json.encodeToString(resultList)
    }

    fun genResultFor2LJBM(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val c1Top = topList[1]
        val t2Top = topList[2]
        val c2Top = topList[3]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val c1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c1Top.start,
            c1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val c2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c2Top.start,
            c2Top.end,
            cardConfig.scanPPMM
        )

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val c1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c1Top.start,
            c1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val c2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c2Top.start,
            c2Top.end
        )
        if (t1Scope > cardConfig.scope || c1Scope > cardConfig.scope || t2Scope > cardConfig.scope || c2Scope > cardConfig.scope) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_scope)
            )
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0

        // 获取临界值极限值
        if (bean.value.state == 0) {
            // 查询多项式
            val varList: List<CardConfigVar> = cardConfig.varList
            Timber.w("===多项式的数量：${varList.size}")
            Timber.w("===多项式：${JsonUtils.toJson(varList)}")

            // 根据T、C值计算内参
            val value1 = t1v / c1v
            val value2 = t2v / c2v
            Timber.w("===计算值2LJ内参1：${value1}")
            Timber.w("===计算值2LJ内参2：${value2}")

            if (varList.isNotEmpty() && varList.size >= 4) {
                // 根据内参与多项式计算结果
                // 糖化血红蛋白
                var resultVar1 = AppWorkCalcUtils.calc2LJValue(value1, varList.subList(0, 2))
                // 胱抑素C
                var resultVar2 = AppWorkCalcUtils.calc2LJValue(value2, varList.subList(2, 4))
                Timber.w("result: resultVar1: $resultVar1 resultVar2: $resultVar2")

                // 根据样本类型判断结果是否需要乘以全血系数
                if (bean.value.caseType == 2) {
                    resultVar1 *= cardConfig.typeScore
                    resultVar2 *= cardConfig.typeScore
                }

                result1 = resultVar1
                result2 = resultVar2
            } else {
                result1 = value1
                result2 = value2
            }
        }
        Timber.w("result: T1: $t1v C1: $c1v T2: $t2v C2: $c2v")

        val age = AppLocalDateUtils.calcAge(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        ).toDouble()

        var glycatedHaemoglobin = result1
        if (result1 == 0.0) {
            glycatedHaemoglobin = 6.0
        } else if (result1 < 0) {
            glycatedHaemoglobin = 4.0
        }

        var cystatinC = result2
        if (result2 == 0.0) {
            cystatinC = 2.5
        } else if (result2 < 0) {
            cystatinC = 0.6
        }

        val assessResult = assessAge(age, glycatedHaemoglobin, cystatinC)
        var bioAge = age
        var baa = 0.0
        if (assessResult != null) {
            // TODO 引入魔法值，提高情绪价值
            bioAge = assessResult.bioAge - 2
            baa = assessResult.baa - 2
            if (baa > 3) {
                baa = 3.0
                bioAge = age + 3
            } else if (baa < -7) {
                baa = -7.0
                bioAge = age - 7
            }
        }

        val result1Str = if (result1 > 0.0) {
            "%.2f".format(result1) + "%"
        } else if (result1 == 0.0) {
            ">6%"
        } else {
            "<4%"
        }

        val result2Str = if (result2 > 0.0) {
            "%.2f".format(result2) + "mg/L"
        } else if (result2 == 0.0) {
            ">2.5mg/L"
        } else {
            "<0.6mg/L"
        }

        var assessResultStr = "green|正常"
        if (baa > 1) {
            assessResultStr = "red|衰老加速"
        } else if (baa < -1) {
            assessResultStr = "green|衰老减速"
        }

        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean._2LJ_B_M_T1,
                result = bioAge.toInt().toString(),
                refer = age.toInt().toString(),
                t1Value = t1v.toString(),
                t2Value = t2v.toString(),
                t3Value = result1Str,
                t4Value = result2Str,
                radioValue = assessResultStr,
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = 0
            )
        )
        return Json.encodeToString(resultList)
    }

    fun genResultFor2LJBF(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("org topList: $topList")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("final topList: $topList")

        val t1Top = topList[0]
        val c1Top = topList[1]
        val t2Top = topList[2]
        val c2Top = topList[3]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val c1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c1Top.start,
            c1Top.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val c2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c2Top.start,
            c2Top.end,
            cardConfig.scanPPMM
        )

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val c1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c1Top.start,
            c1Top.end
        )
        val t2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t2Top.start,
            t2Top.end
        )
        val c2Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            c2Top.start,
            c2Top.end
        )
        if (t1Scope > cardConfig.scope || c1Scope > cardConfig.scope || t2Scope > cardConfig.scope || c2Scope > cardConfig.scope) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_scope)
            )
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        var result2: Double = -1.0

        // 获取临界值极限值
        if (bean.value.state == 0) {
            // 查询多项式
            val varList: List<CardConfigVar> = cardConfig.varList
            Timber.w("===多项式的数量：${varList.size}")
            Timber.w("===多项式：${JsonUtils.toJson(varList)}")

            // 根据T、C值计算内参
            val value1 = t1v / c1v
            val value2 = t2v / c2v
            Timber.w("===计算值2LJ内参1：${value1}")
            Timber.w("===计算值2LJ内参2：${value2}")

            if (varList.isNotEmpty() && varList.size >= 4) {
                // 根据内参与多项式计算结果
                // 糖化血红蛋白
                var resultVar1 = AppWorkCalcUtils.calc2LJValue(value1, varList.subList(0, 2))
                // 胱抑素C
                var resultVar2 = AppWorkCalcUtils.calc2LJValue(value2, varList.subList(2, 4))
                Timber.w("result: resultVar1: $resultVar1 resultVar2: $resultVar2")

                // 根据样本类型判断结果是否需要乘以全血系数
                if (bean.value.caseType == 2) {
                    resultVar1 *= cardConfig.typeScore
                    resultVar2 *= cardConfig.typeScore
                }

                result1 = resultVar1
                result2 = resultVar2
            } else {
                result1 = value1
                result2 = value2
            }
        }
        Timber.w("result: T1: $t1v C1: $c1v T2: $t2v C2: $c2v")

        val age = AppLocalDateUtils.calcAge(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        ).toDouble()

        var glycatedHaemoglobin = result1
        if (result1 == 0.0) {
            glycatedHaemoglobin = 6.0
        } else if (result1 < 0) {
            glycatedHaemoglobin = 4.0
        }

        var cystatinC = result2
        if (result2 == 0.0) {
            cystatinC = 2.5
        } else if (result2 < 0) {
            cystatinC = 0.6
        }

        val assessResult = assessAge(age, glycatedHaemoglobin, cystatinC)
        var bioAge = age
        var baa = 0.0
        if (assessResult != null) {
            // TODO 引入魔法值，提高情绪价值
            bioAge = assessResult.bioAge - 2
            baa = assessResult.baa - 2
            if (baa > 3) {
                baa = 3.0
                bioAge = age + 3
            } else if (baa < -7) {
                baa = -7.0
                bioAge = age - 7
            }
        }

        val result1Str = if (result1 > 0.0) {
            "%.2f".format(result1) + "%"
        } else if (result1 == 0.0) {
            ">6%"
        } else {
            "<4%"
        }

        val result2Str = if (result2 > 0.0) {
            "%.2f".format(result2) + "mg/L"
        } else if (result2 == 0.0) {
            ">2.5mg/L"
        } else {
            "<0.6mg/L"
        }

        var assessResultStr = "green|正常"
        if (baa > 1) {
            assessResultStr = "red|衰老加速"
        } else if (baa < -1) {
            assessResultStr = "green|衰老减速"
        }

        val resultList = ArrayList<CaseResult>()
        resultList.add(
            CaseResult(
                name = CaseBean._2LJ_B_F_T1,
                result = bioAge.toInt().toString(),
                refer = age.toInt().toString(),
                t1Value = t1v.toString(),
                t2Value = t2v.toString(),
                t3Value = result1Str,
                t4Value = result2Str,
                radioValue = assessResultStr,
                cValue = c1v.toString(),
                c2Value = c2v.toString(),
                flag = 0
            )
        )
        return Json.encodeToString(resultList)
    }

    @SuppressLint("DefaultLocale")
    fun genResultFor3LJBAL1(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("genResultFor3LJBAL1 org topList: start: ${topList[0].start} end: ${topList[0].end}")
        Timber.w("genResultFor3LJBAL1 org topList: start: ${topList[1].start} end: ${topList[1].end}")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("genResultFor3LJBAL1 final topList: start: ${topList[0].start} end: ${topList[0].end}")
        Timber.w("genResultFor3LJBAL1 final topList: start: ${topList[1].start} end: ${topList[1].end}")

        val t1Top = topList[0]
        // 去除多余
//        val t2Top = topList[1]
//        val t3Top = topList[2]
        val cTop = topList[1]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        // 去除多余
//        val t2v = AppWorkCalcUtils.calculatePeakArea(
//            pointList,
//            t2Top.start,
//            t2Top.end,
//            cardConfig.scanPPMM
//        )
//        val t3v = AppWorkCalcUtils.calculatePeakArea(
//            pointList,
//            t3Top.start,
//            t3Top.end,
//            cardConfig.scanPPMM
//        )
        val cv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            cTop.start,
            cTop.end,
            cardConfig.scanPPMM
        )

        // TODO 演示屏蔽
        // C值校验
        Timber.w("genResultFor3LJBAL1 cardConfig.cMin ${cardConfig.cMin}")
        if (cv < cardConfig.cMin) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_error)
            )
            bean.value.state = 1
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        // 去除多余
//        val t2Scope = AppWorkCalcUtils.calculateSlope(
//            pointList,
//            t2Top.start,
//            t2Top.end
//        )
//        val t3Scope = AppWorkCalcUtils.calculateSlope(
//            pointList,
//            t3Top.start,
//            t3Top.end
//        )
        val cScope = AppWorkCalcUtils.calculateSlope(
            pointList,
            cTop.start,
            cTop.end
        )

        // TODO 演示屏蔽
        if (t1Scope > cardConfig.scope
            // 去除多余
//            || t2Scope > cardConfig.scope || t3Scope > cardConfig.scope
            || cScope > cardConfig.scope
        ) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_scope)
            )
            bean.value.state = 1
        }

        val t1TopStart = pointList[t1Top.start.toInt()]
        val t1TopEnd = pointList[t1Top.end.toInt()]
        val cTopStart = pointList[cTop.start.toInt()]
        val cTopEnd = pointList[cTop.end.toInt()]

        var t1TopMin = t1TopStart.y
        if (t1TopMin > t1TopEnd.y) {
            t1TopMin = t1TopEnd.y
        }
        var cTopMin = cTopStart.y
        if (cTopMin > cTopEnd.y) {
            cTopMin = cTopEnd.y
        }

        // TODO 演示屏蔽
        if (abs(t1TopMin - cTopMin) >= MAX_DIFF_VAL) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_check_error)
            )
            bean.value.state = 1
        }

        // 结果计算
        var result1: Double = -1.0
        // 去除多余
//        var result2: Double = -1.0
//        var result3: Double = -1.0

        // 根据T、C值计算内参
        val value1 = t1v / cv
        // 去除多余
//            val value2 = t2v / cv
//            val value3 = t3v / cv
        Timber.w("===计算值3LJ内参1：${value1}")

        // 获取临界值极限值
        if (bean.value.state == 0) {
            // 查询多项式
            val varList: List<CardConfigVar> = cardConfig.varList
            Timber.w("===多项式的数量：${varList.size}")
            Timber.w("===多项式：${JsonUtils.toJson(varList)}")

            // 去除多余
//            Timber.w("===计算值3LJ内参2：${value2}")
//            Timber.w("===计算值3LJ内参3：${value3}")

            if (varList.isNotEmpty() && varList.size >= 4) {
                // 根据内参与多项式计算结果
                // hsCRP
                var resultVar1 = AppWorkCalcUtils.calc3LJBAL1Value(value1, varList.subList(0, 4))
                // 去除多余
//                // 糖化血红蛋白
//                var resultVar2 = AppWorkCalcUtils.calc3LJBAL1Value(value2, varList.subList(4, 7))
                // 胱抑素C
//                var resultVar3 = AppWorkCalcUtils.calc3LJBAL1Value(value3, varList.subList(4, 8))
                Timber.w("result: resultVar1: $resultVar1")

                // 根据样本类型判断结果是否需要乘以全血系数
                if (bean.value.caseType == 2) {
                    resultVar1 *= cardConfig.typeScore
                    // 去除多余
//                    resultVar2 *= cardConfig.typeScore
//                    resultVar3 *= cardConfig.typeScore
                }

                result1 = resultVar1
                // 去除多余
//                result2 = resultVar2
//                result3 = resultVar3
            } else {
                result1 = value1
                // 去除多余
//                result2 = value2
//                result3 = value3
            }
        }
        Timber.w("result: T1: $t1v C: $cv")

        val age = AppLocalDateUtils.calcAge(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        ).toDouble()

        // 服务端计算结果
//        val cystatinC = result1 / 0.55
//        val cReactiveProtein = result3 / 0.55
//        val glycatedHaemoglobin = result2

//        val assessResult = assessAge(age, 0.0, cystatinC, cReactiveProtein)
//        var bioAge = age
//        var baa = 0.0
//        if (assessResult != null) {
//            bioAge = assessResult.bioAge
//            baa = assessResult.baa
//        }

        if (result1 <= 0.0) {
            result1 = value1
        }

//        // TODO 演示随机
//        var randResult1 = result1
//        if (randResult1 <= 0.0) {
//            randResult1 = Random.nextDouble(0.1, 0.8)
//        } else if (randResult1 > 10) {
//            randResult1 = Random.nextDouble(9.0, 10.0)
//        }
//        result1 = String.format("%.2f", randResult1).toDouble()

        val result1Str = "%.2f".format(result1) + "mg/L"

        // 服务端计算结果
//        var assessResultStr = "green|正常"
//        if (baa > 1) {
//            assessResultStr = "red|衰老加速"
//        } else if (baa < -1) {
//            assessResultStr = "green|衰老减速"
//        }

        val resultList = ArrayList<CaseResult>()
        // 服务端计算结果
        resultList.add(
            CaseResult(
                name = CaseBean._3LJ_BIOAGE_L1_MAIN_Final,
                result = 0.toString(),
                refer = age.toInt().toString(),
                radioValue = "",
                t1Value = t1v.toString(),
                cValue = cv.toString(),
                c2Value = cv.toString(),
                t1ValueStr = result1Str,
            )
        )
        resultList.add(
            CaseResult(
                name = CaseBean._3LJ_BIOAGE_L1_MAIN_T2,
                result = result1.toString(),
                refer = "0-10mg/L",
                radioValue = result1Str,
                t1Value = t1v.toString(),
                cValue = cv.toString(),
            )
        )
        return Json.encodeToString(resultList)
    }

    fun genResultForBACRP(
        actionState: MutableStateFlow<ActionState>,
        cardConfig: CardConfig,
        bean: MutableStateFlow<CaseBean>,
        pointList: ArrayList<CasePoint>
    ): String {
        // 获取峰值配置，查询T、C值
        val topList: List<CardConfigTop> = cardConfig.topList
        Timber.w("genResultForBACRP org topList: start: ${topList[0].start} end: ${topList[0].end}")
        Timber.w("genResultForBACRP org topList: start: ${topList[1].start} end: ${topList[1].end}")
        Timber.w("genResultForBACRP org topList: start: ${topList[2].start} end: ${topList[2].end}")
        Timber.w("genResultForBACRP org topList: start: ${topList[3].start} end: ${topList[3].end}")
        findRealMinAndMaxTopList(topList, pointList)
        Timber.w("genResultForBACRP final topList: start: ${topList[0].start} end: ${topList[0].end}")
        Timber.w("genResultForBACRP final topList: start: ${topList[1].start} end: ${topList[1].end}")
        Timber.w("genResultForBACRP final topList: start: ${topList[2].start} end: ${topList[2].end}")
        Timber.w("genResultForBACRP final topList: start: ${topList[3].start} end: ${topList[3].end}")

        val t1Top = topList[0]
        val cTop = topList[1]
        val t2Top = topList[2]
        val c2Top = topList[3]

        // 计算T、C值
        val t1v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t1Top.start,
            t1Top.end,
            cardConfig.scanPPMM
        )
        val cv = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            cTop.start,
            cTop.end,
            cardConfig.scanPPMM
        )
        val t2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            t2Top.start,
            t2Top.end,
            cardConfig.scanPPMM
        )
        val c2v = AppWorkCalcUtils.calculatePeakArea(
            pointList,
            c2Top.start,
            c2Top.end,
            cardConfig.scanPPMM
        )

        // C值校验
        Timber.w("genResultForBACRP cardConfig.cMin ${cardConfig.cMin}")
        if (cv < cardConfig.cMin) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_opt_error)
            )
            bean.value.state = 1
        }

        // 斜率校验
        val t1Scope = AppWorkCalcUtils.calculateSlope(
            pointList,
            t1Top.start,
            t1Top.end
        )
        val cScope = AppWorkCalcUtils.calculateSlope(
            pointList,
            cTop.start,
            cTop.end
        )

        if (t1Scope > cardConfig.scope || cScope > cardConfig.scope
        ) {
            actionState.value = ActionState(
                EVT_DEV_ERROR,
                App.getContext().getString(R.string.work_report_opt_error)
            )
            bean.value.state = 1
        }

        // 新批次不适用
//        val t1TopMax = pointList.slice(t1Top.start.toInt()..t1Top.end.toInt()).maxOf { it.y }
//        val cTopStart = pointList[cTop.start.toInt()]
//        val cTopEnd = pointList[cTop.end.toInt()]
//
//        var cTopMin = cTopStart.y
//        if (cTopMin > cTopEnd.y) {
//            cTopMin = cTopEnd.y
//        }

//        if (abs(t1TopMax - cTopMin) <= MIN_DIFF_VAL) {
//            actionState.value = ActionState(
//                EVT_DEV_ERROR,
//                App.getContext().getString(R.string.work_report_check_error)
//            )
//            bean.value.state = 1
//        }

        // 结果计算
        var result1: Double = -1.0

        // 根据T、C值计算内参
        val value1 = t1v / cv
        Timber.w("===计算值3LJ内参1：${value1}")

        // 获取临界值极限值
        if (bean.value.state == 0) {
            // 查询多项式
            val varList: List<CardConfigVar> = cardConfig.varList
            Timber.w("===多项式的数量：${varList.size}")
            Timber.w("===多项式：${JsonUtils.toJson(varList)}")

            if (varList.isNotEmpty() && varList.size >= 4) {
                // 根据内参与多项式计算结果
                // hsCRP
                var resultVar1 = AppWorkCalcUtils.calc3LJBAL1Value(value1, varList.subList(0, 4))
                Timber.w("result: resultVar1: $resultVar1")

                // 根据样本类型判断结果是否需要乘以全血系数
                if (bean.value.caseType == 2) {
                    resultVar1 *= cardConfig.typeScore
                }

                result1 = resultVar1
            } else {
                result1 = value1
            }
        }
        Timber.w("result: T1: $t1v C: $cv T2: $t2v C2: $c2v")

        val age = AppLocalDateUtils.calcAge(
            AppLocalDateUtils.parseDate(bean.value.birthday),
            LocalDate.now()
        ).toDouble()

        if (result1 <= 0.0) {
            result1 = value1
        }

        val result1Str = "%.2f".format(result1) + "mg/L"

        val resultList = ArrayList<CaseResult>()
        // 服务端计算结果
        resultList.add(
            CaseResult(
                name = CaseBean._3LJ_BIOAGE_L1_MAIN_Final,
                result = 0.toString(),
                refer = age.toInt().toString(),
                radioValue = "",
                t1Value = t1v.toString(),
                cValue = cv.toString(),
                t1ValueStr = result1Str,
            )
        )
        resultList.add(
            CaseResult(
                name = CaseBean._3LJ_BIOAGE_L1_MAIN_T2,
                result = result1.toString(),
                refer = "0-10mg/L",
                radioValue = result1Str,
                t1Value = t1v.toString(),
                cValue = cv.toString(),
                t2Value = t2v.toString(),
                c2Value = c2v.toString()
            )
        )
        return Json.encodeToString(resultList)
    }

    private fun retrieve13BitAdcData(rawData: ByteArray): IntArray? {
        if (rawData.isEmpty()) {
            return null
        }

        try {
            // 将字节数据转换为16位无符号整数数组
            val dataArray = IntArray(rawData.size / 2)
            val buffer = ByteBuffer.wrap(rawData).order(ByteOrder.LITTLE_ENDIAN)

            for (i in dataArray.indices) {
                val value = buffer.short.toUShort().toInt()
                // 检查是否为有效的13位值
                if (value > 0x1FFF) {
                    Timber.w("found invalid ADC value: 0x${value.toString(16)}")
                    return null
                }
                dataArray[i] = value
            }

            Timber.w("retrieved ${dataArray.size} 13bit ADC data points")
            return dataArray
        } catch (e: Exception) {
            Timber.w("error processing ADC data: ${e.message}")
            return null
        }
    }

    private fun findRealMinAndMaxTopList(
        topList: List<CardConfigTop>,
        pointList: List<CasePoint>,
    ) {
        val checkGTCount = 3
        val checkSameTotalCount = 5

        for (top in topList) {
            var start = top.start.toInt()
            var startVal = pointList[start].y
            var gtStartCount = checkGTCount
            var sameStartCount = checkSameTotalCount
            while (true) {
                if (start <= 0) {
                    break
                }
                if (sameStartCount <= 0) {
                    start += 3
                    top.start = start.toDouble()
                    break
                }

                start--
                val tmpVal = pointList[start].y
                if (tmpVal > startVal) {
                    gtStartCount--
                    if (gtStartCount <= 0) {
                        top.start = start.toDouble()
                        break
                    }
                } else {
                    gtStartCount = checkGTCount
                    if (tmpVal == startVal) {
                        sameStartCount--
                    } else {
                        sameStartCount = checkSameTotalCount
                    }
                    startVal = tmpVal
                }
            }

            var end = top.end.toInt()
            var endVal = pointList[end].y
            var gtEndCount = checkGTCount
            var sameEndCount = checkSameTotalCount
            while (true) {
                if (end >= pointList.size) {
                    break
                }
                if (sameEndCount <= 0) {
                    end -= 3
                    top.end = end.toDouble()
                    break
                }

                end++
                val tmpVal = pointList[end].y
                if (tmpVal > endVal) {
                    gtEndCount--
                    if (gtEndCount <= 0) {
                        top.end = end.toDouble()
                        break
                    }
                } else {
                    gtEndCount = checkGTCount
                    if (tmpVal == endVal) {
                        sameEndCount--
                    } else {
                        sameEndCount = checkSameTotalCount
                    }
                    endVal = tmpVal
                }
            }
        }
    }

    private fun assessAge(
        age: Double,
        glycatedHaemoglobin: Double,
        cystatinC: Double,
        cReactiveProtein: Double? = null,
    ): BAAService.BAAResult? {
        val service = BAAService()
        val bioFeatures = if (cReactiveProtein != null) {
            listOf(
                BioFeature("age", age),
                BioFeature("glycated_haemoglobin", glycatedHaemoglobin),
                BioFeature("cystatin_c", cystatinC),
                BioFeature("log_c_reactive_protein", cReactiveProtein)
            )
        } else {
            listOf(
                BioFeature("age", age),
                BioFeature("glycated_haemoglobin", glycatedHaemoglobin),
                BioFeature("cystatin_c", cystatinC)
            )
        }

        val validation = service.validateBioFeatures(bioFeatures)
        if (validation.isValid) {
            return service.calculateBAA(bioFeatures)
        } else {
            println("assessAge 数据验证失败: ${validation.errors}")
            return null
        }
    }
}