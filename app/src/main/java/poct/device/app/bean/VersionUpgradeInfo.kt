package poct.device.app.bean

data class VersionUpgradeInfo(
//    var sysInfo: ConfigInfoBean = ConfigInfoBean.Empty,
    var sysInfo: ConfigInfoV2Bean = ConfigInfoV2Bean.Empty,
    var count: Int = 0,
) {
    companion object {
        val Empty = VersionUpgradeInfo()
    }
}
