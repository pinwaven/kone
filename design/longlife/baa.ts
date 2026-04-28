/**
 * 生理年龄加速率计算服务
 * BAA: Biological Age Acceleration
 * 通过收集用户的生物指标，计算用户的生理年龄加速度，从而评估用户的健康状况。
 * 计算公式：
 * BAA = Σ((Biomarker.coefficientENET - Biomarker.coefficientSexAge) * (feature - Biomarker.featureMean))
 * 其中，feature 为用户的生物指标值，Biomarker.featureMean 为该生物指标的均值。
 */

/**
 * 生物标志物，表示了生物指标用于计算“生理年龄加速度”的相关信息，如：
 * - coefficientSexAge：性别年龄系数
 * - coefficientENET：Enet系数
 * - featureMean：特征均值
 * - chineseName：中文名称
 * - unit：单位
 * - shortName：短名称
 * - useLogValue：是否需要使用对数计算
 */
interface Biomarker {
  coefficientSexAge: number;
  coefficientENET: number;
  featureMean: number;
  chineseName: string[];
  unit: string;
  shortName: string;
  useLogValue: boolean;
}

/**
 * 生物指标
 */
export interface BioFeature {
  name: string;
  value: number;
}

/**
 * 生物指标对生理年龄贡献值
 */
interface BioFeatureContribution {
  name: string;
  contribution: number;
}

