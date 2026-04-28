package poct.device.app.thirdparty

import com.google.gson.JsonParseException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.bean.card.Card
import poct.device.app.bean.card.CardBatch
import poct.device.app.bean.card.CardConfig
import poct.device.app.bean.card.CardInfoBean
import poct.device.app.thirdparty.model.sbedge.req.ActivateDeviceReq
import poct.device.app.thirdparty.model.sbedge.req.BaaResultReq
import poct.device.app.thirdparty.model.sbedge.req.CheckDevicePwdReq
import poct.device.app.thirdparty.model.sbedge.req.GetCardInfoReq
import poct.device.app.thirdparty.model.sbedge.req.GetDeviceConfigReq
import poct.device.app.thirdparty.model.sbedge.req.UpdateCardStatusReq
import poct.device.app.thirdparty.model.sbedge.resp.ActivateResp
import poct.device.app.thirdparty.model.sbedge.resp.BaaResultResp
import poct.device.app.thirdparty.model.sbedge.resp.CardInfoResp
import poct.device.app.thirdparty.model.sbedge.resp.CheckResp
import poct.device.app.thirdparty.model.sbedge.resp.ConfigInfoResp
import poct.device.app.thirdparty.model.sbedge.resp.SbEdgeBaseResp
import poct.device.app.utils.common.HttpUtils
import java.io.IOException
import java.lang.reflect.Type

object SbEdgeFunc {
    const val EMPTY_VAL = "获取失败，请先激活设备"

    private val httpUtils: HttpUtils = HttpUtils(baseUrl = getDomain())

