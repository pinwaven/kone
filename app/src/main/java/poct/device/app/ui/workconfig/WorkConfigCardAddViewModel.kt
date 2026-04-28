package poct.device.app.ui.workconfig

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.apache.commons.lang.math.NumberUtils
import poct.device.app.App
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CardConfigBean
import poct.device.app.bean.CardTopBean
import poct.device.app.bean.CardVarBean
import poct.device.app.bean.CaseBean
import poct.device.app.bean.converter.CardConfigConverter
import poct.device.app.entity.User
import poct.device.app.entity.service.CardConfigService
import poct.device.app.state.ActionState
import poct.device.app.state.FieldState
import poct.device.app.state.FieldStateHolder
import poct.device.app.state.ViewState
import poct.device.app.utils.app.AppExperimentUtils
import poct.device.app.utils.app.AppFormValidateUtils
import poct.device.app.utils.app.AppSampleUtils
import poct.device.app.utils.app.AppTypeUtils
import timber.log.Timber

class WorkConfigCardAddViewModel : ViewModel() {
    // 视图状态
    val viewState = MutableStateFlow<ViewState>(ViewState.Default)
    val actionState = MutableStateFlow(ActionState.Default)
    val stepState = MutableStateFlow(STEP_INFO)
    val fieldStateHolder = MutableStateFlow(FieldStateHolder())

    // 记录列表
    val bean = MutableStateFlow(CardConfigBean.Empty)

    fun onLoad() {
        viewState.value = ViewState.Loading()
        viewModelScope.launch(Dispatchers.IO) {
            bean.value = AppParams.varCardConfig

            delay(10)
            viewState.value = ViewState.LoadSuccess()
        }
    }

    fun onClearInteraction() {
        actionState.value = ActionState.Default
    }

    // 退出确认
    fun onBackConfirm() {
        actionState.value = ActionState(event = EVT_EXIT)
    }

    fun onBeanUpdate(newBean: CardConfigBean) {
        bean.value = newBean
    }

    fun onFieldStateHolderUpdate(newHolder: FieldStateHolder) {
        fieldStateHolder.value = newHolder
    }

    fun onInfoReset() {
        bean.value =
            AppParams.varCardConfig.copy(topList = bean.value.topList, varList = bean.value.varList)
    }

    fun onInfoNext() {
        viewModelScope.launch(Dispatchers.IO) {
            validateInfo()
            if (fieldStateHolder.value.get("code") != null) {
                fieldStateHolder.value = fieldStateHolder.value.clone()
                actionState.value = ActionState(
                    EVT_VALIDATE_FAILED, fieldStateHolder.value.get("code")!!.msg
                )
            } else {
                stepState.value = STEP_TOP
            }
        }


    }

    fun onTopAddConfirm() {
        actionState.value = ActionState(event = EVT_ADD_TOP_PRE)
    }

    fun onTopAdd() {
        val newTopList = ArrayList(bean.value.topList)
        newTopList.add(CardTopBean())
        regulateTopList(newTopList)
        onBeanUpdate(bean.value.copy(topList = newTopList))
        actionState.value = ActionState(event = EVT_ADD_TOP_DONE)
    }

    private fun regulateTopList(topList: List<CardTopBean>) {
        for (i in topList.indices) {
            topList[i].index = i
            topList[i].id = i.toString()
        }
    }

    fun onTopRemoveConfirm(top: CardTopBean) {
        actionState.value = ActionState(event = EVT_REMOVE_TOP_PRE, payload = top)
    }

    fun onTopRemove(top: CardTopBean) {
        val newTopList = ArrayList(bean.value.topList)
        newTopList.remove(top)
        regulateTopList(newTopList)
        onBeanUpdate(bean.value.copy(topList = newTopList))
        actionState.value = ActionState(event = EVT_REMOVE_TOP_DONE)
    }

    fun onTopPre() {
        stepState.value = STEP_INFO
    }

