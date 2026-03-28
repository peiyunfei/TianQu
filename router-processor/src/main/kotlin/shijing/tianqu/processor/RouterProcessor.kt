package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import shijing.tianqu.router.Router
import shijing.tianqu.router.Service
import shijing.tianqu.router.Transition

/**
 * 路由符号处理器。
 * 使用 KSP（Kotlin Symbol Processing）在编译期间扫描所有标记了 @Router 注解的函数（Composable），
 * 并自动生成注册表代码 `RouteRegistry`，包含了所有路由的路径、正则表达式模式以及关联的转场动画配置。
 */
class RouterProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {
    private var isGeneratedGlobal = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 使用 warn 而不是 info，因为 Gradle 默认会屏蔽 info 级别的日志。
        // 为了方便在控制台排查注解处理器的问题，这里使用 warn 来打印调试信息。
        logger.warn("----> RouterProcessor start processing <----")
        
        val deferredSymbols = mutableListOf<KSAnnotated>()

        // 1. 获取所有带 @Router 的符号
        val routerSymbols = resolver.getSymbolsWithAnnotation(Router::class.qualifiedName ?: "")
        val functionDeclarations = routerSymbols.filterIsInstance<KSFunctionDeclaration>()
        val validFunctions = functionDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(functionDeclarations.filterNot { it.validate() })

        // 2. 获取所有带 @Service 的符号
        val serviceSymbols = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName ?: "")
        val classDeclarations = serviceSymbols.filterIsInstance<KSClassDeclaration>()
        val validClasses = classDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(classDeclarations.filterNot { it.validate() })

        // 3. 获取所有带 @Transition 的符号
        val transitionSymbols = resolver.getSymbolsWithAnnotation(Transition::class.qualifiedName ?: "")
        val transitionDeclarations = transitionSymbols.filterIsInstance<KSClassDeclaration>()
        val validTransitions = transitionDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(transitionDeclarations.filterNot { it.validate() })

        val hasNewFiles = validFunctions.isNotEmpty() || validClasses.isNotEmpty() || validTransitions.isNotEmpty()

        val moduleNameOption = options["tianqu.moduleName"]
        if (moduleNameOption.isNullOrBlank() && hasNewFiles) {
            // 如果模块内有路由节点、服务或转场动画，但开发者忘记配置模块名，则抛出明确的编译错误
            logger.error("TianQu Router: KSP argument 'tianqu.moduleName' is missing! You must configure it in build.gradle.kts. Example: ksp { arg(\"tianqu.moduleName\", project.name) }")
            return emptyList()
        }
        
        val moduleName = (moduleNameOption ?: "Default")
            .replace("[^a-zA-Z0-9]".toRegex(), "_")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // 生成路由注册表
        if (validFunctions.isNotEmpty()) {
            RouterRegistryGeneratorStrategy(moduleName).generate(validFunctions, codeGenerator, logger)
        }

        // 生成服务注册表
        if (validClasses.isNotEmpty()) {
            ServiceRegistryGeneratorStrategy(moduleName).generate(validClasses, codeGenerator, logger)
        }

        // 生成转场动画注册表
        // 注意：即使 validTransitions 为空，我们也应该生成一个空的 TransitionStrategyRegistry
        // 这是为了保证 RouteRegistry 中引用 TransitionStrategyRegistry 不会报 Unresolved reference 错误。
        TransitionRegistryGeneratorStrategy(moduleName).generate(validTransitions, codeGenerator, logger)

        // 4. 策略：处理 App 级别的全局路由聚合
        val isApp = options["tianqu.isApp"] == "true"
        
        if (isApp) {
            // 我们在没有新文件生成的 Round (也就是最后阶段) 进行全局路由聚合
            // 确保依赖模块的所有 KSP 产物都已经生成并可被扫描到
            if (!hasNewFiles && !isGeneratedGlobal) {
                GlobalRegistryGeneratorStrategy(resolver).generate(emptyList(), codeGenerator, logger)
                isGeneratedGlobal = true
            }
        }

        return deferredSymbols
    }
}