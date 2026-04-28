package poct.device.app.ui.work

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.szyh.common4.android.EventUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.greenrobot.eventbus.Subscribe
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.bean.ConfigReportBean
import poct.device.app.bean.ConfigSysBean
import poct.device.app.bean.PdfBean
import poct.device.app.bean.WorkFlowActionV2
import poct.device.app.bean.WorkFlowV2
import poct.device.app.bean.card.CardConfig
import poct.device.app.bean.card.CardStatus
import poct.device.app.bean.converter.CaseConverter
import poct.device.app.entity.Case
import poct.device.app.entity.CasePoint
import poct.device.app.entity.CaseResult
import poct.device.app.entity.User
import poct.device.app.entity.service.CaseService
import poct.device.app.entity.service.SysConfigService
import poct.device.app.event.AppPdfPrintEvent
import poct.device.app.serial.v2.CtlSerialMessageV2
import poct.device.app.serial.v2.ctl.CtlCommandsV2
import poct.device.app.serial.v2.ctl.CtlConstantsV2
import poct.device.app.serial.v2.ctl.CtlSerialMessageEventV2
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.thirdparty.FrosApi
import poct.device.app.thirdparty.SbEdgeFunc
import poct.device.app.thirdparty.model.sbedge.resp.BaaResultResp
import poct.device.app.utils.app.AppCardUtils
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppExperimentUtils
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppFormValidateUtils
import poct.device.app.utils.app.AppLocalDateUtils
import poct.device.app.utils.app.AppPdfUtils
import poct.device.app.utils.app.AppToastUtil
import poct.device.app.utils.app.AppTypeUtils
import poct.device.app.utils.common.HttpUtils
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.Base64
import java.util.TreeSet

class WorkMainViewModel : ViewModel() {
    val scanDuration = 16000

    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)
    val qrCodeContent = MutableStateFlow("")

    // 步骤条
//    val step = MutableStateFlow(STEP_START)
    // TODO 简化信息
    val step = MutableStateFlow(STEP_CASE)

    // 操作界面
//    val action = MutableStateFlow(ACTION_START)
    // TODO 简化信息
    val action = MutableStateFlow(ACTION_CASE_SAMPLE)

    // 数据
    val bean = MutableStateFlow(CaseBean())

    val showCutOff2Time = MutableStateFlow(true)
    fun onCutOff2TimeFinished() {
        showCutOff2Time.value = false
    }

    // 检测中
    val progress = MutableStateFlow(0F)

    // 检测时间
    val progressTime = MutableStateFlow(0F)

    val showFy0Time = MutableStateFlow(false)

    val isFy0Finished = MutableStateFlow(false)

    val showTotalTime = MutableStateFlow(false)

    var showTime = MutableStateFlow(false)

    // 等待反应中
    var waitTotal = MutableStateFlow(0F)
    var waitProgress = MutableStateFlow(0F)

    // 检测时的具体动作，详细见检测动作字典AppDictUtils.checkStepMap
    var checkStep = MutableStateFlow(10)

    // 试剂卡配置
    var curCardConfig = MutableStateFlow(CardConfig.Empty)

    // 系统配置
    var sysConfig = MutableStateFlow(ConfigSysBean.Empty)

    var continueSacn = MutableStateFlow(true)

    // 检测进度控制
    private var workFlow = WorkFlowV2.EMPTY

    // 试剂卡配置
    private var cardConfig: CardConfig? = null

    private var jobCmdSwitch: Job? = null

    private var jobFy0: Job? = null

    private var jobXs: Job? = null

    private var jobScan: Job? = null

    private var jobQuery: Job? = null

    fun onLoad() {
        onReset()

        viewModelScope.launch {
            viewState.value = ViewState.LoadingOver()

            withContext(Dispatchers.IO) {
                val moveDurationResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(0, -88888, 900))
                Timber.w("moveDurationResult: $moveDurationResult")

                // 等待成功
                CtlCommandsV2.waitMoveDurationStatusSuccess()
            }

            viewState.value = ViewState.LoadSuccess()
        }

//        // TODO 简化信息
//        viewState.value = ViewState.LoadSuccess()

        // TODO 简化信息
        // 第一步将试剂卡吐出
