package poct.device.app.utils.app

import com.patrykandpatrick.vico.core.extension.getFieldValue
import com.patrykandpatrick.vico.core.extension.setFieldValue
import info.szyh.common4.lang.StringUtils
import kotlinx.serialization.json.Json
import poct.device.app.App
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardTopBean
import poct.device.app.bean.CardVarBean
import poct.device.app.entity.CardConfig
import poct.device.app.entity.CardTop
import poct.device.app.entity.CardVar
import timber.log.Timber
import java.nio.charset.Charset
import java.util.Base64
import java.util.stream.Collectors
import kotlin.reflect.full.memberProperties

/**
 * 检测项目帮助类
 */
object AppCardConfigUtils {
    /**
     * 换行符
     */
    private const val LINE_SEP: Byte = 0x0A

    private const val HL7_FIELD_SEP = "|"
    private const val CARD_CONFIG_VERSION = "VERSION"
    private const val CARD_CONFIG_CM = "CM"
    private const val CARD_CONFIG_CT = "CT"
    private const val CARD_CONFIG_CV = "CV"

    /**
     * 格式：
     * 版本行：VERSION|版本号|\n
     * 属性行：CM|属性值|\n
     * 峰位行：CT|峰位值|\n
     * 多项行：CV|多项值|\n
     * 解析二维码扫描内容
     */
    fun toEncodeQrCode(cardConfig: CardConfig): String {
        val fields = CardConfig::class.memberProperties.map { it }
        // 结果： 第一行版本
        var result = "VERSION|V0\n"
        val values = ArrayList<String>()
        for (field in fields) {
            if (field.name == "topList" || field.name == "varList") {
                continue
            }
            // 去除不必要的0
            var curValue = ""
            if (field.returnType.toString() == "kotlin.Double") {
                val curDouble: Double = cardConfig.getFieldValue(field.name)
                curValue = removeTrailingZeros(curDouble.toString())
            } else if (field.returnType.toString() == "kotlin.Int") {
                val curInt: Int = cardConfig.getFieldValue(field.name)
                curValue = curInt.toString()
            } else if (field.returnType.toString() == "java.time.LocalDateTime") {
                continue
            } else {
                curValue = cardConfig.getFieldValue(field.name)
            }
            values.add(curValue)
        }

        val valueStr = values.stream().map<String> { str: String? -> str ?: "" }
            .collect(Collectors.joining("|"))
        // 属性：第二行属性
        result += "CM|$valueStr\n"
        // 峰位值集合
        val topList = Json.decodeFromString(cardConfig.topList) as List<CardTop>
        for (cardTop in topList) {
            val topValues = ArrayList<String>()
            val topFields = CardTop::class.memberProperties.map { it }
            for (field in topFields) {
                // 去除不必要的0
                var curValue = ""
                if (field.returnType.toString() == "kotlin.Double") {
                    val curDouble: Double = cardTop.getFieldValue(field.name)
                    curValue = removeTrailingZeros(curDouble.toString())
                } else if (field.returnType.toString() == "kotlin.Int") {
                    val curInt: Int = cardTop.getFieldValue(field.name)
                    curValue = curInt.toString()
                } else if (field.returnType.toString() == "java.time.LocalDateTime") {
                    continue
                } else {
                    curValue = cardTop.getFieldValue(field.name)
                }
                topValues.add(curValue)
            }
            val topValueStr = topValues.stream().map<String> { str: String? -> str ?: "" }
                .collect(Collectors.joining("|"))
            result += "CT|$topValueStr\n"
        }
        // 多项室集合
        val varList = Json.decodeFromString(cardConfig.varList) as List<CardVar>
        for (cardVar in varList) {
            val varValues = ArrayList<String>()
            val varFields = CardVar::class.memberProperties.map { it }
            for (field in varFields) {
                var curValue = ""
                if (field.returnType.toString() == "kotlin.Double") {
                    val curDouble: Double = cardVar.getFieldValue(field.name)
                    curValue = removeTrailingZeros(curDouble.toString())
                } else if (field.returnType.toString() == "kotlin.Int") {
                    val curInt: Int = cardVar.getFieldValue(field.name)
                    curValue = curInt.toString()
                } else if (field.returnType.toString() == "java.time.LocalDateTime") {
                    continue
                } else {
                    curValue = cardVar.getFieldValue(field.name)
                }
                varValues.add(curValue)
            }
            val varValueStr = varValues.stream().map<String> { str: String? -> str ?: "" }
                .collect(Collectors.joining("|"))
            result += "CV|$varValueStr\n"
        }
        Timber.w("当前结果${result}")
        Timber.w("当前结果${App.gson.toJson(result.toByteArray(Charset.forName("utf-8")))}")
        val base64Str =
            Base64.getEncoder().encodeToString(result.toByteArray(Charset.forName("utf-8")))
        Timber.w("当前结果${base64Str}")
        Timber.w("当前结果${base64Str.length}")
        return base64Str
    }

