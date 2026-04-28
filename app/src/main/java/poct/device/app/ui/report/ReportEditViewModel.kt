package poct.device.app.ui.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.bean.CaseBean
import poct.device.app.bean.ConfigReportBean
import poct.device.app.bean.PdfBean
import poct.device.app.bean.card.CardConfig
import poct.device.app.bean.converter.CaseConverter
import poct.device.app.entity.Case
import poct.device.app.entity.CaseResult
import poct.device.app.entity.service.CaseService
import poct.device.app.entity.service.SysConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppDictUtils
import poct.device.app.utils.app.AppFileUtils
import poct.device.app.utils.app.AppLocalDateUtils
import poct.device.app.utils.app.AppPdfUtils
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime

class ReportEditViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)

    // 试剂卡配置
    private var cardConfig: CardConfig? = null

    val bean = MutableStateFlow(CaseBean.Empty)
    fun onLoad() {
        viewState.value = ViewState.LoadingOver()
        viewModelScope.launch(Dispatchers.IO) {
            bean.value = AppParams.varReport
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    // 退出确认
    fun onExitConfirm() {
        actionState.value = ActionState(event = EVT_EXIT)
    }

    fun onBeanUpdate(newBean: CaseBean) {
        bean.value = newBean
    }

    fun onSave() {
        actionState.value = ActionState(event = EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            val bean = bean.value
            var entity: Case? = CaseService.findById(bean.id)
            if (entity != null) {
                entity = CaseConverter.fillEntity(bean, entity)
                CaseService.update(entity)
            } else {
                entity = CaseConverter.toEntity(bean)
                entity.time = LocalDateTime.now()
                CaseService.add(entity)
            }
            AppParams.varReport = bean
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
            actionState.value = ActionState(event = EVT_SAVE_OK)
        }
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

    companion object {
        const val EVT_LOADING = "loading"
        const val EVT_SAVE_OK = "saveOk"
        const val EVT_EXIT = "exit"
    }
}