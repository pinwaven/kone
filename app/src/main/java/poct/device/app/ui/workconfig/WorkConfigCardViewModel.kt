package poct.device.app.ui.workconfig

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import info.szyh.common4.idgen.UUIDUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.lang.StringUtils
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardTopBean
import poct.device.app.bean.CardVarBean
import poct.device.app.bean.FileInfo
import poct.device.app.bean.converter.CardConfigConverter
import poct.device.app.entity.CardConfig
import poct.device.app.entity.service.CardConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppCardConfigUtils
import poct.device.app.utils.app.AppExcelUtils
import poct.device.app.utils.app.AppLocalDateUtils
import poct.device.app.utils.app.AppTypeUtils
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.util.Collections


class WorkConfigCardViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)
    val stepByUDisk = MutableStateFlow(STEP_IMPORT_DEFAULT)

    // 记录列表
    val records = MutableStateFlow<List<CardConfigBean>>(Collections.emptyList())
    val files = MutableStateFlow<List<FileInfo>>(Collections.emptyList())
    val selectFiles = MutableStateFlow<List<FileInfo>>(Collections.emptyList())

    // 扫码录入信息
    val scannerInfo = MutableStateFlow<String>("")

    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            records.value = CardConfigService.findAll().map {
                Timber.w("===============${it}")
                Timber.w("当前试剂卡配置：${it}")
                CardConfigConverter.fromEntity(it)
            }
            records.value.forEach {
                Timber.w("当前试剂卡配置：${it}")
            }

            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    fun onImportByScannerConfirm(callback: () -> Unit = {}) {
        actionState.value =
            ActionState(EVENT_LOADING, App.getContext().getString(R.string.check_u_disk))
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            val prePath: String = getExternalUsbPaths(true)
            if (prePath == "") {
                Timber.w("U盘不存在。")
                actionState.value = ActionState(
                    event = EVENT_ERROR,
                    msg = App.getContext().getString(R.string.check_u_disk_error)
                )
                return@launch
            }
            // 更改动作状态为导入
            actionState.value = ActionState(event = EVENT_IMPORT_FROM_SCANNER)
        }
    }

    fun onScannerImport() {
        // 导入数据
        Timber.w("开始导入数据${scannerInfo.value}")
        actionState.value = ActionState(EVENT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            val cardConfigBean = AppCardConfigUtils.toDecodeQrCode(scannerInfo.value)
            if (StringUtils.isEmpty(cardConfigBean.code)) {
                Timber.w("当前数据无法解析！")
                scannerInfo.value = ""
                actionState.value = ActionState(
                    event = EVENT_ERROR,
                    msg = App.getContext().getString(R.string.work_config_scanner_error)
                )
                return@launch
            } else {
                val cardConfig =
                    CardConfigService.findByIden(cardConfigBean.type, cardConfigBean.code)
                // 判断更新还是创建
                if (cardConfig != null) {
                    Timber.w("当前正在更新数据${cardConfigBean}")
                    cardConfigBean.id = cardConfig.id
                    Timber.w(
                        "当前正在更新数据${
                            CardConfigConverter.fillEntity(
                                cardConfigBean,
                                cardConfig
                            )
                        }"
                    )
                    CardConfigService.update(
                        CardConfigConverter.fillEntity(
                            cardConfigBean,
                            cardConfig
                        )
                    )
                } else {
                    cardConfigBean.id = UUIDUtils.getUUID()
                    Timber.w("当前正在创建数据！")
                    CardConfigService.add(CardConfigConverter.toEntity(cardConfigBean))
                }
                scannerInfo.value = ""
                actionState.value =
                    ActionState(event = EVENT_IMPORT_FROM_SCANNER_DONE, payload = cardConfigBean)
            }
        }

    }

    fun onScannerFinish(bean: CardConfigBean, callback: () -> Unit = {}) {
        AppParams.varCardConfigForPreview = bean
        AppParams.varCardConfigViewMode = "view"
        onClearInteraction()
        onLoad()
        callback()
    }

    fun onScannerCancel() {
        scannerInfo.value = ""
        onClearInteraction()
    }

    fun onScannerUpdate(curScannerInfo: String) {
        scannerInfo.value = curScannerInfo
    }

    private fun getExternalUsbPaths(flag: Boolean): String {
        if (flag) {
            val storageManager =
                App.getContext().getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val volumeList: List<StorageVolume> = storageManager.storageVolumes
            Timber.w("volumeList=${volumeList}")

            // 获取外部存储路径中第一个
            if (volumeList.size > 1) {
                val storageVolume = volumeList[1]
                val storageMap: Map<*, *>? =
                    App.gson.fromJson(App.gson.toJson(storageVolume), Map::class.java)
                val mPathTmp = storageMap?.get("m_path")
                if (mPathTmp != null) {
                    val mPath: Map<String, String> = mPathTmp as Map<String, String>
                    Timber.w("mPath=${mPath}")
                    if (storageVolume.isRemovable && storageVolume.state.equals("mounted")) {
                        return mPath["path"].toString()
                    }
                }
            }
            return ""
        } else {
            return Environment.getExternalStorageDirectory().absolutePath
        }
    }

    fun onImportByUDiskConfirm(callback: () -> Unit = {}) {
        actionState.value =
            ActionState(EVENT_LOADING, App.getContext().getString(R.string.check_u_disk))
        viewModelScope.launch(Dispatchers.IO) {
            if (!AppParams.devMock) {
                delay(1500)
                val prePath: String = getExternalUsbPaths(true)
                if (prePath == "") {
                    Timber.w("U盘不存在。")
                    actionState.value = ActionState(
                        event = EVENT_ERROR,
                        msg = App.getContext().getString(R.string.check_u_disk_error)
                    )
                    return@launch
                }
                Timber.w("prePath: $prePath")
            }

            // 更改动作状态为导入
            actionState.value = ActionState(event = EVENT_IMPORT_FROM_U_DISK)
        }
    }

    private fun readFileByUDisk(prePath: String) {
        // 存放数据路径
        val uPath = "$prePath/cardConfig/"
        // 文件集合
        val fileList = ArrayList<FileInfo>()
        Timber.w("====${uPath}")
        // 指定文件夹中存在文件
        val filesDir = File(uPath)
        if (filesDir.exists() && filesDir.isDirectory) {
            Timber.w("指定文件夹中存在文件。")
            filesDir.walk().maxDepth(1) //需遍历的目录层次为1，即无须检查子目录
                .filter { it.isFile } //只挑选文件，不处理文件夹
                .filter { it.extension == ".xlsx" }//选择扩展名为xslx的文件
                .forEach {
                    fileList.add(FileInfo(it.name, it.path))
                }//循环 处理符合条件的文件
        } else {
            Timber.w("指定文件夹中不存在文件。")
        }
        stepByUDisk.value = STEP_IMPORT_FILE
        // 填充读取文件
        files.value = fileList
    }

    @SuppressLint("Recycle")
    fun onImportByUDisk(context: Context, selectUri: Uri) {
        // 开始检查U盘
        actionState.value = ActionState(event = EVENT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            Timber.w("selectUri path $selectUri.path")

            // 将文件拷贝到本地目录
            val inputStream = context.contentResolver.openInputStream(selectUri)
            if (inputStream == null) {
                Timber.w("加载文件失败！")
                actionState.value = ActionState(
                    EVENT_ERROR,
                    App.getContext().getString(R.string.work_config_report_logo_error)
                )
                return@launch
            }

            val outputDir = context.getExternalFilesDir(null)
            Timber.w("outputDir $outputDir")

            val fileName = "tmp.xlsx"
            val file = File(outputDir, fileName)
            if (file.exists()) {
                file.delete()
            }

            val outputStream = FileOutputStream(file)
            val buffer = ByteArray(4 * 1024) // 4KB buffer
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            outputStream.flush()
            Timber.w("file ${file.path}")

            if (file.exists()) {
                val beanList = ArrayList<CardConfigBean>()
                try {
                    // 读取excel数据
                    val cardInfoList: List<List<String>> =
                        AppExcelUtils.readExcelSheetName(file, "试剂卡信息")
                    Timber.w("=========试剂卡信息 ${App.gson.toJson(cardInfoList)}")

                    val cardTopList: List<List<String>> =
                        AppExcelUtils.readExcelSheetName(file, "峰位值")
                    Timber.w("=========峰位值 ${App.gson.toJson(cardTopList)}")

                    val cardVarList: List<List<String>> =
                        AppExcelUtils.readExcelSheetName(file, "多项式")
                    Timber.w("=========多项式 ${App.gson.toJson(cardVarList)}")

                    for (cardInfo in cardInfoList) {
                        if (cardInfo[0].isEmpty()) {
                            continue
                        }
                        val cardConfigBean = fillCardInfo(cardInfo)

                        // 根据条形码收集峰位值
                        val topBeanList = ArrayList<CardTopBean>()
                        for ((index, value) in cardTopList.withIndex()) {
                            if (value[0].isEmpty()) {
                                continue
                            }
                            if (value[0] == cardConfigBean.code) {
                                val cardTopBean: CardTopBean = fillCardTop(value, index)
                                topBeanList.add(cardTopBean)
                            }
                        }
                        val varBeanList = ArrayList<CardVarBean>()

                        // 根据条形码收集多项式
                        for ((index, value) in cardVarList.withIndex()) {
                            if (value[0].isEmpty()) {
                                continue
                            }
                            if (value[0] == cardConfigBean.code) {
                                val cardVarBean: CardVarBean = fillCardVar(value, index)
                                varBeanList.add(cardVarBean)
                            }
                        }
                        cardConfigBean.topList = topBeanList
                        cardConfigBean.varList = varBeanList
                        beanList.add(cardConfigBean)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()

                    Timber.w("文件格式不正确！请根据模版正确填写！")
                    actionState.value = ActionState(
                        EVENT_ERROR,
                        App.getContext().getString(R.string.work_config_scanner_error)
                    )
                    return@launch
                }
                Timber.w("=========beanList ${App.gson.toJson(beanList)}")

                for ((index, bean) in beanList.withIndex()) {
                    val curConfig: CardConfig? = CardConfigService.findByIden(bean.type, bean.code)
                    if (curConfig == null) {
                        bean.id = AppLocalDateUtils.formatDateTime(LocalDateTime.now()) + index
                        CardConfigService.add(CardConfigConverter.toEntity(bean))
                    } else {
                        CardConfigService.update(CardConfigConverter.fillEntity(bean, curConfig))
                    }
                }

                // 更改动作状态为导入
                actionState.value = ActionState(event = EVENT_IMPORT_FROM_U_DISK_DONE)
                viewState.value = ViewState.LoadSuccess()
            } else {
                Timber.w("请选择文件后重新操作！")
                actionState.value = ActionState(
                    EVENT_ERROR,
                    App.getContext().getString(R.string.work_config_report_logo_error)
                )
                return@launch
            }
        }
    }

    private fun fillCardInfo(cardInfo: List<String>): CardConfigBean {
        val code = cardInfo[1]
        val type = AppTypeUtils.findTypeV2(code)

        return CardConfigBean(
            name = cardInfo[0],
            code = code,
            type = type ?: "",
            prodDate = AppLocalDateUtils.formatDate(AppLocalDateUtils.parseDate(cardInfo[2])),
            endDate = AppLocalDateUtils.formatDate(AppLocalDateUtils.parseDate(cardInfo[3])),
            scanStart = cardInfo[4],
            scanEnd = cardInfo[5],
            scanPPMM = cardInfo[6],
            ft0 = cardInfo[7],
            xt1 = cardInfo[8],
            ft1 = cardInfo[9],
            typeScore = cardInfo[10],
            scope = cardInfo[11],
            cAvg = cardInfo[12],
            cStd = cardInfo[13],
            cMin = cardInfo[14],
            cMax = cardInfo[15],
            cutOff1 = cardInfo[16],
            cutOff2 = cardInfo[17],
            cutOff3 = cardInfo[18],
            cutOff4 = cardInfo[19],
            cutOffMax = cardInfo[20],
            cutOff5 = cardInfo[21],
            cutOff6 = cardInfo[22],
            cutOff7 = cardInfo[23],
            cutOff8 = cardInfo[24],
            noise1 = cardInfo[25],
            noise2 = cardInfo[26],
            noise3 = cardInfo[27],
            noise4 = cardInfo[28],
            noise5 = cardInfo[29],
        )
    }

    private fun fillCardTop(cardTop: List<String>, index: Int): CardTopBean {
        return CardTopBean(
            index = index,
            id = index.toString(),
            start = cardTop[1],
            end = cardTop[2],
            ctrl = cardTop[3],
        )
    }

    private fun fillCardVar(cardVar: List<String>, index: Int): CardVarBean {
        val code = cardVar[0]
        val type = AppTypeUtils.findTypeV2(code)

        return CardVarBean(
            index = index,
            id = index.toString(),
            type = type ?: "",
            start = cardVar[1],
            end = cardVar[2],
            x0 = cardVar[3],
            x1 = cardVar[4],
            x2 = cardVar[5],
            x3 = cardVar[6],
            x4 = cardVar[7],
        )
    }

    fun onSelectFile(selectFile: FileInfo) {
        val result = ArrayList(selectFiles.value)
        val index = result.indexOf(selectFile)
        if (index < 0) {
            result.add(selectFile)
        } else {
            result.removeAt(index)
        }
        selectFiles.value = result
    }

    fun onDelete(bean: CardConfigBean) {
        actionState.value = ActionState(event = EVENT_DELETE_CONFIRM, payload = bean)
    }

    fun onDeleteConfirm(bean: CardConfigBean) {
        actionState.value = ActionState(EVENT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            val carConfig: CardConfig? = CardConfigService.findById(bean.id)
            if (carConfig != null) {
                CardConfigService.delete(carConfig)
                actionState.value = ActionState(event = EVENT_DELETE_DONE, payload = bean)
            }
        }
    }

    fun onDeleteDone() {
        actionState.value = ActionState(EVENT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            records.value = CardConfigService.findAll().map { CardConfigConverter.fromEntity(it) }
            onClearInteraction()
        }
    }

    fun onViewQrCode(bean: CardConfigBean, callback: () -> Unit = {}) {
        AppParams.varCardConfigForPreview = bean
        callback()
    }

    fun onItemDetail(bean: CardConfigBean, callback: () -> Unit = {}) {
        AppParams.varCardConfigForPreview = bean
        AppParams.varCardConfigViewMode = "view"
        callback()
    }

    companion object {
        const val EVENT_LOADING = "loading"

        const val EVENT_IMPORT_FROM_U_DISK = "uDiskImport"
        const val EVENT_IMPORT_FROM_U_DISK_DONE = "uDiskImportDone"

        // 删除确认与结束提示
        const val EVENT_DELETE_CONFIRM = "deleteConfirm"
        const val EVENT_DELETE_DONE = "deleteDone"
        const val EVENT_ERROR = "error"


        const val STEP_IMPORT_DEFAULT = "default"
        const val STEP_IMPORT_CHECK = "check"
        const val STEP_IMPORT_FILE = "file"
        const val STEP_IMPORT_ING = "ing"
        const val STEP_IMPORT_DONE = "done"

        // 扫码导入
        const val EVENT_IMPORT_FROM_SCANNER = "scannerImport"
        const val EVENT_IMPORT_FROM_SCANNER_DONE = "scannerImportDone"
    }

}