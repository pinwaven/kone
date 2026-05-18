# Kone (Kino-One) 核心检测流程与逻辑文档

本文档详细描述了 Kone 生物标志物分析仪的完整检测流程、业务逻辑及后端交互。旨在为将系统移植到其他硬件平台（如 ESP32/STM32）提供详细的技术蓝图。

---

## 1. 系统架构概览

Kone 应用程序是一个基于 **Jetpack Compose** 和 **Kotlin** 开发的 Android Kiosk 模式应用。

- **UI 层：** 管理用户输入、扫描进度显示和结果可视化。
- **编排层 (`WorkMainViewModel`)：** 控制检测过程的状态机。
- **执行层 (`WorkFlowV2`)：** 根据试剂卡类型定义硬件和 UI 的动作序列。
- **硬件层 (`SerialPort` & `CtlCommandsV2`)：** 处理与底层硬件的串口通信。
- **服务层 (`BAAService` & `AppCardUtils`)：** 负责信号处理、结果计算及生物年龄 (BAA) 分析。
- **后台集成 (`FrosApi` & `SbEdgeFunc`)：** 管理病人报告、试剂卡配置及数据同步。

---

## 2. 检测生命周期（状态机）

检测流程由 `WorkMainViewModel` 中的 `action` 状态管理。

### 阶段 1：准备与识别
1.  **状态 `ACTION_CASE_SAMPLE`：** 初始状态。提示用户准备样本并插入试剂卡。
2.  **卡片检测：** 系统轮询 GPIO (`gpioRead`) 以检测卡片是否插入。
3.  **二维码扫描：** 一旦检测到卡片，触发硬件二维码扫描器 (`readQR`)。
4.  **元数据获取：**
    - 将二维码发送至后台 (`SbEdgeFunc.getCardInfo`) 以获取 **试剂卡配置** (`CardConfig`)。
    - 从 `FrosApi.getPatientCaseReport` 获取病人信息和历史报告。
5.  **工作流生成：** 根据测试类型（如 CRP, IGE, BAA）生成 `WorkFlowV2` 动作序列。

### 阶段 2：反应与扫描（硬件执行）
`WorkFlowV2` 执行一系列动作（UI 等待或硬件指令）：
1.  **复位 (Homing)：** 重置托盘位置。
2.  **反应等待 (`ACTION_CASE_WAIT`)：** UI 显示初始反应倒计时（例如 `ft0` 时间）。
3.  **吸液 (`absorb`)：** 硬件开启吸液阀，持续指定时间 (`xt1`)。
4.  **扫描 (`scan`)：** 硬件驱动激光/传感器在卡片上移动。扫描持续时间通常为 16 秒。
5.  **数据读取 (`queryData`)：** 扫描完成后，从硬件缓冲区拉取原始 ADC 采样数据。

### 阶段 3：处理与完成
1.  **数据解析：** 将原始二进制数据解析为 13 位 ADC 数值（详见第 4 节）。
2.  **结果计算：** `AppCardUtils` 计算峰面积，并应用多项式公式确定生物标志物浓度。
3.  **BAA 分析（可选）：** 若为生物年龄测试，通过 `BAAService` 根据多个生物标志物输入计算“生理年龄加速”。
4.  **报告生成：** 结果保存至本地 Room 数据库并上传至服务器 (`FrosApi.uploadPatientReportDataToServer`)。
5.  **状态 `ACTION_REPORT1`：** 向用户展示最终结果。

---

## 3. 硬件通信协议

### 底层串口帧结构
对于 ESP32/STM32 移植，串口协议遵循以下帧格式：

| 字段 | 大小 | 描述 |
| :--- | :--- | :--- |
| **帧头 (Header)** | 1 字节 | 固定值 `0x21`。 |
| **指令 (Command)** | 1 字节 | 指令标识符（如 `CMD_SCAN`, `CMD_ABSORB`）。 |
| **载荷 (Payload)** | 可变 | 字符串编码的参数（例如 `SID=123,SPEED=600`）。 |
| **校验和 (Checksum)** | 1 字节 | 针对 [帧头 + 指令 + 载荷] 计算的 **CRC-8**。 |

### 核心指令 (`CtlCommandsV2`)
| 指令 | 用途 |
| :--- | :--- |
| `homing()` | 将托盘移动到复位/弹出位置。 |
| `moveToSs(pos, speed, mode)` | 将托盘移动到特定的内部位置。 |
| `gpioRead()` | 读取硬件引脚状态（如卡片传感器）。 |
| `readQR()` | 触发二维码/条码扫描器。 |
| `absorb(timeMs)` | 开启吸液阀，持续指定时长。 |
| `scan(start, duration)` | 开始激光扫描过程。 |
| `queryData()` | 读取采样后的 ADC 数据（二进制）。 |

---

## 4. 信号处理与数据解析

