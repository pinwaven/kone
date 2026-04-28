package poct.device.app.serial.v2.ctl

object CtlConstantsV2 {
    const val UID = "serial"

    const val RESULT_SUCCESS_PREFIX = "!|"

    const val RESULT_ERROR_PREFIX = "?|"

    const val RESULT_HAS_DATA_PREFIX = "!D"

    const val RESULT_NO_DATA_PREFIX = "!|no_data"

    // 短指令
    // 系统状态轮询
    const val CMD_POLL: Byte = 0x01

    // 获取设备信息
    const val CMD_HI: Byte = 0x02

    // 测试命令（保留）
    const val CMD_TEST: Byte = 0x03

    // 写电机寄存器（保留）
    const val CMD_REG_WRITE: Byte = 0x04

    // 读电机寄存器（保留）
    const val CMD_REG_READ: Byte = 0x05

    // 读取GPIO状态（保留）
    const val CMD_GPIO_READ: Byte = 0x06

    // 写入GPIO状态（保留）
    const val CMD_GPIO_WRITE: Byte = 0x07

    // 取消当前动作
    const val CMD_ACTION_CANCEL: Byte = 0x08

    // 长指令
    // 归零操作
    const val CMD_ACTION_HOMING: Byte = 0x40

    const val CMD_ACTION_HOMING_STATUS_COMPLETED: Int = 85

    val HOMING_STATUS_MAP: Map<String, Int> = mapOf(
        "HOMING_START" to 0,
        "HOMING_V_MOTOR" to 15,
        "HOMING_V_COMPLETE" to 35,
        "HOMING_H_MOTOR" to 50,
        "HOMING_H_COMPLETE" to 70,
        "HOMING_COMPLETE" to 85,
        "COMPLETED" to 85
    )

    val HOMING_STATUS_RESULT_MAP: Map<Int, String> = mapOf(
        0 to "A_REV_DOING - 正在执行",
        1 to "A_REV_OK - 已完成",
        -1 to "A_REV_BUSY - 系统忙",
        -2 to "A_REV_BAD_PARAM - 参数错误"
    )

    // 定时运动 (保留)
    const val CMD_ACTION_MOVE_DURATION: Byte = 0x41

    const val CMD_ACTION_MOVE_DURATION_STATUS_COMPLETED: String = "COMPLETED"

    // 移动到传感器
    const val CMD_ACTION_MOVE_TO_SS: Byte = 0x42

    const val CMD_ACTION_MOVE_TO_SS_STATUS_COMPLETED: String = "COMPLETED"

    // 吸收操作
    const val CMD_ACTION_ABSORB: Byte = 0x43

    const val CMD_ACTION_ABSORB_STATUS_COMPLETED: String = "COMPLETED"

    // 激光功率
    const val CMD_LD_PWR_OFFSET: Byte = 0x0a

    // 激光扫描
    const val CMD_ACTION_SCAN: Byte = 0x44

    const val CMD_ACTION_SCAN_STATUS_COMPLETED: String = "COMPLETED"

    // 查询扫描数据
    const val CMD_ACTION_QUERY_DATA: Byte = 0x45

    // QR码扫描
    const val CMD_ACTION_READ_QR: Byte = 0x46

    const val CMD_ACTION_READ_QR_STATUS_COMPLETED: String = "COMPLETED"

    const val CMD_ACTION_READ_QR_STATUS_ERROR: String = "ERROR"

    const val CMD_ACTION_READ_QR_RESULT_NULL: String = "null"

    val READ_QR_STATUS_MAP: Map<String, String> = mapOf(
        "QR_START" to "开始扫描",
        "QR_TRIGGER_LOW" to "触发信号拉低",
        "QR_SCANNING" to "扫描中",
        "QR_DATA_RECEIVED" to "数据接收完成",
        "QR_COMPLETE" to "扫描完成",
        "QR_TIMEOUT" to "超时 (3秒)",
        "QR_ERROR" to "错误"
    )

    const val PARAM_SID = "Sid"
}