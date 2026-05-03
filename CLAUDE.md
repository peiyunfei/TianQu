# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指引。

## 项目简介
**天衢（TianQu）** 是一个专为 **Kotlin Multiplatform (KMP)** 打造的、纯协程驱动的现代路由框架，支持 Android、iOS 和桌面端。框架以 `suspend` 函数全面取代传统回调，彻底摆脱 JVM ASM 字节码插桩依赖。

如果需要知道接入方式和实现的功能，请查看[README.md](README.md)。

如果需要知道当前开发进度，查看[当前进度](/.claude/开发进度.md)

## 架构说明
项目分为以下几个核心层级/模块：

1. **`router-annotations/`**：存放核心注解，包括 `@Router`、`@Service`、`@Transition`、`@InjectViewModel`。
2. **`router-processor/`**：KSP 编译期注解处理器，扫描注解并为每个业务模块生成路由表与服务表。
3. **`router-runtime/`**：路由运行时核心，全面基于协程实现，包含 `Navigator`、`RouterHost`、`ServiceManager` 及各种导航策略。
4. **`feature-b/`**（及其他业务模块）：独立业务模块，通过注解声明路由与服务，模块之间互不依赖。
5. **`composeApp/`**：主工程（壳模块），聚合所有子模块路由表，生成统一的 `GlobalRouteAggregator`。

### 关键技术要点
- **KSP 代码生成**：路由表在编译期生成到 `build/generated/ksp/metadata/commonMain/kotlin`。所有模块必须正确配置 `kspCommonMainKotlinMetadata` 任务依赖并将生成目录加入 `commonMain` 的 source set。
- **协程驱动**：页面跳转（`awaitNavigateForResult`）、路由守卫（`RouterGuard.canActivate`）、跨模块服务获取（`rememberService<T>()`）均为挂起函数或 Compose 状态驱动，不使用任何回调接口。
- **生命周期与作用域**：每个路由节点通过 Compose 的 `SaveableStateHolder` 维护独立的 `CoroutineScope` 与状态，页面出栈时协程作用域自动取消。

## 常用命令

### 构建与编译
KSP 代码生成是本项目的核心环节，编译即可验证注解处理是否正常：
```bash
# 构建整个项目并触发 KSP 生成
./gradlew assembleDebug

# 仅触发 KMP metadata 的 KSP 生成（快速检查 KSP 产物）
./gradlew kspCommonMainKotlinMetadata
```

### 运行 Android 应用
```bash
# 安装到已连接的设备/模拟器
./gradlew :composeApp:installDebug
```

### 运行测试
```bash
# 运行所有模块的单元测试
./gradlew test

# 仅运行某个模块的测试（以 router-runtime 为例）
./gradlew :router-runtime:test
```

## 开发注意事项
- **新增业务模块**：必须在 `build.gradle.kts` 中配置 `ksp { arg("tianqu.moduleName", project.name) }`，并将 KSP 生成目录加入 `commonMain` source set，同时让所有 Kotlin 编译任务依赖 `kspCommonMainKotlinMetadata`。
- **主工程标识**：`composeApp` 模块必须配置 `arg("tianqu.isApp", "true")`，以触发全局路由聚合。
- **优先使用协程**：实现新的路由逻辑时，始终优先使用 `suspend`、`SharedFlow`、`Deferred`，不引入回调接口。
- **KMP 兼容性**：`router-runtime`、`router-annotations`、`router-processor` 模块内禁止使用平台特定 API（如 `java.lang.reflect` 或 Android `Intent`），平台差异必须通过 `expect/actual` 或平台 source set（`androidMain`、`iosMain`）隔离。

## 发版检查清单（每次发版时必须执行）

天衢框架发版时，除了正常的代码发布流程，还需要同步维护 AI 辅助接入组件，确保接入方的 Claude Code 能感知到新功能。

### 需要同步更新的两个文件

每次发版都**必须同时更新**以下两个 `version.json`，二者内容保持完全一致：

1. **`version.json`**（仓库根目录）——远端版本真值源，用于接入方 `tianqu_check_update` 工具检测更新
2. **`tianqu-mcp-server/src/main/resources/tianqu-version.json`**——MCP Server 本地版本，用于本地 vs 远端比对

### 各字段含义与更新规则

| 字段 | 含义 | 何时更新 |
|------|------|----------|
| `serverVersion` | MCP Server JAR 版本 | 每次重新编译并发布新 JAR 时 |
| `frameworkVersion` | 天衢框架本体版本（Maven 版本号） | 每次发布新框架版本时 |
| `skillVersion` | SKILL.md 版本 | 修改 SKILL.md 工作流或规则时 |
| `capabilitiesVersion` | capabilities.json 版本 | 新增/修改/删除能力条目时 |
| `integrationGuideVersion` | 接入指南模板版本 | 修改 integration-guides/*.md 时 |
| `installerVersion` | install-claude.sh 版本 | 修改安装脚本时 |
| `kspVersion` | 推荐的 KSP 版本号 | Kotlin/KSP 版本升级时 |
| `minJavaVersion` | 最低 Java 版本要求 | 调整 Java 要求时 |
| `releaseDate` | 本次发版日期 | 每次发版都更新，格式 `YYYY-MM-DD` |
| `updateMessage` | 给接入方的更新提示语（可选） | 有重要变更需要提醒时填写，否则留空字符串 |

### 新增天衢功能时的完整流程

1. 实现新功能代码
2. 在 `tianqu-mcp-server/src/main/resources/capabilities.json` 的 `capabilities` 数组中新增对应的能力条目
3. 更新 `capabilities.json` 里 `meta.capabilitiesVersion`（例如 `1.0.0` → `1.1.0`）
4. 如需更新接入指南模板，修改 `tianqu-mcp-server/src/main/resources/integration-guides/app.md` 或 `feature.md`
5. 重新编译 MCP Server：`./gradlew :tianqu-mcp-server:shadowJar`
6. 复制新 JAR 到仓库发布目录：`cp tianqu-mcp-server/build/libs/tianqu-mcp-server-all.jar bin/`
7. **同步更新两个 `version.json`**，至少修改 `serverVersion`、`capabilitiesVersion`、`releaseDate`，并在 `updateMessage` 中写上本次更新摘要（例如 `"新增动态路由能力，建议更新 MCP 组件"`）
8. 提交所有改动并 Push 到 Gitee

> 只要 `version.json` 的 `serverVersion` 发生变化，接入方下次使用 `/tianqu` 时就会自动收到更新提示，并可一键升级。
