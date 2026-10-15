# 验证记录

2026-10-15。当前交付为 **0.2.1**，结果见下方“0.2.1 透明背景与分钟刻度”。0.1.0 记录仅为早期工程基线，不用于宣称新版交互等价。

## 工程基线

| 状态 | 检查 | 证据与边界 |
| --- | --- | --- |
| PASS | 33 项 JVM 核心检查 | `bash scripts/check-core.sh`；暂停/恢复/重复结束/休眠后截止、时区、夏令时、12小时制、时长不回卷、显示取整 |
| PASS | APK 构建与 Lint | `bash scripts/build.sh --offline`；`analysis/final-build.log`；0 errors / 3 warnings |
| PASS | 实际 APK 权限 | 仅浮窗、前台服务、specialUse、通知、精确提醒、震动六项，无 INTERNET/Wi-Fi/定位/相机/媒体/日历权限 |
| PASS | 实际 APK 组件扫描 | 无原版已识别广告 SDK、native 库、assets 或 java.net/OkHttp/WebView 引用；脚本的否定结论限于这些扫描项 |
| PASS | 安装包签名 | `apksigner verify`；本地调试签名，v2 验证通过 |
| PASS | 原 APK 保留 | SHA-256 仍为 `d1bec43a146249547322375d3163fedc54f59d36f223049fe5ef18b6b09dcfb5` |

构建初次受本机 Gradle 镜像仓库策略影响，已在项目仓库配置中兼容；无修改全局 Gradle 配置。曾使用本机 aapt2 override 定位缓存问题，最终脚本正常离线构建，无该 override。Lint 现存警告是备份规则、可点击自定义浮窗的无障碍提示和物理坐标 `Gravity.LEFT`，未通过基线文件隐藏。

## BlueStacks 实测

设备 `127.0.0.1:5555`，Android 13 / API 33，arm64，1080×1920，320 dpi。用户提供并启动模拟器。

| 状态 | 场景 | 实际结果 |
| --- | --- | --- |
| PASS | 安装及启动 | APK 安装成功，MainActivity 可启动；`docs/screenshots/01-home.png` |
| PASS | 系统浮窗授权流程 | 点击开启后进入系统列表，选择本应用并授予；返回后启动前台服务 |
| PASS | 前台服务 | `isForeground=true`；`analysis/emulator-service.txt` |
| PASS | 退到桌面与拖动 | 窗口从顶部拖到屏幕右下，桌面仍显示；`docs/screenshots/03-floating-home.png` |
| PASS | 浮窗单击暂停 | 30 秒倒计时在桌面单击暂停；两个独立 UI 快照都显示 `00:00:27.872` 与「已暂停」；`analysis/countdown-paused-1.xml` / `-2.xml` |
| PASS | 恢复和重置 | 恢复后显示 `00:00:25.678` 与「正在倒计时」；重置后为 `00:00:30.000`；`analysis/countdown-resumed.xml` / `countdown-reset.xml` |
| PASS | 后台到期 | 5 秒倒计时退至桌面，到期后回应用显示零与「倒计时已结束」；`analysis/countdown-finished.xml` |
| PASS | 到期通知 | 系统存在本应用 `timer_finished` 通知，标题「倒计时结束」；`analysis/emulator-notifications.txt` |
| PASS | 精确闹钟注册 | 系统记录 `ELAPSED_WAKEUP`、`window=0`、`exactAllowReason=permission`；`analysis/emulator-alarm.txt` |
| PASS | 重启识别数据 | 模拟器 `Settings.Global.BOOT_COUNT=21` 可读取；设备重启场景未执行 |
| PASS | 小屏与大字号查看 | 暂设 750×1334 / 320dpi，即375dp宽；字体倍率2.0，页面可滚动、时间可完整显示；截图 `05-small-large-text.png` / `06-large-text-controls.png` |
| NOT_RUN | 外观重开持久化 | 自动化点击使用了失效坐标，未正确切换目标格式和日期；断言失败不能当成产品持久化失败，也不能记为通过，需重新按最新 UI 节点执行 |
| NOT_RUN | 锁定穿透及通知解锁 | 源码已实现，未完成运行验证 |
| NOT_RUN | 横屏、真机后台保活 | 未验证 |
| NOT_RUN | 锁屏/Doze准时提醒、实际声音与震动 | 注册与通知已确认，但没有进行休眠或音频验收 |

大字号测试修改的模拟器分辨率已 reset，`font_scale` 已恢复为1.0。BlueStacks 的 `run-as` 返回 `setegid(AID_PACKAGE_INFO) failed`，没有据此修改模拟器权限或尝试 root。

