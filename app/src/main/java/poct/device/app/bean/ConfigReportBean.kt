package poct.device.app.bean

data class ConfigReportBean(
    var hosName: String = "",  // 医院名称
    var logoPath: String = "", // logo路径
    var igeName: String = "",  // ige报告名称
    var crpName: String = "",  // crp报告名称
    var cljName: String = "4联：HRV/hMPV/ADV/RSV", // 4lj报告名称
    var sljName: String = "3联：Flu A/Flu B/COVID-19", // 3lj报告名称
    var eljAName: String = "2联：铁蛋白/C反应蛋白", // 2lj报告名称
    var eljBName: String = "2联：糖化血红蛋白/胱抑素C", // 2lj报告名称
) : ConfigBean {
    companion object {
        const val PREFIX = "report_"
        val Empty = ConfigReportBean()
    }
}
