# 天衢（TianQu）AI 辅助实现原理

我们提供了技能（Skill）和 MCP（Model Context Protocol）来实现“一句话接入天衢”、“一句话使用天衢实现某个功能”。这背后的原理，本质上是 **结构化知识库（MCP） + 标准化工作流（Skill） + 大模型代码理解与执行能力（Claude Code）** 的深度结合。

简单来说，相当于为 AI 编写了一份“机器可读的开发文档”，并规定了 AI 阅读和操作的顺序。具体的工作机制和步骤如下：

## 1. MCP Server 扮演“动态知识库”（RAG）
当用户输入一句话时，AI 默认是不知道天衢具体代码细节的。但通过 `tianqu-mcp-server`，AI 可以动态获取最新的框架知识：
* **`capabilities.json`**：告诉 AI“天衢现在能做什么”。AI 会先查阅这个文件，匹配到对应的能力边界和关键 API。
* **`integration-guides/` (接入指南)**：包含了纯净的、标准化的接入模板（比如 Gradle 配置、KSP 参数、初始化 `GlobalRouteAggregator` 等）。

## 2. Skill (`/tianqu`) 扮演“工作流引擎”（SOP）
普通的 AI 聊天是发散的，但 `/tianqu` 技能强制 AI 遵循一个严谨的开发者 SOP（标准作业程序）：
1. **检查更新**（对比 `version.json`）
2. **验证能力边界**（查阅 MCP 里的 capabilities）
3. **分析工程结构**（使用本地工具扫描代码）
4. **生成改动方案并执行**

## 3. AI 是怎么知道要添加“哪些代码”的？（核心过程）
当接收到类似于 **“一句话接入天衢”** 或 **“帮我把 feature-login 模块加上天衢路由”** 的指令时，AI 会进行如下计算：

1. **获取“应该长什么样”（理论知识）**：
   通过 MCP 读取接入指南（如 `feature.md`），明确必须的步骤：
   * 添加 `router-annotations` 依赖
   * 添加 `ksp(router-processor)` 依赖
   * 在 `ksp { arg("tianqu.moduleName", project.name) }` 注册模块名
   * 将 KSP 产物路径加入 `commonMain` 的 sourceSets 并让编译任务依赖 `kspCommonMainKotlinMetadata`。

2. **分析“现在长什么样”（实际环境）**：
   使用工具读取当前模块的 `build.gradle.kts`，检查现有的配置方式、依赖情况以及插件应用情况。

3. **上下文融合（推理要改的代码）**：
   将指南中的“标准代码片段”结合当前工程的“具体结构”进行融合。例如替换 KSP 参数里的模块名占位符；或者根据路由注解用法，找到对应的 Compose 函数加上 `@Router("xxx")`。

4. **精准操作（执行修改）**：
   最后，使用系统自带的文件编辑工具，直接把精准生成的代码注入到工程文件里，并可能自动运行 `./gradlew kspCommonMainKotlinMetadata` 来验证是否生效。

## 总结
“一句话接入”的绝佳体验，是因为**我们将人工查阅文档、Copy-Paste 代码、配置 Gradle 的繁琐过程，抽象成了 MCP 的结构化数据与 Skill 的标准化流程。** AI 在此过程中扮演了一个“秒读文档、秒看源码、且永远按规矩办事的超级打字员”。
