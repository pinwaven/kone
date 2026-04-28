package poct.device.app.bean

import poct.device.app.pdf.AbstractPdfTemplateData

data class PdfTemplateDataSample(
    var applyDate: String? = null,
    var type: String? = null,
    var femaleName: String? = null,
    var maleName: String? = null,
    var sampleCode: String? = null,
    var sampleNo: String? = null,
    var result: String? = null,
    var submitDate: String? = null,
    var submitter: String? = null,
    var remark: String? = null,

) : AbstractPdfTemplateData(){
    companion object {
        val Empty = PdfTemplateDataSample()
    }
}

