# 天衢 路由框架 KSP 注解处理器原理解析

本文档详细解析了 天衢 路由框架中 `router-processor` 模块的实现原理。该模块基于 KSP (Kotlin Symbol Processing) 技术，在编译期间扫描注解并自动生成路由注册表、服务注册表和转场动画注册表，从而实现了解耦和自动化的路由注册。

## 一、KSP 基础概念讲解

### 什么是 KSP？
KSP (Kotlin Symbol Processing) 是 Google 推出的一种用于在 Kotlin 中构建轻量级编译器插件的 API。它类似于 Java 的 APT (Annotation Processing Tool) 或 KAPT，但专门为 Kotlin 设计，直接解析 Kotlin 代码的抽象语法树 (AST)，因此速度比 KAPT 快得多（通常快 2 倍以上），并且能更好地理解 Kotlin 特有的语法（如扩展函数、协程、类型别名等）。

### KSP 的核心工作流程
1. **扫描 (Scanning)**：KSP 框架在编译期扫描源代码，寻找特定的注解（如 `@Router`, `@Service`）。
2. **处理 (Processing)**：将扫描到的符号（类、函数、属性等）交给开发者自定义的 `SymbolProcessor` 进行处理。KSP 的处理是**多轮次 (Multi-Round)** 的。如果在某一轮生成了新的源文件，且新文件中包含了注解，KSP 就会启动下一轮处理，直到没有新文件产生为止。
3. **生成 (Generation)**：处理器根据收集到的信息，使用代码生成工具（如 KotlinPoet）生成新的 Kotlin 文件。
4. **编译 (Compilation)**：生成的新文件会和原有代码一起参与最终的编译。

---

## 二、架构思考：为什么要全局聚合？传统路由是如何做的？

在深入源码前，我们需要理解 天衢 路由框架在宏观架构上的设计考量。

### 1. 传统路由框架（如 ARouter）的做法与痛点
在组件化开发中，每个子模块（如 ModuleA, ModuleB）相互隔离，它们各自通过注解生成了路由表类（例如 `ARouter$$Group$$ModuleA`）。
但在主工程（壳工程）运行时，我们需要把这些散落的路由表汇总到一起。传统框架通常有两种解法：
*   **运行时反射扫描 (Runtime Reflection)**：在 App 冷启动时，遍历 APK / DEX 中的所有类，通过特定的包名前缀或包名，找到所有生成的路由表类并进行实例化注册。**缺点：极其耗时，严重拖慢冷启动速度。**
*   **编译期字节码插桩 (ASM Transform)**：利用 Android Gradle Plugin (AGP) 的 Transform API，在打包成 DEX 前的阶段，通过 ASM 修改字节码，把找到的类硬编码注入到初始化方法中。**缺点：仅限 Android/JVM 平台有效。**

### 2. 天衢 的全局聚合优势与 KMP 适配
天衢 是一个 **Kotlin Multiplatform (KMP)** 框架，目标不仅是 Android，还包括 iOS 等平台。iOS 上没有 JVM 字节码，自然也无法使用 ASM 插桩。
因此，天衢 选择了基于 KSP 的 **纯编译期全局聚合**：
让各个子模块在编译时生成自己的模块级路由注册表，而在主工程编译的最后阶段，利用 KSP 去扫描所有依赖树中的子模块注册表，并在编译期直接生成一个汇总了所有子模块路由的全局聚合类。
*   **运行时零耗时**：运行时不再需要任何扫描，直接加载硬编码的全局类。
*   **全平台通用**：生成的纯 Kotlin 源码，原生支持 KMP 的所有平台。

---

## 三、router-processor 模块源码逐行解析

`router-processor` 模块包含了 7 个核心类，它们共同协作完成了路由信息的收集和代码生成。

### 1. RouterProcessorProvider.kt
**作用**：KSP 处理器的入口工厂类。KSP 框架通过 `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider` 文件找到这个类，并实例化它。

