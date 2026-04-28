package poct.device.app.utils.app

import com.patrykandpatrick.vico.core.entry.FloatEntry
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardTopBean
import poct.device.app.bean.CardVarBean
import poct.device.app.bean.CaseBean
import poct.device.app.bean.ConfigInfoBean
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.FileInfo
import poct.device.app.bean.VersionBean
import poct.device.app.bean.VersionUpgradeInfo
import poct.device.app.entity.CaseResult
import poct.device.app.entity.User
import java.time.LocalDate
import java.util.Random
import java.util.UUID

/**
 * 示例帮助类
 */
object AppSampleUtils {
    private val scanPPMM: String = "40"

    // 旧版
//    private val point1: CardTopBean = CardTopBean(
//        // 576
//        start = 506.toString(), // 3.5.toString(),
//        end = 646.toString(), // 6.5.toString(),
//    )
//    private val point2: CardTopBean = CardTopBean(
//        // 856
//        start = 786.toString(), // 7.5.toString(),
//        end = 926.toString(), // 10.5.toString(),
//    )
//    private val point3: CardTopBean = CardTopBean(
//        // 1136
//        start = 1066.toString(), // 11.5.toString(),
//        end = 1206.toString(), // 14.5.toString(),
//    )
//    private val point4: CardTopBean = CardTopBean(
//        // 1416
//        start = 1346.toString(), // 15.5.toString(),
//        end = 1486.toString(), // 18.5.toString(),
//    )
//    private val point5: CardTopBean = CardTopBean(
//        // 1696
//        start = 1626.toString(), // 19.5.toString(),
//        end = 1766.toString(), // 22.5.toString(),
//    )

    // 新版
    private val point1: CardTopBean = CardTopBean(
        // 197
        start = 91.toString(),
        end = 303.toString(),
    )
    private val point2: CardTopBean = CardTopBean(
        // 482
        start = 376.toString(),
        end = 588.toString(),
    )
    private val point3: CardTopBean = CardTopBean(
        // 767
        start = 661.toString(),
        end = 873.toString(),
    )
    private val point4: CardTopBean = CardTopBean(
        // 1052
        start = 946.toString(),
        end = 1158.toString(),
    )
    private val point5: CardTopBean = CardTopBean(
        // 1337
        start = 1231.toString(),
        end = 1443.toString(),
    )

    fun fillDataMap(dataMap: LinkedHashMap<String, String>) {
        val random = Random()
        for (i in 1..20) {
            var sep = ""
            val builder = StringBuilder()
            for (j in 1..100) {
                builder.append(sep).append(random.nextInt(250000))
                sep = "|"
            }
            dataMap.putIfAbsent(i.toString(), builder.toString())
        }
    }

    fun genChartEntryList(): List<FloatEntry> {
        val entryList = ArrayList<FloatEntry>()
        val random = Random()
        for (i in 1..80) {
            entryList.add(FloatEntry(i.toFloat(), random.nextInt(50) * 0.1F))
        }
        return entryList
    }

    fun genCaseInfos(): List<CaseBean> {
        val caseBeans = ArrayList<CaseBean>()
        for (i in 0 until 5) {
            val record = CaseBean(
                id = UUID.randomUUID().toString(),
                name = "姓名${i + 1}",
                gender = i % 2,
                birthday = "1980-01-06",
                caseId = "AABBCC${i + 1}",
                workTime = "2024-04-07 12:00:01",
            )
            val resultList = ArrayList<CaseResult>()
            for (j in 0 until 4) {
                resultList.add(
                    CaseResult(
                        name = "项目$j",
                        result = "4554 mg/ml",
                        refer = "1-10",
                        t1Value = "",
                        t2Value = "",
                        t3Value = "",
                        t4Value = "",
                        radioValue = "",
                        cValue = "",
                        c2Value = "",
                        flag = 0
                    )
                )
            }
            record.workResult = Json.encodeToString(resultList)
            caseBeans.add(record)
        }
        return caseBeans
    }

    fun genCaseInfo(i: Int = 0): CaseBean {
        val record = CaseBean(
            id = UUID.randomUUID().toString(),
            name = "姓名${i + 1}",
            gender = i % 2,
            birthday = "1980-01-06",
            caseId = "AABBCC${i + 1}",
            workTime = "2024-04-07 12:00:01",
        )
        val resultList = ArrayList<CaseResult>()
        for (j in 0 until 4) {
            resultList.add(
                CaseResult(
                    name = "项目$j",
                    result = "$j mg/ml",
                    refer = "1-10",
                    t1Value = "",
                    t2Value = "",
                    t3Value = "",
                    t4Value = "",
                    cValue = "",
                    c2Value = "",
                    radioValue = "",
                    flag = 0
                )
            )
        }
        record.workResult = Json.encodeToString(resultList)
        return record
    }