//        App.getCtlSerialService().send(
//            CtlCommandsV2.homing(),
//            object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                override suspend fun success(
//                    feedback: CtlSerialMessageV2,
//                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                    scope: CoroutineScope,
//                ) {
//                    onLoadHomingSuccess()
//                }
//
//                override suspend fun error(
//                    sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                    e: Exception?, scope: CoroutineScope,
//                ) {
//                    // TODO 硬件故障
//                    actionState.value = ActionState(
//                        event = EVT_DEV_ERROR,
//                        msg = App.getContext().getString(R.string.work_ing_step6_error)
//                    )
//                    onClearInteraction()
//                }
//            })
    }

    // 重置
    fun onReset() {
        bean.value = CaseBean()
        progress.value = 0F
        progressTime.value = 0F
        waitProgress.value = 0F
        waitTotal.value = 0F
        checkStep.value = 0
        workFlow = WorkFlowV2.EMPTY
        continueSacn.value = true
    }

    fun onDataDetail(record: CaseBean, callback: () -> Unit = {}) {
        AppParams.varReport = record
        // 检查试剂卡配置是否存在
        viewModelScope.launch {
            // 查询试剂卡配置
            cardConfig = bean.value.cardInfo.cardConfig
            callback()
        }
    }

    // 更新和保持录入信息
    fun onBeanUpdate(newBean: CaseBean) {
        bean.value = newBean
    }

    // 清除交互弹窗
    fun onClearInteraction() {
        CtlCommandsV2.isWaitScanStatusSuccessCancel = false
        CtlCommandsV2.isWaitAbsorbStatusSuccessCancel = false

        viewModelScope.launch {
            continueSacn.value = false
            actionState.value = ActionState.Default

            withContext(Dispatchers.IO) {
                val cancelResult = {
                    CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
                }
                Timber.w("cancelResult: $cancelResult")
            }
        }
    }

    // 退出确认
    fun onExitConfirm() {
        actionState.value = ActionState(event = EVT_EXIT)
    }

    /**
     * 第一步：开始页(信息录入)
     * 1、读取扫码配置、判断是否手动设置项目；
     * 2、是，进入手动设置项目页
     * 3、否，移出片仓
     */
    fun onActionStartNext() {
        viewState.value = ViewState.LoadingOver()

        if (!validateUserInfo()) {
            viewState.value = ViewState.LoadSuccess()
            return
        }

        Timber.d("onActionStartNext")

        viewModelScope.launch {
            // 更新新的操作状态
            updateAction(ACTION_CASE_SAMPLE)
            onClearInteraction()

            withContext(Dispatchers.IO) {
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                Timber.w("moveToSsResult: $moveToSsResult")

                // 等待成功
                CtlCommandsV2.waitMoveToSsStatusSuccess()
            }

            viewState.value = ViewState.LoadSuccess()
        }
    }

    /**
     * 第二步、手动配置项目下一步，移出片仓， 上一步开始页
     * 回到上一步
     */
    fun onActionCaseInputPre() {
        if (step.value != STEP_START && step.value != STEP_CASE) {
            viewState.value = ViewState.LoadingOver()
            viewModelScope.launch {
                val cancelResult = withContext(Dispatchers.IO) {
                    CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
                }
                Timber.w("cancelResult: $cancelResult")

                withContext(Dispatchers.IO) {
                    val moveToSsResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                    Timber.w("moveToSsResult: $moveToSsResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveToSsStatusSuccess()

                    if (cardConfig == null ||
                        (cardConfig != null && cardConfig!!.ft0 >= 1 && action.value == ACTION_WORK)
                    ) {
                        val moveDurationResult =
                            CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(0, 88888, 900))
                        Timber.w("moveDurationResult: $moveDurationResult")

                        // 等待成功
                        CtlCommandsV2.waitMoveDurationStatusSuccess()
                    }

                    App.getSerialHelper().reconnect()

                    onReset()
                    viewState.value = ViewState.LoadSuccess()

                    updateAction(ACTION_CASE_SAMPLE)
                }
            }
        }

        onClearInteraction()
    }

    /**
     * 第二步、手动配置项目下一步，移出片仓， 上一步开始页
     * 进入下一步：移出片仓
     */
    fun onActionCaseInputNext() {
        // 加载状态：正在移出片仓
        actionState.value =
            ActionState(EVT_LOADING, App.getContext().getString(R.string.work_ing_step6))
        viewModelScope.launch {
            // 检查配置
            cardConfig = bean.value.cardInfo.cardConfig
            curCardConfig.value = cardConfig!!
            Timber.w("=====cardConfig $cardConfig")

            withContext(Dispatchers.IO) {
                // 移出片仓
                CtlCommandsV2.readAllData(CtlCommandsV2.homing())
                onActionCaseInputNextHomingSuccess()
            }
        }
    }

    /**
     * 第三步：放入芯片，
     * 判断是否手动配置扫码
     * 是，上一步：项目配置页
     * 否，上一步：首页
     */
    fun onActionCaseChipPre() {
        // 判断是否手动
        if (sysConfig.value.scan.isEmpty() || sysConfig.value.scan == "y") {
            onReset()
            // TODO 简化信息
//                updateAction(ACTION_START)
        } else {
            // 更新新的操作状态
            updateAction(ACTION_CASE_INPUT)
        }
        onClearInteraction()
    }

    /**
     * TODO 简化信息 第一步：New
     * 第三步：放入芯片，
     * 判断是否手动配置扫码
     * 是，扫码
     * 否，直接读取配置
     */
    fun onActionCaseChipNext() {
        Timber.d("onActionCaseChipNext")
        actionState.value =
            ActionState(EVT_LOADING, App.getContext().getString(R.string.work_read_chip))

        loadCard()
    }

    private fun workFlowActionSize(): Int {
        return workFlow.actions.filter { it.step == STEP_WORK }.size
    }

    private fun workFlowActionIndex(workFlowAction: WorkFlowActionV2): Int {
        val indexOf = workFlow.actions.filter { it.step == STEP_WORK }.indexOf(workFlowAction)
        return if (indexOf < 0) 0 else indexOf
    }

    private fun workFlowAction(index: Int): WorkFlowActionV2 {
        return workFlow.actions.filter { it.step == STEP_WORK }[index]
    }

    private fun doNext() {
        workFlow.next { it ->
            // 更新新的操作状态
            updateAction(it.action)

            // UI界面操作
            if (it.type == WorkFlowActionV2.TYPE_UI) {
                if (it.time >= 0 && it.step == STEP_CASE) {
                    jobFy0 = viewModelScope.launch {
                        if (it.action == ACTION_CASE_WAIT) {
                            checkStep.value = 5
                            progress.value = 5F
                        }
                    }
                }
                if (it.time >= 0 && it.step == STEP_WORK) {
                    showTime.value = true
                    viewModelScope.launch {
                        if (it.action == ACTION_WORK_WAIT) {
                            withContext(Dispatchers.IO) {
                                val moveToSsResult =
                                    CtlCommandsV2.readAllData(
                                        CtlCommandsV2.moveToSs(
                                            0,
                                            88888,
                                            10000,
                                            0
                                        )
                                    )
                                Timber.d("moveToSsResult: $moveToSsResult")

                                // 等待成功
                                CtlCommandsV2.waitMoveToSsStatusSuccess()
                            }

                            // 初始反应中
                            showFy0Time.value = true
                            checkStep.value = 10
                            progress.value = 10F

                            // 进度从10到94的累计逻辑
                            val totalMillis = cardConfig!!.ft0 * 1000L
                            val startProgress = 10F
                            val endProgress = 94F
                            val progressRange = endProgress - startProgress

                            val updateInterval = 100L // 每100ms更新一次进度
                            val steps = totalMillis / updateInterval

                            if (steps > 0) {
                                val increment = progressRange / steps

                                for (i in 1..steps) {
                                    delay(updateInterval)
                                    progress.value = startProgress + (increment * i)
                                }
                            }

                            // 确保最终进度精确到94
                            progress.value = endProgress

                            showFy0Time.value = false
                            isFy0Finished.value = true

                            checkStep.value = 95
                            progress.value = 95F

                            doNext()
                        } else if (it.action == ACTION_WORK_PROCESS) {
                            checkStep.value = 5
                            progress.value = 5F

                            withContext(Dispatchers.IO) {
                                val moveToSsResult =
                                    CtlCommandsV2.readAllData(
                                        CtlCommandsV2.moveToSs(
                                            0,
                                            88888,
                                            10000,
                                            0
                                        )
                                    )
                                Timber.d("moveToSsResult: $moveToSsResult")

                                // 等待成功
                                CtlCommandsV2.waitMoveToSsStatusSuccess()
                            }

                            doNext()
                        }
                    }
                }
                return@next
            }

            // 对应指令操作
            jobCmdSwitch = viewModelScope.launch(Dispatchers.IO) {
                when (it.cmd.cmd) {
//                    CtlConstantsV2.CMD_ACTION_MOVE_TO_SS -> {
//                        doMoveIn(it)
//                    }
//
                    CtlConstantsV2.CMD_ACTION_HOMING -> {
                        doMoveOut(it)
                    }

                    CtlConstantsV2.CMD_ACTION_ABSORB -> {
                        doOpenXs(it)
                    }

                    CtlConstantsV2.CMD_ACTION_SCAN -> {
//                        Thread {
//                            CoroutineScope(Dispatchers.IO).launch {
//                                for (i in 0..0) {
//                                    delay(1 * 1000L)
//                                    newProgressTime()
//                                }
//                            }
//                        }.start()
//                        if (cardConfig!!.xt1 > 0) {
//                            doOpenXs(it)
//                        } else {
//                            newProgress(it)
//                            doNext()
//                        }
                        doScanTest(it)
                    }

                    CtlConstantsV2.CMD_ACTION_QUERY_DATA -> {
                        doReadData(it)
                    }
                }
            }
        }
    }

    private fun doReadData(workFlowAction: WorkFlowActionV2) {
        jobQuery = viewModelScope.launch {
            Timber.d("doReadData")

//        checkStep.value = 70

            var queryResult: ByteArray?
            var retryTotal = 0
            while (true) {
                queryResult = withContext(Dispatchers.IO) {
                    CtlCommandsV2.readAllDataByteArray(workFlowAction.cmd)
                }
                if (queryResult != null) {
                    Timber.w("queryResult: ${queryResult.toString(Charsets.UTF_8)}")
                    break
                } else {
                    if (retryTotal >= 3) {
                        actionState.value = ActionState(
                            EVT_DEV_ERROR,
                            App.getContext().getString(R.string.work_read_chip_error2)
                        )
                        onClearInteraction()
                        return@launch
                    }

                    delay(500)
                    retryTotal++
                    continue
                }
            }

            // 写入数据文件，作为检测参考
            val file = File(App.getContext().externalCacheDir, "data.bin")
            if (!file.parentFile?.exists()!!) {
                file.parentFile?.mkdirs()
            }
            if (file.exists()) {
                file.delete()
            }
            FileOutputStream(file).use { outputStream ->
                outputStream.write(queryResult!!)
            }

            checkStep.value = 85
            progress.value = 85F

            val scanData = AppCardUtils.parseData(queryResult!!)
            if (scanData == null) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.work_read_chip_error2)
                )
                onClearInteraction()
                return@launch
            }

            checkStep.value = 90
            progress.value = 90F

            // TODO 打印解析信息
