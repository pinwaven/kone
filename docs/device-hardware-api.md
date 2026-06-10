# WLK 设备命令 API

本文档以当前 `Core/Inc/cmd.h`、`Core/Src/cmd.c` 的实现为准。

## 1. 串口与帧格式

### 1.1 串口参数

| 参数 | 值 |
|---|---|
| 波特率 | 230400 |
| 数据位 | 8 bit |
| 停止位 | 1 bit |
| 校验位 | None |
| 设备地址 | `0x21` (`!`) |
| 命令接收缓冲区 | 64 字节 |

### 1.2 请求帧

```text
[ADDR] [CMD] [PARAMS ...] [CRC8]
```

- `ADDR` 固定为 `0x21`。
- CRC 覆盖 CRC 字节之前的全部内容，即 `ADDR + CMD + PARAMS`。
- 除寄存器写入值外，多字节命令参数均使用小端序。
- UART IDLE 被视为一帧结束；合法接收长度为 3 至 64 字节。

### 1.3 文本响应

大部分成功响应和命令参数错误响应采用：

```text
[0x21] [0x7C '|'] [TEXT ...] [CRC8]
```

CRC 覆盖响应中 CRC 字节之前的全部内容。

地址错误和 CRC 错误是当前实现中的例外：它们直接返回以 `?` 开头、以换行结尾的文本，不带地址前缀和响应 CRC。

```text
? my addr should: 0x21, but got: 0x22, len(3)\n
? crc error\n
```

## 2. CRC-8

当前 STM32 CRC 外设配置如下：

| 参数 | 值 |
|---|---|
| 宽度 | 8 bit |
| 多项式 | `0x07`，即 `x^8 + x^2 + x + 1` |
| 初始值 | `0x00` |
| xorout | `0x00` |
| 输入反转 | 按字节反转 (`refin=True`) |
| 输出反转 | 否 (`refout=False`) |

Python 示例：

```python
import anycrc

crc8 = anycrc.CRC(
    width=8,
    poly=0x07,
    init=0x00,
    xorout=0x00,
    refin=True,
    refout=False,
)

body = bytes([0x21, 0x01])
frame = body + bytes([crc8.calc(body)])
```

## 3. 固件信息

| 项目 | 当前值 |
|---|---|
| 固件版本 | `v0.1.7` |
| 最大请求帧 | 64 字节 |
| Bootloader 地址 | `0x08038000` |

版本记录：

- `v0.1.1`：初始版本
- `v0.1.2`：扫描增加激光预热计数，适配新归零位置
- `v0.1.3`：支持 Bootloader
- `v0.1.4`：吸收动作增加第二阶段
- `v0.1.5`：增加激光功率偏移命令
- `v0.1.6`：动态控制传感器电源
- `v0.1.7`：增加 TMC2300 时钟校准

## 4. 命令索引

| 命令 | 值 | 请求参数 | 说明 |
|---|---:|---|---|
| `CMD_POLL` | `0x01` | 无 | 查询动作与电机状态 |
| `CMD_HI` | `0x02` | 无 | 查询版本和 UID |
| `CMD_TEST` | `0x03` | 无 | 调试命令，不应作为稳定 API 使用 |
| `CMD_REG_WRITE` | `0x04` | node, reg, value | 写 TMC2300 寄存器 |
| `CMD_REG_READ` | `0x05` | node, reg | 读 TMC2300 寄存器 |
| `CMD_GPIO_READ` | `0x06` | 无 | 查询 GPIO 和 ADC |
| `CMD_GPIO_WRITE` | `0x07` | pin, value | 写 GPIO |
| `CMD_ACTION_CANCEL` | `0x08` | 无 | 取消当前动作 |
| `CMD_REBOOT` | `0x09` | type | 重启或进入 Bootloader |
| `CMD_LD_PWR_OFFSET` | `0x0A` | 可选 offset | 查询或设置激光 DAC |
| `CMD_ACTION_HOMING` | `0x40` | 无 | 归零 |
| `CMD_ACTION_MOVE_DURATION` | `0x41` | motor, velocity, duration | 定时运动 |
| `CMD_ACTION_MOVE_TO_SS` | `0x42` | motor, velocity, duration, sensor | 运动到传感器 |
| `CMD_ACTION_ABSORB` | `0x43` | duration | 执行吸收动作 |
| `CMD_ACTION_SCAN` | `0x44` | velocity, duration | 激光扫描 |
| `CMD_ACTION_QUERY_DATA` | `0x45` | 无 | 获取最近一次扫描数据 |
| `CMD_ACTION_READ_QR` | `0x46` | 无 | 扫描 QR 码 |

