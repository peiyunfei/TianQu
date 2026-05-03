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

// 导入天衢框架生成的文件及高层装配入口
import shijing.tianqu.router.generated.GlobalRouteAggregator
import shijing.tianqu.runtime.rememberTianQuApp
import shijing.tianqu.runtime.RouterHost

@Composable
fun App() {
    // 【第1步】通过框架提供的零样板装配入口获取 navigator
    // 它会自动为您初始化服务表，并挂载 BackHandler 处理返回按键
    val navigator = rememberTianQuApp {
        // 【必填】注入 KSP 生成的路由表与服务表
        routes = GlobalRouteAggregator.routers
        serviceProviders = GlobalRouteAggregator.services

        // 【必填】应用启动后展示的第一个页面路径
        startRoute = "/home" // 替换为你的实际首页路径
        
        // （可选）配置路由守卫
        // this.guards = listOf(...)
        
        // （可选）配置并发预加载器
        // this.preloaders = mapOf(...)
        
        // （可选）全局路由事件回调，常用于 404 降级或打点
        onRouteEvent = { event, nav ->
            when (event) {
                is RouterEvent.NotFound -> {
                    println("拦截到未注册路由: ${event.url}")
                    nav.navigateTo("/home") // 发生 404 时强制重定向
                }
                else -> {}
            }
        }
    }

    MaterialTheme {
        // 【第2步】将 navigator 注入 RouterHost，由其负责渲染整个应用的导航栈
        RouterHost(navigator = navigator)
    }
}