```kotlin
package shijing.tianqu.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP 处理器工厂类
 * 通过 SPI 机制被 KSP 框架发现和调用
 */
class RouterProcessorProvider : SymbolProcessorProvider {
    // 重写 create 方法，KSP 框架会传入 SymbolProcessorEnvironment 环境对象
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        // 实例化并返回我们自定义的 RouterProcessor
        return RouterProcessor(
            codeGenerator = environment.codeGenerator, // 传入代码生成器，用于后续生成 .kt 文件
            logger = environment.logger,               // 传入日志记录器，用于在编译控制台输出信息
            options = environment.options              // 传入 build.gradle.kts 中配置的 ksp 参数（如模块名）
        )
    }
}
```

---

### 2. RouterProcessor.kt
**作用**：核心处理器类，负责协调整个扫描和生成的流程。

**关键机制解析：怎么判断没有新符号？如何判断所有的模块都处理完成，然后进行全局聚合？**

要理解这个逻辑，我们需要知道 KSP 是**分轮次（Rounds）**运行的。你可以把它想象成在流水线上反复检查：
1. **怎么判断没有新符号？**
   在每一轮检查中，KSP 的解析器只会把**“上一轮刚刚新生成的代码”**或者**“最开始还未被处理的原始代码”**中携带了特定注解的内容交给你。
   如果在本轮检查时，我们去查找所有带有 `@Router` 等注解的函数或类，发现结果是**空的**，这就意味着当前这一轮并没有扫描到任何包含目标注解的新鲜代码。在代码中，这表现为提取到的有效节点列表为空，从而得出“没有新文件需要处理”的结论。
   
2. **为何此时可以安全地进行全局聚合？**
   由于 KSP 的机制是“只要有新文件生成，就会触发下一轮扫描，直到某一轮完全没有任何新文件产生为止”，所以当我们发现当前这一轮**没有新的注解符号**时，就说明代码的生成链条已经彻底停息了，到达了**最后的稳定收尾阶段**。
   在这个阶段，所有依赖的第三方库、子模块的生成物，以及本模块前几轮生成的中间代码，都已经稳稳当当地存在，并且能够被 KSP 解析器完整地“看”到。因此，此时进行一次全局遍历来收集所有的子模块注册表，绝不会漏掉任何信息，聚合出的全局路由表才是最完整、最安全的。