## 5. 基础命令

### 5.1 POLL (`0x01`)

请求：

```text
[21] [01] [CRC]
```

普通响应：

```text
!|a:<action_type>,s:<status>-<detail>,m:<h_state>-<v_state>
```

示例：

```text
!|a:0,s:IDLE-null,m:0-0
!|a:1,s:HOMING_V_MOTOR-moo,m:0-1
!|a:5,s:SCAN:1234-scan start,m:1-0
```

QR 动作运行或完成时使用专用格式：

```text
!|a:7,s:<status>-QR<qr_length>:<qr_data>,m:<h_state>-<v_state>
```

- 运行中只报告 QR 长度，不复制 QR 数据。
- 完成后复制 QR 数据；受 256 字节发送缓冲区限制，数据可能被截断。

动作类型：

| 值 | 类型 |
|---:|---|
| 0 | NONE |
| 1 | HOMING |
| 2 | MOVE_DURATION |
| 3 | MOVE_TO_SS |
| 4 | ABSORB |
| 5 | SCAN |
| 6 | QUERY_DATA |
| 7 | READ_QR |

电机状态：

| 值 | 状态 |
|---:|---|
| 0 | IDLE |
| 1 | MOVING |
| 2 | STOP |
| 3 | ERROR |

动作最终状态字符串包括 `COMPLETED`、`ERROR`、`CANCELLED`。

### 5.2 HI (`0x02`)

请求：

```text
[21] [02] [CRC]
```

响应：

```text
!|ver:v0.1.7~<UID_W0><UID_W1><UID_W2>
```

三个 UID word 均以 8 位小写十六进制文本输出，共 24 个十六进制字符。

### 5.3 TEST (`0x03`)

当前响应为两个电机的速度校正系数：

```text
!|test:<h_correction_factor>,<v_correction_factor>
```

该命令用于调试，返回内容可能随固件修改。

## 6. TMC2300 寄存器命令

### 6.1 REG_WRITE (`0x04`)

请求帧固定 9 字节：

```text
[21] [04] [node:u8] [reg:u8] [value:u32 BE] [CRC]
```

注意：`value` 是大端序，这是当前协议中与动作参数不同的地方。

响应：

```text
!|reg_write:<node>,<reg>,0x<value>
```

### 6.2 REG_READ (`0x05`)

请求帧固定 5 字节：

```text
[21] [05] [node:u8] [reg:u8] [CRC]
```

响应：

```text
!|reg_read:<node>,<reg>,0x<value>
!|reg_read:<node>,<reg>,err:<error_code>
```

## 7. GPIO 命令

### 7.1 GPIO_READ (`0x06`)

响应：

```text
!|ss_p:<v>,ss_ud:<v>,ss_start:<v>,ss_stop:<v>,ss_card:<v>,ld_p:<v>,qr_tgl:<v>,qr_rst:<v>,adc:<adc1>-<adc2>
```

| ID | 名称 | 含义 |
|---:|---|---|
| 0 | `ss_p` | 传感器电源 |
| 1 | `ss_ud` | 垂直方向传感器 |
| 2 | `ss_start` | 起始传感器 |
| 3 | `ss_stop` | 停止传感器 |
| 4 | `ss_card` | 卡片传感器 |
| 5 | `ld_p` | 激光电源 |
| 6 | `qr_tgl` | QR 触发 |
| 7 | `qr_rst` | QR 复位 |