    /**
     * 格式：
     * 版本行：VERSION|版本号\n
     * 属性行：CM|属性值\n
     * 峰位行：CT|峰位值\n
     * 多项行：CV|多项值\n
     * 解析二维码扫描内容
     */
    fun toDecodeQrCode(result: String): CardConfigBean {
        Timber.w("当前结果${result}")
        // 解析base加密数据
        var resultBytes: ByteArray? = null
        try {
            resultBytes = Base64.getDecoder().decode(result)
        } catch (e: Exception) {
            // base64解析失败
            return CardConfigBean.Empty
        }
        if (resultBytes == null) {
            return CardConfigBean.Empty
        }
        Timber.w("当前结果${App.gson.toJson(resultBytes)}")
        val resultStr = String(resultBytes)
        Timber.w("当前结果${resultStr}")
        // 将result进行分行
        val lines: Array<String> = StringUtils.split(resultStr, LINE_SEP.toInt().toChar())
        if (lines.isEmpty()) {
            // 不接写返回空对象
            return CardConfigBean.Empty
        }
        // 解析第一行版本信息，当前版本号V0
        if (lines[0].startsWith(CARD_CONFIG_VERSION)) {
            // 跟俊版本号获取解析规则
            val items: Array<String> = StringUtils.splitPreserveAllTokens(lines[0], HL7_FIELD_SEP)
            if (items.size > 1) {
                Timber.w("当前CARD_CONFIG版本：${items[1]}")
            }
        }
        // 返回结果
        val cardConfig = CardConfigBean()
        val topList = ArrayList<CardTopBean>()
        val varList = ArrayList<CardVarBean>()
        // 分行解析
        for (line in lines) {
            if (line.startsWith(CARD_CONFIG_VERSION)) {
                // 已经解析过不在解析
                continue
            } else if (line.startsWith(CARD_CONFIG_CM)) {
                // 解析属性：只存在一行
                val items: Array<String> = StringUtils.splitPreserveAllTokens(line, HL7_FIELD_SEP)
                val fields =
                    CardConfigBean::class.memberProperties.filter { it.name != "topList" && it.name != "varList" }
                        .map {
                            it.name
                        }
                Timber.w("属性集合${App.gson.toJson(fields)}")
                if (items.size <= fields.size) {
                    // 属性数量不够，不解析
                    Timber.w("属性数量不够，不解析${items.size}|${fields.size}")
                    return cardConfig
                }
                for ((index, field) in fields.withIndex()) {
                    cardConfig.setFieldValue(field, items[index + 1])
                }
            } else if (line.startsWith(CARD_CONFIG_CT)) {
                // 解析峰位值：存在多个
                val topBean = CardTopBean()
                val items: Array<String> = StringUtils.splitPreserveAllTokens(line, HL7_FIELD_SEP)
                val fields = CardTopBean::class.memberProperties.map { it }
                if (items.size <= fields.size) {
                    // 属性数量不够，不解析
                    Timber.w("属性数量不够，不解析")
                    continue
                }
                for ((index, field) in fields.withIndex()) {
                    if (field.returnType.toString() == "kotlin.Int") {
                        topBean.setFieldValue(field.name, items[index + 1].toInt())
                    } else {
                        topBean.setFieldValue(field.name, items[index + 1])
                    }
                }
                topList.add(topBean)
            } else if (line.startsWith(CARD_CONFIG_CV)) {
                // 解析多项式：存在多个
                val varBean = CardVarBean()
                val items: Array<String> = StringUtils.splitPreserveAllTokens(line, HL7_FIELD_SEP)
                val fields = CardVarBean::class.memberProperties.map { it }
                if (items.size <= fields.size) {
                    // 属性数量不够，不解析
                    Timber.w("属性数量不够，不解析")
                    continue
                }
                for ((index, field) in fields.withIndex()) {
                    if (field.returnType.toString() == "kotlin.Int") {
                        varBean.setFieldValue(field.name, items[index + 1].toInt())
                    } else {
                        varBean.setFieldValue(field.name, items[index + 1])
                    }
                }
                varList.add(varBean)
            } else {
                continue
            }

        }
        cardConfig.varList = varList
        cardConfig.topList = topList
        Timber.w("当前转换结果：${App.gson.toJson(cardConfig)}")
        return cardConfig
    }

    fun removeTrailingZeros(number: String): String {
        return number.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
    }
}