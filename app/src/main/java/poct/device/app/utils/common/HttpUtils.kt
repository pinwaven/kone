package poct.device.app.utils.common

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.IOException
import poct.device.app.App
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit

class HttpUtils(
    private val baseUrl: String? = null,
    private val connectTimeout: Long = 5,
    private val readTimeout: Long = 15,
    private val writeTimeout: Long = 10
) {
    private val client: OkHttpClient by lazy { buildClient() }

    private val smartDns = SmartDnsResolver()

    companion object {
        private const val TAG = "HttpUtils"
        private const val DEFAULT_CHECK_URL = "https://www.baidu.com"
        private const val CHECK_CONNECTIVITY_TIMEOUT_SECONDS = 5L
    }

    // 构建可定制的OkHttpClient
    fun buildClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .dns(smartDns)  // 使用智能DNS解析器
            .hostnameVerifier { _, _ -> true } // 跳过主机名验证
            .connectTimeout(connectTimeout, TimeUnit.SECONDS)
            .readTimeout(readTimeout, TimeUnit.SECONDS)
            .writeTimeout(writeTimeout, TimeUnit.SECONDS)
            .addInterceptor(LoggingInterceptor()) // 日志拦截器
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "HttpUtils/1.0")
                    .build()
                chain.proceed(request)
            }
            .retryOnConnectionFailure(true)
            .addInterceptor(RetryInterceptor(3))
            .build()
    }

    fun buildUrl(endpoint: String, params: Map<String, String>? = null): HttpUrl {
        // 1. 处理URL拼接逻辑
        val fullUrl = when {
            // 情况1：baseUrl以/结尾，且endpoint以/开头 → 避免双斜杠
            baseUrl?.endsWith('/') == true && endpoint.startsWith('/') ->
                baseUrl + endpoint.substring(1)
            // 情况2：baseUrl非空 → 直接拼接
            baseUrl != null ->
                "$baseUrl/$endpoint".replace("//", "/") // 防止重复斜杠
            // 情况3：无baseUrl → 直接使用endpoint
            else -> endpoint
        }

        // 2. 解析URL并构建
        return HttpUrl.Builder()
            .scheme("https") // 默认使用HTTPS，可根据需求动态化
            .apply {
                // 解析主机和路径（兼容完整URL和相对路径）
                val parsedUrl = fullUrl.toHttpUrlOrNull()
                    ?: throw IllegalArgumentException("Invalid URL: $fullUrl")

                host(parsedUrl.host)
                port(parsedUrl.port)
                encodedPath(parsedUrl.encodedPath)

                // 添加查询参数（覆盖原始URL中的同名参数）
                params?.forEach { addQueryParameter(it.key, it.value) }
            }
            .build()
    }

    fun buildRequestBody(
        params: Map<String, Any>? = null,
        jsonBody: String? = null
    ): RequestBody {
        return when {
            jsonBody != null -> {
                jsonBody.toRequestBody("application/json".toMediaType())
            }

            params != null -> {
                FormBody.Builder().apply {
                    params.forEach { (key, value) ->
                        add(key, value.toString())
                    }
                }.build()
            }

            else -> {
                "".toRequestBody(null)
            }
        }
    }

    @Throws(IOException::class)
    fun executeRequest(request: Request): String {
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Unexpected code ${response.code}")
            }
            return response.body?.string() ?: ""
        }
    }

    //=============== 同步请求 ===============//
    @Throws(IOException::class)
    fun syncGet(path: String, params: Map<String, String>? = null): Response {
        val request = Request.Builder()
            .url(buildUrl(path, params))
            .get()
            .build()
        return client.newCall(request).execute()
    }

    //=============== 异步请求（协程） ===============//
    suspend fun get(
        path: String,
        params: Map<String, String>? = null,
        headers: Map<String, String>? = null
    ): String = withContext(Dispatchers.IO) {
        val requestBuilder = Request.Builder()
            .url(buildUrl(path, params))
            .get()
        headers?.forEach { requestBuilder.addHeader(it.key, it.value) }
        val request = requestBuilder.build()
        executeRequest(request)
    }

    @Throws(IOException::class)
    fun post(
        path: String,
        params: Map<String, Any>? = null,
        req: Any? = null,
        headers: Map<String, String>? = null
    ) {
        val jsonBody = App.gson.toJson(req)
        println("request: $jsonBody")

        val requestBuilder = Request.Builder()
            .url(buildUrl(path))
            .post(buildRequestBody(params, jsonBody))
        headers?.forEach { requestBuilder.addHeader(it.key, it.value) }
        val request = requestBuilder.build()

        executeRequest(request)
    }

    //=============== 文件操作 ===============//
    suspend fun uploadFile(
        path: String,
        fileParamName: String,
        file: File,
        params: Map<String, String>? = null
    ): String = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .apply {
                params?.forEach { addFormDataPart(it.key, it.value) }
                addFormDataPart(
                    fileParamName,
                    file.name,
                    file.asRequestBody("multipart/form-data".toMediaType())
                )
            }
            .build()

        val request = Request.Builder()
            .url(buildUrl(path))
            .post(requestBody)
            .build()

        executeRequest(request)
    }

    suspend fun downloadFile(
        url: String,
        outputFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext false

            response.body?.use { body ->
                outputFile.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            Timber.Forest.tag(TAG).e(e, "Download failed")
            false
        }
    }

    //=============== 网络连通性检查 ===============//
    /**
     * 检查网络连通性（同步阻塞，应在后台线程调用）
     * 只要能在超时时间内收到响应（任何HTTP状态码）即返回 true，超时或发生 IO 异常返回 false
     * @param testUrl 用于测试的 URL，默认为百度。若传入 null 则尝试使用 baseUrl，否则使用默认地址
     * @return true 表示网络可达且未超时，false 表示超时或网络异常
     */
    fun checkConnectivity(testUrl: String? = null): Boolean {
        val url = testUrl ?: baseUrl ?: DEFAULT_CHECK_URL
        // 创建独立的 client，仅用于连通性检查，强制超时 5 秒
        val checkClient = OkHttpClient.Builder()
            .connectTimeout(CHECK_CONNECTIVITY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(CHECK_CONNECTIVITY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(CHECK_CONNECTIVITY_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false) // 不重试，快速失败
            .build()

        return try {
            val request = Request.Builder().url(url).head().build()
            val response = checkClient.newCall(request).execute()
            // 只要没有抛出异常，无论状态码如何都认为连通
            response.close()
            true
        } catch (e: IOException) {
            Timber.Forest.tag(TAG).d(e, "Connectivity check failed for $url")
            false
        }
    }

    //=============== 日志拦截器 ===============//
    private class LoggingInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            Timber.Forest.tag(TAG).d("Request: ${request.url}")

            val startTime = System.nanoTime()
            val response = chain.proceed(request)
            val endTime = System.nanoTime()

            Timber.Forest.tag(TAG).d(
                """
                Response: ${response.code} in ${(endTime - startTime) / 1e6}ms
                Headers: ${response.headers}
                Body: ${response.peekBody(1024).string()}
            """.trimIndent()
            )

            return response
        }
    }

    //=============== 重试拦截器 ===============//
    class RetryInterceptor(private val maxRetries: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request()
            var lastException: IOException? = null

            for (i in 0..maxRetries) {
                try {
                    val response = chain.proceed(request)
                    if (response.isSuccessful) {
                        return response
                    }
                    // 非成功响应：关闭后再决定是否重试
                    response.close()
                    if (i < maxRetries) {
                        Thread.sleep(1000L * (1 shl i)) // 指数退避：1s, 2s, 4s...
                    }
                } catch (e: IOException) {
                    lastException = e
                    if (i < maxRetries) {
                        Thread.sleep(1000L * (1 shl i))
                    }
                }
            }

            throw lastException ?: IOException("Unknown error after $maxRetries retries")
        }
    }
}