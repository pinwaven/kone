package poct.device.app.service

/**
 * 生物标志物，表示了生物指标用于计算“生理年龄加速度”的相关信息
 */
data class Biomarker(
    val coefficientSexAge: Double,
    val coefficientENET: Double,
    val featureMean: Double,
    val chineseName: List<String>,
    val unit: String,
    val shortName: String,
    val useLogValue: Boolean
)

/**
 * 生物指标
 */
data class BioFeature(
    val name: String,
    val value: Double
)

/**
 * 生物指标对生理年龄贡献值
 */
data class BioFeatureContribution(
    val name: String,
    var contribution: Double
)

/**
 * BAA计算服务类
 * 提供生理年龄加速率计算功能
 */
class BAAService {

    companion object {
        /**
         * 生物标志物映射表
         */
        val biomarkerMap: Map<String, Biomarker> = mapOf(
            "age" to Biomarker(
                coefficientSexAge = 0.100432393,
                coefficientENET = 0.074763266,
                featureMean = 56.0487752,
                chineseName = listOf("年龄"),
                unit = "years",
                shortName = "Age",
                useLogValue = false
            ),
            "albumin" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.011331946,
                featureMean = 45.1238763,
                chineseName = listOf("白蛋白"),
                unit = "g/L",
                shortName = "Albumin",
                useLogValue = false
            ),
            "alkaline_phosphatase" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.00164946,
                featureMean = 82.6847975,
                chineseName = listOf("碱性磷酸酶"),
                unit = "U/L",
                shortName = "ALP",
                useLogValue = false
            ),
            "urea" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.029554872,
                featureMean = 5.3547152,
                chineseName = listOf("血清尿素", "尿素"),
                unit = "mmol/L",
                shortName = "Urea",
                useLogValue = false
            ),
            "cholesterol" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.0805656,
                featureMean = 5.6177437,
                chineseName = listOf("总胆固醇", "胆固醇"),
                unit = "mmol/L",
                shortName = "Cholesterol",
                useLogValue = false
            ),
            "creatinine" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.01095746,
                featureMean = 71.565605,
                chineseName = listOf("肌酐"),
                unit = "µmol/L",
                shortName = "Creatinine",
                useLogValue = false
            ),
            "cystatin_c" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 1.859556436,
                featureMean = 0.900946,
                chineseName = listOf("血清胱抑素C", "胱抑素C"),
                unit = "mg/L",
                shortName = "Cystatin C",
                useLogValue = false
            ),
            "glycated_haemoglobin" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.018116675,
                featureMean = 35.4785711,
                chineseName = listOf("糖化血红蛋白"),
                unit = "mmol/mol",
                shortName = "HbA1c",
                useLogValue = false
            ),
            "log_c_reactive_protein" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.079109916,
                featureMean = 0.3003624,
                chineseName = listOf(
                    "C反应蛋白",
                    "C反应蛋白测定",
                    "C反应蛋白检测",
                    "超敏C反应蛋白"
                ),
                unit = "mg/L",
                shortName = "CRP",
                useLogValue = true
            ),
            "log_gamma_glutamyltransf" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.265550311,
                featureMean = 3.3795613,
                chineseName = listOf(
                    "γ-谷氨酰基转肽酶",
                    "谷氨酰转肽酶",
                    "谷氨酰转氨酶",
                    "γ-谷氨酰转肽酶",
                    "γ-谷氨酰转氨酶",
                    "谷氨酰转移酶"
                ),
                unit = "U/L",
                shortName = "GGT",
                useLogValue = true
            ),
            "red_blood_cell_erythrocyte_count" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.204442153,
                featureMean = 4.4994648,
                chineseName = listOf("红细胞计数", "红细胞", "红细胞数", "红细胞量"),
                unit = "x10^12/L",
                shortName = "RBC",
                useLogValue = false
            ),
            "mean_corpuscular_volume" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.017165356,
                featureMean = 91.9251099,
                chineseName = listOf("平均红细胞体积"),
                unit = "fL",
                shortName = "MCV",
                useLogValue = false
            ),
            "red_blood_cell_erythrocyte_distribution_width" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.202009895,
                featureMean = 13.4342296,
                chineseName = listOf(
                    "红细胞分布宽度-变异系数",
                    "红细胞分布宽度",
                    "红细胞体积分布宽度(CV)"
                ),
                unit = "%",
                shortName = "RDW",
                useLogValue = false
            ),
            "monocyte_count" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.36937314,
                featureMean = 0.4746987,
                chineseName = listOf("单核细胞计数", "单核细胞绝对值"),
                unit = "x10^9/L",
                shortName = "Monocytes",
                useLogValue = false
            ),
            "neutrophill_count" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.06679092,
                featureMean = 4.1849454,
                chineseName = listOf("中性粒细胞计数", "中性粒细胞绝对值"),
                unit = "x10^9/L",
                shortName = "Neutrophils",
                useLogValue = false
            ),
            "lymphocyte_percentage" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.0108158,
                featureMean = 28.5817604,
                chineseName = listOf("淋巴细胞百分比"),
                unit = "%",
                shortName = "Lymphocytes",
                useLogValue = false
            ),
            "mean_sphered_cell_volume" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.006736204,
                featureMean = 83.6363269,
                chineseName = listOf("平均球形细胞体积"),
                unit = "fL",
                shortName = "MSCV",
                useLogValue = false
            ),
            "log_alanine_aminotransfe" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.312442261,
                featureMean = 3.077868,
                chineseName = listOf("丙氨酸氨基转移酶", "丙氨酸转氨酶", "丙氨酸氨基转移酶"),
                unit = "U/L",
                shortName = "ALT",
                useLogValue = true
            ),
            "log_shbg" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.292323186,
                featureMean = 3.8202787,
                chineseName = listOf("性激素结合球蛋白"),
                unit = "nmol/L",
                shortName = "SHBG",
                useLogValue = true
            ),
            "log_vitamin_d" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.265467867,
                featureMean = 3.6052878,
                chineseName = listOf("维生素D", "总25羟维生素D"),
                unit = "nmol/L",
                shortName = "Vitamin D",
                useLogValue = true
            ),
            "high_light_scatter_reticulocyte_percentage" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.169234165,
                featureMean = 0.3988152,
                chineseName = listOf(
                    "网织红细胞百分比",
                    "高光散射网织红细胞百分比",
                    "网织红细胞"
                ),
                unit = "%",
                shortName = "Reticulocytes",
                useLogValue = false
            ),
            "glucose" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.032171478,
                featureMean = 4.9563054,
                chineseName = listOf("葡萄糖", "血糖", "空腹血糖", "空腹血葡萄糖"),
                unit = "mmol/L",
                shortName = "Glucose",
                useLogValue = false
            ),
            "platelet_distribution_width" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.071527711,
                featureMean = 16.4543576,
                chineseName = listOf("血小板分布宽度", "血小板体积分布宽度"),
                unit = "%",
                shortName = "PDW",
                useLogValue = false
            ),
            "mean_corpuscular_haemoglobin" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = 0.02746487,
                featureMean = 31.8396206,
                chineseName = listOf(
                    "平均红细胞血红蛋白含量",
                    "平均血红蛋白含量",
                    "平均红细胞血红蛋白"
                ),
                unit = "pg",
                shortName = "MCH",
                useLogValue = false
            ),
            "platelet_crit" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -1.329561046,
                featureMean = 0.2385396,
                chineseName = listOf("血小板压积"),
                unit = "%",
                shortName = "PCT",
                useLogValue = false
            ),
            "apolipoprotein_a" to Biomarker(
                coefficientSexAge = 0.0,
                coefficientENET = -0.185139395,
                featureMean = 1.5238771,
                chineseName = listOf("载脂蛋白A", "脂蛋白A", "载脂蛋白A1"),
                unit = "g/L",
                shortName = "ApoA",
                useLogValue = false
            )
        )
    }

    /**
     * BAA计算结果数据类
     */
    data class BAAResult(
        val bioAge: Double, // 生理年龄
        val baa: Double, // 生理年龄加速率
        val bioFeatureContributions: List<BioFeatureContribution>
    )

    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String>
    )

    /**
     * 将生物指标标准化（中文名称、shortName转换为英文key）
     * @param bioFeatures 原始生物指标数组
     * @return 标准化后的生物指标数组
     */
    private fun normalizeBioFeatures(bioFeatures: List<BioFeature>): List<BioFeature> {
        return bioFeatures.map { feature ->
            // 如果已经是英文key，直接返回
            if (biomarkerMap.containsKey(feature.name)) {
                return@map feature
            }

            // 查找对应的英文key（通过中文名称）
            val englishKeyByChinese = findBiomarkerByChineseName(feature.name)
            if (englishKeyByChinese != null) {
                return@map BioFeature(name = englishKeyByChinese, value = feature.value)
            }

            // 查找对应的英文key（通过shortName）
            val englishKeyByShortName = findBiomarkerByShortName(feature.name)
            if (englishKeyByShortName != null) {
                return@map BioFeature(name = englishKeyByShortName, value = feature.value)
            }

            // 如果找不到对应的key，保持原样（后续验证会报错）
            feature
        }
    }

    /**
     * 计算生理年龄加速率 (Biological Age Acceleration)
     * @param bioFeatures 生物指标数组，必须包含Age（支持中英文混合）
     * @return BAA计算结果
     */
    fun calculateBAA(bioFeatures: List<BioFeature>): BAAResult {
        // 标准化生物指标
        val normalizedFeatures = normalizeBioFeatures(bioFeatures)

        // 指标对生理年龄贡献
        val bioFeatureContributions = normalizedFeatures
            .filter { biomarkerMap.containsKey(it.name) }
            .map { BioFeatureContribution(name = it.name, contribution = 0.0) }
            .toMutableList()

        // 验证必须包含Age
        val ageFeature = normalizedFeatures.find { it.name == "age" }
        if (ageFeature == null) {
            throw IllegalArgumentException("生物指标中必须包含年龄")
        }

        // 计算公式：
        // BAA = Σ((Biomarker.coefficientENET - Biomarker.coefficientSexAge) * (feature - Biomarker.featureMean))
        var baa = 0.0

        try {
            for (feature in normalizedFeatures) {
                val biomarker = biomarkerMap[feature.name]
                if (biomarker != null) {
                    var processedValue = feature.value

                    // 特殊处理糖化血红蛋白
                    if (feature.name == "glycated_haemoglobin") {
                        // convert % to mmol/mol using the formula provided
                        processedValue = feature.value * 10.929 - 23.5
                    }

                    val coefficientDiff = biomarker.coefficientENET - biomarker.coefficientSexAge
                    val featureValue = if (biomarker.useLogValue) {
                        Math.log(processedValue)
                    } else {
                        processedValue
                    }
                    val normalizedValue = featureValue - biomarker.featureMean
                    val featureBaa = coefficientDiff * normalizedValue * 10
                    baa += featureBaa

                    // 保存指标对生理年龄的贡献度
                    val index = bioFeatureContributions.indexOfFirst { it.name == feature.name }
                    if (index != -1) {
                        bioFeatureContributions[index].contribution = featureBaa
                    }
                }
            }
        } catch (error: Exception) {
            throw IllegalArgumentException("计算BAA时发生错误: ${error.message}")
        }

        return BAAResult(
            bioAge = ageFeature.value + baa, // 生理年龄 = 实际年龄 + BAA
            baa = baa, // 生理年龄加速率
            bioFeatureContributions = bioFeatureContributions
        )
    }

    /**
     * 获取所有支持的生物标志物信息
     * @return 生物标志物映射表
     */
    fun getBiomarkerMap(): Map<String, Biomarker> {
        return biomarkerMap
    }

    /**
     * 根据中文名称查找生物标志物
     * @param chineseName 中文名称
     * @return 匹配的生物标志物key，如果未找到返回null
     */
    fun findBiomarkerByChineseName(chineseName: String): String? {
        for ((key, biomarker) in biomarkerMap) {
            if (biomarker.chineseName.contains(chineseName)) {
                return key
            }
        }
        return null
    }

    /**
     * 根据shortName查找生物标志物, 匹配时会忽略大小写、空格、中划线、下划线等格式书写差异
     * @param shortName 短名称
     * @return 匹配的生物标志物key，如果未找到返回null
     */
    fun findBiomarkerByShortName(shortName: String): String? {
        val normalizedShortName = shortName.replace("[-_\\s]".toRegex(), "").lowercase()
        for ((key, biomarker) in biomarkerMap) {
            val normalizedInputShortName =
                biomarker.shortName.replace("[-_\\s]".toRegex(), "").lowercase()
            if (normalizedInputShortName == normalizedShortName) {
                return key
            }
        }
        return null
    }

    /**
     * 验证生物指标是否有效
     * @param bioFeatures 生物指标数组（支持中英文混合）
     * @return 验证结果，包含是否有效和错误信息
     */
    fun validateBioFeatures(bioFeatures: List<BioFeature>): ValidationResult {
        val errors = mutableListOf<String>()

        // 标准化生物指标
        val normalizedFeatures = normalizeBioFeatures(bioFeatures)

        // 检查是否包含age
        val hasAge = normalizedFeatures.any { it.name == "age" }
        if (!hasAge) {
            errors.add("生物指标中必须包含年龄")
        }

        // 检查其他指标是否在支持的范围内
        for (feature in normalizedFeatures) {
            val biomarker = biomarkerMap[feature.name]
            if (biomarker == null) {
                errors.add("不支持的生物标志物: ${feature.name}")
            } else if (feature.value <= 0 && biomarker.useLogValue) {
                errors.add("使用对数计算时，${feature.name} 的值必须为正数")
            }
        }

        return ValidationResult(
            isValid = errors.isEmpty(),
            errors = errors
        )
    }
}

// 使用示例
fun main() {
    val service = BAAService()

    // 示例生物指标数据
    val bioFeatures = listOf(
        BioFeature("age", 32.0),
        BioFeature("cystatin_c", 1.0),
        BioFeature("log_c_reactive_protein", 0.44),
        BioFeature("glycated_haemoglobin", 4.5),
    )

    // 验证数据
    val validation = service.validateBioFeatures(bioFeatures)
    if (validation.isValid) {
        // 计算BAA
        val result = service.calculateBAA(bioFeatures)
        println("生理年龄: ${result.bioAge}")
        println("生理年龄加速率: ${result.baa}")
        println("各指标贡献:")
        result.bioFeatureContributions.forEach { contribution ->
            println("${contribution.name}: ${contribution.contribution}")
        }
    } else {
        println("数据验证失败: ${validation.errors}")
    }
}