```kotlin
package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import shijing.tianqu.router.Router
import shijing.tianqu.router.Service
import shijing.tianqu.router.Transition

/**
 * 路由符号处理器。
 * 使用 KSP 在编译期间扫描所有标记了 @Router 等注解的符号，并自动生成注册表代码。
 */
class RouterProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    // 标记是否已经生成了全局路由表，防止在多轮处理中重复生成
    private var isGeneratedGlobal = false

    // 核心处理方法，resolver 用于解析代码符号
    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 使用 warn 级别打印日志，因为 Gradle 默认会屏蔽 info 级别，方便开发者调试
        logger.warn("----> RouterProcessor start processing <----")
        
        // 用于存储当前轮次无法解析的符号，留到下一轮处理
        val deferredSymbols = mutableListOf<KSAnnotated>()

        // 1. 获取所有带 @Router 注解的符号
        val routerSymbols = resolver.getSymbolsWithAnnotation(Router::class.qualifiedName ?: "")
        // 过滤出函数声明（因为 @Router 标记在 Composable 函数上）
        val functionDeclarations = routerSymbols.filterIsInstance<KSFunctionDeclaration>()
        // 验证符号是否有效（类型是否已完全解析）
        val validFunctions = functionDeclarations.filter { it.validate() }.toList()
        // 将无效符号加入 deferredSymbols，推迟到下一轮
        deferredSymbols.addAll(functionDeclarations.filterNot { it.validate() })

        // 2. 获取所有带 @Service 注解的符号
        val serviceSymbols = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName ?: "")
        val classDeclarations = serviceSymbols.filterIsInstance<KSClassDeclaration>()
        val validClasses = classDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(classDeclarations.filterNot { it.validate() })

        // 3. 获取所有带 @Transition 注解的符号
        val transitionSymbols = resolver.getSymbolsWithAnnotation(Transition::class.qualifiedName ?: "")
        val transitionDeclarations = transitionSymbols.filterIsInstance<KSClassDeclaration>()
        val validTransitions = transitionDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(transitionDeclarations.filterNot { it.validate() })

        // 核心判断：当前这一轮扫描，是否找到了有效的注解符号？
        // 如果 validFunctions 等集合不为空，说明当前轮次发现了新的需要处理的注解
        val hasNewFiles = validFunctions.isNotEmpty() || validClasses.isNotEmpty() || validTransitions.isNotEmpty()

        // 从 KSP 参数中获取当前模块的名称
        val moduleNameOption = options["tianqu.moduleName"]
        if (moduleNameOption.isNullOrBlank() && hasNewFiles) {
            logger.error("TianQu Router: 缺失KSP 参数 'tianqu.moduleName'! ...")
            return emptyList()
        }
        
        val moduleName = (moduleNameOption ?: "Default")
            .replace("[^a-zA-Z0-9]".toRegex(), "_")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // 将当前模块扫描到的节点，生成对应的模块级注册表代码
        if (validFunctions.isNotEmpty()) {
            RouterRegistryGeneratorStrategy(moduleName).generate(validFunctions, codeGenerator, logger)
        }
        if (validClasses.isNotEmpty()) {
            ServiceRegistryGeneratorStrategy(moduleName).generate(validClasses, codeGenerator, logger)
        }
        TransitionRegistryGeneratorStrategy(moduleName).generate(validTransitions, codeGenerator, logger)

        // 4. 处理 App 级别的全局路由聚合
        val isApp = options["tianqu.isApp"] == "true"
        
        if (isApp) {
            // !hasNewFiles 代表：当前轮次没有发现任何新的注解符号，所有产物已经全部生成完毕并处于稳定状态。
            // 此时启动全局聚合扫描是最安全、最完整的，不会漏扫任何依赖。
            if (!hasNewFiles && !isGeneratedGlobal) {
                GlobalRegistryGeneratorStrategy(resolver).generate(emptyList(), codeGenerator, logger)
                isGeneratedGlobal = true // 标记为已生成，防止重复执行
            }
        }

        // 返回无法解析的符号，交由 KSP 框架放到下一轮重新解析
        return deferredSymbols
    }
}
```

---

### 3. CodeGenerationStrategy.kt
**作用**：定义代码生成的策略接口。

```kotlin
package shijing.tianqu.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * 代码生成策略接口（策略模式）
 * 用于将不同类型注解的代码生成逻辑解耦
 */
interface CodeGenerationStrategy<T : KSAnnotated> {
    fun generate(symbols: List<T>, codeGenerator: CodeGenerator, logger: KSPLogger)
}
```

---

### 4. RouterRegistryGeneratorStrategy.kt
**作用**：负责生成当前模块的路由注册表 `RouteRegistry_[ModuleName]`。

**关键机制解析：为什么要生成 RouterNode？RouterNode 的作用是什么？**

在这个策略中，我们将所有扫描到的带有 `@Router` 注解的 Compose 函数封装成了一个个 `RouterNode` 对象，并组成了一个列表 `List<RouterNode>`。
**`RouterNode` 可以被理解为路由框架在运行时的“导航地图坐标”和“渲染说明书”。** 它的核心作用是将编译期的注解元数据，转化为运行时直接可用的数据结构。一个 `RouterNode` 包含了以下核心信息：

1.  **匹配规则 (`path` & `regexPattern`)**：
    *   保存了原始路径（如 `/user/{id}`）。
    *   框架在 KSP 阶段会将占位符 `{id}` 自动转换成正则表达式（如 `^/user/([\w-]+)$`）。在运行时，当用户请求跳转一个 URL 时，路由引擎只需遍历 `RouterNode` 列表，用这里的正则去匹配 URL，一旦匹配成功，不仅知道了要跳转哪个页面，还能顺便通过正则捕获组提取出动态参数。