### 原始数据格式 (ADC 采样)
硬件返回一个带有 10 字节消息头的二进制包：
1.  `dataLen` (2 字节, 小端模式): ADC 数据点数量。
2.  `laserCurr` (2 字节): 原始激光电流。
3.  `fixed5a` (1 字节): `0x5A`。
4.  `dataBias` (2 字节): ADC 基准偏置。
5.  `laserCurrBias` (2 字节): 激光电流偏置。
6.  `fixedA5` (1 字节): `0xA5`。

**ADC 数值：** 消息头之后是 `dataLen` 个点。每个点是一个 2 字节整数，代表一个 13 位的 ADC 采样值。

### 计算逻辑 (`AppCardUtils`)
1.  **峰值校准 (`findRealMinAndMaxTopList`)：** 系统不会直接使用 `CardConfig` 中的静态窗口，而是通过算法寻找真实的生物信号边界：
    - **起始边界搜索：** 从配置的 `start` 索引开始，向后移动直到信号不再减小（寻找局部极小值）。
    - **稳定检查：** 使用梯度计数器（默认 3-5 个点）忽略微小的噪声波动，确保达到稳定的基线。
2.  **峰面积：** 在校准后的起始/结束边界内，计算基线之上的信号积分。
3.  **多项式应用：** $结果 = \sum (coeff_n \cdot x^n)$，其中 $x$ 是计算出的面积或比例。

---

## 5. 生物年龄分析 (BAAService)

对于生物年龄分析 (BAA) 类型的测试，系统会计算“生理年龄加速”评分。

### 计算原理
生物年龄由实际年龄（Chronological Age）加上计算出的“加速”值（BAA）得出。
$$生物年龄 = 实际年龄 + BAA$$

### BAA 公式
BAA 是标准化的生物标志物数值的加权和：
$$BAA = \sum_{i=1}^{n} (coeffENET_i - coeffSexAge_i) \cdot (Value_i - Mean_i) \cdot 10$$
其中：
- $coeffENET_i$: 该标志物的 Elastic Net 系数。
- $Value_i$: 测量值（若 `useLogValue` 为 true 则取对数）。
- $Mean_i$: 标志物的群体均值。

---

## 6. 端到端 API 调用序列

一个完整的测试会话必须遵循以下 API 调用顺序：

1.  **设备激活 (仅一次)：** `POST business/poct/device/activate` (SbEdge)。
2.  **卡片识别：** `POST api/service/poct/device/queryByCode` (Fros)，使用扫描到的二维码获取病人信息。
3.  **获取规格：** `POST business/poct/card/getInfo` (SbEdge)，获取 `CardConfig`（峰值范围、公式系数）。
4.  **设置状态：** `POST business/poct/card/updateStatus` (SbEdge) 设为 `checking`。
5.  **扫描与计算：** (硬件执行与本地算法阶段)。
6.  **上传结果：** `POST api/service/poct/device/uploadCheckData` (Fros)。
7.  **结束状态：** `POST business/poct/card/updateStatus` (SbEdge) 设为 `success` 或 `failed`。
8.  **BAA 结果（可选）：** `POST business/poct/baa/result` (SbEdge) 进行生物年龄计算。

---

## 7. 后端 API 参考与 JSON 结构

### Fros API
**正式环境 Base URL:** `https://fros-api.gyyyhospital.com`
**开发环境 Base URL:** `https://fros-api-dev.gyyyhospital.com`

#### `queryByCode` (获取病人/报告信息)
- **请求：** `{"code": "二维码字符串"}`
- **响应关键字段：** `patient` (包含 `objectId`, `name_`, `gender`, `birthDate` 的对象)。

#### `uploadCheckData` (上传结果)
- **请求结构：**
  ```json
  {
    "code": "二维码字符串",
    "status": "completed",
    "date": "YYYY-MM-DD HH:mm:ss",
    "result": [
      {
        "name": "项目名称",
        "result": "最终值 (如 5.2)",
        "t1Value": "T1 原始面积",
        "cValue": "C 原始面积",
        "t1ValueStr": "显示字符串"
      }
    ]
  }
  ```

### SbEdge API
**正式环境 Base URL:** `https://supabase.virtualhealth.cn/functions/v1`
**开发环境 Base URL:** `https://sb.fros.cc/functions/v1`

#### `getInfo` (获取试剂卡规格)
- **响应内容：** 包含 `cardConfig`，其中包括 `topList`（峰边界）和 `varList`（多项式系数）。

---

## 8. MCU (ESP32/STM32) 移植注意事项

### 数据映射细节
在解析后台响应时，必须注意字段名的大小写转换（Android 端使用 GSON 自动处理）：
- `objectId` -> `object_id`
- `birthDate` -> `birth_date`
- `BioAge` -> `bio_age`
- `Scores` -> `scores`

### 硬件与资源要求
- **内存管理：** 一次扫描产生约 **130KB** 二进制数据。确保 MCU 有足够的 PSRAM 或采用流式上传。
- **网络栈：** 所有 API 必须支持 **HTTPS (TLS 1.2+)**。User-Agent 建议设置为 `HttpUtils/1.0`。
- **设备身份：** 设备通过 `deviceId` 进行标识。在调用除激活外的其他接口前，必须先完成激活。
