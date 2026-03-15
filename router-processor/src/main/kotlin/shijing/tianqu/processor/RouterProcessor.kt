package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import shijing.tianqu.router.RouteContext
import shijing.tianqu.router.Router

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
        
        // 查找所有带 @Router 的符号
        val symbols = resolver.getSymbolsWithAnnotation(Router::class.qualifiedName ?: "")
        // 我们只处理函数 (KSFunctionDeclaration)
        val functionDeclarations = symbols.filterIsInstance<KSFunctionDeclaration>()
        
        // 过滤出解析完成的符号
        val validFunctions = functionDeclarations.filter { it.validate() }.toList()
        val deferredSymbols = functionDeclarations.filterNot { it.validate() }.toList()

        if (validFunctions.isNotEmpty()) {
            generateRouteRegistry(validFunctions)
        }

        return deferredSymbols
    }

    private fun generateRouteRegistry(functions: List<KSFunctionDeclaration>) {
        val packageName = "shijing.tianqu.router.generated"
        val className = "RouteRegistry"

        // 引入 Compose 的注解
        val composableAnnotation = ClassName("androidx.compose.runtime", "Composable")
        // 引入 RouteContext
        val routeContextClass = ClassName("shijing.tianqu.router", "RouteContext")
        // 引入 RouteTransition
        val transitionClass = ClassName("shijing.tianqu.router", "RouteTransition")
        
        // 定义生成代码中的 RouteNode 数据类类型
        val routeNodeType = ClassName("shijing.tianqu.runtime", "RouteNode")
        
        // 构建 List 的类型：List<RouteNode>
        val listType = ClassName("kotlin.collections", "List").parameterizedBy(routeNodeType)

        // 生成初始化 List 的代码块
        val initBlock = CodeBlock.builder()
            .add("listOf(\n")
            .indent()

        functions.forEachIndexed { index, func ->
            val annotation = func.annotations.first {
                it.shortName.asString() == Router::class.simpleName
            }
            // 获取注解中的参数
            val pathArg = annotation.arguments.first { it.name?.asString() == "path" }
            val pathValue = pathArg.value as String
            
            // 将 {id} 替换为正则表达式 ([\w-]+)，用于运行时匹配
            val regexPattern = pathValue.replace(Regex("\\{[^/]+\\}"), "([\\\\w-]+)")
            
            // 获取枚举值 (默认值为 Slide)
            // KSP 中枚举参数的值是一个 KSClassDeclaration，我们需要获取其简短名称
            fun getEnumName(argName: String): String {
                val arg = annotation.arguments.find { it.name?.asString() == argName }
                val type = arg?.value as? KSType
                return type?.declaration?.simpleName?.asString() ?: "Slide"
            }
            
            val enterTransition = getEnumName("enterTransition")
            val exitTransition = getEnumName("exitTransition")
            
            // 获取函数名和所在的包
            val funcName = func.simpleName.asString()
            val funcPackage = func.packageName.asString()
            val funcClassName = ClassName(funcPackage, funcName)

            val comma = if (index < functions.size - 1) ",\n" else "\n"
            
            // 构造 RouteNode 实例
            // RouteNode(path, regexPattern, enterTransition, exitTransition, { context -> func(context) })
            // 只有当目标函数接受 RouteContext 参数时才传递

            val hasContextParam = func.parameters.any {
                it.type.resolve().declaration.qualifiedName?.asString() == RouteContext::class.qualifiedName
            }
            
            val composableCall = if (hasContextParam) {
                "{ context -> %T(context) }"
            } else {
                "{ _ -> %T() }"
            }

            initBlock.add(
                "RouteNode(\n" +
                "    path = %S,\n" +
                "    regexPattern = %S,\n" +
                "    enterTransition = %T.%L,\n" +
                "    exitTransition = %T.%L,\n" +
                "    composable = $composableCall\n" +
                ")%L",
                pathValue,
                "^$regexPattern\$", // 添加首尾匹配
                transitionClass, enterTransition,
                transitionClass, exitTransition,
                funcClassName,
                comma
            )
        }

        initBlock.unindent().add(")")

        // 创建 routers 属性
        val propertySpec = PropertySpec.builder("routers", listType)
            .initializer("%L", initBlock.build())
            .build()

        // 创建 RouteRegistry Object
        val typeSpec = TypeSpec.objectBuilder(className)
            .addProperty(propertySpec)
            .build()

        // 创建文件
        val fileSpec = FileSpec.builder(packageName, className)
            .addType(typeSpec)
            .build()

        try {
            // 使用聚合依赖
            val dependencies = Dependencies(aggregating = true, *functions.mapNotNull { it.containingFile }.toTypedArray())
            fileSpec.writeTo(codeGenerator, dependencies)
            // 使用 warn 来保证在 Gradle 默认控制台中能够看到成功的输出
            logger.warn("----> Generated RouteRegistry successfully with ${functions.size} routes. <----")
        } catch (e: Exception) {
            logger.error("Error generating RouteRegistry: ${e.message}")
        }
    }
}