2.  **转场动画 (`transition`)**：
    *   通过提前将字符串形式的动画名称映射为具体的动画策略实例，确保该节点在压入导航栈时知道用什么动画进行过渡渲染。
3.  **路由类型 (`type`)**：
    *   标明这个节点是一个全屏页面 (`SCREEN`)，还是一个悬浮弹窗 (`DIALOG`)，以便路由引擎在渲染时挂载到不同的展示树中。
4.  **界面渲染器 (`composable`)**：
    *   **这是最重要的一环！** KSP 无法传递函数指针，但可以生成一段 Lambda 闭包 `{ context -> Func(context) }`。这个闭包将真实的 `Composable` 业务函数包裹了起来。
    *   在运行时，当路由引擎决定要展示这个页面时，只需要调用这个节点保存的 `composable` 函数，并把含有参数的 `RouterContext` 传进去，真实的 UI 页面就被渲染出来了。

因此，`RouterNode` 的存在完美地在编译期注解和运行时 UI 渲染之间架起了一座桥梁，实现了极速的无反射跳转。

```kotlin
package shijing.tianqu.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import shijing.tianqu.router.RouterContext
import shijing.tianqu.router.Router

class RouterRegistryGeneratorStrategy(private val moduleName: String = "Default") : CodeGenerationStrategy<KSFunctionDeclaration> {
    override fun generate(symbols: List<KSFunctionDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val functions = symbols
        val packageName = "shijing.tianqu.router.generated"
        val className = "RouteRegistry_$moduleName" // 动态生成类名，避免多模块冲突

        // 引入 RouterNode 这个数据结构
        val routeNodeType = ClassName("shijing.tianqu.runtime", "RouterNode")
        val listType = ClassName("kotlin.collections", "List").parameterizedBy(routeNodeType)

        val initBlock = CodeBlock.builder().add("listOf(\n").indent()

        functions.forEachIndexed { index, func ->
            val annotation = func.annotations.first { it.shortName.asString() == Router::class.simpleName }
            val pathArg = annotation.arguments.first { it.name?.asString() == "path" }
            val pathValue = pathArg.value as String
            
            // 将路径中的参数占位符 {id} 替换为正则表达式 ([\w-]+)，以便运行时提取参数
            val regexPattern = pathValue.replace(Regex("\\{[^/]+\\}"), "([\\\\w-]+)")
            
            // 读取 transition 和 type
            val transitionArg = annotation.arguments.find { it.name?.asString() == "transition" }
            val transitionName = transitionArg?.value?.toString() ?: "Slide"
            
            val typeArg = annotation.arguments.find { it.name?.asString() == "type" }
            val typeValue = typeArg?.value
            val typeName = if (typeValue is com.google.devtools.ksp.symbol.KSType) {
                typeValue.declaration.simpleName.asString()
            } else {
                typeValue?.toString() ?: "SCREEN"
            }
            val cleanTypeName = typeName.substringAfterLast(".")

            val funcName = func.simpleName.asString()
            val funcClassName = ClassName(func.packageName.asString(), funcName)

            val comma = if (index < functions.size - 1) ",\n" else "\n"
            val hasContextParam = func.parameters.any { it.type.resolve().declaration.qualifiedName?.asString() == RouterContext::class.qualifiedName }
            
            // 封装 UI 渲染闭包，供运行时直接调用
            val composableCall = if (hasContextParam) "{ context -> %T(context) }" else "{ _ -> %T() }"
            val transitionInstantiateStr = "shijing.tianqu.router.generated.TransitionStrategyRegistry_$moduleName.transitions[%S]?.invoke() ?: shijing.tianqu.runtime.transition.SlideTransitionStrategy()"

            // 创建并硬编码一个 RouterNode 对象
            initBlock.add(
                "RouterNode(\n" +
                "    path = %S,\n" +
                "    regexPattern = %S,\n" +
                "    transition = $transitionInstantiateStr,\n" +
                "    type = shijing.tianqu.router.RouteType.%L,\n" +
                "    composable = $composableCall\n" +
                ")%L",
                pathValue, "^$regexPattern\$", transitionName, cleanTypeName, funcClassName, comma
            )
        }
        initBlock.unindent().add(")")

        val propertySpec = PropertySpec.builder("routers", listType).initializer("%L", initBlock.build()).build()

        val typeSpec = TypeSpec.objectBuilder(className)
            // 加上特制聚合注解，给全局扫描提供入口
            .addAnnotation(ClassName("shijing.tianqu.router.aggregation", "ModuleRouteRegistry"))
            .addProperty(propertySpec)
            .build()

        val fileSpec = FileSpec.builder(packageName, className).addType(typeSpec).build()

        try {
            val dependencies = Dependencies(aggregating = true, *functions.mapNotNull { it.containingFile }.toTypedArray())
            fileSpec.writeTo(codeGenerator, dependencies)
        } catch (e: Exception) {
            logger.error("Error generating RouteRegistry: ${e.message}")
        }
    }
}
```

