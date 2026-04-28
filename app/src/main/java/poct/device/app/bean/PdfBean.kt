package poct.device.app.bean

import android.graphics.Bitmap

data class PdfBean(
    var title: String = "苏州大学附属医院检验报告",
    var subTitle: String = "Nanovate_AI report",
    var jcbhLabel: String = "检测编号:",
    var jcrqLabel: String = "检查日期:",
    var jcbh: String = "",
    var type: String = "ige",
    var jcrq: String = "",

    val xmLabel: String = "姓名:",
    val xbLabel: String = "性别:",
    val nlLabel: String = "年龄:",
    val xm: String = "",
    val xb: String = "男/女",
    val nl: String = "",
    val sjIdLabel: String = "试剂ID:",
    val ybIdLabel: String = "样本ID:",
    val sjId: String = "",
    val ybId: String = "",

    var tcLabel: String = "T/C值",
    var t1Label: String = "T1值",
    var t2Label: String = "T2值",
    var t3Label: String = "T3值",
    var t4Label: String = "T4值",
    var cLabel: String = "C值",

    var jcjgLabel: String = "检查结果",
    var jcxmLabel: String = "检查项目",
    var radioLabel: String = "样本/临界值",
    var jgpdLabel: String = "结果判读",
    var ckbzLabel: String = "参考标准",
    var data: List<List<String>>,

    val smLabel: String = "说明",
    val smContent1: String = "1.此结果仅对该标本负责，供医生参考；如有疑问请当日与检查科室联系。",
    var outPath: String,
    var logo: Bitmap?
)
