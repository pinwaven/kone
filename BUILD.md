# Kone 项目构建指南 (Build Guide)

本项目是一个基于 Android 的生物标志物分析仪设备软件 (Kone)。本文档指引您在 Linux (如 Ubuntu) 环境下完成项目的构建与打包。

---

## 🛠️ 环境准备

在开始构建之前，请确保您的开发环境已安装并配置好以下工具：

### 1. 安装 JDK 17
Android SDK 及最新的 Gradle 需要 **JDK 17** 或更高版本。
- **验证安装：**
  ```bash
  java -version
  ```
- **安装参考 (Ubuntu)：**
  ```bash
  sudo apt update
  sudo apt install openjdk-17-jdk
  ```

### 2. 安装与配置 Android SDK
构建至少需要 `compileSdk 35` 对应的 SDK 组件。

- **配置环境变量**（请将以下路径添加到您的 `~/.bashrc` 或 `~/.zshrc` 中，并执行 `source` 使其生效）：
  ```bash
  export ANDROID_HOME=$HOME/Android/Sdk
  export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
  ```
- **通过 sdkmanager 安装必要的 SDK 组件**：
  ```bash
  sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
  ```

---

## 🚀 开始构建

### 第一步：配置本地属性 (local.properties)
在项目根目录下创建 `local.properties` 文件，并指定 Android SDK 的路径：
```bash
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

### 第二步：授予 Gradle 脚本执行权限
默认情况下，`gradlew` 文件可能没有执行权限：
```bash
chmod +x gradlew
```

### 第三步：执行打包命令
运行以下 Gradle 任务来构建 Release 版本的 APK：
```bash
./gradlew app:assembleRelease
```

> [!NOTE]
> 如果构建提示 Gradle 文件权限不足，也可以直接使用 `bash` 解释器来运行：
> ```bash
> bash ./gradlew app:assembleRelease
> ```

---

## 📦 构建产物

编译成功的 APK 文件会输出在以下目录中：
```text
app/build/outputs/apk/release/
```

- **文件名命名规则：** 
  ```text
  poct.device.app_${versionName}_${versionCode}_${packageDate}.apk
  ```
  *示例：* `poct.device.app_1.0.37.7_1_20260527.apk`

---

## 查看 APK 文件签名

```text
apksigner verify --print-certs <APK PATH>
```
  *示例：* 
```bash
apksigner verify --print-certs app/build/outputs/apk/release/poct.device.app_0.3.0_3_20260602.apk
```

## ⚠️ 关键注意事项

### 1. 本地 AAR 依赖库
`app/build.gradle` 引用了 3 个关键的本地 AAR 包，这些文件已内置在项目的 [libs](file:///mnt/project/project/kone/app/libs) 目录下：
- `app/libs/iotLib-comm-debug-20240506201010-V1.0.0.aar`
- `app/libs/iotLib-presenter-debug-20240506201010-V1.0.0.aar`
- `app/libs/iotLib-socket-debug-20240520203741-V1.0.0.aar`

> [!WARNING]
> 编译时如果提示找不到这 3 个 AAR 库，请确认它们已正确放置在 `app/libs/` 目录下。

### 2. Keystore 签名配置
Release 包已配置了 `keystore.jks` 的签名信息，因此通过 `app:assembleRelease` 构建出来的 APK 是已签名的生产包。
- 签名证书文件为根目录下的 `keystore.jks`。
- 签名所需的密码和别名配置已包含在项目 Gradle 脚本中。