//        try {
//            // 访问解析后的 ScanData
//            Timber.w("解析后的 ScanData:")
//            Timber.w("Data Length: ${scanData.dataLen}")
//            Timber.w("Laser Current: ${scanData.laserCurr}")
//            Timber.w("Fixed 5A: 0x${scanData.fixed5a?.toString(16)}")
//            Timber.w("Data Bias: ${scanData.dataBias}")
//            Timber.w("Laser Current Bias: ${scanData.laserCurrBias}")
//            Timber.w("Fixed A5: 0x${scanData.fixedA5?.toString(16)}")
//            Timber.w("Raw Data Points: ${scanData.rawData}")
//            Timber.w("Raw Data Points Size: ${scanData.rawData?.size}")
//            Timber.w("Data Biased: ${scanData.dataBiased}")
//            Timber.w("Data Biased Size: ${scanData.dataBiased?.size}")
//            Timber.w("Laser Current Biased: ${scanData.laserCurrBiased}")
//
//            val stats = scanData.getStatistics()
//            Timber.w("数据统计信息:")
//            stats.forEach { (key, value) ->
//                Timber.w("  $key: $value")
//            }
//        } catch (e: IllegalArgumentException) {
//            Timber.w("数据处理错误: ${e.message}")
//        } catch (e: Exception) {
//            Timber.w("初始化分析器时发生错误: ${e.message}")
//        }

            val pointList = convertToPointListV2(scanData.rawData)
            onBeanUpdate(
                bean.value.copy(
                    workResult = genResult(pointList),
                    workPoints = Json.encodeToString(pointList),
                    workTime = AppLocalDateUtils.formatDateTime(LocalDateTime.now())
                )
            )
            Timber.w("doReadData actionState.value.event ${actionState.value.event}")

            if (actionState.value.event == EVT_DEV_ERROR) {
                return@launch
            }

            withContext(Dispatchers.IO) {
                // 自动上传数据到服务器
                FrosApi.uploadPatientReportDataToServer(bean.value)

                if (bean.value.type == CaseBean.TYPE_3LJ_BIOAGE_L1 || bean.value.type == CaseBean.TYPE_BIOAGE_CRP) {
                    while (true) {
                        val baaResultResp: BaaResultResp? =
                            SbEdgeFunc.baaResult(bean.value.qrCode, bean.value.patientId)
                        if (baaResultResp == null) {
                            delay(500)
                            continue
                        } else {
                            val resultList = bean.value.resultList
                            val baaResult = baaResultResp.detail!!

                            resultList[0].result = baaResult.bioAgeProfile.bioAge.toString()
                            resultList[0].refer = baaResult.bioAgeProfile.chronoAge.toString()

                            var assessResultStr = "green|正常"
                            if (baaResult.bioAgeProfile.ageDifference > 1) {
                                assessResultStr = "red|衰老加速"
                            } else if (baaResult.bioAgeProfile.ageDifference < -1) {
                                assessResultStr = "green|衰老减速"
                            }
                            resultList[0].radioValue = assessResultStr

                            onBeanUpdate(
                                bean.value.copy(
                                    workResult = Json.encodeToString(resultList),
                                    baaResult = baaResult,
                                    baaAssets = baaResultResp.assets!!
                                )
                            )
                            break
                        }
                    }
                }

                checkStep.value = 95
                progress.value = 95F

                saveCase(bean.value)

                // 更新试剂卡状态
                SbEdgeFunc.updateCardStatus(bean.value.qrCode, CardStatus.SUCCESS.statusVal)
            }

            checkStep.value = 100
            progress.value = 100F

            if (bean.value.state == 0) {
                onWorkDone()
            }

            delay(CtlCommandsV2.delayMs)

            // 移除仓门
            doMoveOut()

            Timber.d("doReadData done")
        }
    }

    fun uploadReport(data: CaseBean) {
        viewModelScope.launch(Dispatchers.IO) {
            // 自动上传数据到服务器
            FrosApi.uploadPatientReportDataToServer(data)

            withContext(Dispatchers.Main) {
                AppToastUtil.shortShow(
                    App.getContext()
                        .getString(R.string.report_upload_success)
                )
            }
        }
    }

    private fun appendData(
        feedback: CtlSerialMessageV2?,
        dataMap: LinkedHashMap<String, String>,
    ) {
        // TODO 串口失联时，补救逻辑
//        if (AppParams.devMock) {
//            AppSampleUtils.fillDataMap(dataMap)
//            return
//        }

//        val paramData = feedback!!.paramData
//        val amount = paramData.getParameter(CtlConstantsV2.PARAM_AMOUNT) ?: "1"
//        val end = paramData.getParameter(CtlConstantsV2.PARAM_TAIL_INDEX) ?: "1"
//        val data = paramData.getParameter(CtlConstantsV2.PARAM_DATA)
//        if (!data.isNullOrBlank()) {
//            dataMap.putIfAbsent(end, data)
//        }
    }

    private fun doScanTest(workFlowAction: WorkFlowActionV2) {
        jobScan = viewModelScope.launch {
            Timber.d("doScanTest")

            // 激光功率
            withContext(Dispatchers.IO) {
                val getLDPwr =
                    CtlCommandsV2.readAllData(CtlCommandsV2.getLDPwr())
                Timber.w("getLDPwr: $getLDPwr")

                val setLDPwr =
                    CtlCommandsV2.readAllData(CtlCommandsV2.setLDPwr(cardConfig!!.cutOff1.toInt()))
                Timber.w("setLDPwr: $setLDPwr")
            }

            checkStep.value = 50
            progress.value = 50F

            val scanOk = withContext(Dispatchers.IO) {
                // 并发执行进度更新和扫描任务
                val progressJob = async {
                    // 进度从50到64的累计逻辑
                    val startProgress = 50F
                    val endProgress = 64F
                    val progressRange = endProgress - startProgress

                    val updateInterval = 100L // 每100ms更新一次
                    val steps = scanDuration / updateInterval

                    if (steps > 0) {
                        val increment = progressRange / steps
                        for (i in 1..steps) {
                            delay(updateInterval)
                            val currentProgress = startProgress + (increment * i)
                            progress.value = currentProgress.coerceAtMost(endProgress)
                        }
                    }

                    // 确保最终进度精确到64
                    progress.value = endProgress
                }

                // 整片检测
                val scanResult = CtlCommandsV2.readAllData(workFlowAction.cmd)
                Timber.w("scanResult: $scanResult")

                val scanSuccess = CtlCommandsV2.waitScanStatusSuccess()

                // 等待进度更新完成（如果扫描先完成）
                progressJob.await()

                scanSuccess
            }

            if (!scanOk) {
                return@launch
            }

            checkStep.value = 65
            progress.value = 65F

            doNext()

            // TODO: 当前不使用检测的配置
            // 查询是否扫码的配置
//        sysConfig = SysConfigService.findBean(ConfigSysBean.PREFIX, ConfigSysBean::class)
//        if(sysConfig!!.checkMethod == "whole") {
//
//        }else{
//            // 切片检测
//            val configBean = CardConfigConverter.fromEntity(cardConfig!!)
//            val topList = configBean.topList;
//            var flag = true
//            var count = 0;
//            while (count < topList.size) {
//                if(flag) {
//                    App.getCtlSerialService().send(
//                        CtlCommandsV2.scanTest(
//                            start = topList[count].start.toDouble().toInt(),
//                            end = topList[count].end.toDouble().toInt(),
//                            ppmm = cardConfig!!.scanPPMM,
//                        ),
//                        object : SerialMessageCallbackAdapterV2<CtlSerialMessageV2>() {
//                            override suspend fun delay(
//                                feedback: CtlSerialMessageV2,
//                                sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                scope: CoroutineScope,
//                            ) {
//
//                            }
//
//                            override suspend fun error(
//                                sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                e: Exception?,
//                                scope: CoroutineScope,
//                            ) {
//                                // TODO 硬件故障
//                                actionState.value = ActionState(event = EVT_DEV_ERROR, msg = App.getContext().getString(R.string.work_ing_step5_error))
//                                onClearInteraction()
//                            }
//
//                            override suspend fun success(
//                                feedback: CtlSerialMessageV2,
//                                sender: SocketMessageSenderV2<CtlSerialMessageV2>?,
//                                scope: CoroutineScope,
//                            ) {
//                                count ++;
//                                Timber.d("doScanTest ${count}")
//                                flag = true;
//                            }
//                        })
//                }
//                flag = false
//            }
//            Timber.d("doScanTest done")
//            newProgress(workFlowAction)
//            doNext()
//        }
        }
    }

    private fun doOpenXs(workFlowAction: WorkFlowActionV2) {
        jobXs = viewModelScope.launch {
            Timber.d("doOpenXs")

            checkStep.value = 5
            progress.value = 5F

            withContext(Dispatchers.IO) {
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, 88888, 10000, 0))
                Timber.d("moveToSsResult: $moveToSsResult")

                // 等待成功
                CtlCommandsV2.waitMoveToSsStatusSuccess()
            }

            // 反应中
            showTotalTime.value = true
            checkStep.value = 10
            progress.value = 10F

            // 进度从10到19的累计逻辑
            val totalMillis = cardConfig!!.ft1 * 1000L
            val startProgress = 10F
            val endProgress = 19F
            val progressRange = endProgress - startProgress

            val updateInterval = 100L // 每100ms更新一次进度
            val steps = totalMillis / updateInterval

            if (steps > 0) {
                val increment = progressRange / steps

                for (i in 1..steps) {
                    delay(updateInterval)
                    progress.value = startProgress + (increment * i)
                }
            }

            // 确保最终进度精确到19
            progress.value = endProgress

            if (cardConfig!!.xt1 > 0) {
                // 吸水中
                checkStep.value = 20
                progress.value = 20F

                withContext(Dispatchers.IO) {
                    // 并发执行进度更新和吸水任务
                    val progressJob = async {
                        // 进度从20到49的累计逻辑
                        val totalMillis = cardConfig!!.xt1 * 1000L
                        val startProgress = 20F
                        val endProgress = 49F
                        val progressRange = endProgress - startProgress

                        val updateInterval = 100L // 每100ms更新一次
                        val steps = totalMillis / updateInterval

                        if (steps > 0) {
                            val increment = progressRange / steps
                            for (i in 1..steps) {
                                delay(updateInterval)
                                val currentProgress = startProgress + (increment * i)
                                progress.value = currentProgress.coerceAtMost(endProgress)
                            }
                        }
                        // 确保最终进度精确到49
                        progress.value = endProgress
                    }


                    val absorbResult = CtlCommandsV2.readAllData(workFlowAction.cmd)
                    Timber.w("absorbResult: $absorbResult")

                    delay(cardConfig!!.xt1 * 1000L)
                    CtlCommandsV2.waitAbsorbStatusSuccess()

                    // 等待进度更新完成
                    progressJob.await()
                }
            } else {
                progress.value = 49F
            }

            showTotalTime.value = false
            Timber.d("doOpenXs done")

