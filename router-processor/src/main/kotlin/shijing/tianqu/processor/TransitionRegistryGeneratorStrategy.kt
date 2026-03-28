package shijing.tianqu.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import shijing.tianqu.router.Transition

/**
 * 负责扫描带有 @Transition 注解的类，生成过渡动画的注册表
 */
class TransitionRegistryGeneratorStrategy(private val moduleName: String = "Default") : CodeGenerationStrategy<KSClassDeclaration> {
    override fun generate(symbols: List<KSClassDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val classes = symbols
        val packageName = "shijing.tianqu.router.generated"
        val className = "TransitionStrategyRegistry_$moduleName"

        // Any 类的类型
        val anyType = ClassName("kotlin", "Any")
        
        // TransitionStrategy 接口的类型
        val transitionStrategyType = ClassName("shijing.tianqu.runtime.transition", "TransitionStrategy")

        // Map<String, () -> TransitionStrategy> 类型
        val mapType = ClassName("kotlin.collections", "Map").parameterizedBy(
            ClassName("kotlin", "String"),
            LambdaTypeName.get(returnType = transitionStrategyType)
        )

        // 生成初始化 Map 的代码块
        val initBlock = CodeBlock.builder()
            .add("mapOf(\n")
            .indent()

        // 默认内置注册的动画：Slide, Fade, Scale, None
        initBlock.add("%S to { shijing.tianqu.runtime.transition.SlideTransitionStrategy() },\n", "Slide")
        initBlock.add("%S to { shijing.tianqu.runtime.transition.FadeTransitionStrategy() },\n", "Fade")
        initBlock.add("%S to { shijing.tianqu.runtime.transition.ScaleTransitionStrategy() },\n", "Scale")
        initBlock.add("%S to { shijing.tianqu.runtime.transition.NoneTransitionStrategy() }", "None")

        if (classes.isNotEmpty()) {
            initBlock.add(",\n")
        } else {
            initBlock.add("\n")
        }

        classes.forEachIndexed { index, cls ->
            val annotation = cls.annotations.first {
                it.shortName.asString() == Transition::class.simpleName
            }
            
            // 获取 name 参数，这个 name 对应 @Router 里的动画名称
            val nameArg = annotation.arguments.first { it.name?.asString() == "name" }
            val transitionName = nameArg.value as String

            val clsName = cls.simpleName.asString()
            val clsPackage = cls.packageName.asString()
            val clsClassName = ClassName(clsPackage, clsName)

            val comma = if (index < classes.size - 1) ",\n" else "\n"
            initBlock.add("%S to { %T() }%L", transitionName, clsClassName, comma)
        }

        initBlock.unindent().add(")")

        // 创建 transitions 属性
        val propertySpec = PropertySpec.builder("transitions", mapType)
            .initializer("%L", initBlock.build())
            .build()

        // 创建 TransitionStrategyRegistry Object
        val typeSpec = TypeSpec.objectBuilder(className)
            .addAnnotation(ClassName("shijing.tianqu.router.aggregation", "ModuleTransitionRegistry"))
            .addProperty(propertySpec)
            .build()

        // 创建文件
        val fileSpec = FileSpec.builder(packageName, className)
            .addType(typeSpec)
            .build()

        try {
            val dependencies = if (classes.isNotEmpty()) {
                Dependencies(aggregating = true, *classes.mapNotNull { it.containingFile }.toTypedArray())
            } else {
                Dependencies.ALL_FILES
            }
            fileSpec.writeTo(codeGenerator, dependencies)
            logger.warn("----> Generated TransitionStrategyRegistry successfully with ${classes.size + 4} transitions. <----")
        } catch (e: Exception) {
            if (e.javaClass.simpleName == "FileAlreadyExistsException") {
                // Ignore FileAlreadyExistsException in multi-round or multi-target KSP tasks
            } else {
                logger.error("Error generating TransitionStrategyRegistry: ${e.message}")
            }
        }
    }
}
