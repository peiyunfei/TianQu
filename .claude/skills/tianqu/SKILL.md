---
name: tianqu
description: 接入天衢路由框架，或使用天衢实现具体功能。在执行前必须先检查更新、验证能力边界，再分析工程结构，最后再生成改动方案。
version: 1.0.0
---

# 核心目的
帮助用户在自己的项目中零手写代码接入天衢框架，或基于天衢实现已有能力范围内的功能。

# 何时使用
当用户提出以下类型需求时使用：
- 帮我接入天衢框架
- 在当前项目中配置天衢路由
- 用天衢实现某个路由页面 / 参数传递 / 服务发现 / ViewModel 注入 / 页面结果回传 / DeepLink / 动态路由 等功能
- 判断某项需求是否在天衢支持范围内

# 强制工作流
你必须严格按以下顺序执行，禁止跳步：

1. **先调用 `tianqu_check_update`**
   - 传入当前项目的根目录路径（`projectRoot`）作为参数，用于读写每日缓存文件
   - **如果用户明确要求“强制检查更新”，你必须同时传入 `force: true` 参数**，绕过本地 24 小时缓存直接请求远端最新版本
   - 根据返回结果，按以下规则处理：
     - **`hasUpdate: true`**：主动告知用户发现新版本，并**询问用户是否立即更新**
       - 若用户同意：使用 Bash 工具执行 `upgradeCommand` 字段中的命令，执行完成后**提醒用户必须完全退出并重启 Claude Code**（因为 MCP Server 进程和 Skill 文件均已更新，不重启无法加载），然后**结束本次对话**，不再继续后续步骤
       - 若用户拒绝：告知将基于当前本地版本继续，进入步骤 2
     - **`source: "cache"` 且 `hasUpdate: false`**：缓存命中且无更新，静默继续，进入步骤 2，无需提示用户
     - **`networkError` 字段存在**：告知用户"无法连接远端，将基于本地版本继续"，进入步骤 2
     - **其他无更新情况（如强制检查结果也是无更新）**：告知用户当前已是最新版本，静默继续，进入步骤 2

2. **再调用 `tianqu_get_capabilities`**
   - 判断用户需求是否在天衢支持范围内
   - 返回结果为 JSON 对象，能力清单在 `capabilities` 字段中，版本信息在 `meta` 字段中
   - 如果不支持：明确告诉用户"天衢暂不支持该能力"，停止后续改动
   - 不允许在未完成能力检查前直接修改代码

3. **再调用 `tianqu_analyze_project`**
   - 分析当前接入方工程的模块结构
   - 识别 app 主模块 / feature 子模块
   - 检查是否已有 KSP、是否已有天衢依赖、是否已有入口初始化逻辑

4. **最后调用 `tianqu_get_integration_guide`**
   - 按模块类型获取精确的接入指令、Gradle 配置模板、初始化模板
   - 根据返回结果修改接入方项目

# 约束与限制
- 遇到不确定的地方（版本选择、目录结构冲突、配置歧义、模块类型无法确定）不要自行决定，必须先向用户确认，并给出建议方案
- **生成到接入方项目中的所有代码，必须带必要的中文注释**，解释关键配置、初始化逻辑和使用方式；尤其是模板代码（Gradle 配置、RouterHost 初始化、守卫、ViewModel 注入等），每一段都要有注释
- 优先复用接入方现有工程结构，不要擅自引入额外抽象
- 如果需求超出天衢边界，不要尝试"兼容实现"或偷偷用别的框架替代
- **使用协程实现页面间结果回传时，必须使用 `navigator.coroutineScope.launch { }` 来开启协程，不可以使用 `rememberCoroutineScope()` 或其他外部 scope**。原因是 `awaitNavigateForResult` 是挂起函数，调用方跳转后页面会离开组合树，`rememberCoroutineScope` 的 scope 会随之取消，导致永远收不到返回数据；而 `navigator.coroutineScope` 的生命周期与 Navigator 绑定，跳转期间不会取消。
  正确示例：
  ```kotlin
  Button(onClick = {
      // ✅ 必须用 navigator.coroutineScope，跳转后页面离开组合树，
      //    rememberCoroutineScope 会被取消，导致收不到返回数据
      navigator.coroutineScope.launch {
          val result = navigator.awaitNavigateForResult("/detail")
          // 处理返回结果
      }
  }) { Text("跳转并等待结果") }
  ```
  错误示例（禁止这样写）：
  ```kotlin
  val scope = rememberCoroutineScope() // ❌ 禁止！页面离开组合树后此 scope 会取消
  Button(onClick = {
      scope.launch {
          val result = navigator.awaitNavigateForResult("/detail") // 永远收不到结果
      }
  }) { Text("跳转") }
  ```

# 输出期望
- 如果是"接入框架"类需求：完成依赖接入、KSP 配置、sourceSet 配置、入口初始化，并解释关键改动
- 如果是"实现功能"类需求：仅在天衢支持的能力范围内完成实现；不支持则明确拒绝
- 每次完成工作后，给出简短结果说明，并指出下一步需要用户做什么（如果需要）