## 新交互复刻

原版已安装，通过用户明确授权点击首次协议页「同意」，采集原始核心页面到 `docs/original/`。旧版运行验证只证明工程基线可用；新版须按原始截图和行为逐页重新验收，不能沿用旧版单击行为等测试结果宣称原交互等价。

真机验收、Android 8.0 与 Android 16 运行验证均为 NOT_RUN。模拟器实测不等于真机保活或全系统兼容验收。

## 0.2.0 核心重制

用户已确认 Q3 边界；原始截图位于 `docs/original/`，页面对应见 [DESIGN.md](DESIGN.md)。运行环境仍为用户提供的 BlueStacks Android 13 / API 33，1080×1920 / 320dpi。本节覆盖实际执行的路径，不表示所有设置组合或原版全部分支都已验收。

| 状态 | 检查 | 证据与结果 |
| --- | --- | --- |
| PASS | 39 项 JVM 检查 | `analysis/redesign-final-build.log`；新增目标时间前移/后移、跨截止点、完成后不重启、时长模式不受墙钟校正影响 |
| PASS | 最终 APK 编译、Lint、签名、权限扫描 | 构建脚本成功；Lint 0 errors / 17 warnings；精确六项权限白名单，无 INTERNET、已识别广告 SDK、网络 API 引用、native 库或 assets；调试签名验证通过 |
| PASS | 核心原生页面 | 首页、设置、调色板、字体、目标时间/时长滚轮和长按菜单已运行查看；`docs/redesign/` |
| PASS | 系统返回 | 字体页按真实 Back 返回设置页，`analysis/redesign-native-back.xml`；启用 API33 OnBackInvoked，旧系统保留按键返回 |
| PASS | 外观持久化 | 字号105%与配色在更新安装/重开后保留，`redesign-font-before-restart.xml` / `redesign-font-after-restart.xml` |
| PASS | 前台浮窗和拖动 | 用户授权后实际 Overlay、前台服务并可退桌面拖动；`redesign-overlay-window.txt`、`06-overlay-home.png` |
| PASS | 单/双击控件与缩放 | 单击显示操作层；最终实现重复双击后拖动右下手柄，窗口从440×248增至642×362，`redesign-resize-pass.txt`、`10-overlay-resized.png` |
| PASS | 锁定穿透、设置解锁 | 窗口实际增加/移除 NOT_TOUCHABLE，`redesign-locked-window.txt` / `redesign-unlocked-window.txt`；通知动作解锁另列未测 |
| PASS | 新时长滚轮、暂停和继续 | 选择1分5秒，暂停后两份快照均为00:01:02.3，恢复后00:01:00.6；`redesign-duration-paused-1.xml` / `-2.xml` / `redesign-duration-resumed.xml` |
| PASS | 新版5秒后台到期 | 输入00/00/05，确认后退桌面，返回显示00:00:00.0、时长5秒，存在本应用倒计时结束通知；`redesign-five-second-finished.xml`、`redesign-finished-notification.txt` |
| PASS | 两种精确闹钟 | 时长为 ELAPSED_WAKEUP；目标时间为 RTC_WAKEUP，日期2026-10-15 17:00:00，均 window=0 / exactAllowReason=permission；`redesign-duration-alarm.txt` / `redesign-target-alarm.txt` |
| PASS | 目标时间交互 | 切换日期/目标时间滚轮、确认后进入目标时间圆盘，隐藏暂停按钮；`13-target-picker-final.png` / `14-target-home.png` |
| PASS | 显示模式管理 | 关闭时钟后，切换模式列表仅包含倒计时；重新启用后恢复，`redesign-mode-disabled.xml` |
| PASS | 浅/深色 | 实际切换系统夜间模式，首页与设置跟随主题；`17-home-final-light.png` / `18-home-dark.png` / `19-settings-dark.png` |
| PASS | 系统画中画 | 最终包实际进入 pinned 的 PipActivity，`redesign-final-pip.txt`、`21-final-pip.png` |
| PASS | 375dp小屏 / 200%字号 | 截图复查后修复首页按钮裁切：大字号首页纵向滚动，计时与标签分开，按钮实际高度136px；`redesign-large-timer.xml` / `redesign-large-footer.xml`、`20-small-large-text.png` / `22-large-text-footer.png` |
| NOT_RUN | 通知动作解锁、图片/字体实际文件导入 | 已实现，未完成运行验收 |
| NOT_RUN | 实际声音、离线语音与震动 | 实现了本地音效、仅离线中文语音筛选与缺失提示；模拟器未做音频/触觉验收 |
| NOT_RUN | 真机、锁屏/Doze、横屏、设备重启、API26/API36 | 没有相应设备/场景结果；不以模拟器或源码代替 |