---

### 5. ServiceRegistryGeneratorStrategy.kt
**作用**：负责生成当前模块的服务注册表 `ServiceRegistry_[ModuleName]`。

**关键机制解析：为什么没有使用反射来创建服务对象？**
在传统的 Android 路由框架（如 ARouter）中，解析服务（Service）注解时，通常会提取出目标类的权限定名（如 `"com.example.MyServiceImpl"`）保存为字符串。在运行时，再利用 `Class.forName("...").newInstance()` 这种**反射**手段来创建服务对象。
但这存在几个严重问题：
1. 反射非常慢，容易引发性能瓶颈。
2. 运行时可能发生 `ClassNotFoundException` 或引发混淆 (ProGuard) 问题。
3. **最关键的是：Kotlin Multiplatform (KMP) 在 iOS (Kotlin Native) 和 JavaScript 等平台上根本不支持 Java 的那套运行时反射机制！**

为了实现全平台通用以及极致的性能，天衢 使用了**闭包（Lambda 函数）**来作为工厂方法：
通过 `initBlock.add("%T::class to { %T() }", interfaceName, implClassName)` ，将类生成的代码硬编码为 `Interface::class to { ImplClass() }`。
*   这本质上是一个映射：`Map<KClass<*>, () -> Any>`。
*   **按需懒加载**：当你在业务中想要获取服务时，框架通过接口的 Class 查找到这个 `{ ImplClass() }` 的函数。只要你不调用它，`ImplClass` 就不会被实例化（极其轻量）。
*   **百分百类型安全**：由于是直接写入了类名并在编译期检查，如果这个实现类被删了或者改名了，编译器会当场报错，绝不会把隐患留到运行时。这完全避开了反射的缺点，且在 iOS/JS 端也能完美运行。

