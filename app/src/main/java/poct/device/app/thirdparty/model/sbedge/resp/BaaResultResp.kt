package poct.device.app.thirdparty.model.sbedge.resp

class BaaResultResp(
    var detail: BaaResult?,
    var assets: Assets?,
)

data class BaaResult(
    var bioAgeProfile: BioAgeProfile
) {
    companion object {
        val Empty = BaaResult(bioAgeProfile = BioAgeProfile.Empty)
    }
}

data class BioAgeProfile(
    var chronoAge: Double = 0.0,
    var bioAge: Double = 0.0,
    var ageDifference: Double = 0.0,
    var scores: Map<String, Double> = HashMap(),
) {
    companion object {
        val Empty = BioAgeProfile()
    }
}

data class Assets(
    var titleImg: String = "",
    var diagramImg: String = "",
) {
    companion object {
        val Empty = Assets()
    }
}