package shijing.tianqu.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import shijing.tianqu.router.InjectViewModel
import shijing.tianqu.router.aggregation.ModuleServiceRegistry

/**
 * 负责生成 ServiceRegistry 的具体策略
 */
class ServiceRegistryGeneratorStrategy(private val moduleName: String = "Default") : CodeGenerationStrategy<KSClassDeclaration> {
    override fun generate(symbols: List<KSClassDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val classes = symbols
        val packageName = "shijing.tianqu.router.generated"
        val className = "ServiceRegistry_$moduleName"

        // 构建 Map 的类型：Map<KClass<*>, () -> Any>
        val kClassType = ClassName("kotlin.reflect", "KClass").parameterizedBy(STAR)
        val anyType = ClassName("kotlin", "Any")
        val functionType = LambdaTypeName.get(returnType = anyType)
        val mapType = ClassName("kotlin.collections", "Map").parameterizedBy(kClassType, functionType)

        val initBlock = CodeBlock.builder()
            .add("mapOf(\n")
            .indent()

        classes.forEachIndexed { index, ksClass ->
            // 检查是否带有 @InjectViewModel 注解
            val isViewModel = ksClass.annotations.any {
                it.shortName.asString() == (InjectViewModel::class.simpleName ?: "")
            }
            
            val keyClassName: ClassName? = if (isViewModel) {
                // 如果是 ViewModel，直接使用自身的 Class 作为 Key
                ClassName(
                    ksClass.packageName.asString(),
                    ksClass.simpleName.asString()
                )
            } else {
                // 如果是 Service，获取该类实现的第一个接口作为注册的 Key
                val superType = ksClass.superTypes.firstOrNull()?.resolve()
                val superDeclaration = superType?.declaration as? KSClassDeclaration
                if (superDeclaration != null && superDeclaration.classKind == ClassKind.INTERFACE) {
                    ClassName(
                        superDeclaration.packageName.asString(),
                        superDeclaration.simpleName.asString()
                    )
                } else {
                    null
                }
            }

            if (keyClassName != null) {
                val implClassName = ClassName(
                    ksClass.packageName.asString(),
                    ksClass.simpleName.asString()
                )

                val comma = if (index < classes.size - 1) ",\n" else "\n"
                // 告别反射，生成 Lambda 函数 `{ ImplClass() }` 以供运行时无反射懒加载调用
                initBlock.add(
                    "%T::class to { %T() }%L",
                    keyClassName,
                    implClassName,
                    comma
                )
            } else {
                logger.warn("Class ${ksClass.simpleName.asString()} annotated with @Service must implement at least one interface.")
            }
        }

        initBlock.unindent().add(")")

        val propertySpec = PropertySpec.builder("services", mapType)
            .initializer("%L", initBlock.build())
            .build()

        val typeSpec = TypeSpec.objectBuilder(className)
            // 加上特制聚合注解，给全局扫描提供入口
            .addAnnotation(ClassName(ModuleServiceRegistry::class.java.packageName,
                ModuleServiceRegistry::class.simpleName ?: ""))
            .addProperty(propertySpec)
            .build()

        val fileSpec = FileSpec.builder(packageName, className)
            .addType(typeSpec)
            .build()

        try {
            val dependencies = Dependencies(aggregating = true, *classes.mapNotNull { it.containingFile }.toTypedArray())
            fileSpec.writeTo(codeGenerator, dependencies)
            logger.warn("----> Generated ServiceRegistry successfully with ${classes.size} services. <----")
        } catch (e: Exception) {
            logger.error("Error generating ServiceRegistry: ${e.message}")
        }
    }
}