```kotlin
package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo

class ServiceRegistryGeneratorStrategy(private val moduleName: String = "Default") : CodeGenerationStrategy<KSClassDeclaration> {
    override fun generate(symbols: List<KSClassDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val classes = symbols
        val packageName = "shijing.tianqu.router.generated"
        val className = "ServiceRegistry_$moduleName"

        val kClassType = ClassName("kotlin.reflect", "KClass").parameterizedBy(STAR)
        val anyType = ClassName("kotlin", "Any")
        val functionType = LambdaTypeName.get(returnType = anyType)
        // 核心亮点：Value 是一个 () -> Any 的无参 Lambda 工厂函数
        val mapType = ClassName("kotlin.collections", "Map").parameterizedBy(kClassType, functionType)

        val initBlock = CodeBlock.builder().add("mapOf(\n").indent()

        classes.forEachIndexed { index, ksClass ->
            val superType = ksClass.superTypes.firstOrNull()?.resolve()
            val superDeclaration = superType?.declaration as? KSClassDeclaration
            
            if (superDeclaration != null && superDeclaration.classKind == ClassKind.INTERFACE) {
                val interfaceName = ClassName(superDeclaration.packageName.asString(), superDeclaration.simpleName.asString())
                val implClassName = ClassName(ksClass.packageName.asString(), ksClass.simpleName.asString())

                val comma = if (index < classes.size - 1) ",\n" else "\n"
                // 告别反射，生成 Lambda 函数 `{ ImplClass() }` 以供运行时无反射懒加载调用
                initBlock.add("%T::class to { %T() }%L", interfaceName, implClassName, comma)
            } else {
                logger.warn("Class ${ksClass.simpleName.asString()} annotated with @Service must implement at least one interface.")
            }
        }
        initBlock.unindent().add(")")

        val propertySpec = PropertySpec.builder("services", mapType).initializer("%L", initBlock.build()).build()

        val typeSpec = TypeSpec.objectBuilder(className)
            .addAnnotation(ClassName("shijing.tianqu.router.aggregation", "ModuleServiceRegistry"))
            .addProperty(propertySpec)
            .build()

        val fileSpec = FileSpec.builder(packageName, className).addType(typeSpec).build()

        try {
            val dependencies = Dependencies(aggregating = true, *classes.mapNotNull { it.containingFile }.toTypedArray())
            fileSpec.writeTo(codeGenerator, dependencies)
        } catch (e: Exception) {
            logger.error("Error generating ServiceRegistry: ${e.message}")
        }
    }
}
```

---

### 6. TransitionRegistryGeneratorStrategy.kt
**作用**：生成当前模块的转场动画注册表 `TransitionStrategyRegistry_[ModuleName]`。

```kotlin
package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import shijing.tianqu.router.Transition

class TransitionRegistryGeneratorStrategy(private val moduleName: String = "Default") : CodeGenerationStrategy<KSClassDeclaration> {
    override fun generate(symbols: List<KSClassDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val classes = symbols
        val packageName = "shijing.tianqu.router.generated"
        val className = "TransitionStrategyRegistry_$moduleName"

        val transitionStrategyType = ClassName("shijing.tianqu.runtime.transition", "TransitionStrategy")
        val mapType = ClassName("kotlin.collections", "Map").parameterizedBy(
            ClassName("kotlin", "String"),
            LambdaTypeName.get(returnType = transitionStrategyType)
        )

        val initBlock = CodeBlock.builder().add("mapOf(\n").indent()

        // 预先注册内置的基础动画策略
        initBlock.add("%S to { shijing.tianqu.runtime.transition.SlideTransitionStrategy() },\n", "Slide")
        initBlock.add("%S to { shijing.tianqu.runtime.transition.FadeTransitionStrategy() },\n", "Fade")
        initBlock.add("%S to { shijing.tianqu.runtime.transition.ScaleTransitionStrategy() },\n", "Scale")
        initBlock.add("%S to { shijing.tianqu.runtime.transition.NoneTransitionStrategy() }", "None")

        if (classes.isNotEmpty()) { initBlock.add(",\n") } else { initBlock.add("\n") }

        classes.forEachIndexed { index, cls ->
            val annotation = cls.annotations.first { it.shortName.asString() == Transition::class.simpleName }
            val nameArg = annotation.arguments.first { it.name?.asString() == "name" }
            val transitionName = nameArg.value as String

            val clsClassName = ClassName(cls.packageName.asString(), cls.simpleName.asString())
            val comma = if (index < classes.size - 1) ",\n" else "\n"
            initBlock.add("%S to { %T() }%L", transitionName, clsClassName, comma)
        }

        initBlock.unindent().add(")")

        val propertySpec = PropertySpec.builder("transitions", mapType).initializer("%L", initBlock.build()).build()

        val typeSpec = TypeSpec.objectBuilder(className)
            .addAnnotation(ClassName("shijing.tianqu.router.aggregation", "ModuleTransitionRegistry"))
            .addProperty(propertySpec)
            .build()

        val fileSpec = FileSpec.builder(packageName, className).addType(typeSpec).build()

        try {
            val dependencies = if (classes.isNotEmpty()) {
                Dependencies(aggregating = true, *classes.mapNotNull { it.containingFile }.toTypedArray())
            } else {
                Dependencies.ALL_FILES
            }
            fileSpec.writeTo(codeGenerator, dependencies)
        } catch (e: Exception) {
            if (e.javaClass.simpleName != "FileAlreadyExistsException") {
                logger.error("Error generating TransitionRegistry: ${e.message}")
            }
        }
    }
}
```