//        newProgress(workFlowAction)

            // 打开吸水阀成功，进入下一步
            doNext()
        }
    }

    private fun doMoveIn(workFlowAction: WorkFlowActionV2) {
        viewModelScope.launch {
            Timber.d("doMoveIn")
            if (workFlowAction.step == STEP_WORK) {
                checkStep.value = 5
                progress.value = 5F
            }

            withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(workFlowAction.cmd) // CMD_ACTION_SCAN
                // TODO 轮询
            }

            Timber.d("doMoveIn done")
            if (workFlowAction.step == STEP_WORK) {
                newProgress(workFlowAction)
                // 移入仓门成功，开始下一步
                doNext()
            }
        }
    }

    private fun doMoveOut(workFlowAction: WorkFlowActionV2) {
        viewModelScope.launch {
            doMoveOut()
            doNext()
        }
    }

    private fun newProgress(workFlowAction: WorkFlowActionV2) {
        // 分母（总步骤）+1，是因为最后多1个固定的移出仓门步骤
        if (workFlowActionSize() != 0) {
            progress.value =
                ((100 * (workFlowActionIndex(workFlowAction) + 1) / (workFlowActionSize())).toFloat())
        }
    }

    private fun newProgressTime() {
        progressTime.value += 1
        Timber.w("当前时间进度：${progressTime.value}")
    }

    private fun waitReactProgress(total: Int, curTime: Int) {
        // 分母（总步骤）
        if (curTime >= total) {
            waitProgress.value = 100F
        } else {
            waitProgress.value = ((100 * curTime / total).toFloat())
        }
    }

    // 这里特指检测完成后最后一步
    private fun doMoveOut() {
        viewModelScope.launch {
            Timber.d("doMoveOut")

//        checkStep.value = 90

            withContext(Dispatchers.IO) {
                val moveToSsResult =
                    CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                Timber.w("moveToSsResult: $moveToSsResult")

                // 等待成功
                CtlCommandsV2.waitMoveToSsStatusSuccess()

                if (cardConfig == null ||
                    (cardConfig != null && cardConfig!!.ft0 >= 1 && action.value == ACTION_WORK)
                ) {
                    val moveDurationResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(0, 88888, 900))
                    Timber.w("moveDurationResult: $moveDurationResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveDurationStatusSuccess()
                }

                App.getSerialHelper().reconnect()
            }

            checkStep.value = 100
            progress.value = 100F

//        val moveDurationResult =
//            CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(0, -88888, 5000))
//        Timber.w("moveDurationResult: $moveDurationResult")

//        // 等待成功
//        CtlCommandsV2.waitMoveDurationStatusSuccess()
        }
    }

    private fun genWorkFlow(): WorkFlowV2 {
        val type = bean.value.type
        return when (type) {
            CaseBean.TYPE_IGE, CaseBean.TYPE_4LJ -> workFlowForIge()
            else -> workFlowForCrp()
        }
    }

    private fun workFlowForCrp(): WorkFlowV2 {
        val list = ArrayList<WorkFlowActionV2>()
        // 等待反应界面
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                step = STEP_CASE,
                time = cardConfig!!.ft0,
                action = ACTION_CASE_WAIT
            )
        )
        // 移出仓门
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.homing(),
                time = -2,
                step = STEP_CASE,
                action = ACTION_CASE_WAIT
            )
        )
        // 打开放入样本界面
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                step = STEP_CASE,
                time = -1,
                action = ACTION_CASE_SAMPLE
            )
        )
        // 移入仓门
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.moveToSs(0, -88888, 10000, 1),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        // 等待反应
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = cardConfig!!.ft1,
                step = STEP_WORK,
                action = ACTION_WORK_WAIT
            )
        )
//        // 打开吸水阀
//        list.add(
//            WorkFlowActionV2(
//                type = WorkFlowActionV2.TYPE_SERIAL,
//                cmd = CtlCommandsV2.openXs(xsTime = cardConfig!!.xt1, pos = 40),
//                time = -2,
//                step = STEP_WORK,
//                action = ACTION_WORK
//            )
//        )
        // 开始扫描
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.scan(-16000, scanDuration),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        // 读取数据
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.queryData(),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        return WorkFlowV2(list)
    }

    private fun workFlowForIge(): WorkFlowV2 {
        val list = ArrayList<WorkFlowActionV2>()
        // 样本滴入界面
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                step = STEP_CASE,
                time = -1,
                action = ACTION_CASE_SAMPLE
            )
        )
        // 移入仓门
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.moveToSs(0, -88888, 10000, 1),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        // 等待反应
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = cardConfig!!.ft1,
                step = STEP_WORK,
                action = ACTION_WORK_WAIT
            )
        )
