package poct.device.app.thirdparty.model.fros.resp

import cn.hutool.core.collection.CollUtil.newArrayList

class QueryByCodeResp(
    /**
     * 试剂卡编号
     */
    var code: String = "",

    /**
     * 检测时间
     */
    var date: String = "",

    /**
     * 检测结果
     */
    var result: List<QueryByCodeValueResp> = newArrayList(),

    /**
     * 检测项目
     */
    var type: String = "ige",

    /**
     * 患者信息
     */
    var patient: QueryByCodePatientResp = QueryByCodePatientResp()
)

class QueryByCodeValueResp(
    var value: QueryByCodeResultResp,
)

class QueryByCodePatientResp(
    /**
     * 患者ID
     */
    var objectId: String = "",

    /**
     * 姓名
     */
    var name_: String = "",

    /**
     * 性别
     */
    var gender: String = "",

    /**
     * 生日
     */
    var birthDate: String = "",
)

class QueryByCodeResultResp(
    /**
     * 检测项目 - 这里是指具体的项目
     */
    var name: String?,

    /**
     * 检测结果
     */
    var result: String?,

    /**
     * 结果比值
     */
    var radioValue: String?,

    /**
     * 结果范围
     */
    var refer: String?,

    /**
     * T1
     */
    var t1Value: String?,

    /**
     * T2
     */
    var t2Value: String?,

    /**
     * T3
     */
    var t3Value: String?,

    /**
     * T4
     */
    var t4Value: String?,

    /**
     * C1V
     */
    var cValue: String?,

    /**
     * C2V
     */
    var c2Value: String?,

    /**
     * T1 Name
     */
    var t1ValueName: String?,
    /**
     * T2 Name
     */
    var t2ValueName: String?,
    /**
     * T3 Name
     */
    var t3ValueName: String?,
    /**
     * T4 Name
     */
    var t4ValueName: String?,

    /**
     * T1 Str
     */
    var t1ValueStr: String?,
    /**
     * T2 Str
     */
    var t2ValueStr: String?,
    /**
     * T3 Str
     */
    var t3ValueStr: String?,
    /**
     * T4 Str
     */
    var t4ValueStr: String?,
)