const biomarkerMap: Record<string, Biomarker> = {
  age: {
    coefficientSexAge: 0.100432393,
    coefficientENET: 0.074763266,
    featureMean: 56.0487752,
    chineseName: ['年龄'],
    unit: 'years',
    shortName: 'Age',
    useLogValue: false,
  },
  albumin: {
    coefficientSexAge: 0,
    coefficientENET: -0.011331946,
    featureMean: 45.1238763,
    chineseName: ['白蛋白'],
    unit: 'g/L',
    shortName: 'Albumin',
    useLogValue: false,
  },
  alkaline_phosphatase: {
    coefficientSexAge: 0,
    coefficientENET: 0.00164946,
    featureMean: 82.6847975,
    chineseName: ['碱性磷酸酶'],
    unit: 'U/L',
    shortName: 'ALP',
    useLogValue: false,
  },
  urea: {
    coefficientSexAge: 0,
    coefficientENET: -0.029554872,
    featureMean: 5.3547152,
    chineseName: ['血清尿素', '尿素'],
    unit: 'mmol/L',
    shortName: 'Urea',
    useLogValue: false,
  },
  cholesterol: {
    coefficientSexAge: 0,
    coefficientENET: -0.0805656,
    featureMean: 5.6177437,
    chineseName: ['总胆固醇', '胆固醇'],
    unit: 'mmol/L',
    shortName: 'Cholesterol',
    useLogValue: false,
  },
  creatinine: {
    coefficientSexAge: 0,
    coefficientENET: -0.01095746,
    featureMean: 71.565605,
    chineseName: ['肌酐'],
    unit: 'µmol/L',
    shortName: 'Creatinine',
    useLogValue: false,
  },
  cystatin_c: {
    coefficientSexAge: 0,
    coefficientENET: 1.859556436,
    featureMean: 0.900946,
    chineseName: ['血清胱抑素C', '胱抑素C'],
    unit: 'mg/L',
    shortName: 'Cystatin C',
    useLogValue: false,
  },
  glycated_haemoglobin: {
    coefficientSexAge: 0,
    coefficientENET: 0.018116675,
    featureMean: 35.4785711,
    chineseName: ['糖化血红蛋白'],
    unit: 'mmol/mol',
    shortName: 'HbA1c',
    useLogValue: false,
  },
  log_c_reactive_protein: {
    coefficientSexAge: 0,
    coefficientENET: 0.079109916,
    featureMean: 0.3003624,
    chineseName: [
      'C反应蛋白',
      'C反应蛋白测定',
      'C反应蛋白检测',
      '超敏C反应蛋白',
    ],
    unit: 'mg/L',
    shortName: 'CRP',
    useLogValue: true,
  },
  log_gamma_glutamyltransf: {
    coefficientSexAge: 0,
    coefficientENET: 0.265550311,
    featureMean: 3.3795613,
    chineseName: [
      'γ-谷氨酰基转肽酶',
      '谷氨酰转肽酶',
      '谷氨酰转氨酶',
      'γ-谷氨酰转肽酶',
      'γ-谷氨酰转氨酶',
      '谷氨酰转移酶',
    ],
    unit: 'U/L',
    shortName: 'GGT',
    useLogValue: true,
  },
  red_blood_cell_erythrocyte_count: {
    coefficientSexAge: 0,
    coefficientENET: -0.204442153,
    featureMean: 4.4994648,
    chineseName: ['红细胞计数', '红细胞', '红细胞数', '红细胞量'],
    unit: 'x10^12/L',
    shortName: 'RBC',
    useLogValue: false,
  },
  mean_corpuscular_volume: {
    coefficientSexAge: 0,
    coefficientENET: 0.017165356,
    featureMean: 91.9251099,
    chineseName: ['平均红细胞体积'],
    unit: 'fL',
    shortName: 'MCV',
    useLogValue: false,
  },
  red_blood_cell_erythrocyte_distribution_width: {
    coefficientSexAge: 0,
    coefficientENET: 0.202009895,
    featureMean: 13.4342296,
    chineseName: [
      '红细胞分布宽度-变异系数',
      '红细胞分布宽度',
      '红细胞体积分布宽度(CV)',
    ],
    unit: '%',
    shortName: 'RDW',
    useLogValue: false,
  },
  monocyte_count: {
    coefficientSexAge: 0,
    coefficientENET: 0.36937314,
    featureMean: 0.4746987,
    chineseName: ['单核细胞计数', '单核细胞绝对值'],
    unit: 'x10^9/L',
    shortName: 'Monocytes',
    useLogValue: false,
  },
  neutrophill_count: {
    coefficientSexAge: 0,
    coefficientENET: 0.06679092,
    featureMean: 4.1849454,
    chineseName: ['中性粒细胞计数', '中性粒细胞绝对值'],
    unit: 'x10^9/L',
    shortName: 'Neutrophils',
    useLogValue: false,
  },
  lymphocyte_percentage: {
    coefficientSexAge: 0,
    coefficientENET: -0.0108158,
    featureMean: 28.5817604,
    chineseName: ['淋巴细胞百分比'],
    unit: '%',
    shortName: 'Lymphocytes',
    useLogValue: false,
  },
  mean_sphered_cell_volume: {
    coefficientSexAge: 0,
    coefficientENET: 0.006736204,
    featureMean: 83.6363269,
    chineseName: ['平均球形细胞体积'],
    unit: 'fL',
    shortName: 'MSCV',
    useLogValue: false,
  },
  log_alanine_aminotransfe: {
    coefficientSexAge: 0,
    coefficientENET: -0.312442261,
    featureMean: 3.077868,
    chineseName: ['丙氨酸氨基转移酶', '丙氨酸转氨酶', '丙氨酸氨基转移酶'],
    unit: 'U/L',
    shortName: 'ALT',
    useLogValue: true,
  },
  log_shbg: {
    coefficientSexAge: 0,
    coefficientENET: 0.292323186,
    featureMean: 3.8202787,
    chineseName: ['性激素结合球蛋白'],
    unit: 'nmol/L',
    shortName: 'SHBG',
    useLogValue: true,
  },
  log_vitamin_d: {
    coefficientSexAge: 0,
    coefficientENET: -0.265467867,
    featureMean: 3.6052878,
    chineseName: ['维生素D', '总25羟维生素D'],
    unit: 'nmol/L',
    shortName: 'Vitamin D',
    useLogValue: true,
  },
  high_light_scatter_reticulocyte_percentage: {
    coefficientSexAge: 0,
    coefficientENET: 0.169234165,
    featureMean: 0.3988152,
    chineseName: [
      '网织红细胞百分比',
      '高光散射网织红细胞百分比',
      '网织红细胞',
    ],
    unit: '%',
    shortName: 'Reticulocytes',
    useLogValue: false,
  },
  glucose: {
    coefficientSexAge: 0,
    coefficientENET: 0.032171478,
    featureMean: 4.9563054,
    chineseName: ['葡萄糖', '血糖', '空腹血糖', '空腹血葡萄糖'],
    unit: 'mmol/L',
    shortName: 'Glucose',
    useLogValue: false,
  },
  platelet_distribution_width: {
    coefficientSexAge: 0,
    coefficientENET: 0.071527711,
    featureMean: 16.4543576,
    chineseName: ['血小板分布宽度', '血小板体积分布宽度'],
    unit: '%',
    shortName: 'PDW',
    useLogValue: false,
  },
  mean_corpuscular_haemoglobin: {
    coefficientSexAge: 0,
    coefficientENET: 0.02746487,
    featureMean: 31.8396206,
    chineseName: [
      '平均红细胞血红蛋白含量',
      '平均血红蛋白含量',
      '平均红细胞血红蛋白',
    ],
    unit: 'pg',
    shortName: 'MCH',
    useLogValue: false,
  },
  platelet_crit: {
    coefficientSexAge: 0,
    coefficientENET: -1.329561046,
    featureMean: 0.2385396,
    chineseName: ['血小板压积'],
    unit: '%',
    shortName: 'PCT',
    useLogValue: false,
  },
  apolipoprotein_a: {
    coefficientSexAge: 0,
    coefficientENET: -0.185139395,
    featureMean: 1.5238771,
    chineseName: ['载脂蛋白A', '脂蛋白A', '载脂蛋白A1'],
    unit: 'g/L',
    shortName: 'ApoA',
    useLogValue: false,
  },
};