//        // 打开吸水阀
//        list.add(
//            WorkFlowActionV2(
//                type = WorkFlowActionV2.TYPE_SERIAL,
//                cmd = CtlCommandsV2.openXs(xsTime = cardConfig!!.xt1, pos = 40),
//                time = -2,
//                step = STEP_WORK,
//                action = ACTION_WORK
//            )
//        )
        // 扫描检测
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.scan(-16000, scanDuration),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        // 读取数据
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.queryData(),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        return WorkFlowV2(list)
    }

    private fun genWorkFlowV2(): WorkFlowV2? {
        return if (cardConfig!!.ft0 >= 1) {
            genDichotomyWorkFlowV2()
        } else {
            genDefaultWorkFlowV2()
        }
    }

    private fun genDefaultWorkFlowV2(): WorkFlowV2? {
        val list = ArrayList<WorkFlowActionV2>()
        // 1
        // 样品准备就位
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = 1,
                step = STEP_CASE,
                action = ACTION_CASE_WAIT
            )
        )

        // 2
        // 确认开始检测
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = 1,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        // 开始检测
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = 1,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        // 打开吸水阀
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.absorb(cardConfig!!.xt1 * 1000),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        // 扫描检测
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.scan(-16000, scanDuration),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        // 读取数据
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.queryData(),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        return WorkFlowV2(list)
    }

    private fun genDichotomyWorkFlowV2(): WorkFlowV2? {
        val list = ArrayList<WorkFlowActionV2>()
        // 1
        // 加样后插入芯片
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                step = STEP_CASE,
                time = cardConfig!!.ft0,
                action = ACTION_CASE_WAIT
            )
        )

        // 2
        // 等待反应
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = 1,
                step = STEP_WORK,
                action = ACTION_WORK_WAIT
            )
        )
        // 移出仓门
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.homing(),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK_WAIT
            )
        )

        // 3
        // 样品准备就位
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                step = STEP_CASE,
                time = cardConfig!!.ft0,
                action = ACTION_CASE_WAIT
            )
        )

        // 4
        // 确认开始检测
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = 1,
                step = STEP_WORK,
                action = ACTION_WORK
            )
        )
        // 开始检测
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_UI,
                time = 1,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        // 打开吸水阀
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.absorb(cardConfig!!.xt1 * 1000),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        // 扫描检测
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.scan(-16000, scanDuration),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        // 读取数据
        list.add(
            WorkFlowActionV2(
                type = WorkFlowActionV2.TYPE_SERIAL,
                cmd = CtlCommandsV2.queryData(),
                time = -2,
                step = STEP_WORK,
                action = ACTION_WORK_PROCESS
            )
        )
        return WorkFlowV2(list)
    }

    // 样本页放入样本上一步
    fun onActionCaseSamplePre() {
//        updateAction(ACTION_CASE_CHIP)

        // 更新新的操作状态
        updateAction(ACTION_START)
    }

    // 样本页放入样本下一步，开始检测->读取数据
    fun onActionCaseSampleNext() {
        Timber.d("onActionCaseSampleNext")
        actionState.value =
            ActionState(EVT_LOADING, App.getContext().getString(R.string.work_read_chip))

        loadCard()

//            // 放入样本成功，进入下一步
//            doNext()
    }

    // 样本页放入样本上一步
    fun onActionCaseWaitPre() {
        // 更新新的操作状态
        updateAction(ACTION_CASE_SAMPLE)
    }

    // 样本页放入样本下一步，开始检测
    fun onActionCaseWaitNext() {
        Timber.d("onActionCaseWaitNext")
        viewState.value = ViewState.LoadingOver()
        doNext()
        viewState.value = ViewState.LoadSuccess()
    }

    private fun validateUserInfo(): Boolean {
        val name = bean.value.name
        val birthday = bean.value.birthday
        val qrCode = bean.value.qrCode

        if (AppParams.curUser.role != User.ROLE_DEV) {
            if (!AppFormValidateUtils.validateRequired(name)) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.msg_user_name_required)
                )
                return false
            }
            if (!AppFormValidateUtils.validateRequired(birthday)) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.msg_user_birthday_required)
                )
                return false
            }
        } else {
            if (sysConfig.value.scan == "n" && !AppFormValidateUtils.validateRequired(qrCode)) {
                actionState.value = ActionState(
                    EVT_DEV_ERROR,
                    App.getContext().getString(R.string.msg_user_qrcode_required)
                )
                return false
            }
        }
        return true
    }

    /**
     * 加载卡片信息
     */
    private fun loadCard() {
        viewModelScope.launch {
            if (sysConfig.value.scan == "y" || sysConfig.value.scan.isEmpty()
                || AppParams.curUser.role != User.ROLE_DEV
            ) {
                // 判断卡片是否插到位
                val gpioReadResult = withContext(Dispatchers.IO) {
                    CtlCommandsV2.readAllData(CtlCommandsV2.gpioRead())
                }
                Timber.d("gpioReadResult: $gpioReadResult")

                val hasCard = withContext(Dispatchers.IO) {
                    CtlCommandsV2.gpioReadHasCard(gpioReadResult)
                }
                if (!hasCard) {
                    actionState.value =
                        ActionState(
                            EVT_DEV_ERROR,
                            App.getContext()
                                .getString(R.string.work_case_put_chip_tip)
                        )
                    return@launch
                } else {
                    // 扫描二维码
                    val readQRResult = withContext(Dispatchers.IO) {
                        CtlCommandsV2.readAllData(CtlCommandsV2.readQR())
                    }
                    Timber.d("readQRResult: $readQRResult")
                }
            }

            continueSacn.value = true
            readQrSuccess()
        }
    }

    private fun readQrSuccess() {
        if (
            sysConfig.value.scan == "y"
            || sysConfig.value.scan.isEmpty()
            || AppParams.curUser.role != User.ROLE_DEV
        ) {
            CtlCommandsV2.processReadQRStatus { scanQrSuccessCustomFunction(it) }
        } else {
            scanQrSuccessCustomFunction(bean.value.qrCode)
        }
    }

    private fun scanQrSuccessCustomFunction(qrCodeData: String) {
        viewModelScope.launch {
            Timber.w("======qrCodeData $qrCodeData")
            if (qrCodeData.isEmpty() && continueSacn.value) {
                withContext(Dispatchers.IO) {
                    readQrSuccess()
                }
            } else {
                Timber.d("scanQrSuccessCustomFunction done")
                val cardCode = qrCodeData
                Timber.w("======${cardCode}")

                if (cardCode == CtlConstantsV2.CMD_ACTION_READ_QR_RESULT_NULL) {
                    actionState.value =
                        ActionState(
                            EVT_DEV_ERROR,
                            App.getContext()
                                .getString(R.string.work_read_chip_error1)
                        )
                    return@launch
                }

                if (cardCode.isEmpty()) {
                    actionState.value =
                        ActionState(
                            EVT_DEV_ERROR,
                            App.getContext()
                                .getString(R.string.work_read_chip_error1)
                        )
                    return@launch
                }
                Timber.w("++++++++++${cardCode}")

//                // 先读取本地
//                if (AppParams.curUser.role != User.ROLE_DEV) {
//                    val case: Case? = withContext(Dispatchers.IO) {
//                        CaseService.findByCardCode(cardCode)
//                    }
//                    if (case != null) {
//                        Timber.w("case 已存在 ${case.qrCode}")
//                        if (AppParams.removeReport || AppParams.devMock) {
//                            withContext(Dispatchers.IO) {
//                                // 更新试剂卡状态
//                                SbEdgeFunc.updateCardStatus(cardCode, CardStatus.ACTIVE.statusVal)
//
//                                CaseService.delete(case)
//                            }
//                        }
//
//                        val caseBean: CaseBean = CaseConverter.fromEntity(case)
//
//                        onBeanUpdate(
//                            bean.value.copy(
//                                name = caseBean.name,
//                                gender = caseBean.gender,
//                                birthday = caseBean.birthday,
//                                reagentId = caseBean.reagentId,
//                                type = caseBean.type,
//                                qrCode = cardCode,
//                                caseId = caseBean.caseId,
//                                workResult = caseBean.workResult,
//                                workPoints = caseBean.workPoints,
//                                workTime = caseBean.workTime
//                            )
//                        )
//                        onWorkDone()
//
//                        // 移除仓门
//                        doMoveOut()
//                        return@launch
//                    }
//                }

                // 从芯片二维码查找项目类型
                var type: String? = null
                val cardBatchCode: String
                val cardId: String
                if (AppParams.curUser.role == User.ROLE_DEV) {
                    type = AppExperimentUtils.getType(cardCode)
                }
                if (type == null) {
                    type = AppTypeUtils.findTypeV2(cardCode)
                    cardBatchCode = AppTypeUtils.findCardBatchCode(cardCode)
                    cardId = AppTypeUtils.findCardId(cardCode)
                } else {
                    cardBatchCode = cardCode
                    cardId = cardCode
                }
                if (type == null) {
                    actionState.value =
                        ActionState(
                            EVT_DEV_ERROR,
                            App.getContext()
                                .getString(R.string.work_read_chip_error2)
                        )
                    return@launch
                }

                val httpUtil = HttpUtils()
                if (
                    !withContext(Dispatchers.IO) {
                        httpUtil.checkConnectivity(SbEdgeFunc.getDomain())
                    }
                ) {
                    actionState.value = ActionState(
                        event = EVT_DEV_ERROR_NETWORK,
                        msg = App.getContext().getString(R.string.wlan_not_connect)
                    )
                    return@launch
                }

                val cardInfo = withContext(Dispatchers.IO) {
                    SbEdgeFunc.getCardInfo(cardBatchCode, cardCode)
                }

                if (cardCode != cardInfo.card.code) {
                    actionState.value = ActionState(
                        event = EVT_DEV_ERROR,
                        msg = App.getContext().getString(R.string.report_card_undefined)
                    )
                    return@launch
                }

                if (cardInfo.card.status == CardStatus.INACTIVE.statusVal) {
                    actionState.value = ActionState(
                        event = EVT_DEV_ERROR,
                        msg = App.getContext().getString(R.string.report_card_undefined)
                    )
                    return@launch
                }

                // 更新芯片查询到的数据
                onBeanUpdate(
                    bean.value.copy(
                        reagentId = cardBatchCode,
                        type = type,
                        qrCode = cardCode,
                        caseId = cardId,
                        cardInfo = cardInfo
                    )
                )

                cardConfig = bean.value.cardInfo.cardConfig
                curCardConfig.value = cardConfig!!

                // 读取服务端报告信息
                val frosData = withContext(Dispatchers.IO) {
                    FrosApi.getPatientCaseReport(cardCode)
                }

                showCutOff2Time.value = curCardConfig.value.cutOff2 > 0

                if (frosData.qrCode.isEmpty()) {
                    actionState.value = ActionState(
                        event = EVT_DEV_ERROR,
                        msg = App.getContext().getString(R.string.report_card_not_bind)
                    )
                    return@launch
                }

                onBeanUpdate(
                    bean.value.copy(
                        patientId = frosData.patientId,
                        name = frosData.name,
                        gender = frosData.gender,
                        birthday = frosData.birthday,
                    )
                )

                if (cardInfo.card.status == CardStatus.SUCCESS.statusVal) {
                    val baaResultResp: BaaResultResp? =
                        SbEdgeFunc.baaResult(bean.value.qrCode, bean.value.patientId)

                    val resultList = frosData.resultList
                    if (baaResultResp != null) {
                        val baaResult = baaResultResp.detail!!

                        resultList[0].result = baaResult.bioAgeProfile.bioAge.toString()
                        resultList[0].refer = baaResult.bioAgeProfile.chronoAge.toString()

                        var assessResultStr = "green|正常"
                        if (baaResult.bioAgeProfile.ageDifference > 1) {
                            assessResultStr = "red|衰老加速"
                        } else if (baaResult.bioAgeProfile.ageDifference < -1) {
                            assessResultStr = "green|衰老减速"
                        }
                        resultList[0].radioValue = assessResultStr

                        onBeanUpdate(
                            bean.value.copy(
                                type = frosData.type,
                                qrCode = cardCode,
                                workPoints = frosData.workPoints,
                                workTime = frosData.workTime,
                                workResult = Json.encodeToString(resultList),
                                baaResult = baaResult,
                                baaAssets = baaResultResp.assets!!,
                            )
                        )
                    } else {
                        onBeanUpdate(
                            bean.value.copy(
                                type = frosData.type,
                                qrCode = cardCode,
                                workPoints = frosData.workPoints,
                                workTime = frosData.workTime,
                                workResult = Json.encodeToString(resultList),
                            )
                        )
                    }

                    withContext(Dispatchers.IO) {
                        saveCase(bean.value)
                    }
                    onWorkDone()

                    // 移除仓门
                    doMoveOut()
                    return@launch
                }

                withContext(Dispatchers.IO) {
                    // 更新试剂卡状态
                    SbEdgeFunc.updateCardStatus(cardCode, CardStatus.CHECKING.statusVal)
                }

                // 生成步骤操作
                val workFlowTmp = genWorkFlowV2()
                if (workFlowTmp != null) {
                    workFlow = workFlowTmp
                    doNext()
                    onClearInteraction()
                }
            }
        }
    }

    private fun convertToPointList(dataMap: LinkedHashMap<String, String>): ArrayList<CasePoint> {
        val list = ArrayList<Int>()
        val keyIntList = TreeSet(dataMap.keys.map { it.toInt() })
        for (keyInt in keyIntList) {
            val data = dataMap[keyInt.toString()]!!

            // TODO 串口失联时，补救逻辑
//            if (AppParams.devMock) {
//                val split = data.split("|")
//                list.addAll(split.map { it.toInt() })
//            } else {
            // 使用base64解析数据
            val dataBytes: ByteArray = Base64.getDecoder().decode(data)
            list.addAll(dataBytes.toListOfInts())
//            }
        }
        Timber.w("======上传的数据${App.gson.toJson(list)}")
        Timber.w("======上传数据数量${list.size}")
        val casePoints = ArrayList<CasePoint>()
        for ((index, value) in list.withIndex()) {
            if (index + 4 < list.size) {
                var newValue = 0
                for (i in 0..4) {
                    newValue += list[index + i]
                }
                newValue /= 5
                casePoints.add(CasePoint((index + 1).toDouble(), newValue.toDouble()))
            } else {
                casePoints.add(CasePoint((index + 1).toDouble(), value.toDouble()))
            }
        }
        Timber.w("======处理数据数量${list.size}")
        Timber.w("======处理数据数量${App.gson.toJson(casePoints)}")
        return casePoints
    }

    private fun convertToPointListV2(rawData: List<Int>): ArrayList<CasePoint> {
        Timber.w("======解析后数据数量${rawData.size}")
        Timber.w("======解析后的数据${App.gson.toJson(rawData)}")

        val casePoints = ArrayList<CasePoint>()
        for ((index, value) in rawData.withIndex()) {
            casePoints.add(CasePoint((index + 1).toDouble(), value.toDouble()))
        }

        Timber.w("======图表处理数据数量${casePoints.size}")
        Timber.w("======图表处理的数据${App.gson.toJson(casePoints)}")
        return casePoints
    }

    fun ByteArray.toListOfInts(): ArrayList<Int> {
        val result = ArrayList<Int>()
        for (i in indices step 2) {
            val low = this[i].toInt() and 0xFF
            val high = this[i + 1].toInt() and 0xFF
            result.add((high shl 8) or (low))
        }
        return result
    }

    private fun genResult(pointList: ArrayList<CasePoint>): String {
        val beanValue = bean.value
        when (beanValue.type) {
            CaseBean.TYPE_4LJ -> {
                return AppCardUtils.genResultFor4LJ(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_IGE -> {
                return AppCardUtils.genResultForIge(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_CRP -> {
                return AppCardUtils.genResultForCrp2(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_SF -> {
                return AppCardUtils.genResultForSfCrp2(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_3LJ -> {
                return AppCardUtils.genResultFor3LJ(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_2LJ_A -> {
                return AppCardUtils.genResultFor2LJA(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_2LJ_B -> {
                return AppCardUtils.genResultFor2LJB(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_2LJ_B_M -> {
                return AppCardUtils.genResultFor2LJBM(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_2LJ_B_F -> {
                return AppCardUtils.genResultFor2LJBF(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_3LJ_BIOAGE_L1 -> {
                return AppCardUtils.genResultFor3LJBAL1(actionState, cardConfig!!, bean, pointList)
            }

            CaseBean.TYPE_BIOAGE_CRP -> {
                return AppCardUtils.genResultForBACRP(actionState, cardConfig!!, bean, pointList)
            }

            else -> {
                return ""
            }
        }
    }

    fun onWorkDone() {
        onClearInteraction()

        viewModelScope.launch {
            val cancelResult = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
            }
            Timber.w("cancelResult: $cancelResult")

//        // 获取当前用户角色
//        if (AppParams.curUser.role != User.ROLE_CHECKER) {
            updateAction(ACTION_REPORT1)
//        } else {
//            onActionReportGen()
//        }
        }
    }

    // 退出检测
    fun onActionWorkOutConfirm() {
        actionState.value = ActionState(EVT_OUT_CONFIRM)
    }

    fun onActionContinueConfirm() {
        viewModelScope.launch {
            if (sysConfig.value.scan == "y" || sysConfig.value.scan.isEmpty()
                || AppParams.curUser.role != User.ROLE_DEV
            ) {
                // 判断卡片是否插到位
                val gpioReadResult = withContext(Dispatchers.IO) {
                    CtlCommandsV2.readAllData(CtlCommandsV2.gpioRead())
                }
                Timber.d("gpioReadResult: $gpioReadResult")

                val hasCard = withContext(Dispatchers.IO) {
                    CtlCommandsV2.gpioReadHasCard(gpioReadResult)
                }
                if (!hasCard) {
                    actionState.value =
                        ActionState(
                            EVT_DEV_ERROR,
                            App.getContext()
                                .getString(R.string.work_case_put_chip_tip)
                        )
                    return@launch
                }
            }

            doNext()
        }
    }

    // 样本页放入样本上一步
    fun onActionWorkPre() {
        // 更新新的操作状态
        updateAction(ACTION_CASE_WAIT)
    }

    // 样本页放入样本下一步，开始检测
    fun onActionWorkNext() {
        Timber.d("onActionWorkWaitPre")
        viewState.value = ViewState.LoadingOver()
        if (showCutOff2Time.value) {
            actionState.value = ActionState(EVT_CUT_OFF2_WAIT)
        } else {
            doNext()
        }
        viewState.value = ViewState.LoadSuccess()
    }

    // 退出检测
    fun onActionWorkOut() {
        workFlow = WorkFlowV2.EMPTY
        CtlCommandsV2.isWaitAbsorbStatusSuccessCancel = true
        CtlCommandsV2.isWaitScanStatusSuccessCancel = true

        actionState.value =
            ActionState(EVT_LOADING, App.getContext().getString(R.string.work_report_exit))

        viewModelScope.launch {
            cancelJobs()

            val cancelResult = withContext(Dispatchers.IO) {
                CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
            }
            Timber.w("cancelResult: $cancelResult")

            actionState.value = ActionState(EVT_CHIP_TO_REMOVE)
        }
    }

    private suspend fun cancelJobs() {
        if (jobCmdSwitch != null) {
            jobCmdSwitch!!.cancelAndJoin()
        }
        if (jobFy0 != null) {
            jobFy0!!.cancelAndJoin()
        }
        if (jobXs != null) {
            jobXs!!.cancelAndJoin()
        }
        if (jobScan != null) {
            jobScan!!.cancelAndJoin()
        }
        if (jobQuery != null) {
            jobQuery!!.cancelAndJoin()
        }

        jobCmdSwitch = null
        jobFy0 = null
        jobXs = null
        jobScan = null
        jobQuery = null
    }

    fun onActionWorkOutDone(callback: () -> Unit) {
        CtlCommandsV2.isWaitScanStatusSuccessCancel = true
        CtlCommandsV2.isWaitAbsorbStatusSuccessCancel = true

        actionState.value =
            ActionState(EVT_LOADING, App.getContext().getString(R.string.work_ing_reset))

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                cancelJobs()

                val cancelResult = CtlCommandsV2.readAllData(CtlCommandsV2.cancel())
                Timber.w("cancelResult: $cancelResult")

                if ((cardConfig != null && cardConfig!!.ft0 >= 1 && action.value == ACTION_WORK_WAIT) ||
                    action.value == ACTION_WORK_PROCESS
                ) {
                    val homingResult = CtlCommandsV2.readAllData(CtlCommandsV2.homing())
                    Timber.w("homingResult: $homingResult")

                    onActionWorkOutDoneHomingSuccess(callback)
                } else {
                    val moveToSsResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                    Timber.w("moveToSsResult: $moveToSsResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveToSsStatusSuccess()

                    val moveDurationResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(0, 88888, 900))
                    Timber.w("moveDurationResult: $moveDurationResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveDurationStatusSuccess()

                    onClearInteraction()
                    callback()
                }
            }
        }
    }

    @Subscribe
    fun handleSerialEvent(event: CtlSerialMessageEventV2) {
        // 根据反馈更新数据
        val message = event.message
        if (message.cmd == CtlConstantsV2.CMD_ACTION_QUERY_DATA) {
//            val testSid = message.paramData.getParameter(CtlConstantsV2.PARAM_TEST_SID)
//            Timber.d(testSid)
            AppParams.testCount += 1
        }
    }

    fun onActionWorkContinue() {
        onClearInteraction()
    }

    fun onActionReportGen() {
        actionState.value =
            ActionState(
                EVT_LOADING,
                App.getContext().getString(R.string.work_report_gen)
            )
        viewModelScope.launch {
            val bean = bean.value
            val entity: Case = saveCase(bean)
            val context = App.getContext()
            val resultList: ArrayList<CaseResult> =
                Json.decodeFromString(bean.workResult) as ArrayList<CaseResult>
            for (caseResult in resultList) {
                if (caseResult.result.isEmpty()) {
                    actionState.value = ActionState(
                        EVT_DEV_ERROR,
                        App.getContext().getString(R.string.work_report_result) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                    return@launch
                }
            }
            val configRepostBean = withContext(Dispatchers.IO) {
                SysConfigService.findBean(ConfigReportBean.PREFIX, ConfigReportBean::class)
            }
            val title = configRepostBean.hosName
            var subTitle = "report"
            if (bean.type == CaseBean.TYPE_CRP) {
                subTitle = configRepostBean.crpName
            } else if (bean.type == CaseBean.TYPE_4LJ) {
                subTitle = configRepostBean.cljName
            } else if (bean.type == CaseBean.TYPE_IGE) {
                subTitle = configRepostBean.igeName
            } else if (bean.type == CaseBean.TYPE_3LJ) {
                subTitle = configRepostBean.sljName
            } else if (bean.type == CaseBean.TYPE_2LJ_A) {
                subTitle = configRepostBean.eljAName
            } else if (bean.type == CaseBean.TYPE_2LJ_B) {
                subTitle = configRepostBean.eljBName
            }

            withContext(Dispatchers.IO) {
                var logo: Bitmap
                val logoFile = File(AppFileUtils.getLogoUrl())
                if (logoFile.exists()) {
                    FileInputStream(AppFileUtils.getLogoUrl()).use {
                        logo = BitmapFactory.decodeStream(it)
                    }
                } else {
                    App.getContext().assets.open("report_template/logo.png").use {
                        logo = BitmapFactory.decodeStream(it)
                    }
                }
                AppPdfUtils.generatePdf(
                    PdfBean(
                        title = title,
                        subTitle = subTitle,
                        logo = logo,
                        outPath = bean.pdfPath,
                        jcbh = entity.caseId,
                        type = bean.type,
                        jcrq = AppLocalDateUtils.formatDate(entity.time.toLocalDate()),
                        xm = entity.name,
                        xb = AppDictUtils.label(AppDictUtils.genderOptions(context), entity.gender),
                        nl = AppLocalDateUtils.calcAge(entity.birthday, entity.time.toLocalDate())
                            .toString(),
                        sjId = entity.reagentId,
                        ybId = entity.caseId,
                        data = (resultList).map { result ->
                            listOf(
                                result.name,
                                result.radioValue,
                                result.result,
                                result.refer,
                                result.flag.toString(),
                                result.t1Value,
                                result.t2Value,
                                result.t3Value,
                                result.t4Value,
                                result.cValue,
                                result.c2Value
                            )
                        }
                    )
                )
            }

            onClearInteraction()
            updateAction(ACTION_REPORT2)
        }
    }

    fun onActionReportGet() {
        actionState.value =
            ActionState(EVT_LOADING, App.getContext().getString(R.string.work_config_view_qr))

        // 获取报告二维码
        viewModelScope.launch {
            val content = withContext(Dispatchers.IO) {
                FrosApi.getReportUrl(bean.value.qrCode)

//                val case = bean.value
//                val result = case.resultList[0]
//                FrosApi.getReportUrlV2(
//                    case.name,
//                    result.refer, result.result,
//                    result.t4Value, result.t3Value
//                )
            }

            Timber.w("onActionReportGet: $content")
            viewState.value = ViewState.LoadSuccess()
            qrCodeContent.value = content
            actionState.value = ActionState(EVT_SHOW_REPORT_QRCODE)
        }
    }

    private suspend fun saveCase(bean: CaseBean): Case {
        var entity: Case? = CaseService.findById(bean.id)
        if (entity != null) {
            entity = CaseConverter.fillEntity(bean, entity)
            CaseService.update(entity)
        } else {
            entity = CaseConverter.toEntity(bean)
            entity.time = LocalDateTime.now()
            CaseService.add(entity)
        }
        return entity
    }

    fun onActionReportPre() {
//        viewModelScope.launch(Dispatchers.IO) {
//            updateAction(workFlowAction(0).action)
//        }
        updateAction(ACTION_REPORT1)
    }

    fun onActionReportContinue() {
        onReset()
        // TODO 简化信息
//        updateAction(ACTION_START)
        updateAction(ACTION_CASE_CHIP)
        onClearInteraction()
    }

    fun onActionReportPrintConfirm() {
        actionState.value = ActionState(EVT_PRINT_CONFIRM)
    }

    fun onActionReportPrint(pdfPath: String) {
//        if (!AppParams.wlanEnabled) {
//            actionState.value = ActionState(
//                ReportPDFViewModel.EVT_REPORT_ERROR,
//                App.getContext().getString(R.string.wlan_not_connect)
//            )
//            return
//        }
        onClearInteraction()
        viewModelScope.launch(Dispatchers.IO) {
            EventUtils.publishEvent(AppPdfPrintEvent(pdfPath))
        }
    }

    // 更新操作
    private fun updateAction(actionValue: String) {
        action.value = actionValue
        if (actionValue.startsWith(STEP_CASE)) {
            step.value = STEP_CASE
        } else if (actionValue.startsWith(STEP_REPORT)) {
            step.value = STEP_REPORT
        } else {
            step.value = actionValue
        }
    }

    private fun onLoadHomingSuccess() {
        CtlCommandsV2.processHomingStatus { onLoadHomingSuccessCustomFunction(it) }
    }

    private fun onLoadHomingSuccessCustomFunction(progressVal: Int) {
        viewModelScope.launch {
            if (progressVal < CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                withContext(Dispatchers.IO) {
                    onLoadHomingSuccess()
                }
            } else {
                Timber.d("onLoadHomingSuccessCustomFunction done")
                viewState.value = ViewState.LoadSuccess()
            }
        }
    }

    private fun onActionStartNextHomingSuccess() {
        CtlCommandsV2.processHomingStatus { onActionStartNextHomingSuccessCustomFunction(it) }
    }

    private fun onActionStartNextHomingSuccessCustomFunction(progressVal: Int) {
        viewModelScope.launch {
            if (progressVal < CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                withContext(Dispatchers.IO) {
                    onActionStartNextHomingSuccess()
                }
            } else {
                Timber.d("onActionStartNextHomingSuccessCustomFunction done")
                updateAction(ACTION_CASE_CHIP)
                onClearInteraction()
            }
        }
    }

    private fun onActionCaseInputNextHomingSuccess() {
        CtlCommandsV2.processHomingStatus { onActionCaseInputNextHomingSuccessCustomFunction(it) }
    }

    private fun onActionCaseInputNextHomingSuccessCustomFunction(progressVal: Int) {
        viewModelScope.launch {
            if (progressVal < CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                withContext(Dispatchers.IO) {
                    onActionCaseInputNextHomingSuccess()
                }
            } else {
                Timber.d("doMoveOut for chip done")
                updateAction(ACTION_CASE_CHIP)
                onClearInteraction()
            }
        }
    }

    private fun onActionWorkOutDoneHomingSuccess(callback: () -> Unit) {
        CtlCommandsV2.processHomingStatus {
            onActionWorkOutDoneHomingSuccessCustomFunction(
                it,
                callback
            )
        }
    }

    private fun onActionWorkOutDoneHomingSuccessCustomFunction(
        progressVal: Int,
        callback: () -> Unit
    ) {
        viewModelScope.launch {
            if (progressVal < CtlConstantsV2.CMD_ACTION_HOMING_STATUS_COMPLETED) {
                withContext(Dispatchers.IO) {
                    onActionWorkOutDoneHomingSuccess(callback)
                }
            } else {
                withContext(Dispatchers.IO) {
                    val moveToSsResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveToSs(0, -88888, 10000, 1))
                    Timber.w("moveToSsResult: $moveToSsResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveToSsStatusSuccess()

                    val moveDurationResult =
                        CtlCommandsV2.readAllData(CtlCommandsV2.moveDuration(0, 88888, 900))
                    Timber.w("moveDurationResult: $moveDurationResult")

                    // 等待成功
                    CtlCommandsV2.waitMoveDurationStatusSuccess()

                    App.getSerialHelper().reconnect()
                }

                onClearInteraction()
                callback()
            }
        }
    }

    companion object {
        const val STEP_START = "info"
        const val STEP_CASE = "case"
        const val STEP_WORK = "work"
        const val STEP_REPORT = "report"

        //        val STEPS = arrayOf(STEP_START, STEP_CASE, STEP_WORK, STEP_REPORT)
        // TODO 简化信息
        val STEPS = arrayOf(STEP_CASE, STEP_WORK, STEP_REPORT)

        const val ACTION_START = STEP_START

        // 待试剂卡插入
        const val ACTION_CASE_CHIP = "${STEP_CASE}_chip"
        const val ACTION_CASE_INPUT = "${STEP_CASE}_input"
        const val ACTION_CASE_WAIT = "${STEP_CASE}_wait"
        const val ACTION_CASE_SAMPLE = "${STEP_CASE}_sample"
        const val ACTION_WORK = STEP_WORK
        const val ACTION_WORK_WAIT = "${STEP_WORK}_wait"
        const val ACTION_WORK_PROCESS = "${STEP_WORK}_process"
        const val ACTION_REPORT1 = "${STEP_REPORT}1"
        const val ACTION_REPORT2 = "${STEP_REPORT}2"

        const val EVT_EXIT = "exitConfirm"
        const val EVT_OUT_CONFIRM = "outConfirm"
        const val EVT_LOADING = "loading"

        const val EVT_DEV_ERROR = "devError"

        const val EVT_DEV_ERROR_NETWORK = "devErrorNetwork"
        const val EVT_DEV_WARNING = "devWarning"
        const val EVT_CHIP_TO_REMOVE = "chipToRemove"

        const val EVT_PRINT_CONFIRM = "printConfirm"

        const val EVT_SHOW_REPORT_QRCODE = "showReportQRCode"

        const val EVT_CUT_OFF2_WAIT = "cutOff2Wait"
    }
}