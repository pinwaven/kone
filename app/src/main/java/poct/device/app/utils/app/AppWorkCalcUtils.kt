@file:Suppress("UNCHECKED_CAST")

package poct.device.app.utils.app

import android.annotation.SuppressLint
import org.apache.commons.lang.math.NumberUtils
import poct.device.app.App
import poct.device.app.bean.card.CardConfig
import poct.device.app.entity.CasePoint
import poct.device.app.thirdparty.model.sbedge.resp.CardConfigVar
import timber.log.Timber
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object AppWorkCalcUtils {
    /**
     * 根据C值计算，峰的偏移量
     * 找到质控点的所在位置，确定当前配置与实际位置的偏移量并纠正
     */
    fun findCValuePos(
        pointList: ArrayList<CasePoint>,
        start: Double,
        end: Double,
        scanPm: Int
    ): Double {
        // 记录C值中心点位置
        val oldCValue = (start + end) / 2
        // 从配置获取1毫米被切成了多少份
        if (scanPm <= 0 || start >= end || pointList.isEmpty()) {
            return 0.0
        }
        var startPoint = (start * scanPm).toInt()
        var endPoint = (end * scanPm).toInt()
        if (startPoint < 0) {
            startPoint = 0
        }
        if (endPoint < 0) {
            endPoint = 0
        }
        if (startPoint >= pointList.size - 1) {
            startPoint = pointList.size - 1
        }
        if (endPoint >= pointList.size - 1) {
            endPoint = pointList.size - 1
        }
        var curCValue: Double
        // 收集面积所有的可能型
        val tResultMap = calcTopMap(startPoint, endPoint, 2.0, scanPm, pointList)
        if (tResultMap.isNotEmpty()) {
            // 找到面积最大时对应的位置
            curCValue = (tResultMap.maxBy { it.value }.key).toDouble() / scanPm + 1
            Timber.w("C值初始中心位置:${oldCValue}")
            Timber.w("C值计算后中心位置:${curCValue}")
            Timber.w("C值偏移量:${curCValue - oldCValue}")
            return curCValue - oldCValue
        } else {
            return 0.0
        }
    }

    /**
     * 根据C值计算，峰的偏移量
     * 找到质控点的所在位置，确定当前配置与实际位置的偏移量并纠正
     */
    fun findCValuePosV2(
        pointList: ArrayList<CasePoint>,
        start: Double,
        end: Double,
        scanPm: Int
    ): Double {
        // 从配置获取1毫米被切成了多少份
        if (scanPm <= 0 || start >= end || pointList.isEmpty()) {
            return 0.0
        }

        // 记录C值中心点位置
        val oldCValue = (start + end) / 2
        var startPoint = start.toInt()
        var endPoint = end.toInt()
        if (startPoint < 0) {
            startPoint = 0
        }
        if (endPoint < 0) {
            endPoint = 0
        }
        if (startPoint >= pointList.size - 1) {
            startPoint = pointList.size - 1
        }
        if (endPoint >= pointList.size - 1) {
            endPoint = pointList.size - 1
        }

        var curCValue: Double

        // 收集面积所有的可能型
        val tResultMap = calcTopMapV2(startPoint, endPoint, scanPm, pointList)
        if (tResultMap.isNotEmpty()) {
            // 找到面积最大时对应的位置
            val maxValue = tResultMap.maxBy { it.value }.value
            val maxKeys = tResultMap.filter { it.value == maxValue }.keys.toList()
            val sortedKeys = maxKeys.sorted()
            val middleIndex = sortedKeys.size / 2

            curCValue = if (sortedKeys.size % 2 == 1) {
                // 奇数个元素，直接取中间
                sortedKeys[middleIndex].toDouble()
            } else {
                // 偶数个元素，取中间两个的左边那个（或者右边，根据需求调整）
                // 这里取左边那个，如果想要右边那个，改为 sortedKeys[middleIndex]
                sortedKeys[middleIndex - 1].toDouble()
            }

            Timber.w("C值初始中心位置:${oldCValue}")
            Timber.w("C值计算后中心位置:${curCValue}")
            Timber.w("C值偏移量:${curCValue - oldCValue}")
            return curCValue - oldCValue
        } else {
            return 0.0
        }
    }

    /**
     *  峰位值计算T、C值
     *  1、纠正偏移量找到峰的真正开始位置与结束位置
     *  2、判断峰的中心点峰值与重数比较，判断当前峰时波峰还是波谷
     *  3、波峰：以1.5mm的从开始点到结束点找到最大面积为T值
     *  4、波谷：以中心点左右0.75mm直接计算面积为T值
     *  5、底噪：以重数计算底噪
     */
    fun findTopValue1(
        pointList: ArrayList<CasePoint>,
        start: Double,
        end: Double,
        offset: Double,
        scanPm: Int
    ): Double {
        // 计算面积时的矩形框宽度
        val wideTotal = 1.5
        // 从配置获取1毫米被切成了多少份
        if (scanPm <= 0 || start >= end || pointList.isEmpty()) {
            return 0.0
        }
        // 当前以微米为单位计算高度
        val high: Double = 1.0 * 1000 / scanPm.toDouble()
        // 规范开始点与结束点
        var startPoint = ((start + offset) * scanPm).toInt()
        var endPoint = ((end + offset) * scanPm).toInt()
        if (startPoint < 0) {
            startPoint = 0
        }
        if (endPoint < 0) {
            endPoint = 0
        }
        if (startPoint >= pointList.size - 1) {
            startPoint = pointList.size - 1
        }
        if (endPoint >= pointList.size - 1) {
            endPoint = pointList.size - 1
        }
        // 获取最初的开始位置与结束位置
        Timber.w("firstStartPoint:${startPoint}")
        Timber.w("firstEndPoint:${endPoint}")
        if (startPoint == endPoint) {
            return 0.0
        }
        // 获取当前中心点值
        val centerX = (startPoint + endPoint) / 2
        val centerY = pointList[centerX].y
        // 查询当前重数
        val maxNumber = findMostFrequentNumber(pointList)
        // 计算当前的底噪面积
        val newBaseValue = maxNumber * high * 1.5 * scanPm.toDouble()
        // 找到最大的面积
        var tMaxResult = 0.0
        // 判断当前图像时波峰还是波谷
        if (centerY > maxNumber) {
            // 波峰时计算T值与C值
            // 收集面积所有的可能型
            val tResultMap = calcTopMap(startPoint, endPoint, wideTotal, scanPm, pointList)
            // 找到最大的面积
            tMaxResult = tResultMap.maxBy { it.value }.value
        } else {
            // 波谷时计算T值与C值
            // 查找中间值
            val centerValue = (startPoint + endPoint) / 2
            // 重新定义开始值与结束值计算波谷面积
            startPoint = centerValue - (wideTotal / 2 * scanPm).toInt()
            endPoint = centerValue + (wideTotal / 2 * scanPm).toInt()
            if (startPoint < 0) {
                startPoint = 0
            }
            if (endPoint < 0) {
                endPoint = 0
            }
            if (startPoint >= pointList.size - 1) {
                startPoint = pointList.size - 1
            }
            if (endPoint >= pointList.size - 1) {
                endPoint = pointList.size - 1
            }
            // 查找最新的区域
            val list = pointList.subList(
                startPoint,
                endPoint,
            )
            // 总面积
            for ((index, casePoint) in list.withIndex()) {
                if (index == list.size - 1) {
                    break
                }
                var y1 = list[index].y
                var y2 = list[index + 1].y
                if (y1 <= 0.0) {
                    y1 = 0.0
                }
                if (y2 <= 0.0) {
                    y2 = 0.0
                }
                val item: Double = (y1 + y2) * high / 2
                tMaxResult += item
            }
        }
        // 判断结束是否大于0
        if (tMaxResult - newBaseValue > 0) {
            return tMaxResult - newBaseValue
        } else {
            return 0.0
        }
    }

    /**
     *  根据峰位值计算T、C值
     *  1、纠正偏移量找到峰的真正开始位置与结束位置
     *  2、以中心点区分左右区域
     *  3、以左边区域最小值和右边区域最小值连线，上面区域为T值
     */
    fun findTopValueV2(
        pointList: ArrayList<CasePoint>,
        start: Double,
        end: Double,
        scanPm: Int
    ): Double {
        // 从配置获取1毫米被切成了多少份
        if (scanPm <= 0 || start >= end || pointList.isEmpty()) {
            return 0.0
        }

        // 当前以微米为单位计算高度
        val high: Double = 1.0 * 1000 / scanPm.toDouble()

        Timber.w("start:${start}")
        Timber.w("end:${end}")

        var startPoint = start.toInt()
        var endPoint = end.toInt()
        if (startPoint < 0) {
            startPoint = 0
        }
        if (endPoint < 0) {
            endPoint = 0
        }
        if (startPoint >= pointList.size - 1) {
            startPoint = pointList.size - 1
        }
        if (endPoint >= pointList.size - 1) {
            endPoint = pointList.size - 1
        }

        // 获取最初的开始位置与结束位置
        Timber.w("firstStartPoint:${startPoint}")
        Timber.w("firstEndPoint:${endPoint}")
        if (startPoint == endPoint) {
            return 0.0
        }

        // 查找中间值
        val centerValue = (startPoint + endPoint) / 2

        // 查找左边区域最小值
        val leftList = pointList.subList(
            startPoint,
            centerValue,
        )

        // 查找右边区域最小值
        val rightList = pointList.subList(
            centerValue,
            endPoint,
        )
        val leftPoint = leftList.minBy { it.y }.x.toInt()
        val rightPoint = rightList.minBy { it.y }.x.toInt()

        // 获取计算峰位值的开始位置与结束位置
        Timber.w("leftPoint:${leftPoint}")
        Timber.w("rightPoint:${rightPoint}")

        // 获取计算总面积的区域
        val list = pointList.subList(
            leftPoint,
            rightPoint,
        )

        // 总面积
        var tResult = 0.0
        for ((index, casePoint) in list.withIndex()) {
            if (index == list.size - 1) {
                break
            }
            var y1 = list[index].y
            var y2 = list[index + 1].y
            if (y1 <= 0.0) {
                y1 = 0.0
            }
            if (y2 <= 0.0) {
                y2 = 0.0
            }
            val item: Double = (y1 + y2) * high / 2
            tResult += item
        }

        // 底噪面积
        var mResult: Double
        val startY = pointList[leftPoint].y
        val endY = pointList[rightPoint].y
        mResult = (startY + endY) * (rightPoint - leftPoint) * high / 2
        Timber.w("===========原始面积：${tResult}")
        Timber.w("===========底噪面积：${mResult}")
        Timber.w("===========算法面积：${tResult - mResult}")

        return if (tResult - mResult < 0) {
            0.0
        } else {
            tResult - mResult
        }
    }

    /**
     *  CRP根据峰位值计算T、C值
     *  1、纠正偏移量找到峰的真正开始位置与结束位置
     *  2、判断峰的中心点峰值与重数比较，判断当前峰时波峰还是波谷
     *  3、波峰：以1.5mm的从开始点到结束点找到最大面积为T值
     *  4、波谷：以中心点左右0.75mm直接计算面积为T值
     *  5、底噪：以重数计算底噪
     */
    fun findCrpTopValue(
        pointList: ArrayList<CasePoint>,
        start: Double,
        end: Double,
        offset: Double,
        scanPm: Int
    ): Double {
        // 计算面积时的矩形框宽度
        val wideTotal = 1.5
        // 从配置获取1毫米被切成了多少份
        if (scanPm <= 0 || start >= end || pointList.isEmpty()) {
            return 0.0
        }
        // 当前以微米为单位计算高度
        val high: Double = 1.0 * 1000 / scanPm.toDouble()
        // 规范开始点与结束点
        var startPoint = ((start + offset) * scanPm).toInt()
        var endPoint = ((end + offset) * scanPm).toInt()
        if (startPoint < 0) {
            startPoint = 0
        }
        if (endPoint < 0) {
            endPoint = 0
        }
        if (startPoint >= pointList.size - 1) {
            startPoint = pointList.size - 1
        }
        if (endPoint >= pointList.size - 1) {
            endPoint = pointList.size - 1
        }
        // 获取最初的开始位置与结束位置
        Timber.w("firstStartPoint:${startPoint}")
        Timber.w("firstEndPoint:${endPoint}")
        if (startPoint == endPoint) {
            return 0.0
        }
        // 获取当前中心点值
        val centerX = (startPoint + endPoint) / 2
        val centerY = pointList[centerX].y
        // 查询当前重数
        val maxNumber = findMostFrequentNumber(pointList)
        // 计算当前的底噪面积
        val newBaseValue = maxNumber * high * 1.5 * scanPm.toDouble()
        // 找到最大的面积
        var tMaxResult = 0.0
        // 判断当前图像时波峰还是波谷
        if (centerY > maxNumber) {
            // 波峰时计算T值与C值
            // 收集面积所有的可能型
            val tResultMap = calcTopMap(startPoint, endPoint, wideTotal, scanPm, pointList)
            // 找到最大的面积
            tMaxResult = tResultMap.maxBy { it.value }.value
        } else {
            // 波谷时计算T值与C值
            // 查找中间值
            val centerValue = (startPoint + endPoint) / 2
            // 重新定义开始值与结束值计算波谷面积
            startPoint = centerValue - (wideTotal / 2 * scanPm).toInt()
            endPoint = centerValue + (wideTotal / 2 * scanPm).toInt()
            if (startPoint < 0) {
                startPoint = 0
            }
            if (endPoint < 0) {
                endPoint = 0
            }
            if (startPoint >= pointList.size - 1) {
                startPoint = pointList.size - 1
            }
            if (endPoint >= pointList.size - 1) {
                endPoint = pointList.size - 1
            }
            // 查找最新的区域
            val list = pointList.subList(
                startPoint,
                endPoint,
            )
            // 总面积
            for ((index, casePoint) in list.withIndex()) {
                if (index == list.size - 1) {
                    break
                }
                var y1 = list[index].y
                var y2 = list[index + 1].y
                if (y1 <= 0.0) {
                    y1 = 0.0
                }
                if (y2 <= 0.0) {
                    y2 = 0.0
                }
                val item: Double = (y1 + y2) * high / 2
                tMaxResult += item
            }
        }
        // 判断结束是否大于0
        if (tMaxResult - newBaseValue > 0) {
            return tMaxResult - newBaseValue
        } else {
            return 0.0
        }
    }

    /**
     * 根据多项式计算结果：IGE
     */
    @SuppressLint("DefaultLocale")
    fun calIgeValue(value: Double, varList: List<CardConfigVar>): Double {
        for (cardVar in varList) {
            if (cardVar.start > 0 && value < cardVar.start) {
                continue
            }
            if (cardVar.end > 0 && value > cardVar.end) {
                continue
            }

            val result =
                cardVar.x0 + cardVar.x1 * value + cardVar.x2 * value.pow(2.0) + cardVar.x3 * value.pow(
                    3.0
                ) + cardVar.x4 * value.pow(4.0)
            return NumberUtils.toDouble(String.format("%.2f", result))
        }

        return 0.0
    }

    /**
     * 根据多项式计算结果：CRP
     */
    @SuppressLint("DefaultLocale")
    fun calCrpValue(value: Double, varList: List<CardConfigVar>, type: String): Double {
        for (cardVar in varList) {
            if (cardVar.type != type) {
                continue
            }
            if (cardVar.start > 0 && value < cardVar.start) {
                continue
            }
            if (cardVar.end > 0 && value > cardVar.end) {
                continue
            }

            val result =
                cardVar.x0 + cardVar.x1 * value + cardVar.x2 * value.pow(2.0) + cardVar.x3 * value.pow(
                    3.0
                ) + cardVar.x4 * value.pow(4.0)
            return NumberUtils.toDouble(String.format("%.2f", result))
        }

        return 0.0
    }

    /**
     * 根据多项式计算结果：2LJ
     */
    @SuppressLint("DefaultLocale")
    fun calc2LJValue(value: Double, varList: List<CardConfigVar>): Double {
        var min = varList[0].start
        for (cardVar in varList) {
            if (min > cardVar.start) {
                min = cardVar.start
            }

            if (value < cardVar.start) {
                continue
            }
            if (value > cardVar.end) {
                continue
            }

            val result = cardVar.x0 * value + cardVar.x1
            return NumberUtils.toDouble(String.format("%.2f", result))
        }

        if (value < min) {
            return -1.0
        }
        return 0.0
    }

    /**
     * 根据多项式计算结果：2LJ
     */
    @SuppressLint("DefaultLocale")
    fun calc3LJBAL1Value(value: Double, varList: List<CardConfigVar>): Double {
        var min = varList[0].start
        for (cardVar in varList) {
            if (min > cardVar.start) {
                min = cardVar.start
            }

            if (value < cardVar.start) {
                continue
            }
            if (value > cardVar.end) {
                continue
            }

            val result = cardVar.x0 * value + cardVar.x1
            return NumberUtils.toDouble(String.format("%.2f", result))
        }

        if (value < min) {
            return -1.0
        }
        return 0.0
    }

    /**
     * 查询重数
     */
    fun findMostFrequentNumber(pointList: ArrayList<CasePoint>): Double {
        val yList = pointList.filter { it.y > 15 }.map { it.y }
        val counts = yList.groupBy { it }.mapValues { entry -> entry.value.size }
        if (counts.isEmpty() || counts.values.isEmpty()) {
            return 8.0
        }
        // 如果没有数字出现，返回-1
        val maxCount = counts.values.max()
        return counts.filterValues { it == maxCount }.keys.stream().findFirst().orElse(0.0)
    }

    /**
     * 根据开始点与结束点，面积计算范围，
     */
    private fun calcTopMap(
        start: Int,
        end: Int,
        wideTotal: Double,
        scanPm: Int,
        pointList: ArrayList<CasePoint>
    ): HashMap<Int, Double> {
        // 当前以微米为单位计算高度
        val high: Double = 1.0 * 1000 / scanPm.toDouble()
        // 赋予初始值：
        var startPoint = start
        var endPoint = end
        // 开始结束位置左右偏移1mm
        if (startPoint - scanPm * wideTotal / 2 > 0) {
            startPoint -= scanPm
        } else {
            startPoint = 0
        }
        if (endPoint + scanPm * wideTotal / 2 < pointList.size) {
            endPoint += scanPm
        } else {
            endPoint = pointList.size - 1
        }
        Timber.w("偏移量计算开始位置:${startPoint}")
        Timber.w("偏移量计算结束位置:${endPoint}")

        val tResultMap = HashMap<Int, Double>()
        val list = pointList.subList(
            startPoint,
            endPoint,
        )
        Timber.w("list ${list.size}")
        Timber.w("list ${App.gson.toJson(list)}")

        for ((index, casePoint) in list.withIndex()) {
            if (index + scanPm * wideTotal >= (list.size - 1)) {
                break
            }
            var result = 0.0
            // 计算
            for (i in 0..floor(scanPm * wideTotal).toInt()) {
                var y1 = list[index + i].y
                var y2 = list[index + i + 1].y
                if (y1 <= 0.0) {
                    y1 = 0.0
                }
                if (y2 <= 0.0) {
                    y2 = 0.0
                }
                val item: Double = (y1 + y2) * high / 2
                result += item
            }
            tResultMap[casePoint.x.toInt()] = result
        }
        return tResultMap
    }

    /**
     * 根据开始点与结束点，面积计算范围，
     */
    private fun calcTopMapV2(
        start: Int,
        end: Int,
        scanPm: Int,
        pointList: ArrayList<CasePoint>
    ): HashMap<Int, Double> {
        // 当前以微米为单位计算高度
        val high: Double = 1.0 * 1000 / scanPm.toDouble()

        // 赋予初始值：
        val startPoint = start
        val endPoint = end
        Timber.w("偏移量计算开始位置:${startPoint}")
        Timber.w("偏移量计算结束位置:${endPoint}")

        val tResultMap = HashMap<Int, Double>()
        val list = pointList.subList(
            startPoint,
            endPoint,
        )
        Timber.w("list ${list.size}")
        Timber.w("list ${App.gson.toJson(list)}")

        // 总面积
        for ((index, casePoint) in list.withIndex()) {
            if (index == list.size - 1) {
                break
            }
            var y1 = list[index].y
            var y2 = list[index + 1].y
            if (y1 <= 0.0) {
                y1 = 0.0
            }
            if (y2 <= 0.0) {
                y2 = 0.0
            }
            val item: Double = (y1 + y2) * high / 2
            tResultMap[casePoint.x.toInt()] = item
        }
        return tResultMap
    }

    /**
     * 计算四联检使用规则
     */
    fun calc4LjRule(
        cardConfig: CardConfig,
        t1: Double,
        t2: Double,
        t3: Double,
        t4: Double
    ): HashMap<String, Double> {
        val tMap = HashMap<String, Double>()
        if (t1 - cardConfig.cutOff1 > cardConfig.cutOffMax) {
            tMap["t1"] = t1 - cardConfig.cutOff1
        } else if (t2 - cardConfig.cutOff2 > cardConfig.cutOffMax) {
            tMap["t2"] = t2 - cardConfig.cutOff2
        } else if (t3 - cardConfig.cutOff3 > cardConfig.cutOffMax) {
            tMap["t3"] = t3 - cardConfig.cutOff3
        } else if (t4 - cardConfig.cutOff4 > cardConfig.cutOffMax) {
            tMap["t4"] = t4 - cardConfig.cutOff4
        }
        val resultMap = HashMap<String, Double>()
        // tMap若为空使用第一套规则
        if (tMap.isEmpty()) {
            val result1 = if (t1 > cardConfig.cutOff1) 1.0 else -1.0
            val result2 = if (t2 > cardConfig.cutOff2) 1.0 else -1.0
            val result3 = if (t3 > cardConfig.cutOff3) 1.0 else -1.0
            val result4 = if (t4 > cardConfig.cutOff4) 1.0 else -1.0

            val t1RadioValue = t1 / cardConfig.cutOff1
            val t2RadioValue = t2 / cardConfig.cutOff2
            val t3RadioValue = t3 / cardConfig.cutOff3
            val t4RadioValue = t4 / cardConfig.cutOff4

            resultMap["result1"] = result1
            resultMap["result2"] = result2
            resultMap["result3"] = result3
            resultMap["result4"] = result4
            resultMap["t1RadioValue"] = t1RadioValue
            resultMap["t2RadioValue"] = t2RadioValue
            resultMap["t3RadioValue"] = t3RadioValue
            resultMap["t4RadioValue"] = t4RadioValue
        } else {
            // tMap不为空使用第二套规则，返回一定为阳性的序号
            val mapKey = tMap.maxBy { it.value }.key
            var result1 = if (t1 > cardConfig.cutOff5) 1.0 else -1.0
            var result2 = if (t2 > cardConfig.cutOff6) 1.0 else -1.0
            var result3 = if (t3 > cardConfig.cutOff7) 1.0 else -1.0
            var result4 = if (t4 > cardConfig.cutOff8) 1.0 else -1.0

            var t1RadioValue = t1 / cardConfig.cutOff5
            var t2RadioValue = t2 / cardConfig.cutOff6
            var t3RadioValue = t3 / cardConfig.cutOff7
            var t4RadioValue = t4 / cardConfig.cutOff8

            if (mapKey == "t1") {
                result1 = 1.0
                t1RadioValue = t1 / cardConfig.cutOff1
            }
            if (mapKey == "t2") {
                result2 = 1.0
                t2RadioValue = t2 / cardConfig.cutOff2
            }
            if (mapKey == "t3") {
                result3 = 1.0
                t3RadioValue = t3 / cardConfig.cutOff3
            }
            if (mapKey == "t4") {
                result4 = 1.0
                t4RadioValue = t4 / cardConfig.cutOff4
            }

            resultMap["result1"] = result1
            resultMap["result2"] = result2
            resultMap["result3"] = result3
            resultMap["result4"] = result4
            resultMap["t1RadioValue"] = t1RadioValue
            resultMap["t2RadioValue"] = t2RadioValue
            resultMap["t3RadioValue"] = t3RadioValue
            resultMap["t4RadioValue"] = t4RadioValue
        }
        return resultMap
    }

    /**
     * 计算四联检使用规则
     */
    fun calc3LjRule(
        cardConfig: CardConfig,
        t1: Double,
        t2: Double,
        t3: Double
    ): HashMap<String, Double> {
        val tMap = HashMap<String, Double>()
        if (t1 - cardConfig.cutOff1 > cardConfig.cutOffMax) {
            tMap["t1"] = t1 - cardConfig.cutOff1
        } else if (t2 - cardConfig.cutOff2 > cardConfig.cutOffMax) {
            tMap["t2"] = t2 - cardConfig.cutOff2
        } else if (t3 - cardConfig.cutOff3 > cardConfig.cutOffMax) {
            tMap["t3"] = t3 - cardConfig.cutOff3
        }
        val resultMap = HashMap<String, Double>()
        // tMap若为空使用第一套规则
        if (tMap.isEmpty()) {
            val result1 = if (t1 > cardConfig.cutOff1) 1.0 else -1.0
            val result2 = if (t2 > cardConfig.cutOff2) 1.0 else -1.0
            val result3 = if (t3 > cardConfig.cutOff3) 1.0 else -1.0

            val t1RadioValue = t1 / cardConfig.cutOff1
            val t2RadioValue = t2 / cardConfig.cutOff2
            val t3RadioValue = t3 / cardConfig.cutOff3

            resultMap["result1"] = result1
            resultMap["result2"] = result2
            resultMap["result3"] = result3
            resultMap["t1RadioValue"] = t1RadioValue
            resultMap["t2RadioValue"] = t2RadioValue
            resultMap["t3RadioValue"] = t3RadioValue
        } else {
            // tMap不为空使用第二套规则，返回一定为阳性的序号
            val mapKey = tMap.maxBy { it.value }.key
            var result1 = if (t1 > cardConfig.cutOff5) 1.0 else -1.0
            var result2 = if (t2 > cardConfig.cutOff6) 1.0 else -1.0
            var result3 = if (t3 > cardConfig.cutOff7) 1.0 else -1.0

            var t1RadioValue = t1 / cardConfig.cutOff5
            var t2RadioValue = t2 / cardConfig.cutOff6
            var t3RadioValue = t3 / cardConfig.cutOff7

            if (mapKey == "t1") {
                result1 = 1.0
                t1RadioValue = t1 / cardConfig.cutOff1
            }
            if (mapKey == "t2") {
                result2 = 1.0
                t2RadioValue = t2 / cardConfig.cutOff2
            }
            if (mapKey == "t3") {
                result3 = 1.0
                t3RadioValue = t3 / cardConfig.cutOff3
            }

            resultMap["result1"] = result1
            resultMap["result2"] = result2
            resultMap["result3"] = result3
            resultMap["t1RadioValue"] = t1RadioValue
            resultMap["t2RadioValue"] = t2RadioValue
            resultMap["t3RadioValue"] = t3RadioValue
        }
        return resultMap
    }

    /**
     * 计算二联检使用规则
     */
    fun calc2LJRule(
        cardConfig: CardConfig,
        t1: Double,
        t2: Double
    ): HashMap<String, Double> {
        val tMap = HashMap<String, Double>()
        if (t1 - cardConfig.cutOff1 > cardConfig.cutOffMax) {
            tMap["t1"] = t1 - cardConfig.cutOff1
        } else if (t2 - cardConfig.cutOff2 > cardConfig.cutOffMax) {
            tMap["t2"] = t2 - cardConfig.cutOff2
        }
        val resultMap = HashMap<String, Double>()
        // tMap若为空使用第一套规则
        if (tMap.isEmpty()) {
            val result1 = if (t1 > cardConfig.cutOff1) 1.0 else -1.0
            val result2 = if (t2 > cardConfig.cutOff2) 1.0 else -1.0

            val t1RadioValue = t1 / cardConfig.cutOff1
            val t2RadioValue = t2 / cardConfig.cutOff2

            resultMap["result1"] = result1
            resultMap["result2"] = result2
            resultMap["t1RadioValue"] = t1RadioValue
            resultMap["t2RadioValue"] = t2RadioValue
        } else {
            // tMap不为空使用第二套规则，返回一定为阳性的序号
            val mapKey = tMap.maxBy { it.value }.key
            var result1 = if (t1 > cardConfig.cutOff5) 1.0 else -1.0
            var result2 = if (t2 > cardConfig.cutOff6) 1.0 else -1.0

            var t1RadioValue = t1 / cardConfig.cutOff5
            var t2RadioValue = t2 / cardConfig.cutOff6

            if (mapKey == "t1") {
                result1 = 1.0
                t1RadioValue = t1 / cardConfig.cutOff1
            }
            if (mapKey == "t2") {
                result2 = 1.0
                t2RadioValue = t2 / cardConfig.cutOff2
            }

            resultMap["result1"] = result1
            resultMap["result2"] = result2
            resultMap["t1RadioValue"] = t1RadioValue
            resultMap["t2RadioValue"] = t2RadioValue
        }
        return resultMap
    }

    /**
     * 斜率校验，
     */
    fun checkTopSlope(
        pointList: ArrayList<CasePoint>,
        start: Double,
        end: Double,
        offset: Double,
        scanPm: Int
    ): Double {
        // 从配置获取1毫米被切成了多少份
        if (scanPm <= 0 || start >= end || pointList.isEmpty()) {
            return 0.0
        }
        // 当前以微米为单位计算高度
        val high: Double = 1.0 * 1000 / scanPm.toDouble()
        // 规范开始点与结束点
        var startPoint = ((start + offset) * scanPm).toInt()
        var endPoint = ((end + offset) * scanPm).toInt()
        if (startPoint < 0) {
            startPoint = 0
        }
        if (endPoint < 0) {
            endPoint = 0
        }
        if (startPoint >= pointList.size - 1) {
            startPoint = pointList.size - 1
        }
        if (endPoint >= pointList.size - 1) {
            endPoint = pointList.size - 1
        }
        // 获取最初的开始位置与结束位置
        Timber.w("firstStartPoint:${startPoint}")
        Timber.w("firstEndPoint:${endPoint}")
        if (startPoint == endPoint) {
            return 0.0
        }
        // 查找中间值
        val centerValue = (startPoint + endPoint) / 2
        // 查找左边区域最小值
        val leftList = pointList.subList(
            startPoint,
            centerValue,
        )
        // 查找右边区域最小值
        val rightList = pointList.subList(
            centerValue,
            endPoint,
        )
        // 左边的点
        val leftX = leftList.minBy { it.y }.x
        val leftY = leftList.minBy { it.y }.y
        // 右边的点
        val rightX = rightList.minBy { it.y }.x
        val rightY = rightList.minBy { it.y }.y
        // 计算斜率
        var slope: Double
        slope = (rightY - leftY) / (rightX - leftX)
        return abs(slope)
    }

    /**
     * 斜率校验，
     */
    fun checkTopSlopeV2(
        pointList: ArrayList<CasePoint>,
        start: Double,
        end: Double
    ): Double {
        var startPoint = start.toInt()
        var endPoint = end.toInt()
        if (startPoint < 0) {
            startPoint = 0
        }
        if (endPoint < 0) {
            endPoint = 0
        }
        if (startPoint >= pointList.size - 1) {
            startPoint = pointList.size - 1
        }
        if (endPoint >= pointList.size - 1) {
            endPoint = pointList.size - 1
        }

        // 获取最初的开始位置与结束位置
        Timber.w("firstStartPoint:${startPoint}")
        Timber.w("firstEndPoint:${endPoint}")
        if (startPoint == endPoint) {
            return 0.0
        }

        // 查找中间值
        val centerValue = (startPoint + endPoint) / 2

        // 查找左边区域最小值
        val leftList = pointList.subList(
            startPoint,
            centerValue,
        )

        // 查找右边区域最小值
        val rightList = pointList.subList(
            centerValue,
            endPoint,
        )

        // 左边的点
        val leftX = leftList.minBy { it.y }.x
        val leftY = leftList.minBy { it.y }.y

        // 右边的点
        val rightX = rightList.minBy { it.y }.x
        val rightY = rightList.minBy { it.y }.y

        // 计算斜率
        val slope: Double = (rightY - leftY) / (rightX - leftX)
        return abs(slope)
    }

    // 计算单个区间的波峰面积
    fun calculatePeakArea(
        pointList: List<CasePoint>,
        start: Double,
        end: Double,
        scanPm: Int
    ): Double {
        // 1. 提取区间内的点
        val intervalPoints = extractIntervalPoints(pointList, start, end)
        if (intervalPoints.isEmpty()) return 0.0

        // 2. 数据预处理和降噪
        val processedPoints = preprocessData(intervalPoints, scanPm)

        // 3. 计算基线（考虑浓度影响）
        val baseline = calculateBaseline(processedPoints, scanPm)
        Timber.w("calculatePeakArea baseline: $baseline")

        // 4. 计算波峰面积（只计算高于基线的部分）
        return calculateAreaAboveBaseline(processedPoints, baseline)
    }

    fun calculateSlope(
        pointList: List<CasePoint>,
        start: Double,
        end: Double
    ): Double {
        // 1. 过滤出区间内的点并按x排序
        val filteredPoints = pointList
            .filter { it.x in start..end }
            .sortedBy { it.x }
            .takeIf { it.size >= 3 } // 至少需要3个点才能进行可靠计算
            ?: return 0.0

        // 2. 计算移动平均用于基线校正
        val windowSize = 5.coerceAtMost(filteredPoints.size / 4)
        val smoothedPoints = filteredPoints.mapIndexed { index, point ->
            val startIdx = (index - windowSize).coerceAtLeast(0)
            val endIdx = (index + windowSize).coerceAtMost(filteredPoints.size - 1)
            val window = filteredPoints.subList(startIdx, endIdx + 1)
            val avgY = window.map { it.y }.average()
            point.copy(y = point.y - avgY) // 减去局部平均值以消除基线漂移
        }

        // 3. 计算中位数绝对偏差(MAD)用于异常值检测
        val values = smoothedPoints.map { it.y }
        val median = values.sorted()[values.size / 2]
        val mad = values.map { abs(it - median) }.sorted()[values.size / 2]

        // 4. 过滤异常值（超过3倍MAD）
        val cleanedPoints = smoothedPoints.filter {
            abs(it.y - median) <= 3 * mad
        }.takeIf { it.size >= 3 } ?: return 0.0

        // 5. 使用加权最小二乘法计算斜率
        val n = cleanedPoints.size
        val sumX = cleanedPoints.sumOf { it.x }
        val sumY = cleanedPoints.sumOf { it.y }
        val sumXY = cleanedPoints.sumOf { it.x * it.y }
        val sumX2 = cleanedPoints.sumOf { it.x.pow(2) }

        val denominator = n * sumX2 - sumX.pow(2)
        if (denominator == 0.0) return 0.0

        return abs((n * sumXY - sumX * sumY) / denominator)
    }

    // 提取指定区间内的点
    private fun extractIntervalPoints(
        pointList: List<CasePoint>,
        start: Double,
        end: Double
    ): List<CasePoint> {
        return pointList.filter { it.x in start..end }.sortedBy { it.x }
    }

    // 数据预处理和降噪
    private fun preprocessData(
        points: List<CasePoint>,
        scanPm: Int
    ): List<CasePoint> {
        // 根据浓度选择不同的平滑策略
        val windowSize = when {
            scanPm < 10 -> 3  // 极低浓度，轻度平滑
            scanPm < 50 -> 5  // 低浓度，中度平滑
            else -> 7         // 高浓度，较强平滑
        }

        return applySavitzkyGolaySmoothing(points, windowSize)
    }

    // Savitzky-Golay平滑滤波
    private fun applySavitzkyGolaySmoothing(
        points: List<CasePoint>,
        windowSize: Int
    ): List<CasePoint> {
        if (points.size <= windowSize) return points

        val smoothed = mutableListOf<CasePoint>()
        val halfWindow = windowSize / 2

        for (i in points.indices) {
            val startIndex = max(0, i - halfWindow)
            val endIndex = min(points.size - 1, i + halfWindow)

            // 计算窗口内点的加权平均值
            var totalWeight = 0.0
            var weightedSum = 0.0

            for (j in startIndex..endIndex) {
                // 中心点权重最高，边缘点权重较低
                val distance = abs(i - j)
                val weight = 1.0 - (distance.toDouble() / (halfWindow + 1))
                weightedSum += points[j].y * weight
                totalWeight += weight
            }

            val smoothedY = weightedSum / totalWeight
            smoothed.add(CasePoint(points[i].x, smoothedY))
        }

        return smoothed
    }

    // 计算基线（考虑浓度影响）
    private fun calculateBaseline(
        points: List<CasePoint>,
        scanPm: Int
    ): Double {
        if (points.isEmpty()) return 0.0

        // 方法1：取区间开始和结束部分的平均值
        val startPercent = 0.1 // 前10%
        val endPercent = 0.1   // 后10%

        val startCount = max(1, (points.size * startPercent).toInt())
        val endCount = max(1, (points.size * endPercent).toInt())

        val startAvg = points.take(startCount).map { it.y }.average()
        val endAvg = points.takeLast(endCount).map { it.y }.average()

        // 方法2：取整个区间的最小值
        val minValue = points.minByOrNull { it.y }?.y ?: startAvg

        // 根据浓度调整基线计算策略
        return when {
            scanPm < 10 -> (startAvg * 0.7 + endAvg * 0.3) // 极低浓度，主要依赖起点
            scanPm < 50 -> (startAvg * 0.4 + endAvg * 0.4 + minValue * 0.2) // 低浓度，平衡考虑
            else -> (startAvg * 0.3 + endAvg * 0.3 + minValue * 0.4) // 高浓度，更多考虑最小值
        }
    }

    // 计算高于基线的面积
    private fun calculateAreaAboveBaseline(
        points: List<CasePoint>,
        baseline: Double
    ): Double {
        if (points.size < 2) return 0.0

        var area = 0.0
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            // 只计算高于基线的部分
            val h1 = max(p1.y - baseline, 0.0)
            val h2 = max(p2.y - baseline, 0.0)

            val width = p2.x - p1.x
            area += (h1 + h2) * width / 2.0
        }

        return area
    }
}