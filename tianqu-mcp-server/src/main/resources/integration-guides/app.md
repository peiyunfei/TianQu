=== 天衢框架接入指南：App 主模块（{{moduleName}}）===

【重要说明】
- App 主模块是所有业务模块路由的聚合入口
- 必须配置 tianqu.isApp=true，KSP 才会生成全局路由聚合器 GlobalRouteAggregator
- Java {{minJavaVersion}} 是必填项，否则 KSP 可能编译失败

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Step 1：在 gradle/libs.versions.toml 中添加依赖声明
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[versions]
# 天衢路由框架版本，请替换为 Maven Central 上的最新版本
tianqu-router = "{{frameworkVersion}}"
# KSP 版本必须与项目的 Kotlin 版本严格对应，例如 Kotlin 2.1.10 对应 ksp 2.1.10-1.0.31
ksp = "{{kspVersion}}"

[libraries]
tianqu-router-annotations = { module = "io.gitee.zhongte:tianqu-router-annotations", version.ref = "tianqu-router" }
tianqu-router-processor = { module = "io.gitee.zhongte:tianqu-router-processor", version.ref = "tianqu-router" }
tianqu-router-runtime = { module = "io.gitee.zhongte:tianqu-router-runtime", version.ref = "tianqu-router" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Step 2：修改 {{moduleName}}/build.gradle.kts
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

plugins {
    // ...其他已有插件保持不变...
    // 引入 KSP 编译期注解处理器插件
    alias(libs.plugins.ksp)
}

kotlin {
    // 【必填】确保使用 Java {{minJavaVersion}}，KSP 代码生成依赖此版本
    jvmToolchain({{minJavaVersion}})

    sourceSets {
        commonMain.dependencies {
            // ...其他依赖保持不变...
            // 天衢注解库：提供 @Router、@Service、@Transition、@InjectViewModel 等注解
            implementation(libs.tianqu.router.annotations)
            // 天衢运行时库：提供 RouterHost、Navigator、ServiceManager 等核心运行时能力
            implementation(libs.tianqu.router.runtime)

            // 依赖所有业务模块（确保 KSP 可以扫描并聚合所有子模块的路由表）
            // implementation(project(":feature-login"))
            // implementation(project(":feature-home"))
        }
    }
}

// 【必填】声明当前模块为 App 主模块
// tianqu.isApp=true 触发 KSP 生成 GlobalRouteAggregator（全局路由聚合器）
ksp {
    arg("tianqu.moduleName", project.name)
    arg("tianqu.isApp", "true")
}

dependencies {
    // 注册天衢 KSP 处理器，在编译期扫描注解并生成路由表
    add("kspCommonMainMetadata", libs.tianqu.router.processor)
}

// 【必填】将 KSP 生成的聚合路由表代码目录加入 commonMain，使编译器能找到生成的类
kotlin.sourceSets.commonMain {
    kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
}

// 【必填】确保所有 Kotlin 编译任务在 KSP 代码生成完成后执行
tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinCompile<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Step 3：初始化 RouterHost（App.kt 或 CommonUI 入口）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

// 导入天衢框架生成的全局路由聚合器（由 KSP 自动生成，编译后可见）
import shijing.tianqu.router.generated.GlobalRouteAggregator
import shijing.tianqu.runtime.RouterHost
import shijing.tianqu.runtime.rememberNavigator
import shijing.tianqu.runtime.service.ServiceManager

@Composable
fun App() {
    // 【第1步】初始化跨模块服务通信大表
    // 必须在 RouterHost 启动前调用，否则 @Service 注解的实现类无法被发现
    ServiceManager.init(GlobalRouteAggregator.services)

    // 【第2步】创建并记住 Navigator 实例
    // routes：由 KSP 聚合的全工程路由表
    // startRoute：应用启动后展示的第一个页面路径
    val navigator = rememberNavigator(
        routes = GlobalRouteAggregator.routers,
        startRoute = "/home" // 替换为你的实际首页路径
    )

    MaterialTheme {
        // 【第3步】将 navigator 注入 RouterHost，由 RouterHost 负责渲染导航栈
        RouterHost(navigator = navigator)
    }
}