    fun genSysInfo(): ConfigInfoBean {
        return ConfigInfoBean(
            name = "微流控",
            code = "6132",
            type = "wlc001",
            software = "V1.3.0",
            hardware = "V1.2.2.101",
        )
    }

    fun genSysInfoV2(): ConfigInfoV2Bean {
        return ConfigInfoV2Bean(
            name = "微流控",
            code = "6132",
            type = "wlc001",
            software = "V1.3.0",
            hardware = "V1.2.2.101",
        )
    }

    fun genUserInfos(): List<User> {
        val list = ArrayList<User>()
        list.add(
            User(
                username = User.ROLE_CHECKER,
                nickname = "CHECKER",
                role = "checker",
                pwd = "xxxxxx"
            )
        )
        list.add(
            User(
                username = User.ROLE_ADMIN,
                nickname = "ADMIN",
                role = "admin",
                pwd = "xxxxxx"
            )
        )
        list.add(
            User(
                username = User.ROLE_DEV,
                nickname = "DEV",
                role = "dev",
                pwd = "xxxxxx"
            )
        )
        return list
    }

    fun genUserInfo(): User {
        return User(
            username = "checker", nickname = "CHECKER", role = "checker", pwd = "xxxxxx"
        )
    }

    fun genFiles(): ArrayList<FileInfo> {
        val list = ArrayList<FileInfo>()
        for (i in 1..10) {
            list.add(
                FileInfo(
                    name = "fileXXXXXX$i.xml",
                    path = "afaewfawe/abwe/afwa/f/awef/aw/faw/fa/wf/awf/aw/fa/wf/aw/faw/ef/awf/aw/weaf"
                )
            )
        }
        return list
    }

    fun genCardInfoList(): ArrayList<CardConfigBean> {
        val list = ArrayList<CardConfigBean>()
        for (i in 1..10) {
            list.add(genCardInfo(i))
        }
        return list
    }

    fun genCardInfo(n: Int = 1): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        for (i in 1..5) {
            topList.add(
                CardTopBean(
                    index = i - 1,
                    id = "id$i",
                    start = 0.0.toString(),
                    end = i.toString(),
                    ctrl = "y",
                )
            )
        }

        val varList = ArrayList<CardVarBean>()
        for (i in 1..3) {
            varList.add(
                CardVarBean(
                    index = i - 1,
                    id = "id$i",
                    x0 = i.toString(),
                    x1 = i.toString(),
                    x2 = i.toString(),
                    x3 = i.toString(),
                    x4 = i.toString(),
                )
            )
        }

