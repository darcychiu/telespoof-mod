# TeleSpoof

按 App 伪造 TelephonyManager / 基站 / Wi-Fi 信息的 LSPosed 模块。

> **状态：源码工程，尚未编译、未上机验证。** 当前这台 Windows 机器只有 platform-tools，没有 JDK / Android SDK / NDK，无法在这里出 APK。代码按 LSPosed 旧版 Xposed API（82）编写，需要你或 CI 编译。

## 覆盖范围

| 目标 | 实现 |
|---|---|
| `getSimOperator` / `getNetworkOperator` | 返回 `MCC+MNC` |
| `getSimOperatorName` / `getNetworkOperatorName` | 返回运营商名 |
| `getSimCountryIso` / `getNetworkCountryIso` | 返回 `us` 等 |
| `getSimState` / `isNetworkRoaming` / `getPhoneType` / 网络类型 | 返回 READY / false / GSM / LTE |
| `getAllCellInfo` | 尝试反射构造 `CellInfoLte` 返回假基站；失败则返回空列表（隐藏真实基站） |
| `getCellLocation` | 返回 `null`（隐藏） |
| `WifiManager.getScanResults` | 返回空列表（隐藏周围热点） |
| `WifiInfo.getSSID` / `getBSSID` | 返回配置值 |

## 构建

### 方式 A：GitHub Actions（不用装环境）

1. 新建一个空仓库（public/private 都行）；
2. 把本目录**里面的内容**传到仓库根目录（要包含 `.github/`、`app/`、`build.gradle` 等，`build.gradle` 必须在根目录，不能多套一层文件夹）；
   - 网页方式：仓库页 → Add file → Upload files → 把 `telespoof` 文件夹里的所有内容拖进去 → Commit；
   - 或 git：
     ```
     cd telespoof
     git init && git add . && git commit -m "init"
     git branch -M main
     git remote add origin https://github.com/<你的用户名>/<仓库名>.git
     git push -u origin main
     ```
3. 打开仓库的 **Actions** 标签，左侧选 `build`；push 会自动触发，没触发就点 **Run workflow**；
4. 等 2–4 分钟，进入这次 run，页面底部 **Artifacts** 下载 `TeleSpoof-debug.zip`，解压得到 `app-debug.apk`。

### 方式 B：Android Studio

用 Android Studio 打开本目录，等 Gradle 同步完成，`Build > Build APK(s)`。

依赖：Xposed API 82 的 jar 已经内置在 `app/libs/api-82.jar`，不需要联网拉 `de.robv.android.xposed:api:82`（jcenter 已关，直接依赖大概率拉不到）。

## 安装与使用

1. 装 **LSPosed**（Android 17 用 2.0.1+；Zygisk 用 Magisk 内置或 Zygisk Next）；
2. 安装 `app-debug.apk`，在 LSPosed 里启用 **TeleSpoof**；
3. 作用域勾选目标 App；Google 系要额外勾 **GMS**（`com.google.android.gms`）；
4. 打开 TeleSpoof，填 MCC/MNC/国家/运营商/基站/Wi-Fi 值，保存；
5. 强制停止目标 App（设置 → 应用 → 强行停止），再打开；
6. 改完值后同样需要重启目标 App。

## 验证

- 目标 App 内看显示的运营商/国家；
- 或临时把作用域勾到一个能显示 SIM 信息的工具 App（如 DevCheck / Phone Info），看是否变成配置值。

## 限制与风险

- **未验证**：代码没编译过，也没在 Android 17 上跑过；首次构建大概率需要修一两处编译错误。
- 基站伪造依赖反射隐藏 API，可能被 Android 17 的 hidden API 限制拦下，届时只会“隐藏”不会“伪造”。
- 只影响被勾选的 App 进程；system_server、GMS 之外的系统组件不受影响。
- `getScanResults` 是清空而非伪造热点列表；要伪造需要自己构造 `ScanResult`（构造器同样是隐藏 API）。
- 卸载模块并重启即可完全恢复，不改任何分区。

## 目录

```
app/src/main/java/com/example/telespoof/
  HookEntry.java    Xposed 入口
  Hooks.java        所有 hook
  SpoofConfig.java  配置读取/默认值
  MainActivity.java 配置界面
app/src/main/assets/xposed_init
.github/workflows/build.yml
```
