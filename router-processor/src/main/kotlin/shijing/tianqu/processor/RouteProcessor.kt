package shijing.tianqu.processor

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import shijing.tianqu.router.Router

class RouteProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        // 查找所有带 @Route 的符号
        val symbols = resolver.getSymbolsWithAnnotation("shijing.tianqu.router.Router")
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
        
        // 构建 Map 的类型：Map<String, @Composable () -> Unit>
        val mapType = ClassName("kotlin.collections", "Map").parameterizedBy(
            STRING,
            LambdaTypeName.get(returnType = UNIT)
                .copy(annotations = listOf(AnnotationSpec.builder(composableAnnotation).build()))
        )

        // 生成初始化 Map 的代码块
        val initBlock = CodeBlock.builder()
            .add("mapOf(\n")
            .indent()

        functions.forEachIndexed { index, func ->
            val annotation = func.annotations.first {
                it.shortName.asString() == Router::class.simpleName
            }
            // 获取注解中的 path 参数
            val pathArg = annotation.arguments.first { it.name?.asString() == "path" }
            val pathValue = pathArg.value as String
            
            // 获取函数名和所在的包
            val funcName = func.simpleName.asString()
            val funcPackage = func.packageName.asString()
            val funcClassName = ClassName(funcPackage, funcName)

            val comma = if (index < functions.size - 1) ",\n" else "\n"
            
            // 例如: "/home" to { com.example.HomeScreen() }
            initBlock.add("%S to { %T() }%L", pathValue, funcClassName, comma)
        }

        initBlock.unindent().add(")")

        // 创建 routes 属性
        val propertySpec = PropertySpec.builder("routers", mapType)
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
            // 使用聚合依赖，因为我们把多个文件聚合成了一个注册表
            val dependencies = Dependencies(aggregating = true, *functions.mapNotNull { it.containingFile }.toTypedArray())
            fileSpec.writeTo(codeGenerator, dependencies)
            logger.info("Generated RouteRegistry successfully with ${functions.size} routes.")
        } catch (e: Exception) {
            logger.error("Error generating RouteRegistry: ${e.message}")
        }
    }
}