import { BAAService } from './service/baa.ts';

Deno.test('BAA服务-中英文混合输入-生理年龄计算', () => {
  const baaService = new BAAService();

  // 测试数据
  /**
   * 年龄 Age：47.3岁
血清胱抑素C  Cystatin C：无
红细胞分布宽度-变异系数RDW：0.13
γ-谷氨酰基转肽酶 GGT：22U/L
性激素结合球蛋白SHBG：无
糖化血红蛋白 HbA1c：无
中性粒细胞计数Neutrophils：2.83*109/L
C反应蛋白  CRP：无
平均红细胞体积 MCV：95.1fL
单核细胞计数Monocytes：0.36*109/L
平均红细胞血红蛋白含量 MCH：31.9pg
碱性磷酸酶ALP：无
空腹血葡萄糖  Glucose：5mmol/L
白蛋白Albumin：44.2g/L
血清尿素  Urea：3.1mmol/L
载脂蛋白A  ApoA：无
淋巴细胞百分比Lymphocytes：30.9%
总胆固醇  Cholesterol：3.39mmol/L
红细胞计数RBC：4.27*1012/L
维生素D  Vitamin D：无
丙氨酸氨基转移酶 ALT：12U/L
肌酐  Creatinine：61umol/L


平均球形细胞体积MSCV：无
网织红细胞百分比 Reticulocytes：无
血小板分布宽度 PDW：14.1fL
血小板压积PCT：0.24%
   */
  const testData = [
    { name: '年龄', value: 42 },
    { name: 'Cystatin C', value: 1.57 },
    { name: 'HbA1c', value: 2.21 },
    // { name: 'RDW', value: 13 },
    // { name: 'GGT', value: 22 },
    // { name: 'Neutrophils', value: 2.83 },
    // { name: 'MCV', value: 95.1 },
    // { name: 'Monocytes', value: 0.36 },
    // { name: 'MCH', value: 31.9 },
    // { name: 'Glucose', value: 5 },
    // { name: 'Albumin', value: 44.2 },
    // { name: 'Urea', value: 3.1 },
    // { name: 'Lymphocytes', value: 30.9 },
    // { name: 'Cholesterol', value: 3.39 },
    // { name: 'RBC', value: 4.27 },
    // { name: 'ALT', value: 12 },
    // { name: 'Creatinine', value: 61 },
    // { name: 'PDW', value: 14.1 },
    // { name: 'PCT', value: 0.24 },
  ];

  // 验证输入
  const validation = baaService.validateBioFeatures(testData);
  console.log('验证结果:', validation);

  // 计算BAA
  console.log('开始计算BAA...');
  const result = baaService.calculateBAA(testData);
  console.log('计算结果:', result);

  // 断言生理年龄在预期范围
  const expectedBioAge = 70;
  const tolerance = 1;
  console.log(
    '生理年龄:',
    result.bioAge,
    '预期:',
    expectedBioAge,
    '误差:',
    tolerance,
  );

  // 可选：断言贡献度数组长度
  console.log('贡献度数组长度:', result.bioFeatureContributions.length);

  // 可选：输出调试信息
  // console.log("生理年龄:", result.bioAge, "贡献度:", result.bioFeatureContributions);
});
