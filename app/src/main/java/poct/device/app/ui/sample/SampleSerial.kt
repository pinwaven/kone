package poct.device.app.ui.sample

import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import poct.device.app.App
import poct.device.app.RouteConfig
import poct.device.app.bean.ConfigInfoV2Bean
import poct.device.app.component.AppFilledButton
import poct.device.app.component.AppPreviewWrapper
import poct.device.app.component.AppScaffold
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.serial.v2.ctl.CtlConstantsV2
import poct.device.app.theme.filledFontColor
import poct.device.app.theme.inputFontColor
import poct.device.app.thirdparty.SbEdgeFunc
import poct.device.app.utils.app.AppSystemUtils
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * 页面定义
 */
@Composable
fun SampleSerial(
    navController: NavController,
    viewModel: SampleSerialViewModel = viewModel(),
) {
    val text by viewModel.text.collectAsState()
    AppScaffold(
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(70.dp)
            ) {
                Text(
                    modifier = Modifier.fillMaxSize(),
                    fontSize = 13.sp,
                    color = inputFontColor,
                    text = text
                )
            }
            Row {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "获取设备ID",
                        onClick = { viewModel.getDeviceId() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "串口重连",
                        onClick = { viewModel.reconnect() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "握手",
                        onClick = { viewModel.handshake() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "gpio_read",
                        onClick = { viewModel.gpioRead() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "片仓复位",
                        onClick = { viewModel.sendResetCase() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "片仓移入",
                        onClick = { viewModel.moveIn() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "片仓移出",
                        onClick = { viewModel.moveOut() }
                    )
                    Spacer(modifier = Modifier.height(36.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "版本更新",
                        onClick = { viewModel.upgrade() }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "返回",
                        onClick = { navController.navigate(RouteConfig.SETTING_MAIN) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "首页",
                        onClick = { navController.navigate(RouteConfig.HOME_MAIN) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    AppFilledButton(
                        modifier = Modifier
                            .width(120.dp)
                            .height(36.dp),
                        fontSize = 14.sp,
                        text = "退出应用",
                        onClick = { viewModel.exit() }
                    )
                }
                Spacer(modifier = Modifier.width(60.dp))
                Column {
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ),
                        onClick = { viewModel.activateDevice() }) {
                        Text(text = "设备激活")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ),
                        onClick = { viewModel.poll() }) {
                        Text(text = "poll")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ), onClick = { viewModel.absorb() }) {
                        Text(text = "吸水240秒")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ), onClick = { viewModel.scanCard() }) {
                        Text(text = "扫描试剂卡")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ), onClick = { viewModel.readData() }) {
                        Text(text = "扫描结果写入文件")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ), onClick = { viewModel.cancel() }) {
                        Text(text = "取消命令")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ), onClick = { viewModel.scanQRCode() }) {
                        Text(text = "扫描二维码")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ), onClick = { viewModel.powerOff() }) {
                        Text(text = "关闭供电")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = filledFontColor
                        ), onClick = { viewModel.powerOn() }) {
                        Text(text = "开启供电")
                    }
                    Spacer(modifier = Modifier.height(12.dp))
//                    Button(onClick = { viewModel.moveDatabase(navController) }) {
//                        Text(text = "移动数据库")
//                    }
//                    Spacer(modifier = Modifier.height(12.dp))
//                    Button(onClick = { viewModel.sendTest2(navController) }) {
//                        Text(text = "上传测试")
//                    }
//                    Spacer(modifier = Modifier.height(12.dp))
//                    Button(onClick = { viewModel.updatePdf(navController) }) {
//                        Text(text = "更新PDF")
//                    }
                }
            }
        }
    }
}

class SampleSerialViewModel : ViewModel() {
    val text = MutableStateFlow("")

    fun getDeviceId() {
        val deviceId: String = App.getDeviceId()
        text.value = ("获取获取设备ID success: $deviceId")
    }

    fun activateDevice() {
        viewModelScope.launch(Dispatchers.IO) {
            val deviceId = App.getDeviceId()
            val deviceConfigInfo: ConfigInfoV2Bean = SbEdgeFunc.getDeviceConfig(deviceId)

            var isOk = true
            if (deviceConfigInfo.code == SbEdgeFunc.EMPTY_VAL) {
                isOk = SbEdgeFunc.activateDevice(deviceId)
            }
            withContext(Dispatchers.Main) {
                text.value = ("设备激活 $isOk")
            }
        }
    }

