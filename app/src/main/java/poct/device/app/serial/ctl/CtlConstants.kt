package poct.device.app.serial.ctl

import info.szyh.socket.SocketConstants

object CtlConstants {
    const val UID = "serial"

    const val TYPE_CONTROL = SocketConstants.TYPE_CONTROL

    const val TYPE_FEEDBACK = SocketConstants.TYPE_FEEDBACK

    const val TYPE_RESPONSE = SocketConstants.TYPE_RESPONSE

    // 握手
    const val CMD_HANDSHAKE: Byte = 0x01

    // 自检
    const val CMD_SELF_CHECK: Byte = 0x02

    // 自建中断
    const val CMD_SELF_CHECK_STOP: Byte = 0x09

    // 手动自检
    const val CMD_SELF_CHECK_MANUAL: Byte = 0x10

    // 心跳
    const val CMD_HEARTBEAT: Byte = 0x03

    // 重置
    const val CMD_RESET: Byte = 0x04

    // 日志上传
    const val CMD_LOG: Byte = 0x05

    // 重置状态
    const val CMD_RESET_STATE: Byte = 0x06

    // 关机
    const val CMD_SHUTDOWN: Byte = 0x07

    // 低功耗
    const val CMD_LOW_POWER: Byte = 0x08

    // 片仓复位
    const val CMD_RESET_CASE: Byte = 0x21

    // 移出片仓（插入样本）
    const val CMD_MOVE_OUT: Byte = 0x22

    // 扫码
    const val CMD_SCAN_CODE: Byte = 0x23

    // 开始检测（包含片仓复位）
    const val CMD_START_CHECK: Byte = 0x24

    // 查询检测进度
    const val CMD_PROGRESS_CHECK: Byte = 0x25

    // 读取数据
    const val CMD_READ_DATA: Byte = 0x26

    // 停止检测
    const val CMD_STOP_CHECK: Byte = 0x27

    // 打开吸水阀
    const val CMD_OPEN_XS: Byte = 0x28

    // 关闭吸水阀
    const val CMD_CLOSE_XS: Byte = 0x29

    // 扫描测试点
    const val CMD_SCAN_TEST: Byte = 0x30

    // 移入片仓
    const val CMD_MOVE_IN: Byte = 0x31

    // 手动控制
    const val CMD_CTL_MANUAL: Byte = 0x40

    // 测试
    const val CMD_TEST_REQUEST: Byte = 0x50
    const val CMD_TEST_RESPONSE: Byte = 0x51

    // 读取系统信息
    const val CMD_READ_SYS: Byte = 0x76

    // 设置系统信息
    const val CMD_SET_SYS: Byte = 0x77

    // 设置系统信息
    const val CMD_ADJUST_CONFIG: Byte = 0x78

    // 设置系统信息
    const val CMD_ADJUST_START: Byte = 0x7A

    // 设置系统信息
    const val CMD_ADJUST_STOP: Byte = 0x7B

    //  进入升级功能
    const val CMD_CTL_UPGRADE: Byte = 0xF0.toByte()

    // 控制板重启
    const val CMD_CTL_REBOOT: Byte = 0xF1.toByte()

    // 控制板异常恢复
    const val CMD_CTL_RESET: Byte = 0xF2.toByte()

    const val PARAM_START = "Start"
    const val PARAM_END = "End"
    const val PARAM_PPMM = "PPMM"
    const val PARAM_MODE = "Mode"
    const val PARAM_UPGRADE_COUNT = "BKIdx"
    const val PARAM_UPGRADE_CRC = "Crc"
    const val PARAM_UPGRADE_TOTAL = "BKcnt"
    const val PARAM_UPGRADE_SIZE = "FileSize"
    const val PARAM_XS_TIME = "XsTime"
    const val PARAM_ACTION = "Action"
    const val PARAM_DELAY = "Delay"
    const val PARAM_SID = "Sid"
    const val PARAM_SPEED = "Speed"
    const val PARAM_POS = "Pos"
    const val PARAM_STEP = "Step"
    const val PARAM_ID = "ID"
    const val PARAM_VERSION0 = "Ver0"
    const val PARAM_VERSION1 = "Ver1"
    const val PARAM_FILE = "File"
    const val PARAM_STATE = "State"
    const val PARAM_AMOUNT = "All"
    const val PARAM_TAIL_INDEX = "Index"
    const val PARAM_DATA = "Data"
    const val PARAM_PROCESS = "Process"
    const val PARAM_OUT_POS = "PlatOutPos"
    const val PARAM_READ_POS = "PlatReadyPos"
    const val PARAM_CHECK_POS = "BeginCheckLen"
    const val PARAM_ENHANCE = "SensorEnhance"
    const val PARAM_LASER = "laserStrength"
    const val PARAM_TEST = "testContent"
    const val PARAM_TEST_SID = "testSid"
}