    fun onTopNext() {
        viewModelScope.launch(Dispatchers.IO) {
            doValidateTop(bean.value.topList)
            val fieldStateList: ArrayList<FieldState> = fieldStateHolder.value.getStateList()
            for (fieldState in fieldStateList) {
                if (fieldState.name.contains("cardTop")) {
                    fieldStateHolder.value = fieldStateHolder.value.clone()
                    actionState.value = ActionState(
                        EVT_VALIDATE_FAILED,
                        App.getContext().getString(R.string.msg_validate_failed)
                    )
                    return@launch
                }
            }
            stepState.value = STEP_VAR
        }
    }

    fun onVarAddConfirm() {
        actionState.value = ActionState(event = EVT_ADD_VAR_PRE)
    }

    fun onVarAdd() {
        val newVarList = ArrayList(bean.value.varList)
        newVarList.add(CardVarBean())
        regulateVarList(newVarList)
        onBeanUpdate(bean.value.copy(varList = newVarList))
        actionState.value = ActionState(event = EVT_ADD_VAR_DONE)
    }

    fun onVarRemoveConfirm(varBean: CardVarBean) {
        actionState.value = ActionState(event = EVT_REMOVE_VAR_PRE, payload = varBean)
    }

    fun onVarRemove(varBean: CardVarBean) {
        val newVarList = ArrayList(bean.value.varList)
        newVarList.remove(varBean)
        regulateVarList(newVarList)
        onBeanUpdate(bean.value.copy(varList = newVarList))
        actionState.value = ActionState(event = EVT_REMOVE_VAR_DONE)
    }

    private fun regulateVarList(varList: List<CardVarBean>) {
        for (i in varList.indices) {
            varList[i].index = i
            varList[i].id = i.toString()
        }
    }

    fun onVarPre() {
        stepState.value = STEP_TOP
    }

    fun onPreview(callback: () -> Unit = {}) {
        AppParams.varCardConfigForPreview = bean.value
        AppParams.varCardConfigViewMode = "preview"
        callback()
    }

    fun onSave() {
        actionState.value = ActionState(EVT_LOADING)
        viewModelScope.launch(Dispatchers.IO) {
            validateBean()
            if (fieldStateHolder.value.hasErrors()) {
                fieldStateHolder.value = fieldStateHolder.value.clone()
                actionState.value = ActionState(
                    EVT_VALIDATE_FAILED, App.getContext().getString(R.string.msg_validate_failed)
                )
                return@launch
            }
            if (AppParams.varCardConfigMode == "add") {
                CardConfigService.add(CardConfigConverter.toEntity(bean.value))
            } else {
                CardConfigService.findById(bean.value.id)?.let {
                    CardConfigService.update(CardConfigConverter.fillEntity(bean.value, it))
                }
            }
            AppParams.varCardConfig = bean.value
            AppParams.varCardConfigForPreview = bean.value
            actionState.value = ActionState(EVT_SAVE_DONE)
        }
    }

    fun onCheck() {
        viewState.value = ViewState.LoadingOver()

        viewModelScope.launch(Dispatchers.IO) {
            val code = bean.value.code
            onValidateCode(code)

            if (fieldStateHolder.value.get("code") != null) {
                viewModelScope.launch {
                    onBeanUpdate(bean.value.copy(showDetail = false))
                    onFieldStateHolderUpdate(fieldStateHolder.value.clone())
                }
            } else {
                var type: String? = null
                if (AppParams.curUser.role == User.ROLE_DEV) {
                    type = AppExperimentUtils.getType(code)
                }
                if (type == null) {
                    type = AppTypeUtils.findTypeV2(code)
                }
                if (type == null) {
                    fieldStateHolder.value.put(
                        FieldState.error(
                            "code",
                            App.getContext().getString(R.string.work_config_scanner_error)
                        )
                    )
                } else {
                    val currentBean: CardConfigBean
                    when (type) {
                        CaseBean.TYPE_4LJ -> {
                            currentBean = AppSampleUtils.genCardConfigFor4lJ()
                        }

                        CaseBean.TYPE_IGE -> {
                            currentBean = AppSampleUtils.genCardConfigForIgE()
                        }

                        CaseBean.TYPE_CRP -> {
                            currentBean = AppSampleUtils.genCardConfigForCrp()
                        }

                        CaseBean.TYPE_SF -> {
                            currentBean = AppSampleUtils.genCardConfigForSfCrp()
                        }

                        CaseBean.TYPE_3LJ -> {
                            currentBean = AppSampleUtils.genCardConfigFor3lJ()
                        }

                        CaseBean.TYPE_2LJ_A -> {
                            currentBean = AppSampleUtils.genCardConfigFor2LJA()
                        }

                        CaseBean.TYPE_2LJ_B -> {
                            currentBean = AppSampleUtils.genCardConfigFor2LJB()
                        }

                        else -> {
                            currentBean = CardConfigBean.Empty
                        }
                    }
                    Timber.w("=====${currentBean}")

                    viewModelScope.launch {
                        currentBean.name = bean.value.name
                        currentBean.code = bean.value.code
                        currentBean.showDetail = true
                        onBeanUpdate(currentBean)
                    }
                }
            }
            viewState.value = ViewState.LoadSuccess()
        }
    }