执行该命令时会停止 ADC1/ADC2 DMA，分别执行一次阻塞式 ADC 转换。

### 7.2 GPIO_WRITE (`0x07`)

请求帧固定 5 字节：

```text
[21] [07] [pin_id:u8] [pin_value:u8] [CRC]
```

响应：

```text
!|gpio_write:<pin_id>,<pin_value>
```

- `pin_id` 对应上表的 0 至 7。
- 实际输出只使用 `pin_value` 的最低位。
- 当前实现不会拒绝大于 7 的 `pin_id`，而是仅返回 echo，不执行 GPIO 写入；调用方应自行限制范围。

## 8. 动作命令

动作启动返回值在响应文本中以有符号十进制显示：

| 值 | 名称 | 含义 |
|---:|---|---|
| 0 | `A_REV_DOING` | 已接受 |
| -1 | `A_REV_BUSY` | 另一个动作正在运行 |
| -2 | `A_REV_BAD_PARAM` | 参数错误 |
| -3 | `A_REV_NEED_HOMING` | 尚未归零 |

动作状态通过 `POLL` 查询。同一时刻只能有一个运行中的动作。

### 8.1 CANCEL (`0x08`)

请求：

```text
[21] [08] [CRC]
```

响应：

```text
!|cancel:0
```

如果动作正在运行，其状态变为 `CANCELLED`。命令总会停止两个电机；扫描时还会停止 ADC DMA 并关闭激光。

### 8.2 HOMING (`0x40`)

请求：

```text
[21] [40] [CRC]
```

响应：

```text
!|homing:<result>
```

典型状态：

```text
HOMING_START
HOMING_V_MOTOR
HOMING_V_COMPLETE
HOMING_H_MOTOR
HOMING_H_COMPLETE
HOMING_COMPLETE
COMPLETED
```

归零完成后，扫描和吸收动作才可启动。

### 8.3 MOVE_DURATION (`0x41`)

请求帧固定 12 字节：

```text
[21] [41] [motor:u8] [velocity:i32 LE] [duration_ms:u32 LE] [CRC]
```

- `motor`: `0` 为水平电机，`1` 为垂直电机。
- `velocity`: 有符号目标速度。
- `duration_ms`: 运动时间，单位毫秒。

响应：

```text
!|move_dur:<result>.<motor>,<corrected_velocity>,<duration_ms>
```

固件会先将请求速度乘以对应电机的 `correction_factor`，因此响应中的速度可能与请求值不同。

### 8.4 MOVE_TO_SS (`0x42`)

请求帧固定 13 字节：

```text
[21] [42] [motor:u8] [velocity:i32 LE] [duration_ms:u32 LE] [sensor:u8] [CRC]
```

- 水平电机 `motor=0` 支持 `sensor=0/1`。
- 垂直电机 `motor=1` 仅支持 `sensor=0`。
- 其他组合返回 `A_REV_BAD_PARAM`。

响应：

```text
!|move_to_ss:<result>,<motor>,<corrected_velocity>,<duration_ms>,<sensor>
```

### 8.5 ABSORB (`0x43`)

请求帧固定 7 字节：

```text
[21] [43] [duration_ms:u32 LE] [CRC]
```

响应：

```text
!|absorb:<result>,<requested_duration_ms>
```

- 需要先归零。
- 固件内部将持续时间限制在 `1500..100000 ms`。
- 响应 echo 的是请求值，不是限制后的内部值。
- 动作内部将等待时间分为两个阶段执行。

### 8.6 SCAN (`0x44`)

请求帧固定 11 字节：

```text
[21] [44] [velocity:i32 LE] [duration_ms:u32 LE] [CRC]
```

响应：

```text
!|scan:<result>,<corrected_velocity>,<duration_ms>
```

