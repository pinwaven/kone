package poct.device.app.ui.report

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.bean.CaseQueryBean
import poct.device.app.bean.ConfigReportBean
import poct.device.app.bean.PdfBean
import poct.device.app.bean.converter.CaseConverter
import poct.device.app.entity.Case
import poct.device.app.entity.CaseResult
import poct.device.app.entity.service.CardConfigService
import poct.device.app.entity.service.CaseService
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.thirdparty.FrosApi
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppExcelUtils
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppLocalDateUtils
import poct.device.app.utils.app.AppPdfUtils
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Collections


class ReportMainViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    val exportUrl = MutableStateFlow("")

    // 查询条件
    val query = MutableStateFlow(
        CaseQueryBean(
            name = "",
            caseType = "",
            dateStarted = AppLocalDateUtils.formatDate(LocalDate.now().minusDays(7)),
            dateEnded = AppLocalDateUtils.formatDate(LocalDate.now())
        )
    )

    // 结果集
    val records = MutableStateFlow<List<CaseBean>>(emptyList())

    // 选择项
    val selected = MutableStateFlow<List<String>>(emptyList())

    fun pdfReportExist(bean: CaseBean): Boolean {
        return File(bean.pdfPath).exists()
    }

    private fun isAllSelected(): Boolean {
        return selected.value.size == records.value.size
    }

    fun load() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            records.value =
                CaseService.query(query = query.value).map { CaseConverter.fromEntity(it) }
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onQueryUpdate(newBean: CaseQueryBean) {
        query.value = newBean
        actionState.value = ActionState(EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            delay(1000)
            records.value =
                CaseService.query(query = query.value).map { CaseConverter.fromEntity(it) }
            onClearInteraction()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    fun onCheckAll() {
        viewModelScope.launch(Dispatchers.IO) {
            if (isAllSelected()) {
                selected.value = emptyList()
            } else {
                selected.value = ArrayList(records.value.map { it.id })
            }
        }
    }

    fun onCheckItem(caseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ArrayList(selected.value)
            val index = result.indexOf(caseId)
            if (index < 0) {
                result.add(caseId)
            } else {
                result.removeAt(index)
            }
            selected.value = result
        }

    }

    fun onItemDeleteConfirm(bean: CaseBean) {
        actionState.value = ActionState(EVT_DEL_CONFIRM, payload = bean)
    }

    fun onItemExportConfirm(bean: CaseBean) {
        actionState.value = ActionState(EVT_EXP_CONFIRM, payload = bean)
    }

    fun onDelete(bean: CaseBean, callback: () -> Unit = {}) {
        actionState.value = ActionState(EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            CaseService.findById(bean.id)?.let { CaseService.delete(it) }
            records.value =
                CaseService.query(query = query.value).map { CaseConverter.fromEntity(it) }
            onClearInteraction()
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun onDelMoreConfirm() {
        if (selected.value.isEmpty()) {
            actionState.value = ActionState(EVT_NO_SELECTED)
        } else {
            actionState.value = ActionState(EVT_DEL_MORE_CONFIRM)
        }
    }

    fun onDelMore(callback: () -> Unit = {}) {
        actionState.value = ActionState(EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            for (id in selected.value) {
                CaseService.findById(id)?.let { CaseService.delete(it) }
            }
            records.value =
                CaseService.query(query = query.value).map { CaseConverter.fromEntity(it) }
            selected.value = Collections.emptyList()
            onClearInteraction()
            withContext(Dispatchers.Main) {
                callback()
            }
        }
    }

    fun onExportMoreConfirm() {
        actionState.value = ActionState(EVT_EXP_MORE_CONFIRM)
    }

    private fun getExternalUsbPaths(flag: Boolean): String {
        if (flag) {
            val storageManager =
                App.getContext().getSystemService(Context.STORAGE_SERVICE) as StorageManager
            val volumeList: List<StorageVolume> = storageManager.storageVolumes

            // 获取外部存储路径中第一个
            for (storageVolume in volumeList) {
                val storageMap: Map<*, *>? =
                    App.gson.fromJson(App.gson.toJson(storageVolume), Map::class.java)
                val mPath: Map<String, String> = storageMap?.get("m_path") as Map<String, String>
                if (storageVolume.isRemovable && storageVolume.state.equals("mounted")) {
                    return mPath["path"].toString()
                }
            }
            return ""
        } else {
            return Environment.getExternalStorageDirectory().absolutePath
        }
    }

    private fun writeToExternalStorage(filePath: String, caseList: List<CaseBean>) {
        val file = File(filePath)
        if (!file.parentFile?.exists()!!) {
            file.parentFile?.mkdirs()
        }
        if (file.exists()) {
            file.delete()
        }
        val data = ArrayList<List<String?>>()
        data.add(
            listOf(
                "姓名",
                "性别",
                "生日",
                "样本编号",
                "检测项目",
                "检测批号",
                "检测时间",
                "样本状态",
                "检测结果"
            )
        )
        // 病例
        for (caseBean in caseList) {
            val caseRow = listOf(
                AppExcelUtils.valueOf(caseBean.name),
                AppExcelUtils.valueOf(
                    AppDictUtils.label(
                        AppDictUtils.genderOptions(App.getContext()),
                        caseBean.gender
                    )
                ),
                AppExcelUtils.valueOf(caseBean.birthday),
                AppExcelUtils.valueOf(caseBean.caseId),
                AppExcelUtils.valueOf(caseBean.type),
                AppExcelUtils.valueOf(caseBean.reagentId),
                AppExcelUtils.valueOf(caseBean.workTime),
                AppExcelUtils.valueOf(caseBean.state),
                AppExcelUtils.valueOf(caseBean.workResult)
            )
            data.add(caseRow)
        }
        AppExcelUtils.exportExcel(filePath, data)
    }

    /**
     *  0-U盘 1-DISK 2-Email
     */
    fun onExport(record: CaseBean, expType: Int, callback: () -> Unit = {}) {
        actionState.value = ActionState(EVT_EXPORT_ACTION_ING)
        viewModelScope.launch {
            val beanList = ArrayList<CaseBean>()
            beanList.add(record)

            // TODO: 根据导出类型将数据进行导出，expType 0-U盘， 1-DISK，2-Email
            if (expType == 0) {
                // 检查U盘
                var prePath: String = getExternalUsbPaths(true)
                if (prePath == "") {
                    Timber.w("U盘不存在。")
                    actionState.value = ActionState(
                        EVT_EXPORT_ACTION_ERROR, App.getContext().getString(
                            R.string.check_u_disk_error
                        )
                    )
                    return@launch
                }

                prePath = Environment.getExternalStorageDirectory().absolutePath
                // 文件名
                val fileName: String = record.qrCode + ".xlsx"
                // 存放数据路径
                val uPath = "$prePath/cardReport/$fileName"
                exportUrl.value = uPath

                withContext(Dispatchers.IO) {
                    // 写入文件
                    writeToExternalStorage(uPath, beanList)
                }
            } else if (expType == 1) {
                viewModelScope.launch(Dispatchers.IO) {
                    // 文件名
                    val fileName: String = record.qrCode + ".xlsx"
                    // 存放数据路径
                    val uPath = "${App.getContext().externalCacheDir!!.path}/$fileName"
                    exportUrl.value = uPath

                    // 写入文件
                    writeToExternalStorage(uPath, beanList)
                }
            } else if (expType == 2) {

            }
            actionState.value = ActionState(event = EVT_EXPORT_ACTION_SUCCESS, msg = "")
            delay(3000)
            actionState.value = ActionState(EVT_EXPORT_ACTION_DONE)
            callback()
        }
    }

    fun onExportReset() {
        onClearInteraction()
    }

    /**
     *  0-U盘 1-DISK 2-Email
     */
    fun onExportMore(expType: Int, callback: () -> Unit = {}) {
        actionState.value = ActionState(EVT_EXPORT_ACTION_ING)
        viewModelScope.launch {
            // TODO:根据导出类型将数据进行导出，expType 0-U盘， 1-DISK，2-Email
            if (expType == 0) {
                // 检查U盘
                val prePath: String = getExternalUsbPaths(false)
                if (prePath == "") {
                    Timber.w("U盘不存在。")
                    actionState.value = ActionState(
                        EVT_EXPORT_ACTION_ERROR, App.getContext().getString(
                            R.string.check_u_disk_error
                        )
                    )
                    return@launch
                }
                val caseList = ArrayList<CaseBean>()
                for (selectId in selected.value) {
                    val record: CaseBean? = records.value.filter { it.id == selectId }.getOrNull(0)
                    if (record != null) {
                        caseList.add(record)
                    }
                }
                // 文件名
                val fileName = "样本报告.xlsx"
                // 存放数据路径
                val uPath = "$prePath/cardConfig/$fileName"
                withContext(Dispatchers.IO) {
                    // 写入文件
                    writeToExternalStorage(uPath, caseList)
                }
            } else if (expType == 1) {

            } else if (expType == 2) {

            }
            actionState.value = ActionState(EVT_EXPORT_ACTION_SUCCESS)
            delay(3000)
            actionState.value = ActionState(EVT_EXPORT_ACTION_DONE)
            callback()
        }
    }

    fun onItemDetail(record: CaseBean, callback: () -> Unit = {}) {
        AppParams.varReport = record
        // 检查试剂卡配置是否存在
        viewModelScope.launch {
            // 查询试剂卡配置
            val cardConfig = withContext(Dispatchers.IO) {
                CardConfigService.findByIden(
                    record.type,
                    record.reagentId
                )
            }
            if (cardConfig == null) {
                actionState.value = ActionState(
                    event = EVT_ERROR,
                    msg = App.getContext().getString(R.string.report_card_error)
                )
                return@launch
            } else {
                callback()
            }
        }
    }

    fun onItemPDF(record: CaseBean, callback: () -> Unit = {}) {
        AppParams.varReport = record
        callback()
    }

    fun onGenPDF(bean: CaseBean) {
        viewState.value = ViewState.LoadingOver()
        actionState.value = ActionState(event = EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            var entity: Case? = CaseService.findById(bean.id)
            if (entity != null) {
                entity = CaseConverter.fillEntity(bean, entity)
                CaseService.update(entity)
            } else {
                entity = CaseConverter.toEntity(bean)
                entity.time = LocalDateTime.now()
                CaseService.add(entity)
            }
            val context = App.getContext()
            val configBean =
                SysConfigService.findBean(ConfigReportBean.PREFIX, ConfigReportBean::class)
            val title = configBean.hosName
            var subTitle = "report"
            if (bean.type == CaseBean.TYPE_CRP) {
                subTitle = configBean.crpName
            } else if (bean.type == CaseBean.TYPE_4LJ) {
                subTitle = configBean.cljName
            } else if (bean.type == CaseBean.TYPE_IGE) {
                subTitle = configBean.igeName
            } else if (bean.type == CaseBean.TYPE_3LJ) {
                subTitle = configBean.sljName
            } else if (bean.type == CaseBean.TYPE_2LJ_A) {
                subTitle = configBean.eljAName
            } else if (bean.type == CaseBean.TYPE_2LJ_B) {
                subTitle = configBean.eljBName
            }

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
                    type = bean.type,
                    outPath = bean.pdfPath,
                    jcbh = entity.caseId,
                    jcrq = AppLocalDateUtils.formatDate(entity.time.toLocalDate()),
                    xm = entity.name,
                    xb = AppDictUtils.label(AppDictUtils.genderOptions(context), entity.gender),
                    nl = AppLocalDateUtils.calcAge(entity.birthday, entity.time.toLocalDate())
                        .toString(),
                    sjId = entity.reagentId,
                    ybId = entity.caseId,
                    data = (Json.decodeFromString(bean.workResult) as List<CaseResult>).map { result ->
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
            viewState.value = ViewState.LoadSuccess()
            onClearInteraction()
        }
    }

    fun uploadReport(data: CaseBean) {
        viewModelScope.launch(Dispatchers.IO) {
            // 自动上传数据到服务器
            FrosApi.uploadPatientReportDataToServer(data)
        }
    }

    companion object {
        const val EVT_LOADING = "loading"
        const val EVT_ERROR = "error"

        // 删除
        const val EVT_DEL_CONFIRM = "delConfirm"
        const val EVT_DEL_MORE_CONFIRM = "delMoreConfirm"

        // 导出
        const val EVT_EXP_CONFIRM = "expConfirm"
        const val EVT_EXP_MORE_CONFIRM = "expMoreConfirm"

        // 选择文件
        const val EVT_NO_SELECTED = "noSelected"

        // 导出动作
        const val EVT_EXPORT_ACTION = "expAction"
        const val EVT_EXPORT_ACTION_ERROR = "expActionError"
        const val EVT_EXPORT_ACTION_ING = "expActionIng"
        const val EVT_EXPORT_ACTION_SUCCESS = "expActionSuccess"
        const val EVT_EXPORT_ACTION_DONE = "expActionDone"
    }
}