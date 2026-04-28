package poct.device.app.bean

/**
 * 结果参考
 */
data class ResultReferBean(
    /**
     * 参考指标
     */
    var items: List<ResultReferItem>,

    ) {

    /**
     * 查询参考值
     */
    fun findItem(itemName: String, ageInMonth: Int): ResultReferItem {
        return items.filter { it.name == itemName && ageInMonth >= it.ageMin && ageInMonth < it.ageMax }
            .getOrElse(0) {
                ResultReferItem.EMPTY
            }
    }

    /**
     * 婴儿(<1岁):0 - 15 IU/mL
     * 幼儿(1-5岁): 0 - 60 IU/mL
     * 儿童(6-9岁): 0 - 90 IU/mL
     * 少年(10-15岁): 0 - 200 IU/mL
     * 青年(16岁以上): 0 - 100 IU/mL
     * 查询是否超出范围
     */
    fun findIgeItem(result: Double, months: Int): Boolean {
        if (months in 0..12) {
            if (result > 15.0) {
                return true
            }
        }
        if (months in 13..60) {
            if (result > 60.0) {
                return true
            }
        }
        if (months in 61..120) {
            if (result > 90.0) {
                return true
            }
        }
        if (months in 121..192) {
            if (result > 200.0) {
                return true
            }
        }
        if (months > 192) {
            if (result > 100.0) {
                return true
            }
        }
        return false
    }

    /**
     * 查询是否超出范围
     */
    fun findSfItem(result: Double, months: Int, crpResult: Double): Boolean {
        if (crpResult > 5) {
            // 感染或炎症患者
            if (months in 0..60) {
                if (result < 30.0) {
                    return true
                }
            }
            if (months > 60) {
                if (result < 70.0) {
                    return true
                }
            }
        } else {
            // 健康人群
            if (months in 0..60) {
                if (result < 12.0) {
                    return true
                }
            }
            if (months > 60) {
                if (result < 15.0) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 查询是否超出范围
     */
    fun findCrpItem(result: Double): Boolean {
        if (result > 5) {
            return true
        }
        return false
    }

    fun writeToConfig() {
        // TODO 从文件读取
    }

    //    remark = "Babies (< 1 year):0 - 15 IU/mL\n" +
//    "Toddlers (1 - 5 years): 0 - 60 IU/mL\n" +
//    "Children (6 - 9 years): 0 - 90 IU/mL\n" +
//    "Young adults (10 - 15 years): 0 - 200 IU/mL\n" +
//    "Adults (older than 16 years): 0 - 100 IU/mL",
    companion object {
        private var cache: Map<String, ResultReferBean>? = null
        fun getConfigMap(): Map<String, ResultReferBean> {
            // TODO 从文件读取
            return cache ?: synchronized(this) {
                cache = createDefaultConfig()
                return cache as Map<String, ResultReferBean>
            }
        }

        private fun createDefaultConfig(): Map<String, ResultReferBean> {
            val map = HashMap<String, ResultReferBean>()
            with(CaseBean.TYPE_IGE) {
                val itemList = ArrayList<ResultReferItem>()
                itemList.add(
                    ResultReferItem(
                        name = CaseBean.IGE_T1,
                        ageMin = 0,
                        ageMax = 12,
                        valueMin = 0,
                        valueMax = 13,
                        remark = "婴儿(<1岁):0 - 15 IU/mL@",
                        unit = "IU/mL",
                    )
                )
                itemList.add(
                    ResultReferItem(
                        name = CaseBean.IGE_T1,
                        ageMin = 13,
                        ageMax = 60,
                        valueMin = 0,
                        valueMax = 30,
                        remark = "幼儿(1-5岁): 0 - 60 IU/mL@",
                        unit = "IU/mL",
                    )
                )
                itemList.add(
                    ResultReferItem(
                        name = CaseBean.IGE_T1,
                        ageMin = 61,
                        ageMax = 120,
                        valueMin = 0,
                        valueMax = 100,
                        remark = "儿童(6-9岁): 0 - 90 IU/mL@",
                        unit = "IU/mL",
                    )
                )
                itemList.add(
                    ResultReferItem(
                        name = CaseBean.IGE_T1,
                        ageMin = 120,
                        ageMax = 192,
                        valueMin = 0,
                        valueMax = 200,
                        remark = "少年(10-15岁): 0 - 200 IU/mL@",
                        unit = "IU/mL",
                    )
                )
                itemList.add(
                    ResultReferItem(
                        name = CaseBean.IGE_T1,
                        ageMin = 192,
                        ageMax = 2400,
                        valueMin = 0,
                        valueMax = 100,
                        remark = "成年(16岁以上): 0 - 100 IU/mL",
                        unit = "IU/mL",
                    )
                )
                val config = ResultReferBean(
                    items = itemList
                )
                map[this] = config
            }
            with(CaseBean.TYPE_CRP) {
                val itemList = ArrayList<ResultReferItem>()
                itemList.add(
                    ResultReferItem(
                        name = CaseBean.CRP_T2,
                        ageMin = 0,
                        ageMax = 1200,
                        valueMin = 0,
                        valueMax = 12,
                        remark = "健康人群@" + "(<5岁): <12ng/mL@" +
                                "(5岁以上): <15ng/mL@" + "感染或炎症患者@" + "(<5岁): <30ng/mL@" +
                                "(5岁以上): <70ng/mL",
                        unit = "ng/mL",
                    )
                )
                itemList.add(
                    ResultReferItem(
                        name = CaseBean.CRP_T1,
                        ageMin = 0,
                        ageMax = 1200,
                        valueMin = 0,
                        valueMax = 13,
                        remark = ">5mg/L@" + "为感染或炎症患者",
                        unit = "mg/L",
                    )
                )
                val config = ResultReferBean(
                    items = itemList
                )
                map[this] = config
            }
            return map
        }
    }
}

data class ResultReferItem(
    /**
     * 指标名称
     */
    var name: String,
    /**
     * 最小年纪，月，>=
     */
    var ageMin: Int,
    /**
     * 最大年纪，月，<
     */
    var ageMax: Int,
    /**
     * 指标最小值
     */
    var valueMin: Int,
    /**
     * 指标最大值
     */
    var valueMax: Int,
    /**
     * 备注：
     */
    var remark: String,
    /**
     * 单位
     */
    var unit: String,
) {
    companion object {
        val EMPTY = ResultReferItem("outOfRange", 0, Int.MAX_VALUE, 0, Int.MAX_VALUE, "", unit = "")
    }
}