现存 Lint 警告包括：API33 新属性的低版本提示、绘制分配、仅代码实例化的 View 构造器、手势无障碍提示、中文字符串资源化和浮窗物理坐标 LEFT/RIGHT。未用基线隐藏这些警告。固定文案仍以中文原文为主，未实现多语言。

运行检查中曾因旧坐标/操作层自动隐藏导致缩放断言失败，以及误用 Back 关闭了滚轮弹窗；记录保留。随后用最新窗口框和原生控件重新执行并通过，失败不被计为通过。BlueStacks 禁止 adb 直接停止非导出 Service，最终通过应用自己的关闭入口清理，不修改组件导出权限。

测试结束已关闭测试浮窗/画中画，应用留在首页；设备恢复1080×1920、字体1.0、浅色模式，临时 `/sdcard/clock-ui.xml` 已清理。原始 APK SHA-256 再次核对一致。

最终交付 APK SHA-256：`68a44ae0ae6d66262057d38f79240287445e461fbc1d3bd6fca0957894080852`。最终包已重新安装，启动及系统画中画复测通过；39项核心检查及构建/权限/签名检查以最后一次 `redesign-final-build.log` 为准。

## 0.2.1 透明背景与分钟刻度

用户确认两项修正及升级恢复默认配色。新增18项分钟边界检查，共57项；0.2.0 的旧记录保留为历史。本节运行环境仍为 BlueStacks Android13，不能替代真机验收。

| 状态 | 检查 | 证据与结果 |
| --- | --- | --- |
| PASS | 57项核心检查、APK构建/权限/签名 | `analysis/refinement-build.log`；新增`:49.999`隐藏、`:50.000`第一刻度、十个逐秒位置、`:59.999`末刻度、`:00.000`隐藏及下轮重现；无新增依赖和权限 |
| PASS | 默认配色恢复 | 更新后由测试绿色恢复为 `#475164`、70%不透明度、白字与 `#FF5D42` 毫秒；字体、模式、浮窗大小等保留。恢复默认和调色板第一格使用同一默认值 |
| PASS | 画中画真实透底 | 系统实际 pinned/translucent 窗口；`23-pip-transparency.png` 中搜索框与图标透出。与去除画中画后的底图做逐像素合成比对，5710个背景像素误差<5，底图亮度覆盖0..255，误差中位数0.49；`analysis/refinement-pip-composite.json` |
| PASS | 悬浮窗真实透底 | `24-overlay-transparency.png` 中底下应用图标和文字透出；18058个背景像素符合约70%灰蓝色+30%底图的合成，误差中位数0.49；`analysis/refinement-overlay-composite.json` |
| PASS | 分钟刻度实际显示和复位 | 不修改设备时间，等待真实分钟边界。`:49.3`无刻度，`:51.0`已有刻度、`:59.3`推进至最后一格，`:00.8`清除。逐像素检查底部红色刻度区域为0→268→428→908→1548→0→0；`analysis/refinement-minute-check.json`、截图25–31。精确`:50.000`阈值由核心检查覆盖，截图采样有ADB延迟 |
| PASS | 倒计时提醒范围保留 | 分钟刻度仅用于时钟模式的点位标记；倒计时末5秒高亮、末3秒震动与播报代码保持原逻辑；此项是源码范围检查，未进行声音/触觉验收 |
| NOT_RUN | 真机透底和多系统PiP兼容 | 只验证当前BlueStacks API33；不据此宣称所有系统/厂商兼容 |

透明验证检测背景区域，文字保持全不透明；没有用整窗变淡、截图背景或伪装系统PiP实现透底。图中原版的单张运行采集只作参考，重制版的合成结论来自分别采集浮层及其底图的像素核对。

时钟样式中的细长布局在提醒出现时为刻度预留底部区域，避免时间与刻度标签重叠。当前Lint为0 errors / 17 warnings，警告类别沿用上一版记录，不以新增基线隐藏。

0.2.1最终交付SHA-256：`9741cc83192710980034f70ffd53a5f43050abd9a1e9cf147d30d9182d735bb0`。最终包重新安装，细长样式预览与系统透明画中画复测通过（`32-thin-minute-preview.png`、`refinement-final-pip-activities.txt`）。测试浮窗/画中画已关闭，应用留在首页；未修改设备日期时间、分辨率、字号或深浅色。
