package poct.device.app.thirdparty

import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Request
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.bean.CaseBean
import poct.device.app.entity.CaseResult
import poct.device.app.thirdparty.model.fros.req.QueryByCodeReq
import poct.device.app.thirdparty.model.fros.req.UploadCheckDataReq
import poct.device.app.thirdparty.model.fros.req.UploadCheckDataResult
import poct.device.app.thirdparty.model.fros.resp.FrosBaseResp
import poct.device.app.thirdparty.model.fros.resp.QueryByCodeResp
import poct.device.app.utils.common.HttpUtils
import timber.log.Timber
import java.io.IOException
import java.lang.reflect.Type
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object FrosApi {
    private val httpUtils: HttpUtils = HttpUtils(baseUrl = getDomain())

    fun uploadPatientReportDataToServer(bean: CaseBean) {
        println("uploadPatientReportDataToServer：" + App.gson.toJson(bean).toString())

        val results = ArrayList<UploadCheckDataResult>()
        if (bean.resultList.isNotEmpty()) {
            bean.resultList.forEachIndexed { _, item ->
                val r = UploadCheckDataResult(
                    name = item.name,
                    result = item.result,
                    radioValue = item.radioValue,
                    refer = item.refer,
                    t1Value = item.t1Value,
                    t2Value = item.t2Value,
                    t3Value = item.t3Value,
                    t4Value = item.t4Value,
                    cValue = item.cValue,
                    c2Value = item.c2Value,

                    t1ValueName = item.t1ValueName,
                    t2ValueName = item.t2ValueName,
                    t3ValueName = item.t3ValueName,
                    t4ValueName = item.t4ValueName,

                    t1ValueStr = item.t1ValueStr,
                    t2ValueStr = item.t2ValueStr,
                    t3ValueStr = item.t3ValueStr,
                    t4ValueStr = item.t4ValueStr,
                )
                results.add(r)
            }
        }

        val req = UploadCheckDataReq(
            code = bean.qrCode,
//            type = bean.type,
//            patient = bean.patientId,
            date = bean.workTime,
            result = results,
        )

        try {
            httpUtils.post(
                path = "api/service/poct/device/uploadCheckData",
                req = req,
            )
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    suspend fun getPatientCaseReport(cardCode: String): CaseBean {
        println("cardCode：$cardCode")

        val req = QueryByCodeReq(
            code = cardCode,
        )

        val data = CaseBean()
        try {
            val resp = postWithFrosResp<QueryByCodeResp>(
                path = "api/service/poct/device/queryByCode",
                req = req,
            )
            Timber.w("getPatientCaseReport：%s", App.gson.toJson(resp).toString())

            if (resp.isSuccess()) {
                data.qrCode = resp.result!!.code
                data.workTime = resp.result!!.date
                data.type = resp.result!!.type

                data.patientId = resp.result!!.patient.objectId
                data.name = resp.result!!.patient.name_
                data.gender = if (resp.result!!.patient.gender == "female") 2 else 1

                val instant = Instant.parse(resp.result!!.patient.birthDate)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    .withZone(ZoneId.systemDefault())
                data.birthday = formatter.format(instant)

                val caseResults = ArrayList<CaseResult>()
                if (!resp.result!!.result.isNullOrEmpty()) {
                    resp.result!!.result.forEachIndexed { _, item ->
                        val r = CaseResult(
                            name = item.value.name ?: "",
                            result = item.value.result ?: "",
                            radioValue = item.value.radioValue ?: "",
                            refer = item.value.refer ?: "",
                            t1Value = item.value.t1Value ?: "",
                            t2Value = item.value.t2Value ?: "",
                            t3Value = item.value.t3Value ?: "",
                            t4Value = item.value.t4Value ?: "",
                            cValue = item.value.cValue ?: "",
                            c2Value = item.value.c2Value ?: "",

                            t1ValueName = item.value.t1ValueName ?: "",
                            t2ValueName = item.value.t2ValueName ?: "",
                            t3ValueName = item.value.t3ValueName ?: "",
                            t4ValueName = item.value.t4ValueName ?: "",

                            t1ValueStr = item.value.t1ValueStr ?: "",
                            t2ValueStr = item.value.t2ValueStr ?: "",
                            t3ValueStr = item.value.t3ValueStr ?: "",
                            t4ValueStr = item.value.t4ValueStr ?: "",
                        )
                        caseResults.add(r)
                    }
                }
                data.workResult = Json.encodeToString(caseResults)
                Timber.w("getPatientCaseReport data：%s", App.gson.toJson(data).toString())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return data
    }

    fun getReportUrl(cardCode: String): String {
//        return "http://my-test.virtualhealth.cn/#/pages/testReports/testReportDetail?test_code=$cardCode"

        var domain = "http://my.virtualhealth.cn"
        if (AppParams.devMock) {
            // test
            domain = "http://my-test.virtualhealth.cn"
        }
        return "$domain/#/pages/testReports/testReportDetail?test_code=$cardCode"
    }

//    fun getReportUrlV2(
//        name: String,
//        age: String,
//        bioAge: String,
//        cysc: String,
//        hba1c: String
//    ): String {
//        var urlTemplate =
//            "http://my-test.virtualhealth.cn/#/pages/testReports/baa_demo?name=%s&age=%s&bioAge=%s&cysc=%s&hba1c=%s"
//        return String.format(urlTemplate, name, age, bioAge, cysc, hba1c)
//    }

    private fun getDomain(): String {
//        return "https://fros-api-dev.gyyyhospital.com"

        var domain = "https://fros-api.gyyyhospital.com"
        if (AppParams.devMock) {
            // test
            domain = "https://fros-api-dev.gyyyhospital.com"
        }
        return domain
    }

    @Throws(IOException::class)
    private suspend inline fun <reified T> postWithFrosResp(
        path: String,
        params: Map<String, Any>? = null,
        req: Any? = null,
        headers: Map<String, String>? = null
    ): FrosBaseResp<T> = withContext(Dispatchers.IO) {
        postWithFrosResp(
            path, params, req, headers,
            type = object : TypeToken<FrosBaseResp<T>>() {}.type
        )
    }

    @Throws(IOException::class)
    private suspend fun <T> postWithFrosResp(
        path: String,
        params: Map<String, Any>? = null,
        req: Any? = null,
        headers: Map<String, String>? = null,
        type: Type,
    ): FrosBaseResp<T> = withContext(Dispatchers.IO) {
        val jsonBody = App.gson.toJson(req)
        println("request: $jsonBody")

        val request = Request.Builder()
            .url(httpUtils.buildUrl(path))
            .apply { headers?.forEach { Request.Builder().addHeader(it.key, it.value) } }
            .post(httpUtils.buildRequestBody(params, jsonBody))
            .build()

        var respData = httpUtils.executeRequest(request)

        // TODO json格式统一字段命名规则
        respData = respData.replace("objectId", "object_id")
            .replace("birthDate", "birth_date")
        App.gson.fromJson(respData, type)
    }
}