    fun reconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            App.getSerialHelper().close()
            App.getSerialHelper().open()
            withContext(Dispatchers.Main) {
                text.value = ("串口重连 success")
            }
        }
    }

    fun handshake() {
        viewModelScope.launch {
            text.value = ("握手中。。。")
            val version = withContext(Dispatchers.IO) {
                val hiResult = CtlCommandsV2.readAllData(CtlCommandsV2.hi())
                var version = ""
                if (hiResult.isNotEmpty()) {
                    val versionData = hiResult.split("ver:")
                    if (versionData.size > 1) {
                        version = versionData[1]
                    }
                }
                version
            }
            text.value = ("握手 success: $version")
        }
    }

    fun gpioRead() {
        viewModelScope.launch {
            text.value = ("gpio_read。。。")
            val result = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.gpioRead())
            }
            text.value =
                ("gpio_read success: $result")
        }
    }

    fun moveIn() {
        viewModelScope.launch {
            text.value = ("片仓移入中。。。")
            withContext(Dispatchers.IO) {
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, 88888, 10000, 0))
                Timber.d("moveToSsResult: $moveToSsResult")

                // 等待成功
                CtlCommandsV2.waitMoveToSsStatusSuccess()
            }
            text.value = ("片仓移入 success")
        }
    }

    fun moveOut() {
        viewModelScope.launch {
            text.value = ("片仓移出中。。。")
            withContext(Dispatchers.IO) {
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                Timber.d("moveToSsResult: $moveToSsResult")

                // 等待成功
                CtlCommandsV2.waitMoveToSsStatusSuccess()
            }
            text.value = ("片仓移出 success")
        }
    }

    fun poll() {
        viewModelScope.launch {
            text.value = ("poll。。。")
            val result = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.poll())
            }
            text.value =
                ("poll success: $result")
        }
    }

    fun absorb() {
        viewModelScope.launch {
            text.value = ("吸水中。。。")
            withContext(Dispatchers.IO) {
                val result = CtlCommandsV2.readAllData(CtlCommandsV2.absorb(240 * 1000))

                withContext(Dispatchers.Main) {
                    text.value =
                        ("吸水 $result")
                }

                CtlCommandsV2.waitAbsorbStatusSuccess()
            }
            text.value =
                ("吸水 success")
        }
    }

    fun scanCard() {
        viewModelScope.launch {
            text.value = ("扫描中。。。")
            withContext(Dispatchers.IO) {
                val getLDPwr =
                    CtlCommandsV2.readAllData(CtlCommandsV2.getLDPwr())

                withContext(Dispatchers.Main) {
                    text.value =
                        ("getLDPwr: $getLDPwr")
                }

                val setLDPwr =
                    CtlCommandsV2.readAllData(CtlCommandsV2.setLDPwr(-10))

                withContext(Dispatchers.Main) {
                    text.value =
                        ("setLDPwr: $setLDPwr")
                }

                val result = CtlCommandsV2.readAllData(CtlCommandsV2.scan(-16000, 16000))

                withContext(Dispatchers.Main) {
                    text.value =
                        ("扫描 $result")
                }

                CtlCommandsV2.waitScanStatusSuccess()
            }
            text.value =
                ("扫描 success")
        }
    }

    fun cancel() {
        viewModelScope.launch {
            text.value = ("取消中。。。")
            withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
            }
            text.value =
                ("取消 success")
        }
    }

