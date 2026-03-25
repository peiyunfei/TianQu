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

/**
 * 负责生成 RouteRegistry 的具体策略
 */
class RouterRegistryGeneratorStrategy : CodeGenerationStrategy<KSFunctionDeclaration> {
    override fun generate(symbols: List<KSFunctionDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val functions = symbols
        val packageName = "shijing.tianqu.router.generated"
        val className = "RouteRegistry"

        // 引入 Compose 的注解
        val composableAnnotation = ClassName("androidx.compose.runtime", "Composable")
        // 引入 RouteContext
        val routeContextClass = ClassName("shijing.tianqu.router", "RouteContext")
        
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
            
            // 获取动画策略名称 (默认值为 "Slide")
            val transitionArg = annotation.arguments.find { it.name?.asString() == "transition" }
            val transitionName = transitionArg?.value?.toString() ?: "Slide"
            
            // 获取函数名和所在的包
            val funcName = func.simpleName.asString()
            val funcPackage = func.packageName.asString()
            val funcClassName = ClassName(funcPackage, funcName)

            val comma = if (index < functions.size - 1) ",\n" else "\n"
            
            // 构造 RouteNode 实例
            // RouteNode(path, regexPattern, transition, { context -> func(context) })
            // 只有当目标函数接受 RouteContext 参数时才传递

            val hasContextParam = func.parameters.any {
                it.type.resolve().declaration.qualifiedName?.asString() == RouterContext::class.qualifiedName
            }
            
            val composableCall = if (hasContextParam) {
                "{ context -> %T(context) }"
            } else {
                "{ _ -> %T() }"
            }

            // 在生成路由表时，通过 TransitionStrategyRegistry 去获取动画策略的实例工厂
            val transitionInstantiateStr = "shijing.tianqu.router.generated.TransitionStrategyRegistry.transitions[%S]?.invoke() ?: shijing.tianqu.runtime.transition.SlideTransitionStrategy()"

            initBlock.add(
                "RouteNode(\n" +
                "    path = %S,\n" +
                "    regexPattern = %S,\n" +
                "    transition = $transitionInstantiateStr,\n" +
                "    composable = $composableCall\n" +
                ")%L",
                pathValue,
                "^$regexPattern\$", // 添加首尾匹配
                transitionName,
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