/**
 * BAA计算服务类
 * 提供生理年龄加速率计算功能
 */
export class BAAService {
  /**
   * 将生物指标标准化（中文名称、shortName转换为英文key）
   * @param bioFeatures 原始生物指标数组
   * @returns 标准化后的生物指标数组
   */
  private normalizeBioFeatures(bioFeatures: BioFeature[]): BioFeature[] {
    return bioFeatures.map((feature) => {
      // 如果已经是英文key，直接返回
      if (biomarkerMap[feature.name]) {
        return feature;
      }

      // 查找对应的英文key（通过中文名称）
      const englishKeyByChinese = this.findBiomarkerByChineseName(feature.name);
      if (englishKeyByChinese) {
        return { name: englishKeyByChinese, value: feature.value };
      }

      // 查找对应的英文key（通过shortName）
      const englishKeyByShortName = this.findBiomarkerByShortName(feature.name);
      if (englishKeyByShortName) {
        return { name: englishKeyByShortName, value: feature.value };
      }

      // 如果找不到对应的key，保持原样（后续验证会报错）
      return feature;
    });
  }

  /**
   * 计算生理年龄加速率 (Biological Age Acceleration)
   * @param bioFeatures 生物指标数组，必须包含Age（支持中英文混合）
   * @returns BAA计算结果
   */
  calculateBAA(bioFeatures: BioFeature[]): {
    bioAge: number; // 生理年龄
    baa: number; // 生理年龄加速率
    bioFeatureContributions: BioFeatureContribution[];
  } {
    // 标准化生物指标
    const normalizedFeatures = this.normalizeBioFeatures(bioFeatures);
    // 指标对生理年龄贡献
    const bioFeatureContributions: BioFeatureContribution[] = normalizedFeatures
      .filter((feature) => biomarkerMap[feature.name])
      .map((feature) => {
        return {
          name: feature.name,
          contribution: 0,
        };
      });
    // 验证必须包含Age
    const ageFeature = normalizedFeatures.find((feature) =>
      feature.name === 'age'
    );
    if (!ageFeature) {
      throw new Error('生物指标中必须包含年龄');
    }

    // 计算公式：
    // BAA = Σ((Biomarker.coefficientENET - Biomarker.coefficientSexAge) * (feature - Biomarker.featureMean))

    let baa = 0;
    try {
      for (const feature of normalizedFeatures) {
        const biomarker = biomarkerMap[feature.name];
        if (biomarker) {
          if (feature.name === 'glycated_haemoglobin') {
            // convert % to mmol/mol using the formula provided
            feature.value = feature.value * 10.929 - 23.5;
          }
          const coefficientDiff = biomarker.coefficientENET -
            biomarker.coefficientSexAge;
          const featureValue = biomarker.useLogValue
            ? Math.log(feature.value)
            : feature.value;
          const normalizedValue = featureValue - biomarker.featureMean;
          const featureBaa = coefficientDiff * normalizedValue * 10;
          baa += featureBaa;
          // 保存指标对生理年龄的贡献度
          const index = bioFeatureContributions.findIndex((item) =>
            item.name === feature.name
          );
          if (index !== -1) {
            bioFeatureContributions[index].contribution = featureBaa;
          }
        }
      }
    } catch (error) {
      throw new Error('计算BAA时发生错误: ' + error);
    }
    return {
      bioAge: ageFeature.value + baa, // 生理年龄 = 实际年龄 + BAA
      baa: baa, // 生理年龄加速率
      bioFeatureContributions,
    };
  }

