package shijing.tianqu.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.writeTo
import shijing.tianqu.router.aggregation.ModuleRouterRegistry
import shijing.tianqu.router.aggregation.ModuleServiceRegistry
import shijing.tianqu.router.aggregation.ModuleTransitionRegistry

/**
 * 全局注册表生成策略。
 * 负责扫描依赖库中所有标注了 @Module*Registry 的注册表类，
 * 并在配置了 ksp 选项 `tianqu.isApp = "true"` 的壳工程中生成最终的聚合类 GlobalRouteAggregator。
 */
class GlobalRegistryGeneratorStrategy(private val resolver: Resolver) : CodeGenerationStrategy<KSClassDeclaration> {

    @OptIn(com.google.devtools.ksp.KspExperimental::class)
    override fun generate(symbols: List<KSClassDeclaration>, codeGenerator: CodeGenerator, logger: KSPLogger) {
        val packageName = "shijing.tianqu.router.generated"
        val className = "GlobalRouteAggregator"

        // KMP Metadata 下有时 getSymbolsWithAnnotation 无法发现依赖库的类
        // 因此我们同时扫描目标包 shijing.tianqu.router.generated
        val generatedPackage = "shijing.tianqu.router.generated"
        val packageDeclarations = resolver.getDeclarationsFromPackage(generatedPackage)
            .filterIsInstance<KSClassDeclaration>()
            .toList()

        // 1. 获取所有模块的 RouterRegistry
        val routeRegistries = (resolver.getSymbolsWithAnnotation(ModuleRouterRegistry::class.qualifiedName ?: "")
            .filterIsInstance<KSClassDeclaration>() + packageDeclarations.filter { it.simpleName.asString().startsWith("RouterRegistry_") })
            .distinct()
            .toList()

        // 2. 获取所有模块的 ServiceRegistry
        val serviceRegistries = (resolver.getSymbolsWithAnnotation(ModuleServiceRegistry::class.qualifiedName ?: "")
            .filterIsInstance<KSClassDeclaration>() + packageDeclarations.filter { it.simpleName.asString().startsWith("ServiceRegistry_") })
            .distinct()
            .toList()

        // 构建 init() 函数：fun init(navigator: Navigator) { ... }
        // 注意：GlobalRouteAggregator 可以作为整个路由初始化的地方，或者直接提供合并后的 routers/services/transitions
        
        // 方案：生成 properties，把所有的 List/Map 合并起来。
        // val allRouters: List<RouterNode> = ModuleA.routers + ModuleB.routers ...
        
        // RouterNode
        val routeNodeType = ClassName("shijing.tianqu.runtime", "RouterNode")
        val listType = ClassName("kotlin.collections", "List").parameterizedBy(routeNodeType)

        // Services
        val kClassType = ClassName("kotlin.reflect", "KClass").parameterizedBy(STAR)
        val anyType = ClassName("kotlin", "Any")
        val functionType = LambdaTypeName.get(returnType = anyType)
        val serviceMapType = ClassName("kotlin.collections", "Map").parameterizedBy(kClassType, functionType)

        // 生成合并 Routers 的代码
        val routersInit = CodeBlock.builder().add("listOf<%T>()", routeNodeType)
        routeRegistries.forEach { registry ->
            val registryClass = ClassName(registry.packageName.asString(), registry.simpleName.asString())
            routersInit.add(" + %T.routers", registryClass)
        }

        // 生成合并 Services 的代码
        val servicesInit = CodeBlock.builder().add("emptyMap<%T, %T>()", kClassType, functionType)
        serviceRegistries.forEach { registry ->
            val registryClass = ClassName(registry.packageName.asString(), registry.simpleName.asString())
            servicesInit.add(" + %T.services", registryClass)
        }

        val routersProperty = PropertySpec.builder("routers", listType)
            .initializer(routersInit.build())
            .build()

        val servicesProperty = PropertySpec.builder("services", serviceMapType)
            .initializer(servicesInit.build())
            .build()

        // 创建 GlobalRouteAggregator Object
        val typeSpec = TypeSpec.objectBuilder(className)
            .addProperty(routersProperty)
            .addProperty(servicesProperty)
            .build()

        // 创建文件
        val fileSpec = FileSpec.builder(packageName, className)
            .addType(typeSpec)
            .build()

        try {
            // Aggregating true is necessary as it depends on newly found classes
            val sourceFiles = (routeRegistries + serviceRegistries).mapNotNull { it.containingFile }.toTypedArray()
            val dependencies = Dependencies(aggregating = true, *sourceFiles)
            fileSpec.writeTo(codeGenerator, dependencies)
            logger.warn("----> Generated GlobalRouteAggregator successfully. Modules: Routers(${routeRegistries.size}), Services(${serviceRegistries.size}) <----")
        } catch (e: Exception) {
            logger.error("Error generating GlobalRouteAggregator: ${e.message}")
        }
    }
}