    private suspend fun validateBean() {
        validateInfo()
    }

    private suspend fun validateInfo() {
        if (!AppFormValidateUtils.validateRequired(bean.value.code)) {
            fieldStateHolder.value.put(
                FieldState.error(
                    "code",
                    App.getContext().getString(R.string.work_config_code_required)
                )
            )
            return
        }
        if (!AppFormValidateUtils.validateRequired(bean.value.type)) {
            fieldStateHolder.value.put(
                FieldState.error(
                    "code",
                    App.getContext().getString(R.string.work_config_code_uncheck)
                )
            )
            return
        }
        doValidateCode(bean.value.code)
        doValidateTop(bean.value.topList)
        doValidateVar(bean.value.varList)
    }

    suspend fun onValidateCode(code: String) {
        doValidateCode(code)
    }

    private suspend fun doValidateCode(code: String) {
        var type: String? = null
        if (AppParams.curUser.role == User.ROLE_DEV) {
            type = AppExperimentUtils.getType(code)
        }
        if (type == null) {
            type = AppTypeUtils.findTypeV2(code)
        }
        if (type == null) {
            fieldStateHolder.value.put(
                FieldState.error(
                    "code",
                    App.getContext().getString(R.string.work_config_scanner_error)
                )
            )
            return
        }

        fieldStateHolder.value.remove("code")
        if (!AppFormValidateUtils.validateRequired(code)) {
            fieldStateHolder.value.put(
                FieldState.error(
                    "code",
                    App.getContext().getString(R.string.work_config_code_required)
                )
            )
            return
        }
        if (!AppFormValidateUtils.validateIden(code)) {
            fieldStateHolder.value.put(
                FieldState.error(
                    "code",
                    App.getContext().getString(R.string.msg_format_code)
                )
            )
            return
        }
        val exists = CardConfigService.findByIden(type, code)
        if (AppParams.varCardConfigMode != "add") {
            if (exists != null && exists.id != bean.value.id) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "code",
                        App.getContext().getString(R.string.work_config_code_used)
                    )
                )
                return
            }
        } else {
            if (exists != null) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "code",
                        App.getContext().getString(R.string.work_config_code_used)
                    )
                )
                return
            }
        }
        return
    }

    private fun doValidateTop(topList: List<CardTopBean>) {
        for (cardTopBean in topList) {
            fieldStateHolder.value.remove("cardTopStart" + cardTopBean.id)
            fieldStateHolder.value.remove("cardTopTop" + cardTopBean.id)
            val index = cardTopBean.index + 1
//            val start = BigDecimal(cardTopBean.start)
//            val end = BigDecimal(cardTopBean.end)
            if (!AppFormValidateUtils.validateRequired(cardTopBean.start)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardTopStart" + cardTopBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_start) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateRequired(cardTopBean.end)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardTopEnd" + cardTopBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_end) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardTopBean.start)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardTopStart" + cardTopBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_start) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardTopBean.end)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardTopEnd" + cardTopBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_end) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (NumberUtils.toDouble(cardTopBean.end) < NumberUtils.toDouble(cardTopBean.start)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardTopStart" + cardTopBean.id,
                        App.getContext().getString(R.string.work_config_check_compare)
                    )
                )
                return
            }
        }
    }

    private fun doValidateVar(varList: List<CardVarBean>) {
        for (cardVarBean in varList) {
            fieldStateHolder.value.remove("cardVarStart" + cardVarBean.id)
            fieldStateHolder.value.remove("cardVarEnd" + cardVarBean.id)
            fieldStateHolder.value.remove("cardVarX0" + cardVarBean.id)
            fieldStateHolder.value.remove("cardVarX1" + cardVarBean.id)
            fieldStateHolder.value.remove("cardVarX2" + cardVarBean.id)
            fieldStateHolder.value.remove("cardVarX3" + cardVarBean.id)
            fieldStateHolder.value.remove("cardVarX4" + cardVarBean.id)
//            val start = BigDecimal(cardVarBean.start)
//            val end = BigDecimal(cardVarBean.end)
//            val x0 = BigDecimal(cardVarBean.x0)
//            val x1 = BigDecimal(cardVarBean.x1)
//            val x2 = BigDecimal(cardVarBean.x2)
//            val x3 = BigDecimal(cardVarBean.x3)
//            val x4 = BigDecimal(cardVarBean.x4)
            if (!AppFormValidateUtils.validateRequired(cardVarBean.start)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarStart" + cardVarBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_start) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateRequired(cardVarBean.end)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarEnd" + cardVarBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_end) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateRequired(cardVarBean.x0)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX0" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x0) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateRequired(cardVarBean.x1)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX1" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x1) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateRequired(cardVarBean.x2)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX2" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x2) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateRequired(cardVarBean.x3)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX3" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x3) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateRequired(cardVarBean.x4)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX4" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x4) + App.getContext()
                            .getString(R.string.work_config_check_null)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardVarBean.start)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarStart" + cardVarBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_start) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardVarBean.end)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarEnd" + cardVarBean.id,
                        App.getContext()
                            .getString(R.string.work_config_f_top_end) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardVarBean.x0)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX0" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x0) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardVarBean.x1)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX1" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x1) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardVarBean.x2)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX2" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x2) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardVarBean.x3)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX3" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x3) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (!AppFormValidateUtils.validateNumber(cardVarBean.x4)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarX4" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_f_var_x4) + App.getContext()
                            .getString(R.string.work_config_check_num)
                    )
                )
                return
            }
            if (NumberUtils.toDouble(cardVarBean.end) < NumberUtils.toDouble(cardVarBean.start)) {
                fieldStateHolder.value.put(
                    FieldState.error(
                        "cardVarEnd" + cardVarBean.id,
                        App.getContext().getString(R.string.work_config_check_compare)
                    )
                )
                return
            }
        }
    }

    companion object {
        const val STEP_INFO = "info"
        const val STEP_TOP = "top"
        const val STEP_VAR = "var"
        val STEPS = arrayOf(STEP_INFO, STEP_TOP, STEP_VAR)

        const val EVT_EXIT = "exit"
        const val EVT_TO_VIEW = "toView"
        const val EVT_ADD_TOP_PRE = "addTopPre"
        const val EVT_ADD_TOP_DONE = "addTopDone"
        const val EVT_REMOVE_TOP_PRE = "removeTopPre"
        const val EVT_REMOVE_TOP_DONE = "removeTopDone"
        const val EVT_ADD_VAR_PRE = "addVarPre"
        const val EVT_ADD_VAR_DONE = "addVarDone"
        const val EVT_REMOVE_VAR_PRE = "removeVarPre"
        const val EVT_REMOVE_VAR_DONE = "removeVarDone"
        const val EVT_LOADING = "loading"
        const val EVT_SAVE_DONE = "saveDone"
        const val EVT_VALIDATE_FAILED = "EVT_VALIDATE_FAILED"
    }
}