  /**
   * 获取所有支持的生物标志物信息
   * @returns 生物标志物映射表
   */
  getBiomarkerMap(): Record<string, Biomarker> {
    return biomarkerMap;
  }

  /**
   * 根据中文名称查找生物标志物
   * @param chineseName 中文名称
   * @returns 匹配的生物标志物key，如果未找到返回null
   */
  findBiomarkerByChineseName(chineseName: string): string | null {
    for (const [key, biomarker] of Object.entries(biomarkerMap)) {
      if (biomarker.chineseName.includes(chineseName)) {
        return key;
      }
    }
    return null;
  }

  /**
   * 根据shortName查找生物标志物, 匹配时会忽略大小写、空格、中划线、下划线等格式书写差异
   * @param shortName 短名称
   * @returns 匹配的生物标志物key，如果未找到返回null
   */
  findBiomarkerByShortName(shortName: string): string | null {
    const normalizedShortName = shortName.replace(/[-_\s]/g, '')
      .toLowerCase();
    for (const [key, biomarker] of Object.entries(biomarkerMap)) {
      const normalizedInputShortName = biomarker.shortName.replace(
        /[-_\s]/g,
        '',
      ).toLowerCase();
      if (normalizedInputShortName === normalizedShortName) {
        return key;
      }
    }
    return null;
  }

  /**
   * 验证生物指标是否有效
   * @param bioFeatures 生物指标数组（支持中英文混合）
   * @returns 验证结果，包含是否有效和错误信息
   */
  validateBioFeatures(
    bioFeatures: BioFeature[],
  ): { isValid: boolean; errors: string[] } {
    const errors: string[] = [];

    // 标准化生物指标
    const normalizedFeatures = this.normalizeBioFeatures(bioFeatures);

    // 检查是否包含age
    const hasAge = normalizedFeatures.some((feature) => feature.name === 'age');
    if (!hasAge) {
      errors.push('生物指标中必须包含年龄');
    }

    // 检查其他指标是否在支持的范围内
    for (const feature of normalizedFeatures) {
      if (!biomarkerMap[feature.name]) {
        errors.push(`不支持的生物标志物: ${feature.name}`);
      }

      if (feature.value <= 0 && biomarkerMap[feature.name]?.useLogValue) {
        errors.push(
          `使用对数计算时，${feature.name} 的值必须为正数`,
        );
      }
    }

    return {
      isValid: errors.length === 0,
      errors,
    };
  }
}
