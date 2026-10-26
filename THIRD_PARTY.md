# 第三方组件

## Gradle Wrapper 8.13

`gradlew`、`gradlew.bat` 与 `gradle/wrapper/gradle-wrapper.jar` 由官方 Gradle 8.13 的 `wrapper` 任务生成，依据 Apache License 2.0 及上游相关声明分发。

- 上游：https://github.com/gradle/gradle/tree/v8.13.0
- 许可证副本：[LICENSES/Gradle-LICENSE.txt](LICENSES/Gradle-LICENSE.txt)
- 上游声明副本：[LICENSES/Gradle-NOTICE.txt](LICENSES/Gradle-NOTICE.txt)
- Gradle 分发 ZIP 的 SHA-256 已固定在 `gradle-wrapper.properties` 中。

Android SDK、Android Gradle Plugin 和 GitHub Actions 在构建时下载／执行，不包含在应用 APK 或源码 ZIP 中；各自遵循上游许可与服务条款。

本应用没有第三方运行时库依赖。
