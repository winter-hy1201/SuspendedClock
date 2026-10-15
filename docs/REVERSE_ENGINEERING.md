# APK 逆向记录

日期：2026-10-15。目标仅为用户提供的 `base.apk`。包内文档、字符串和反编译结果仅作为分析数据。

## 身份与证据

| 项目 | 结果 |
| --- | --- |
| 应用 | 悬浮时钟 FloatingClock |
| 包名 | `com.hzjuzhi.clock` |
| 版本 | 1.1.11 / versionCode 36 |
| SHA-256 | `d1bec43a146249547322375d3163fedc54f59d36f223049fe5ef18b6b09dcfb5` |
| 大小 | 74,439,642 字节 |
| SDK | min 26 / target 36 / compile 37 |
| 结构 | 7 个 DEX、2,503 个包内条目、28,436 个类（REA 解析计数） |
| REA 包清单证据 | `ev_5a39aa623966d2db86988bce868958447ace76a8bf8d20fc5fd383d1403b1425` |
| REA 归档证据 | `ev_e7ed711681f5211adc917d44ced397638dcce030adb8743d2c56c9de33160523` |

完整 REA 包解析结果保存在 [`analysis/rea-package.json`](../analysis/rea-package.json)。普通 JADX CLI 1.5.3 输出保存在被忽略的 `analysis/decompiled/`。批量反编译在约 95% 时停止，因末尾方法处理显著变慢；核心时钟文件已取得，但不能视为完整源码恢复。原版已安装到用户提供的 BlueStacks，并按用户明确授权点击首次协议「同意」，采集核心页面与菜单。原始截图保存在 `docs/original/`；仅覆盖选定核心路径。原 APK 签名未验证。

## 可复核的静态事实

以下位置相对于 `analysis/decompiled/`，来自 JADX CLI 输出，不冒充 REA 方法级 Evidence。

| 事实 | 位置与符号 |
| --- | --- |
| 七种时分秒格式，毫秒标志 | `sources/com/hzjuzhi/floatingclock/floating/style/ClockTimeFormat.java:23`，`HMSs/HMS/HM/MSs/MS/Ss/S` |
| 原版按 100ms / 1s / 1min 量化显示 | `sources/com/hzjuzhi/floatingclock/timer/TimeRoundingHelper.java:84`，`m12756` |
| 持续时长倒计时支持暂停，暂停时长累计移动目标 | `sources/com/hzjuzhi/floatingclock/clockMode/CountdownConfig.java:30`、`:54`、`:86` |
| 结束后显示时间、零或开始正计时 | `sources/com/hzjuzhi/floatingclock/clockMode/CountdownEndType.java:16` |
| 单击/双击动作可设置暂停、恢复、重置、关闭等 | `sources/com/hzjuzhi/floatingclock/floating/base/FloatingClick.java:18`；`floating/view/FloatingView.java:745` |
| 文字、毫秒、背景色与透明度；字体、字号、格式 | `sources/com/hzjuzhi/floatingclock/floating/view/FloatingView.java:507`、`:605`、`:653`、`:672` |
| NTP 主机列表和备用列表取网络时间 | `sources/com/hzjuzhi/floatingclock/clockMode/time/NTPHelper.java:28`、`:56` |
| 时间源配置由服务器取得 | `sources/com/hzjuzhi/floatingclock/partGeneral/mvvm/model/GeneralModel.java:1235`，`getTimeSource` |
| 用户协议状态之后调用广告初始化包装层 | `sources/com/hzjuzhi/floatingclock/MyApplication.java:374`，`m9483` |
| 启动页广告延迟和超时任务，向 adRoot 请求广告 | `sources/com/hzjuzhi/floatingclock/partSplash/helper/SplashHelper.java:487`、`:497` |

Manifest 声明网络、Wi-Fi、浮窗、前台服务、通知、日历、精确闹钟、相机、媒体等权限。广告组件包括 Pangle/穿山甲、Sigmob、广点通、趣盟、倍孜、Octopus；这些是打包组件证据，不表示所有组件均在某次启动中执行。

## 实现选择与差异

