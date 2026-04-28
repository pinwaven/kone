@file:Suppress("UNCHECKED_CAST")

package poct.device.app.utils.app

import android.content.Context
import poct.device.app.AppParams
import poct.device.app.R
import poct.device.app.bean.CaseBean
import poct.device.app.entity.User
import java.time.YearMonth
import java.util.TimeZone

object AppDictUtils {
    /**
     * 检测步骤
     */
    fun checkStepMap(context: Context): Map<Int, String> {
        val key = "checkStepMap"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
//                0 to context.getString(R.string.work_read_chip),
//                10 to context.getString(R.string.work_ing_step2_0),
//                20 to context.getString(R.string.work_ing_step2_0),
//                23 to context.getString(R.string.work_ing_step2_0),
//                25 to context.getString(R.string.work_ing_step2_5),
//                30 to context.getString(R.string.work_ing_step7),
//                40 to context.getString(R.string.work_ing_step7),
//                50 to context.getString(R.string.work_ing_step7),
//                60 to context.getString(R.string.work_ing_step7),
//                70 to context.getString(R.string.work_ing_step7),
//                80 to context.getString(R.string.work_ing_step8),
                0 to context.getString(R.string.work_read_chip),
                5 to context.getString(R.string.work_ing_step1),
                10 to context.getString(R.string.work_ing_step2_0),
                20 to context.getString(R.string.work_ing_step2_5),
//                20 to context.getString(R.string.work_ing_step2_2),
                35 to context.getString(R.string.work_ing_step4),
                50 to context.getString(R.string.work_ing_step5),
                65 to context.getString(R.string.work_ing_step5),
                85 to context.getString(R.string.work_ing_step5),
                90 to context.getString(R.string.work_ing_step7),
                95 to context.getString(R.string.work_ing_step6),
                100 to context.getString(R.string.work_ing_step8),
            )
        } as Map<Int, String>
    }

    /**
     * 语言区域选项
     */
    fun timeZoneOptions(): Map<String, String> {
        val idList: Array<String> = TimeZone.getAvailableIDs()
        val map = LinkedHashMap<String, String>()
        val curZone = TimeZone.getDefault()
        val curId = curZone.id
        val curOffset = curZone.rawOffset / 3600 / 1000
        // 时区
        for (offset in -12..12) {
            if (curOffset == offset) {
                map[curId] = curZone.getDisplayName(false, TimeZone.SHORT)
                continue
            }
            for (id in idList) {
                val timeZone = TimeZone.getTimeZone(id)
                val timeOffset = timeZone.rawOffset / 3600 / 1000
                if (timeOffset == offset) {
                    map[id] = timeZone.getDisplayName(false, TimeZone.SHORT)
                    break
                }
            }
        }
        // 把当前时区设置到选项里
        return map
    }

    /**
     * 任务状态选项
     */
    fun taskStateOptions(context: Context): Map<Int, String> {
        val key = "taskStateOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                0 to context.getString(R.string.after_sale_task_state_unfinished),
                1 to context.getString(R.string.after_sale_task_state_canceled),
                2 to context.getString(R.string.after_sale_task_state_finished),
                3 to context.getString(R.string.after_sale_task_state_failed),
                4 to context.getString(R.string.after_sale_task_state_expired),
            )
        } as Map<Int, String>
    }

    /**
     * 语言选项
     */
    fun langOptions(): Map<String, String> {
        val key = "langOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                "cn" to "中文",
                "en" to "English",
            )
        } as Map<String, String>
    }

    fun yesNoOptions(context: Context): Map<String, String> {
        val key = "yesNoOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                "y" to context.getString(R.string.yes),
                "n" to context.getString(R.string.no),
            )
        } as Map<String, String>
    }

    /**
     * 检查类型
     */
    fun caseTypeOptions(context: Context): Map<String, String> {
        val key = "caseTypeOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                CaseBean.TYPE_4LJ to context.getString(R.string.case_type_4lj),
                CaseBean.TYPE_2LJ_A to context.getString(R.string.case_type_2lj_a),
                CaseBean.TYPE_2LJ_B to context.getString(R.string.case_type_2lj_b),
                CaseBean.TYPE_3LJ to context.getString(R.string.case_type_3lj),
                CaseBean.TYPE_CRP to context.getString(R.string.case_type_crp),
                CaseBean.TYPE_SF to context.getString(R.string.case_type_sf),
                CaseBean.TYPE_IGE to context.getString(R.string.case_type_ige),
                CaseBean.TYPE_3LJ_BIOAGE_L1 to context.getString(R.string.case_type_3lj_bioage_l1),
                CaseBean.TYPE_BIOAGE_CRP to context.getString(R.string.case_type_bioage_crp),
            )
        } as Map<String, String>
    }

    /**
     * CRP类型
     */
    fun caseCrpTypeOptions(context: Context): Map<String, String> {
        val key = "caseCrpTypeOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                "crp" to context.getString(R.string.case_crp_type_crp),
                "sf" to context.getString(R.string.case_crp_type_sf),
            )
        } as Map<String, String>
    }

    fun genderOptions(context: Context): Map<Int, String> {
        val key = "genderOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                1 to context.getString(R.string.male), 2 to context.getString(R.string.female)
            )
        } as Map<Int, String>
    }

    fun bloodTypeOptions(context: Context): Map<Int, String> {
        val key = "bloodTypeOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                1 to context.getString(R.string.plasma), 2 to context.getString(R.string.blood)
            )
        } as Map<Int, String>
    }

    fun yearOptions(): Map<Int, String> {
        val key = "yearOptions"
        return AppParams.dictMap.getOrPut(key) {
            val options = LinkedHashMap<Int, String>()
            for (i in 1900..2100) {
                options[i] = i.toString()
            }
            return options
        } as Map<Int, String>
    }

    fun monthOptions(): Map<Int, String> {
        val key = "monthOptions"
        return AppParams.dictMap.getOrPut(key) {
            val options = LinkedHashMap<Int, String>()
            for (i in 1..12) {
                options[i] = i.toString()
            }
            return options
        } as Map<Int, String>
    }

    fun dayOptions(yearValue: Int, monthValue: Int): Map<Int, String> {
        val key = "dayOptions@$yearValue@$monthValue"
        return AppParams.dictMap.getOrPut(key) {
            val yearMonth = YearMonth.of(yearValue, monthValue)
            val endDay = yearMonth.atEndOfMonth().dayOfMonth
            val options = LinkedHashMap<Int, String>()
            for (i in 1..endDay) {
                options[i] = i.toString()
            }
            return options
        } as Map<Int, String>
    }

    fun hourOptions(): Map<Int, String> {
        val key = "hourOptions"
        return AppParams.dictMap.getOrPut(key) {
            val options = LinkedHashMap<Int, String>()
            for (i in 0..23) {
                options[i] = i.toString()
            }
            return options
        } as Map<Int, String>
    }

    fun minuteOptions(): Map<Int, String> {
        val key = "minuteOptions"
        return AppParams.dictMap.getOrPut(key) {
            val options = LinkedHashMap<Int, String>()
            for (i in 0..59) {
                options[i] = i.toString()
            }
            return options
        } as Map<Int, String>
    }

    fun secondOptions(): Map<Int, String> {
        val key = "secondOptions"
        return AppParams.dictMap.getOrPut(key) {
            val options = LinkedHashMap<Int, String>()
            for (i in 0..59) {
                options[i] = i.toString()
            }
            return options
        } as Map<Int, String>
    }

    fun roleOptions(context: Context): Map<String, String> {
        val key = "roleOptions"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                User.ROLE_ADMIN to context.getString(R.string.sys_fun_xtpz_user_role_admin),
                User.ROLE_DEV to context.getString(R.string.sys_fun_xtpz_user_role_dev),
                User.ROLE_CHECKER to context.getString(R.string.sys_fun_xtpz_user_role_checker),
            )
        } as Map<String, String>
    }

    fun <T> label(options: Map<T, String>, key: T): String {
        return options[key] ?: "-"
    }

    /**
     * BAA得分项目
     */
    fun baaScoreName(): Map<String, String> {
        val key = "baaScoreName"
        return AppParams.dictMap.getOrPut(key) {
            return mapOf(
                "ILI" to "炎性负荷指数",
                "MFI" to "线粒体机能指数",
                "MRI" to "代谢韧性指数",
                "MVII" to "微血管发展指数",
            )
        } as Map<String, String>
    }
}