        return CardConfigBean(
            name = "试剂卡$n",
            code = "card$n",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            xt1 = 120.toString(),
            ft1 = 160.toString(),
            topList = topList,
            varList = varList,
        )
    }

    // 4联：HRV/hMPV/ADV/RSV
    fun genCardConfigFor4lJ(): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        topList.add(
            CardTopBean(
                index = 0,
                id = "id0",
                start = point1.start,
                end = point1.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 1,
                id = "id1",
                start = point2.start,
                end = point2.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 2,
                id = "id2",
                start = point3.start,
                end = point3.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 3,
                id = "id3",
                start = point4.start,
                end = point4.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 4,
                id = "id4",
                start = point5.start,
                end = point5.end,
                ctrl = "y",
            )
        )

        val varList = ArrayList<CardVarBean>()
        return CardConfigBean(
            name = "4联：HRV/hMPV/ADV/RSV",
            type = CaseBean.TYPE_4LJ,
            code = "",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            topList = topList,
            varList = varList,
            scanStart = 0.toString(),
            scanEnd = 25.toString(),
            scanPPMM = scanPPMM,
            ft0 = 0.toString(),
            xt1 = 120.toString(),
            ft1 = 160.toString(),
            typeScore = 1.toString(),
            scope = 1.toString(),
            cAvg = 208327.166666667.toString(),
            cStd = 37852.3547282362.toString(),
            cMin = 132622.457210194.toString(),
            cMax = 284031.876123139.toString(),
            cutOff1 = 19.74.toString(),
            cutOff2 = 92.45.toString(),
            cutOff3 = 73.10.toString(),
            cutOff4 = 71.35.toString(),
            cutOffMax = 1000.toString(),
            cutOff5 = 19.74.toString(),
            cutOff6 = 92.45.toString(),
            cutOff7 = 73.10.toString(),
            cutOff8 = 71.35.toString(),
            noise1 = 0.toString(),
            noise2 = 0.toString(),
            noise3 = 0.toString(),
            noise4 = 0.toString(),
            noise5 = 0.toString(),
        )
    }

    fun genCardConfigForCrp(): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        topList.add(
            CardTopBean(
                index = 0,
                id = "id0",
                start = point3.start,
                end = point3.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 1,
                id = "id1",
                start = point4.start,
                end = point4.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 2,
                id = "id2",
                start = point5.start,
                end = point5.end,
                ctrl = "y",
            )
        )

        val varList = ArrayList<CardVarBean>()
        varList.add(
            CardVarBean(
                index = 0,
                id = "id0",
                type = "crp",
                start = 0.0.toString(),
                end = 16.5.toString(),
                x0 = "0.00000000000001",
                x1 = 4.9203.toString(),
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 1,
                id = "id1",
                type = "sf",
                start = 16.5.toString(),
                end = 999999.toString(),
                x0 = 268.7.toString(),
                x1 = (-5.1246).toString(),
                x2 = 0.0901.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )

        return CardConfigBean(
            name = "CRP试剂卡",
            type = CaseBean.TYPE_CRP,
            code = "",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            topList = topList,
            varList = varList,
            scanStart = 0.toString(),
            scanEnd = 25.toString(),
            scanPPMM = scanPPMM,
            ft0 = 0.toString(),
            xt1 = 120.toString(),
            ft1 = 160.toString(),
            scope = 1.toString(),
            typeScore = 1.toString(),
            cAvg = 208327.166666667.toString(),
            cStd = 37852.3547282362.toString(),
            cMin = 132622.457210194.toString(),
            cMax = 284031.876123139.toString(),
        )
    }

    fun genCardConfigForSfCrp(): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        topList.add(
            CardTopBean(
                index = 0,
                id = "id0",
                start = point1.start,
                end = point1.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 1,
                id = "id1",
                start = point2.start,
                end = point2.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 2,
                id = "id2",
                start = point4.start,
                end = point4.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 3,
                id = "id3",
                start = point5.start,
                end = point5.end,
                ctrl = "y",
            )
        )

        val varList = ArrayList<CardVarBean>()
        varList.add(
            CardVarBean(
                index = 0,
                id = "id0",
                type = "crp",
                start = 0.0.toString(),
                end = 16.5.toString(),
                x0 = "0.00000000000001",
                x1 = 4.9203.toString(),
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 1,
                id = "id1",
                type = "sf",
                start = 16.5.toString(),
                end = 999999.toString(),
                x0 = 268.7.toString(),
                x1 = (-5.1246).toString(),
                x2 = 0.0901.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )

        return CardConfigBean(
            name = "SF/CRP试剂卡",
            type = CaseBean.TYPE_SF,
            code = "",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            topList = topList,
            varList = varList,
            scanStart = 0.toString(),
            scanEnd = 25.toString(),
            scanPPMM = scanPPMM,
            ft0 = 0.toString(),
            xt1 = 120.toString(),
            ft1 = 160.toString(),
            scope = 1.toString(),
            typeScore = 1.toString(),
            cAvg = 208327.166666667.toString(),
            cStd = 37852.3547282362.toString(),
            cMin = 132622.457210194.toString(),
            cMax = 284031.876123139.toString(),
        )
    }

    fun genCardConfigForIgE(): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        topList.add(
            CardTopBean(
                index = 0,
                id = "id0",
                start = point2.start,
                end = point2.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 1,
                id = "id1",
                start = point4.start,
                end = point4.end,
                ctrl = "y",
            )
        )

        val varList = ArrayList<CardVarBean>()
        varList.add(
            CardVarBean(
                index = 0,
                id = "id0",
                start = 0.0.toString(),
                end = 16.5.toString(),
                x0 = "0.00000000000001",
                x1 = 4.9203.toString(),
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 1,
                id = "id1",
                start = 16.5.toString(),
                end = 9999999.toString(),
                x0 = 268.7.toString(),
                x1 = (-5.1246).toString(),
                x2 = 0.0901.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )

        return CardConfigBean(
            name = "IgE试剂卡",
            type = CaseBean.TYPE_IGE,
            code = "",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            topList = topList,
            varList = varList,
            scanStart = 0.toString(),
            scanEnd = 25.toString(),
            scanPPMM = scanPPMM,
            xt1 = 120.toString(),
            ft1 = 160.toString(),
            scope = 1.toString(),
            typeScore = 1.toString(),
            cAvg = 208327.166666667.toString(),
            cStd = 37852.3547282362.toString(),
            cMin = 132622.457210194.toString(),
            cMax = 284031.876123139.toString(),
        )
    }

    // 3联：Flu A/Flu B/COVID-19
    fun genCardConfigFor3lJ(): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        topList.add(
            CardTopBean(
                index = 0,
                id = "id0",
                start = point2.start,
                end = point2.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 1,
                id = "id1",
                start = point3.start,
                end = point3.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 2,
                id = "id2",
                start = point4.start,
                end = point4.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 3,
                id = "id3",
                start = point5.start,
                end = point5.end,
                ctrl = "y",
            )
        )

        val varList = ArrayList<CardVarBean>()
        return CardConfigBean(
            name = "3联：Flu A/Flu B/COVID-19",
            type = CaseBean.TYPE_3LJ,
            code = "",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            topList = topList,
            varList = varList,
            scanStart = 0.toString(),
            scanEnd = 25.toString(),
            scanPPMM = scanPPMM,
            ft0 = 0.toString(),
            xt1 = 120.toString(),
            ft1 = 160.toString(),
            typeScore = 1.toString(),
            scope = 1.toString(),
            cAvg = 208327.166666667.toString(),
            cStd = 37852.3547282362.toString(),
            cMin = 132622.457210194.toString(),
            cMax = 284031.876123139.toString(),
            cutOff1 = 92.45.toString(),
            cutOff2 = 73.10.toString(),
            cutOff3 = 71.35.toString(),
            cutOffMax = 1000.toString(),
            cutOff5 = 92.45.toString(),
            cutOff6 = 73.10.toString(),
            cutOff7 = 71.35.toString(),
            noise1 = 0.toString(),
            noise2 = 0.toString(),
            noise3 = 0.toString(),
            noise4 = 0.toString(),
            noise5 = 0.toString(),
        )
    }

    // 2联：铁蛋白/C反应蛋白
    fun genCardConfigFor2LJA(): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        topList.add(
            CardTopBean(
                index = 0,
                id = "id0",
                start = point1.start,
                end = point1.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 1,
                id = "id1",
                start = point2.start,
                end = point2.end,
                ctrl = "y",
            )
        )
        topList.add(
            CardTopBean(
                index = 2,
                id = "id2",
                start = point4.start,
                end = point4.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 3,
                id = "id3",
                start = point5.start,
                end = point5.end,
                ctrl = "y",
            )
        )

        val varList = ArrayList<CardVarBean>()
        varList.add(
            CardVarBean(
                index = 0,
                id = "id0",
                start = "0.0",
                end = "0.94",
                x0 = "0.6332",
                x1 = "-0.095",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 1,
                id = "id1",
                start = "0.94",
                end = "1.53",
                x0 = "7.4146",
                x1 = "-6.5831",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 2,
                id = "id2",
                start = "1.53",
                end = "1.77",
                x0 = "20.872",
                x1 = "-26.911",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 3,
                id = "id3",
                start = "0.0",
                end = "0.3066",
                x0 = "16.31",
                x1 = "0.0",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 4,
                id = "id4",
                start = "0.3066",
                end = "0.956",
                x0 = "146.97",
                x1 = "-40.215",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 5,
                id = "id5",
                start = "0.956",
                end = "1.312",
                x0 = "280.79",
                x1 = "-168.45",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )

        return CardConfigBean(
            name = "2联：铁蛋白/C反应蛋白",
            type = CaseBean.TYPE_2LJ_A,
            code = "",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            topList = topList,
            varList = varList,
            scanStart = 0.toString(),
            scanEnd = 25.toString(),
            scanPPMM = scanPPMM,
            ft0 = 40.toString(),
            xt1 = 240.toString(),
            ft1 = 40.toString(),
            typeScore = 1.toString(),
            scope = 0.2.toString(),
            cAvg = 0.toString(),
            cStd = 0.toString(),
            cMin = 0.toString(),
            cMax = 0.toString(),
            cutOff1 = 0.toString(),
            cutOff2 = 0.toString(),
            cutOff3 = 0.toString(),
            cutOff4 = 0.toString(),
            cutOffMax = 9999999.toString(),
            cutOff5 = 0.toString(),
            cutOff6 = 0.toString(),
            cutOff7 = 0.toString(),
            cutOff8 = 0.toString(),
            noise1 = 0.toString(),
            noise2 = 0.toString(),
            noise3 = 0.toString(),
            noise4 = 0.toString(),
            noise5 = 0.toString(),
        )
    }

    // 2联：糖化血红蛋白/胱抑素C
    fun genCardConfigFor2LJB(): CardConfigBean {
        val topList = ArrayList<CardTopBean>()
        topList.add(
            CardTopBean(
                index = 0,
                id = "id0",
                start = point1.start,
                end = point1.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 1,
                id = "id1",
                start = point2.start,
                end = point2.end,
                ctrl = "y",
            )
        )
        topList.add(
            CardTopBean(
                index = 2,
                id = "id2",
                start = point4.start,
                end = point4.end,
                ctrl = "n",
            )
        )
        topList.add(
            CardTopBean(
                index = 3,
                id = "id3",
                start = point5.start,
                end = point5.end,
                ctrl = "y",
            )
        )

        val varList = ArrayList<CardVarBean>()
        varList.add(
            CardVarBean(
                index = 0,
                id = "id0",
                start = "0.0",
                end = "1.4121",
                x0 = "0.77934",
                x1 = "3.7988",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 1,
                id = "id1",
                start = "1.4121",
                end = "2.8242",
                x0 = "0.77934",
                x1 = "3.7988",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 2,
                id = "id2",
                start = "0.0",
                end = "0.889566",
                x0 = "0.0562",
                x1 = "0.6",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )
        varList.add(
            CardVarBean(
                index = 3,
                id = "id3",
                start = "0.889566",
                end = "0.99",
                x0 = "18.416",
                x1 = "-15.732",
                x2 = 0.0.toString(),
                x3 = 0.0.toString(),
                x4 = 0.0.toString(),
            )
        )

        return CardConfigBean(
            name = "2联：糖化血红蛋白/胱抑素C",
            type = CaseBean.TYPE_2LJ_B,
            code = "",
            prodDate = AppLocalDateUtils.formatDate(LocalDate.now()),
            endDate = AppLocalDateUtils.formatDate(LocalDate.now().plusYears(1)),
            topList = topList,
            varList = varList,
            scanStart = 0.toString(),
            scanEnd = 25.toString(),
            scanPPMM = scanPPMM,
            ft0 = 0.toString(),
            xt1 = 240.toString(),
            ft1 = 40.toString(),
            typeScore = 1.toString(),
            scope = 0.2.toString(),
            cAvg = 0.toString(),
            cStd = 0.toString(),
            cMin = 0.toString(),
            cMax = 0.toString(),
            cutOff1 = 0.toString(),
            cutOff2 = 0.toString(),
            cutOff3 = 0.toString(),
            cutOff4 = 0.toString(),
            cutOffMax = 9999999.toString(),
            cutOff5 = 0.toString(),
            cutOff6 = 0.toString(),
            cutOff7 = 0.toString(),
            cutOff8 = 0.toString(),
            noise1 = 0.toString(),
            noise2 = 0.toString(),
            noise3 = 0.toString(),
            noise4 = 0.toString(),
            noise5 = 0.toString(),
        )
    }

    fun genVersionUpgradeInfo(count: Int = 10): VersionUpgradeInfo {
        return VersionUpgradeInfo(
//            sysInfo = genSysInfo(),
            sysInfo = genSysInfoV2(),
            count = count
        )
    }

    fun genVersionInfo(type: Int = 0): VersionBean {
        return VersionBean(
            id = "1",
            software = "V1.0",
            hardware = "V1.0",
            softwareRemark = """
                1) XXXX
                2) abcabc
            """.trimIndent(),
            hardwareRemark = """
                1) XXXX
                2) abcabc
            """.trimIndent()
        )
    }

    fun genVersionInfos(): List<VersionBean> {
        val list = ArrayList<VersionBean>()
        list.add(
            VersionBean(
                id = "1",
                software = "V1.0",
                hardware = "V1.0",
                state = 0
            )
        )

        list.add(
            VersionBean(
                id = "2",
                software = "V1.0",
                hardware = "V1.0",
                state = 1
            )
        )

        list.add(
            VersionBean(
                id = "3",
                software = "V1.0", hardware = "V1.0", state = 2
            )
        )

        list.add(
            VersionBean(
                id = "4",
                software = "V1.0", hardware = "V1.0", state = 3
            )
        )

        list.add(
            VersionBean(
                id = "5",
                software = "V1.0", hardware = "V1.0", state = 4
            )
        )

        return list
    }
}