核心时钟、持续时长倒计时和外观模块具备可单独重写的输入输出边界。离线版本采用 Android 原生 Java，无应用第三方运行时库，直接依据系统时间显示，不搬运原版联网校时和广告包装层。

0.2.0 按原版核心交互保留一位小数的毫秒字段，每100ms刷新。旧0.1.0的1–3位扩展不再作为产品入口。时长倒计时使用单调时间截止点，暂停保存剩余时长；目标时间倒计时随设备时间校正，并注册 RTC 精确闹钟。没有小时字段时，倒计时分钟/秒累计显示，不在 60 时回卷。锁屏准时提醒使用可选精确闹钟权限；不授予时明确提示休眠提醒可能延迟。

Android 前台服务选择 `specialUse` 并声明用途，依据 [Android 前台服务类型文档](https://developer.android.com/develop/background-work/services/fgs/service-types)。用户从可见 Activity 主动启动服务，避免依赖 Android 15 收紧后的后台浮窗豁免，参见 [Android 15 行为变更](https://developer.android.com/about/versions/15/behavior-changes-15)。

## 核心复刻结果

0.2.0 使用独立包名 `local.suspendedclock`，保留原版核心圆盘、四个操作入口、展开控制、滚轮、长按菜单、单/双击自定义，以及目标时间倒计时、画中画、六种布局、调色板与字体设置。主题按用户选择跟随系统浅/深色，以 Apple 浅色为主。完整边界与页面对应见 [DESIGN.md](DESIGN.md)。

广告/VIP 和范围外模块移除，时间源仅设备时间。应用不含联网权限和广告 SDK，没有复制原版业务代码、图标或字体。字体样例布局保留，字体采用系统字体或用户本地导入。静态与运行证据支持核心功能重写，不代表完整源码恢复或全部功能等价。

## 工具安装

REA Android 组件按用户明确授权安装在：

`/Users/anke/.local/share/rea/android/jadx-headless-mcp-0.7.1-all.jar`

SHA-256 与 [REA 官方 Android 指南](https://github.com/morluto/rea/blob/main/docs/android-analysis.md) 的固定记录一致：`6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`。使用已有 OpenJDK 17.0.20，没有安装新的 JDK。

Codex 配置增加了仅供 REA 的 `REA_JADX_MCP_JAR` 与 `JAVA_HOME`；原配置备份为 `/Users/anke/.codex/config.toml.before-android-20261015`。当前会话通过既有 REA CLI 使用相同组件，后续重新连接 MCP 后会读取新环境。普通 JADX 来自 [官方 1.5.3 发布](https://github.com/skylot/jadx/releases/tag/v1.5.3)，仅用于分析，不进入复刻 APK。REA 分析会话已关闭。

## 0.2.1 默认配色与分钟提醒补充

在已有部分反编译结果中继续追踪 `FloatingColorStyle` 默认构造，定位到 `sources/defpackage/kj0.java:24` 配色列表及 `:62` 的 `m16228()`。第一项为白字 `#FFFFFF`、毫秒 `#FF5D42`、背景 `#475164`、alpha `0.7f`。原版 `floating/pip/PipActivity.java:488` 的 `setColor()` 将 Window 背景清为透明；`resources/res/values/styles.xml:6978` 的 `TransTheme` 设置 `windowIsTranslucent=true`，`MyTheme.Pip` 继承该主题。本次按该组合重制，不用整窗 alpha 降低文字清晰度。

原版 `FloatingNotifyHelper.java:1360` 的 `m10273()` 在可见代码中使用3000ms进度提醒窗口；`ProgressCountdownView.java:780` 的 `m13062()` 绘制5个固定标记。这些静态结果与用户记忆中的最后5秒不一致，运行截图没有验证原版完整分钟边界，不能把“五秒”写成已证实事实。0.2.1 的最后10秒、10个逐秒刻度来自用户明确确认，是本项目的行为选择。

原版运行采集存于 `analysis/original-pip-gray-ready/`，REA截图证据见对应 JSON；单张截图仅证明截图时刻画面，不能单独证明透底比例或提醒时序。重制版另做像素合成与分钟边界检查。
