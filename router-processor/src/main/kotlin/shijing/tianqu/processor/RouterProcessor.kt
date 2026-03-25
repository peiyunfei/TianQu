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
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 使用 warn 而不是 info，因为 Gradle 默认会屏蔽 info 级别的日志。
        // 为了方便在控制台排查注解处理器的问题，这里使用 warn 来打印调试信息。
        logger.warn("----> RouterProcessor start processing <----")
        
        val deferredSymbols = mutableListOf<KSAnnotated>()

        // 查找所有带 @Router 的符号并生成路由表
        // 1. 策略：处理路由节点
        val routerSymbols = resolver.getSymbolsWithAnnotation(Router::class.qualifiedName ?: "")
        val functionDeclarations = routerSymbols.filterIsInstance<KSFunctionDeclaration>()
        val validFunctions = functionDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(functionDeclarations.filterNot { it.validate() })

        if (validFunctions.isNotEmpty()) {
            RouterRegistryGeneratorStrategy().generate(validFunctions, codeGenerator, logger)
        }

        // 2. 策略：处理服务注入
        val serviceSymbols = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName ?: "")
        val classDeclarations = serviceSymbols.filterIsInstance<KSClassDeclaration>()
        val validClasses = classDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(classDeclarations.filterNot { it.validate() })

        if (validClasses.isNotEmpty()) {
            ServiceRegistryGeneratorStrategy().generate(validClasses, codeGenerator, logger)
        }

        // 3. 策略：处理自定义动画过渡注入
        val transitionSymbols = resolver.getSymbolsWithAnnotation(Transition::class.qualifiedName ?: "")
        val transitionDeclarations = transitionSymbols.filterIsInstance<KSClassDeclaration>()
        val validTransitions = transitionDeclarations.filter { it.validate() }.toList()
        deferredSymbols.addAll(transitionDeclarations.filterNot { it.validate() })

        // 注意：即使 validTransitions 为空，我们也应该生成一个空的 TransitionStrategyRegistry
        // 这是为了保证 RouteRegistry 中引用 TransitionStrategyRegistry 不会报 Unresolved reference 错误。
        TransitionRegistryGeneratorStrategy().generate(validTransitions, codeGenerator, logger)

        return deferredSymbols
    }
}