---

### 7. GlobalRegistryGeneratorStrategy.kt
**作用**：主工程（App 壳工程）专属。通过 Resolver 全局扫描整个工程及依赖库中通过前几步生成的 `Module*Registry`，并生成聚合代码。

**关键机制解析：为什么采用“注解扫描 + 包名扫描”的双保险策略？**

在 KMP (Kotlin Multiplatform) 架构下，子模块（如 `feature-b`）编译后会生成 KLIB (Kotlin Library) 产物。
*   **什么是 KLIB Metadata？**
    Kotlin 代码最终要被编译成其他平台的产物（如 Android 的 `.class` 或 iOS 的机器码），但底层平台并不认识 Kotlin 的高级特性（如扩展函数、协程、空安全、类型别名等）。为了让编译器在引用第三方库时能知道这些“隐藏属性”，Kotlin 编译器会在生成的二进制产物中塞入一份数据，这就是 **Metadata（元数据）**。在 KMP 中，它被打包在 KLIB 描述文件里，用来告诉依赖它的模块：“我有哪些类、函数，以及它们上面的**注解**”。

主工程的 KSP 在扫描这些 KLIB 时，由于 KSP 底层解析跨平台 Metadata 的缺陷（特别是跨平台的 `commonMain` source set 的预编译产物处理上），经常会**漏读** KLIB 中类身上的注解。这会导致 `resolver.getSymbolsWithAnnotation()` 只能扫到主模块自己刚生成的类，而丢失所有子模块的路由表。

为了解决这个致命问题，我们引入了包名扫描作为兜底：
1. **包名扫描一定慢吗？**
   不慢。KSP 内部维护了基于内存的符号索引表（Symbol Index）。当我们传入 `"shijing.tianqu.router.generated"` 这个极其具体的包名时，KSP 是直接去 Hash Map 里命中查找的，复杂度接近 O(1)。加上该包下类极少，过滤操作在微秒级完成，根本不存在性能瓶颈。
2. **既然包扫描能兜底，为什么还要保留注解扫描？**
   * **捕获当前模块新产物**：在 KSP 多轮处理中，当前模块刚生成的代码，其包名索引可能还未完美构建，但注解符号最先被识别。注解扫描能 100% 抓住当前主模块的新类。
   * **严谨性与契约精神**：包名只是一种弱约定，而 `@ModuleRouteRegistry` 注解是“官方防伪身份证”，防止误扫同名类。
   * **防御性编程与未来扩展**：如果未来允许开发者自定义生成类的包名，包名扫描就会瘫痪。保留注解扫描是为了拥抱未来（如 KSP 2.0 修复 Metadata 缺陷后），届时无需改动代码即可完美接管。

因此，“注解扫描（正规军） + 包名扫描（兜底补丁）”是兼顾架构优雅与绝对稳定性的最优解。

