# 庄园手记 · Manor Companion

一个为《第五人格》玩家制作的 **Android 手动计时与复盘工具**。独立运行，支持悬浮计时与本地对局记录。

> 非官方项目。所有计时由用户手动触发；示例 60／90／120 秒不代表任何角色当前版本的技能数据。

## 功能

- **计时台**：1–8 个自定义计时器，名称可编辑，时长 1–3600 秒；开始、暂停、继续、重置，新一局批量重置。
- **悬浮窗**：手动授权后启动，可拖动、收起、展开；点击计时行开始／暂停，长按重置；和主界面使用同一份计时状态。
- **结束震动**：可关闭；不请求后台保活或精确闹钟权限。
- **对局记录**：阵营、胜平负、角色、地图、复盘笔记；支持编辑、确认删除、总局数与胜率统计，最多 200 条。
- **CSV 导出**：通过系统文件选择器保存，UTF-8 BOM，处理逗号、引号、换行和表格公式前缀。当前不支持导入。
- **离线保存**：无账号、无广告、无网络权限、无分析服务；不上传个人数据。

## 环境与使用

Android 8.0（API 26）及以上，手机／平板均可。不是 iPhone 安装包。

1. 打开应用，在计时台点“编辑”，按实际情况设置名称和秒数。
2. 观察到事件后，手动点“开始”。应用不会识别游戏画面或自动启动计时。
3. 点“开启悬浮窗”，按提示前往系统设置授权，返回应用后再点一次开启。
4. Android 13 及以上可允许通知，方便从通知栏关闭悬浮窗；拒绝通知权限也可以使用应用内功能和悬浮窗。
5. 对局结束后在“对局记录”里保存复盘。建议定期导出；卸载或清除应用数据后，本地记录会删除。

### 使用限制

- 不读取或修改游戏内存、文件、网络数据，不注入进程，不使用 Root、无障碍、录屏、自动点击或隐藏信息显示。
- 不修改内存不等于符合游戏所有规则。请自行确认当前游戏及赛事规则；项目不承诺零处罚风险。
- 部分游戏或手机系统会禁止／隐藏悬浮窗，此时使用应用内计时。系统可能在后台结束服务。
- 计时基于 `elapsedRealtime()`，不受手动修改日期影响。同一次开机中重开应用会校正计时；手机重启后原运行中的计时重置，暂停状态保留。
- 关闭悬浮窗并离开应用后不持续运行后台服务；锁屏、进程被结束时，结束提醒可能延迟，返回应用时会校正剩余时间。不是精确闹钟。
- 示例数值需要自行调整。本项目不维护角色技能数据库，也没有自动匹配游戏版本的能力。

## 从 GitHub 获取 APK

上传本项目文件到自己的 GitHub 仓库，推荐仓库名 `idv-companion`。

1. 在 GitHub 新建仓库，将解压后目录中的所有源码文件上传至仓库根目录，**包括 `.github` 等隐藏目录**；不要只上传 ZIP，也不要把项目额外嵌套一层。
2. 打开仓库 **Actions → Build Android APK**，等待自动构建。也可以点 **Run workflow** 手动执行；分支名 `main`／`master` 的推送会自动触发。
3. 绿色通过后，在该次运行的 **Artifacts** 下载 `manor-companion-debug`，解压得到 `app-debug.apk`。
4. 将 APK 传到安卓手机，按系统提示允许来自该安装来源的应用，然后安装。

Actions 产物是已使用开发用密钥签名的 **debug APK**，用于体验和测试。不同 CI 运行的开发密钥可能不同，更新安装如遇到签名冲突，需要卸载旧版（请先导出记录）。长期发布应使用自己的固定密钥生成签名版本；不要把私钥上传到仓库。

工作流只构建和上传运行附件，**不会自动创建公开 Release**。

## 本地构建

使用 Android Studio 导入项目根目录，安装 Android SDK Platform 35 和 Build Tools 35.0.0，使用 JDK 17 或 21。

构建版本固定为 Android Gradle Plugin 8.13.2 + Gradle 8.13，参考 [官方兼容表](https://developer.android.com/build/releases/agp-8-13-0-release-notes)。悬浮窗采用 [Android 前台服务 specialUse 类型](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)。

命令行（项目包含官方 Gradle Wrapper，会按固定版本下载构建工具）：

```sh
# Windows
.\gradlew.bat assembleDebug lintDebug

# macOS / Linux
sh gradlew assembleDebug lintDebug
```

构建时需要联网下载 SDK 和构建依赖；安装后的应用本身不需要联网。

APK 位置：`app/build/outputs/apk/debug/app-debug.apk`。

单独验证核心逻辑（仅需 JDK 17+，无需 Android SDK）：

```powershell
# Windows PowerShell
.\scripts\test-core.ps1
```

```sh
# macOS / Linux
sh scripts/test-core.sh
```

## 项目结构

```text
app/src/main/
  AndroidManifest.xml       权限、活动与悬浮服务
  java/io/github/manorcompanion/
    MainActivity.java       计时台、记录、导出、说明
    OverlayService.java     可拖动悬浮窗与前台通知
    AppStore.java           本机持久化与共享状态
    TimerState.java         纯 Java 倒计时模型
    Csv.java                CSV 字段转义
    Ui.java                 原生界面组件与配色
  res/                     主题与原创矢量图标
tests/CoreTest.java         核心行为检查
scripts/                    本地测试入口
.github/workflows/          APK 自动构建与 lint
docs/DEVICE_TESTS.md         待执行的真机验收清单
```

## 验证状态

2026-10-25 已在 Windows / JDK 21 环境完成 `assembleDebug`、`lintDebug`（0 错误），并通过 APK 签名校验。Lint 保留 3 条工具／SDK 版本更新建议；本项目固定使用前述构建版本。

核心行为测试覆盖倒计时取整、暂停／继续、重复开始、到期只触发一次、重置、范围校验、CSV 转义与公式前缀，共 32 项，全部通过。

本项目仍需真机验收，尤其是不同 Android 版本的悬浮窗授权、系统后台限制与旋转／大字体布局。请参照 [真机验收清单](docs/DEVICE_TESTS.md)。不要将源码静态检查或构建通过当作真机测试通过。

## 隐私、许可与声明

详见 [隐私说明](PRIVACY.md)。应用源码使用 [MIT License](LICENSE)，Gradle Wrapper 遵循其上游许可，见 [第三方说明](THIRD_PARTY.md)。不包含官方游戏图片、音效、角色素材或客户端文件。《第五人格》名称与相关商标归相应权利人所有，本项目与官方无隶属或合作关系。