//    fun upgrade() {
//        viewModelScope.launch(Dispatchers.IO) {
//            val filePath: String = AppFileUtils.getHardWareApkPath()
//            val msgList = CtlCommandsV2.upgrade(0, filePath)
//            val total = msgList.size
//            var count = 0
//            var flag = true
//            if (msgList.isEmpty()) {
//                Timber.w("######不存在升级文件#######")
//                return@launch
//            }
//            Timber.w("#######${total}")
//            while (count < total) {
//                if (flag) {
//                    if (count != 0) {
//                        Timber.w("#######step2##${count}")
//                        Timber.w("#######step2##${App.gson.toJson(msgList[count])}")
//                    }
//                    App.getCtlSerialService()
//                        .send(
//                            msgList[count],
//                            object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                                override suspend fun error(
//                                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                    e: Exception?,
//                                    scope: CoroutineScope,
//                                ) {
//                                    // 对当前发送失败的切片进行重发
//                                    flag = true
//                                    Timber.w("#######ERROR${flag}")
//                                    Timber.w("#######ERROR${e?.message}")
//                                }
//
//                                override suspend fun delay(
//                                    feedback: CtlSerialMessageV2,
//                                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                    scope: CoroutineScope,
//                                ) {
//                                    text.value = ("Delay:${feedback.paramData.toQueryString()}")
//                                    Timber.w("#######Delay${flag}")
//                                    Timber.w("#######Delay${feedback.paramData.toQueryString()}")
//                                }
//
//                                override suspend fun success(
//                                    feedback: CtlSerialMessageV2,
//                                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                    scope: CoroutineScope,
//                                ) {
//                                    text.value = ("success:${feedback.paramData.toQueryString()}")
//                                    count++
//                                    Timber.w("#######success${count}")
//                                    Timber.w("#######success${total}")
//                                    flag = true
//                                }
//                            })
//                }
//                flag = false
//                Thread.sleep(300)
//            }
//        }
//    }

    fun readData() {
        viewModelScope.launch {
            text.value = ("读取成功，写入中。。。")

            val path = withContext(Dispatchers.IO) {
                val queryResult = CtlCommandsV2.readAllDataByteArray(CtlCommandsV2.queryData())

                val file = File(App.getContext().externalCacheDir, "data.bin")
                if (!file.parentFile?.exists()!!) {
                    file.parentFile?.mkdirs()
                }
                if (file.exists()) {
                    file.delete()
                }
                FileOutputStream(file).use { outputStream ->
                    outputStream.write(queryResult)
                }
                file.path
            }

            text.value =
                ("写入 success $path")
        }
    }

    fun sendResetCase() {
        viewModelScope.launch {
            text.value = ("仓片复位中。。。")

            withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.homing())
                homingSuccess()
            }
        }
    }

    private fun homingSuccess() {
        CtlCommandsV2.processHomingStatus { homingSuccessCustomFunction(it) }
    }

    private fun homingSuccessCustomFunction(progressVal: Int) {
        viewModelScope.launch {
            if (progressVal < CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                withContext(Dispatchers.IO) {
                    homingSuccess()
                }
            } else {
                withContext(Dispatchers.IO) {
                    val moveToSsResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                    Timber.w("moveToSsResult: $moveToSsResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveToSsStatusSuccess()
                }

                text.value = ("仓片复位 success")
            }
        }
    }

    fun scanQRCode() {
        viewModelScope.launch {
            text.value = ("扫描二维码中。。。")
            val readQRResult = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.readQR())
            }
            text.value = ("扫描二维码 success $readQRResult")
        }
    }

    suspend fun readQrSuccess() {
        CtlCommandsV2.processReadQRStatus { scanQrSuccessCustomFunction(it) }
    }

    fun scanQrSuccessCustomFunction(qrCodeData: String) {
        viewModelScope.launch {
            if (qrCodeData.isEmpty()) {
                withContext(Dispatchers.IO) {
                    readQrSuccess()
                }
            } else {
                text.value = ("扫描二维码 success: $qrCodeData")
            }
        }
    }

    // TODO aabbcc
    fun upgrade() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 启动主屏幕来清除所有顶层Activity
        App.getContext().startActivity(intent)
    }

    fun exit() {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // 启动主屏幕来清除所有顶层Activity
        App.getContext().startActivity(intent)
    }

    fun powerOff() {
        AppSystemUtils.powerOffCtlBoard()
        text.value = ("关闭供电 success")
    }

    fun powerOn() {
        AppSystemUtils.powerOnCtlBoard()
        text.value = ("开启供电 success")
    }

