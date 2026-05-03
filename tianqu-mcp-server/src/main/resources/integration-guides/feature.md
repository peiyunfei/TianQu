=== 天衢框架接入指南：Feature 业务子模块（{{moduleName}}）===

【重要说明】
- 业务子模块只需配置 KSP 并声明模块名即可，KSP 会在编译期自动生成该模块的路由表
- 子模块不需要依赖其他业务模块，模块间通过路由路径和 @Service 接口通信，实现物理解耦
- 生成的路由表会被 App 主模块的 KSP 自动聚合，无需手动注册

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Step 1：确认 gradle/libs.versions.toml 已有以下配置（若已有则跳过）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

[versions]
tianqu-router = "{{frameworkVersion}}"
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
    // 【必填】确保使用 Java {{minJavaVersion}}
    jvmToolchain({{minJavaVersion}})

    sourceSets {
        commonMain.dependencies {
            // ...其他依赖保持不变...
            // 天衢注解库：提供 @Router、@Service 等注解
            implementation(libs.tianqu.router.annotations)
            // 天衢运行时库：提供页面内 Navigator、RouterContext 等运行时能力
            implementation(libs.tianqu.router.runtime)
        }
    }
}

// 【必填】为该模块指定名称，KSP 会以此为前缀生成路由注册类（如 FeatureLoginRouteRegistry）
// 建议直接使用 project.name 动态获取，避免硬编码
ksp {
    arg("tianqu.moduleName", project.name)
}

dependencies {
    // 注册天衢 KSP 处理器
    add("kspCommonMainMetadata", libs.tianqu.router.processor)
}

// 【必填】将 KSP 生成的路由表代码目录加入 commonMain
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
Step 3：声明你的第一个路由页面
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

import shijing.tianqu.router.Router
import shijing.tianqu.router.RouteType
import shijing.tianqu.runtime.RouterContext
import shijing.tianqu.runtime.LocalNavigator

// @Router 注解将此 Composable 函数注册为路由页面
// path：路由路径，全局唯一，支持路径变量如 /user/{id}
// transition：可选，指定页面切换动画，默认为 Slide
// type：可选，配置为全屏页面（SCREEN，默认）或悬浮弹窗（DIALOG）
@Router(
    path = "/{{moduleNameUnderscore}}/home",
    type = RouteType.SCREEN
)
@Composable
fun {{moduleNamePascal}}HomeScreen(
    // RouterContext 由框架自动注入，包含路由路径、路径参数、Query 参数及 extra 对象
    context: RouterContext
) {
    val navigator = LocalNavigator.current

    Column {
        Text("欢迎来到 {{moduleName}} 模块")
        Button(onClick = { navigator.pop() }) {
            Text("返回")
        }
    }
}