```kotlin
package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo

class GlobalRegistryGeneratorStrategy(private val resolver: Resolver) : CodeGenerationStrategy<KSClassDeclaration> {

    @OptIn(com.google.devtools.ksp.KspExperimental::class)
    override fun generate(symbols: List<KSClassDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val packageName = "shijing.tianqu.router.generated"
        val className = "GlobalRouteAggregator"

        // 解决 KMP Metadata 下，直接扫注解可能漏扫依赖库产物的问题
        // 通过目标包名进行全量检索兜底
        val generatedPackage = "shijing.tianqu.router.generated"
        val packageDeclarations = resolver.getDeclarationsFromPackage(generatedPackage)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        // 1. 获取所有模块的 RouteRegistry（带注解扫描 + 包名前缀匹配兜底）
        val routeRegistries = (resolver.getSymbolsWithAnnotation("shijing.tianqu.router.aggregation.ModuleRouteRegistry")
            .filterIsInstance<KSClassDeclaration>() + packageDeclarations.filter { it.simpleName.asString().startsWith("RouteRegistry_") })
            .distinct()
            .toList()

        // 2. 获取所有模块的 ServiceRegistry
        val serviceRegistries = (resolver.getSymbolsWithAnnotation("shijing.tianqu.router.aggregation.ModuleServiceRegistry")
            .filterIsInstance<KSClassDeclaration>() + packageDeclarations.filter { it.simpleName.asString().startsWith("ServiceRegistry_") })
            .distinct()
            .toList()

        // 3. 获取所有模块的 TransitionRegistry
        val transitionRegistries = (resolver.getSymbolsWithAnnotation("shijing.tianqu.router.aggregation.ModuleTransitionRegistry")
            .filterIsInstance<KSClassDeclaration>() + packageDeclarations.filter { it.simpleName.asString().startsWith("TransitionStrategyRegistry_") })
            .distinct()
            .toList()

        val routeNodeType = ClassName("shijing.tianqu.runtime", "RouterNode")
        val listType = ClassName("kotlin.collections", "List").parameterizedBy(routeNodeType)

        // 把所有子模块生成的路由 List 相加
        // 最终效果：listOf() + RouteRegistry_A.routers + RouteRegistry_B.routers ...
        val routersInit = CodeBlock.builder().add("listOf<%T>()", routeNodeType)
        routeRegistries.forEach { registry ->
            val registryClass = ClassName(registry.packageName.asString(), registry.simpleName.asString())
            routersInit.add(" + %T.routers", registryClass)
        }

        // 把所有子模块生成的 Service Map 和 Transition Map 也进行相加组合
        val servicesInit = CodeBlock.builder().add("emptyMap<%T, %T>()", kClassType, functionType)
        serviceRegistries.forEach { registry ->
            val registryClass = ClassName(registry.packageName.asString(), registry.simpleName.asString())
            servicesInit.add(" + %T.services", registryClass)
        }

        val transitionsInit = CodeBlock.builder().add("emptyMap<%T, %T>()", ClassName("kotlin", "String"), LambdaTypeName.get(returnType = transitionStrategyType))
        transitionRegistries.forEach { registry ->
            val registryClass = ClassName(registry.packageName.asString(), registry.simpleName.asString())
            transitionsInit.add(" + %T.transitions", registryClass)
        }

        // 组装最终类属性并生成单例对象
        val routersProperty = PropertySpec.builder("routers", listType).initializer(routersInit.build()).build()
        val servicesProperty = PropertySpec.builder("services", serviceMapType).initializer(servicesInit.build()).build()
        val transitionsProperty = PropertySpec.builder("transitions", transitionMapType).initializer(transitionsInit.build()).build()

        val typeSpec = TypeSpec.objectBuilder(className)
            .addProperty(routersProperty)
            .addProperty(servicesProperty)
            .addProperty(transitionsProperty)
            .build()

        val fileSpec = FileSpec.builder(packageName, className).addType(typeSpec).build()

        try {
            val sourceFiles = (routeRegistries + serviceRegistries + transitionRegistries).mapNotNull { it.containingFile }.toTypedArray()
            val dependencies = Dependencies(aggregating = true, *sourceFiles)
            fileSpec.writeTo(codeGenerator, dependencies)
        } catch (e: Exception) {
            logger.error("Error generating GlobalRouteAggregator: ${e.message}")
        }
    }
}