    suspend fun activateDevice(deviceId: String): Boolean {
        val req = ActivateDeviceReq(deviceId = deviceId)

        try {
            val resp = postWithSbEdgeResp<ActivateResp>(
                path = "business/poct/device/activate",
                req = req,
            )

            if (resp.isSuccess() && resp.data != null) {
                return resp.data!!.ok
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
        return false
    }

    suspend fun getDeviceConfig(deviceId: String): ConfigInfoV2Bean {
        val req = GetDeviceConfigReq(deviceId = deviceId)

        val respData = ConfigInfoV2Bean(
            name = EMPTY_VAL,
            code = EMPTY_VAL,
            type = EMPTY_VAL,
            software = EMPTY_VAL,
            hardware = EMPTY_VAL,
        )
        try {
            val resp = postWithSbEdgeResp<ConfigInfoResp>(
                path = "business/poct/device/getConfig",
                req = req,
            )

            if (resp.isSuccess() && resp.data != null) {
                respData.name = resp.data!!.name
                respData.code = resp.data!!.code
                respData.type = resp.data!!.type
                respData.software = resp.data!!.apkVersion
                respData.hardware = resp.data!!.firmwareVersion
                return respData
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
        return respData
    }

    suspend fun checkDevicePwd(deviceId: String, pwd: String): Boolean {
        val req = CheckDevicePwdReq(deviceId = deviceId, pwd = pwd)

        try {
            val resp = postWithSbEdgeResp<CheckResp>(
                path = "business/poct/device/checkPwd",
                req = req,
            )

            if (resp.isSuccess() && resp.data != null) {
                return resp.data!!.ok
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
        return false
    }

    suspend fun getCardInfo(cardBatchCode: String, cardCode: String): CardInfoBean {
        val req = GetCardInfoReq(cardBatchCode = cardBatchCode, cardCode = cardCode)

        val respData = CardInfoBean.Empty
        try {
            val resp = postWithSbEdgeResp<CardInfoResp>(
                path = "business/poct/card/getInfo",
                req = req,
            )

            if (resp.isSuccess() && resp.data != null) {
                val card = resp.data!!.card
                val cardBatch = resp.data!!.cardBatch
                val cardConfig = resp.data!!.cardConfig
                respData.card = Card(
                    id = card.id,
                    cardBatchId = card.cardBatchId,
                    code = card.code,
                    usedDate = card.usedDate,
                    status = card.status,
                    createdAt = card.createdAt,
                    updatedAt = card.updatedAt,
                )
                respData.cardBatch = CardBatch(
                    id = cardBatch.id,
                    name = cardBatch.name,
                    type = cardBatch.type,
                    code = cardBatch.code,
                    prodDate = cardBatch.prodDate,
                    expDate = cardBatch.expDate,
                    status = cardBatch.status,
                    guideVideo = cardBatch.guideVideo,
                    guideText = cardBatch.guideText,
                    createdAt = cardBatch.createdAt,
                    updatedAt = cardBatch.updatedAt,
                )

                if (cardConfig != null) {
                    respData.cardConfig = CardConfig(
                        scanPPMM = cardConfig.scanPpmm,
                        topList = cardConfig.topList,
                        varList = cardConfig.varList,
                        ft0 = cardConfig.ft0,
                        xt1 = cardConfig.xt1,
                        ft1 = cardConfig.ft1,
                        scope = cardConfig.scope,
                        typeScore = cardConfig.typeScore,
                        cAvg = cardConfig.cAvg,
                        cStd = cardConfig.cStd,
                        cMin = cardConfig.cMin,
                        cMax = cardConfig.cMax,
                        cutOff1 = cardConfig.cutOff1,
                        cutOff2 = cardConfig.cutOff2,
                        cutOff3 = cardConfig.cutOff3,
                        cutOff4 = cardConfig.cutOff4,
                        cutOff5 = cardConfig.cutOff5,
                        cutOff6 = cardConfig.cutOff6,
                        cutOff7 = cardConfig.cutOff7,
                        cutOff8 = cardConfig.cutOff8,
                        cutOffMax = cardConfig.cutOffMax,
                        noise1 = cardConfig.noise1,
                        noise2 = cardConfig.noise2,
                        noise3 = cardConfig.noise3,
                        noise4 = cardConfig.noise4,
                        noise5 = cardConfig.noise5,
                    )
                }
                return respData
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
        return respData
    }

    suspend fun updateCardStatus(code: String, status: String): Boolean {
        val req = UpdateCardStatusReq(cardCode = code, status = status)

        try {
            val resp = postWithSbEdgeResp<CheckResp>(
                path = "business/poct/card/updateStatus",
                req = req,
            )

            if (resp.isSuccess() && resp.data != null) {
                return resp.data!!.ok
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
        return false
    }

    suspend fun baaResult(code: String, dmsPatientId: String): BaaResultResp? {
        val req = BaaResultReq(code = code, dmsPatientId = dmsPatientId)

        try {
            val resp = postWithSbEdgeResp<BaaResultResp>(
                path = "business/poct/baa/result",
                req = req,
            )

            if (resp.isSuccess() && resp.data != null) {
                return resp.data!!
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: JsonParseException) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(IOException::class)
    private suspend inline fun <reified T> postWithSbEdgeResp(
        path: String,
        params: Map<String, Any>? = null,
        req: Any? = null,
        headers: Map<String, String>? = null
    ): SbEdgeBaseResp<T> = withContext(Dispatchers.IO) {
        postWithSbEdgeResp(
            path, params, req, headers,
            type = object : TypeToken<SbEdgeBaseResp<T>>() {}.type
        )
    }

    @Throws(IOException::class)
    private suspend fun <T> postWithSbEdgeResp(
        path: String,
        params: Map<String, Any>? = null,
        req: Any? = null,
        headers: Map<String, String>? = null,
        type: Type,
    ): SbEdgeBaseResp<T> = withContext(Dispatchers.IO) {
        val jsonBody = App.gson.toJson(req)
        println("request: $jsonBody")

        val request = Request.Builder()
            .url(httpUtils.buildUrl(path))
            .apply { headers?.forEach { Request.Builder().addHeader(it.key, it.value) } }
            .post(httpUtils.buildRequestBody(params, jsonBody))
            .build()

        var respData = httpUtils.executeRequest(request)

        // TODO json格式统一字段命名规则
        respData = respData.replace("BioAgeProfile", "bio_age_profile")
            .replace("ChronoAge", "chrono_age")
            .replace("BioAge", "bio_age")
            .replace("AgeDifference", "age_difference")
            .replace("Scores", "scores")
        App.gson.fromJson(respData, type)
    }

    fun getDomain(): String {
//        return "https://sb.fros.cc/functions/v1"

        var domain = "https://supabase.virtualhealth.cn/functions/v1"
        if (AppParams.devMock) {
            // test
            domain = "https://sb.fros.cc/functions/v1"
        }
        return domain
    }
}