//    fun moveDatabase(
//        navController: NavController
//    ) {
//        viewModelScope.launch(Dispatchers.IO) {
//            // 移动数据到外部存储
//            val srcDir = File("/data/data/poct.device.app/databases/")
//            val destDir = File(AppFileUtils.getBaseFileDirPath() + "/database/")
//            moveDir(srcDir, destDir)
//            withContext(Dispatchers.Main) {
//                text.value = ("移动数据库完成！！")
//            }
//        }
//    }

    private fun moveDir(srcDir: File, dstDir: File) {
        if (!srcDir.exists() || !srcDir.isDirectory) {
            // 源目录不存在或不是目录
            return
        }
        val files = srcDir.listFiles()
        if (files != null) {
            for (file in files) {
                val newFile = File(dstDir, file.name)
                if (file.isFile) {
                    moveFile(file, newFile)
                } else if (file.isDirectory) {
                    moveDir(file, newFile)
                }
            }
        }
    }

    private fun moveFile(srcFile: File, dstFile: File) {
        try {
            if (!dstFile.exists()) {
                if (!dstFile.createNewFile()) {
                    // 创建目标文件失败
                    return
                }
            }
            val inputStream = FileInputStream(srcFile)
            val inChannel = inputStream.channel
            val outputStream = FileOutputStream(dstFile)
            val outChannel = outputStream.channel
            try {
                // 将源文件内容传输到目标文件
                inChannel!!.transferTo(0, inChannel.size(), outChannel)
            } finally {
                inChannel?.close()
                outChannel?.close()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

//    fun sendTest(time: Long) {
//        viewModelScope.launch(Dispatchers.IO) {
//            withContext(Dispatchers.Main) {
//                text.value =
//                    ("指令长度：" + CtlCommandsV2.testCmdRequest(1, 159).toByteBuf().writerIndex())
//            }
//            for (i in 0..9999) {
//                withContext(Dispatchers.Main) {
//                    text.value = ("发送次数：" + i)
//                }
//                App.getCtlSerialService().send(
//                    CtlCommandsV2.testCmdRequest(i, 159),
//                    object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                        override suspend fun delay(
//                            feedback: CtlSerialMessageV2,
//                            sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                            scope: CoroutineScope,
//                        ) {
//                            text.value = ("Delay:${feedback.paramData.toQueryString()}")
//                        }
//
//                        override suspend fun success(
//                            feedback: CtlSerialMessageV2,
//                            sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                            scope: CoroutineScope,
//                        ) {
//                            text.value = ("success:${feedback.paramData.toQueryString()}")
//                        }
//                    })
//                // 50ms发送一次
//                Thread.sleep(time)
//            }
//        }
//    }

//    fun sendTest2(
//        navController: NavController
//    ) {
//        viewModelScope.launch(Dispatchers.IO) {
//            App.getCtlSerialService().send(
//                CtlCommandsV2.testCmdRequest(),
//                object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                    override suspend fun delay(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        text.value = ("Delay:${feedback.paramData.toQueryString()}")
//                    }
//
//                    override suspend fun success(
//                        feedback: CtlSerialMessageV2,
//                        sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                        scope: CoroutineScope,
//                    ) {
//                        text.value = ("success:${feedback.paramData.toQueryString()}")
//                    }
//                })
//        }
//    }

//    fun updatePdf(
//        navController: NavController
//    ) {
//        var templateData = PdfTemplateDataSample()
//        templateData.applyDate = "2024年11月11日"
//        templateData.type = "AID"
//        templateData.femaleName = "女方姓名"
//        templateData.maleName = "男方姓名"
//        templateData.sampleCode = "样本编码"
//        templateData.sampleNo = "样本编号"
//        templateData.result = "匹配通过"
//        templateData.submitDate = "2024年11月19日"
//        templateData.submitter = "某某医生"
//        templateData.remark = "建议复查XXXXXXXXX"
//
//        val dataDir =
//            File(Environment.getExternalStorageDirectory().absolutePath + File.separator + "Android" + File.separator + "Nanovate_AI" + File.separator + "1_data_iot")
//        dataDir.mkdirs()
//        val output = File(dataDir.absolutePath + "/card_test.pdf")
//        Timber.w("当前开始转换PDF")
//        PdfUtils.replaceText(templateData, output)
//    }
}

@Preview
@Composable
fun SampleSerialPreview() {
    val navController = rememberNavController()
    val viewModel: SampleSerialViewModel = viewModel()
    LaunchedEffect(key1 = Unit) {
        viewModel.text.value =
            "afwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfawafwefawfaw"
    }
    AppPreviewWrapper {

        SampleSerial(navController, viewModel)
    }
}