- 需要先归零。
- `duration_ms` 必须为 `1..60000`。
- 固件会对水平电机速度应用校正系数。
- 扫描运行时，`POLL` 状态为 `SCAN:<sample_count>`。

### 8.7 READ_QR (`0x46`)

请求：

```text
[21] [46] [CRC]
```

响应：

```text
!|read_qr:<result>
```

QR 动作超时为 3000 ms。状态包括：

```text
QR_START
QR_TRIGGER_LOW
QR_SCANNING
QR_DATA_RECEIVED
QR_TIMEOUT
QR_COMPLETE
QR_ERROR
```

QR 内容通过 `POLL` 的 QR 专用响应返回。

## 9. 扫描数据查询 (`0x45`)

请求：

```text
[21] [45] [CRC]
```

无扫描数据时返回普通文本帧：

```text
!|no_data<CRC>
```

有数据时返回二进制帧：

```text
offset  size  field
0       1     0x21 ('!')
1       1     0x44
2       2     sample_count, uint16 LE
4       2     adc2_value, uint16 LE
6       1     0x5A
7       2     adc1_offset, uint16 LE
9       2     adc2_offset, uint16 LE
11      1     0xA5
12      2*N   ADC1 samples, N 个 uint16 LE
12+2*N  1     CRC8
```

总长度：

```text
12 + sample_count * 2 + 1
```

`sample_count` 最大为 4096，因此最大响应为 8205 字节。CRC 覆盖头部和全部 ADC1 数据。

注意：虽然请求命令是 `0x45`，当前二进制响应头第 1 字节由实现固定填写为 `0x44`。客户端应按当前线协议解析，不要将其误认为文本响应的 `|`。

## 10. 系统命令

### 10.1 REBOOT (`0x09`)

请求帧固定 4 字节：

```text
[21] [09] [type:u8] [CRC]
```

| type | 行为 | 响应 |
|---:|---|---|
| `0xBB` | MCU 系统复位 | `!|rebooting...` |
| `0xA0` | 跳转到 Bootloader | `!|entering bootloader...` |

固件发送带 CRC 的响应，等待约 25 ms 后执行复位或跳转。其他值返回：

```text
!|? bad reboot val: (<raw char>)
```

### 10.2 LD_PWR_OFFSET (`0x0A`)

查询当前 DAC 值：

```text
请求: [21] [0A] [CRC]
响应: !|ld_dac:<dac_value>
```

设置激光功率偏移：

```text
请求: [21] [0A] [offset:i8] [CRC]
响应: !|ld_dac_val:<dac_value>
```

`offset` 范围应为 `-128..127`。计算方式：

```text
ld_pwr_dac_offset = 1.0 + offset / 127 * LD_PWR_DAC_OFFSET_MAX
dac_value = LD_PWR_DAC_BASE * ld_pwr_dac_offset
```

响应返回设置后的 12 bit DAC 通道值，而不是原始 `offset`。

## 11. 常见参数错误

命令已通过地址和 CRC 检查，但命令未知或固定长度不符时，通常返回带 CRC 的文本帧：

```text
!|? bad cmd: (<raw cmd char>)
!|? bad gpio_write: (<raw cmd char>)
!|? bad reboot: (<raw cmd char>)
!|? bad ld_pwr_offset: (<raw cmd char>)
```

括号中的命令字使用 `%c` 输出，可能是不可打印字符，并非十六进制字符串。

## 12. 字节序汇总

| 字段 | 字节序 |
|---|---|
| 动作命令 `velocity` | 小端 |
| 动作命令 `duration_ms` | 小端 |
| 扫描数据头部中的 16 bit 字段 | 小端 |
| ADC1 样本 | 小端 |
| `REG_WRITE` 的 32 bit value | 大端 |

示例：水平电机以速度 1000 运行 2000 ms：

```text
[21] [41] [00] [E8 03 00 00] [D0 07 00 00] [CRC]
```