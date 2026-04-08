package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import shijing.tianqu.router.Router
import shijing.tianqu.router.Service
import shijing.tianqu.router.Transition
import shijing.tianqu.router.InjectViewModel

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

        // 2. 获取所有带 @Service 和 @InjectViewModel 注解的符号
        val serviceSymbols = resolver.getSymbolsWithAnnotation(Service::class.qualifiedName ?: "")
        val viewModelSymbols = resolver.getSymbolsWithAnnotation(InjectViewModel::class.qualifiedName ?: "")
        
        // 合并 Service 和 InjectViewModel 的符号，因为它们都使用相同的注册表生成策略
        val combinedServiceSymbols = serviceSymbols + viewModelSymbols
        
        // 过滤出类声明（因为 @Service 和 @InjectViewModel 标记在实现类上）
        val classDeclarations = combinedServiceSymbols.filterIsInstance<KSClassDeclaration>().distinct()
        // 验证符号是否有效（类型是否已完全解析）
        val validClasses = classDeclarations.filter { it.validate() }.toList()
        // 将无效符号加入 deferredSymbols，推迟到下一轮
        deferredSymbols.addAll(classDeclarations.filterNot { it.validate() })

        // 3. 获取所有带 @Transition 注解的符号
        val transitionSymbols = resolver.getSymbolsWithAnnotation(Transition::class.qualifiedName ?: "")
        // 过滤出类声明（因为 @Transition 标记在动画策略类上）
        val transitionDeclarations = transitionSymbols.filterIsInstance<KSClassDeclaration>()
        // 验证符号是否有效（类型是否已完全解析）
        val validTransitions = transitionDeclarations.filter { it.validate() }.toList()
        // 将无效符号加入 deferredSymbols，推迟到下一轮
        deferredSymbols.addAll(transitionDeclarations.filterNot { it.validate() })

        // 判断当前模块是否有需要处理的新文件
        val hasNewFiles = validFunctions.isNotEmpty() || validClasses.isNotEmpty() || validTransitions.isNotEmpty()

        // 从 KSP 参数中获取当前模块的名称
        val moduleNameOption = options["tianqu.moduleName"]
        // 如果有新文件但没有配置模块名，则抛出编译错误，强制开发者配置，避免生成的类名冲突
        if (moduleNameOption.isNullOrBlank() && hasNewFiles) {
            logger.error("TianQu Router: 缺失KSP 参数 'tianqu.moduleName'! 你需要在 build.gradle.kts 文件中添加代码: ksp { arg(\"tianqu.moduleName\", project.name) }")
            return emptyList()
        }

        // 格式化模块名，移除非字母数字字符，并将首字母大写，确保生成的类名合法
        val moduleName = (moduleNameOption ?: "Default")
            .replace("[^a-zA-Z0-9]".toRegex(), "_")
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

        // 如果有有效的路由函数，调用策略类生成当前模块的路由注册表
        if (validFunctions.isNotEmpty()) {
            RouterRegistryGeneratorStrategy(moduleName).generate(validFunctions, codeGenerator, logger)
        }

        // 如果有有效的服务类，调用策略类生成当前模块的服务注册表
        if (validClasses.isNotEmpty()) {
            ServiceRegistryGeneratorStrategy(moduleName).generate(validClasses, codeGenerator, logger)
        }

        // 生成转场动画注册表。注意：即使 validTransitions 为空也生成，
        // 这是为了保证 RouteRegistry 中引用 TransitionStrategyRegistry 时不会报找不到类的错误（提供默认动画）
        TransitionRegistryGeneratorStrategy(moduleName).generate(validTransitions, codeGenerator, logger)

        // 4. 处理 App 级别的全局路由聚合
        // 判断当前模块是否是主工程（壳工程）
        val isApp = options["tianqu.isApp"] == "true"

        if (isApp) {
            // 在没有新文件生成的 阶段 (也就是 KSP 的最后阶段) 进行全局路由聚合
            // 这样可以确保依赖模块的所有 KSP 产物都已经生成完毕并可被扫描到
            if (!hasNewFiles && !isGeneratedGlobal) {
                GlobalRegistryGeneratorStrategy(resolver).generate(emptyList(), codeGenerator, logger)
                isGeneratedGlobal = true // 标记为已生成，防止重复执行
            }
        }

        // 返回无法解析的符号，KSP 会在下一轮尝试重新解析它们
        return deferredSymbols
    }
}