# KSP 从零开始完全指南

## 第一章：KSP 是什么？

### 一句话定义
**KSP (Kotlin Symbol Processing)** 是 Google 开发的一个**编译期代码处理工具**。它能在你点击"编译"的时候，扫描你写的 Kotlin 源代码，找到特定的注解/类/函数，然后**自动生成新的 Kotlin 文件**。

### 生活类比
想象你是一个大学教授（KSP），学生们交了作业（源代码）。你的工作是：
1. **扫描**所有作业，找到标了"需要批改"标签（注解）的作业
2. **读取**这些作业的内容（类名、函数名、参数类型等）
3. 根据作业内容**写出一份批改报告**（生成新的 .kt 文件）
4. 这份报告会和原始作业一起被存档（一起参与编译）

**关键：你只能写新报告，不能修改学生的原始作业。**（KSP 只能生成新文件，不能修改已有代码）

### KSP vs KAPT

| 对比项 | KAPT (老技术) | KSP (新技术) |
|--------|-------------|-------------|
| 全称 | Kotlin Annotation Processing Tool | Kotlin Symbol Processing |
| 原理 | 先把 Kotlin 转成 Java 的存根(stubs)，再用 Java 的注解处理器处理 | 直接处理 Kotlin 符号，不需要转换 |
| 速度 | 慢（需要生成 Java stubs） | **快 2 倍以上** |
| KMP 支持 | ❌ 不支持（只能处理 JVM） | ✅ 支持全平台 |
| Kotlin 特性支持 | 丢失（扩展函数、sealed class 等信息在 Java stubs 中丢失） | **完整保留**所有 Kotlin 特性 |
| 未来 | Google 已宣布逐步废弃 | 官方推荐的替代方案 |

---

## 第二章：核心概念——符号 (Symbol)

KSP 的名字中有 "Symbol Processing"（符号处理）。什么是符号？

**符号就是你代码中的每一个"有名字的东西"。**

```kotlin
package com.example              // ← 包名也是一种符号

@MyAnnotation                    // ← 注解
class User(                      // ← 类声明 (KSClassDeclaration)
    val name: String,            // ← 属性 (KSPropertyDeclaration)
    val age: Int
) {
    fun greet(): String {        // ← 函数 (KSFunctionDeclaration)
        return "Hello, $name"
    }
}
```

KSP 把这些代码解析成了一棵**符号树**，你可以用代码来遍历和读取它们：

| 你写的代码 | KSP 中的类型 | 你能读到什么 |
|-----------|-------------|------------|
| `class User` | `KSClassDeclaration` | 类名、父类、实现的接口、注解、泛型参数 |
| `val name: String` | `KSPropertyDeclaration` | 属性名、类型、是否可空、是否 var |
| `fun greet()` | `KSFunctionDeclaration` | 函数名、参数列表、返回类型 |
| `@MyAnnotation` | `KSAnnotation` | 注解名、注解参数值 |
| `String` | `KSType` | 类型全名、是否可空、泛型参数 |

---

## 第三章：KSP 处理器的工作流程

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   你的源代码       │     │   KSP 处理器      │     │   生成的新文件     │
│                  │     │   (你写的)        │     │                  │
│  @Route("/home") │ ──→ │  1. 找到注解      │ ──→ │  RouteTable.kt   │
│  class HomeScreen│     │  2. 读取信息      │     │                  │
│                  │     │  3. 生成代码      │     │  object RouteTable│
│  @Route("/user") │     │                  │     │    { ... }        │
│  class UserScreen│     │                  │     │                  │
└──────────────────┘     └──────────────────┘     └──────────────────┘
         ↑                                                 ↑
    开发者写的代码                                     KSP 自动生成的代码
    （编译前就存在）                                   （编译时才产生）
```

---

## 第四章：动手写第一个 KSP 处理器

### 目标
写一个 `@AutoToString` 注解。加了这个注解的 data class，KSP 会自动生成一个 `toFormattedString()` 扩展函数。

```kotlin
// 开发者写的：
@AutoToString
data class User(val name: String, val age: Int)

// KSP 自动生成的：
fun User.toFormattedString(): String {
    return "User(name=$name, age=$age)"
}
```

### 工程结构（3 个模块）

```
my-ksp-project/
├── annotations/          ← 模块1：放注解定义（纯 Kotlin，无依赖）
│   └── src/main/kotlin/
│       └── AutoToString.kt
│
├── processor/            ← 模块2：放 KSP 处理器（依赖 KSP API）
│   └── src/main/kotlin/
│       └── AutoToStringProcessor.kt
│       └── AutoToStringProvider.kt
│   └── src/main/resources/
│       └── META-INF/services/
│           └── com.google.devtools.ksp.processing.SymbolProcessorProvider
│
├── app/                  ← 模块3：使用注解的业务代码
│   └── src/main/kotlin/
│       └── Main.kt
│
├── build.gradle.kts      ← 根构建脚本
└── settings.gradle.kts
```

**为什么需要 3 个模块？**
- 注解模块被所有人依赖（处理器要读它，业务代码要用它）
- 处理器模块只在编译期工作，不会打包进最终的 App
- 业务代码模块通过 KSP 插件调用处理器

### 第一步：定义注解（annotations 模块）

```kotlin
// annotations/src/main/kotlin/com/example/AutoToString.kt
package com.example

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)  // 只在源码级别保留，不进字节码
annotation class AutoToString
```

`annotations/build.gradle.kts`:
```kotlin
plugins {
    kotlin("jvm")
}
```

### 第二步：编写 KSP 处理器（processor 模块）

这是核心部分。一个 KSP 处理器由两个类组成：

**1. SymbolProcessorProvider —— 工厂类（告诉 KSP 怎么创建你的处理器）**

```kotlin
// processor/src/main/kotlin/com/example/AutoToStringProvider.kt
package com.example

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class AutoToStringProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return AutoToStringProcessor(
            codeGenerator = environment.codeGenerator,  // 用来生成文件
            logger = environment.logger                  // 用来打印日志
        )
    }
}
```

**2. SymbolProcessor —— 处理器本体（扫描代码 + 生成文件）**

```kotlin
// processor/src/main/kotlin/com/example/AutoToStringProcessor.kt
package com.example

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import java.io.OutputStream

class AutoToStringProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    // 这个方法会被 KSP 调用，传入一个 Resolver（解析器）
    override fun process(resolver: Resolver): List<KSAnnotated> {
        
        // 第1步：找到所有带 @AutoToString 注解的符号
        val symbols = resolver.getSymbolsWithAnnotation("com.example.AutoToString")
        
        // 过滤出类声明（因为我们的注解只能加在 class 上）
        val classDeclarations = symbols.filterIsInstance<KSClassDeclaration>()
        
        // 收集无法处理的符号（延迟到下一轮处理）
        val unableToProcess = symbols.filterNot { it.validate() }.toList()
        
        // 第2步：遍历每个类，生成代码
        classDeclarations.forEach { classDecl ->
            generateToString(classDecl)
        }
        
        return unableToProcess
    }
    
    private fun generateToString(classDecl: KSClassDeclaration) {
        // 读取类的信息
        val className = classDecl.simpleName.asString()         // "User"
        val packageName = classDecl.packageName.asString()      // "com.example"
        
        // 读取所有属性
        val properties = classDecl.getAllProperties().toList()
        
        // 打印日志（会显示在编译输出中）
        logger.info("正在为 $className 生成 toFormattedString()")
        
        // 第3步：用 CodeGenerator 创建新文件
        val file: OutputStream = codeGenerator.createNewFile(
            dependencies = Dependencies(false, classDecl.containingFile!!),
            packageName = packageName,
            fileName = "${className}_AutoToString"  // 生成的文件名
        )
        
        // 第4步：往文件里写入生成的代码
        val code = buildString {
            appendLine("package $packageName")
            appendLine()
            appendLine("fun $className.toFormattedString(): String {")
            appendLine("    return buildString {")
            appendLine("        append(\"$className(\")")
            
            properties.forEachIndexed { index, prop ->
                val propName = prop.simpleName.asString()
                val separator = if (index < properties.size - 1) ", " else ""
                appendLine("        append(\"$propName=\$$propName$separator\")")
            }
            
            appendLine("        append(\")\")")
            appendLine("    }")
            appendLine("}")
        }
        
        file.write(code.toByteArray())
        file.close()
    }
}
```

**3. 注册处理器（通过 SPI 机制）**

创建文件 `processor/src/main/resources/META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider`：

```
com.example.AutoToStringProvider
```

这个文件告诉 KSP："嘿，`AutoToStringProvider` 是一个处理器提供者，请在编译时调用它。"

`processor/build.gradle.kts`:
```kotlin
plugins {
    kotlin("jvm")
}

dependencies {
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.22-1.0.17")
    implementation(project(":annotations"))
}
```

### 第三步：使用注解（app 模块）

```kotlin
// app/src/main/kotlin/com/example/Main.kt
package com.example

@AutoToString
data class User(val name: String, val age: Int)

fun main() {
    val user = User("张三", 25)
    // 调用 KSP 生成的扩展函数
    println(user.toFormattedString())
    // 输出: User(name=张三, age=25)
}
```

`app/build.gradle.kts`:
```kotlin
plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.9.22-1.0.17"
}

dependencies {
    implementation(project(":annotations"))
    ksp(project(":processor"))  // 关键！用 ksp 配置引入处理器
}
```

`settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "my-ksp-project"
include(":annotations", ":processor", ":app")
```

### 第四步：编译并查看生成的代码

```bash
./gradlew :app:kspKotlin
```

生成的文件在：`app/build/generated/ksp/main/kotlin/com/example/User_AutoToString.kt`

```kotlin
// 这是 KSP 自动生成的！
package com.example

fun User.toFormattedString(): String {
    return buildString {
        append("User(")
        append("name=$name, ")
        append("age=$age")
        append(")")
    }
}
```

---

## 第五章：KSP API 核心类速查

### Resolver（解析器）—— 你的入口

```kotlin
// 按注解查找
resolver.getSymbolsWithAnnotation("com.example.MyAnnotation")

// 获取所有文件
resolver.getAllFiles()

// 按全限定名查找类
resolver.getClassDeclarationByName("com.example.User")
```

### KSClassDeclaration（类声明）—— 读取类信息

```kotlin
classDecl.simpleName.asString()          // "User"
classDecl.qualifiedName?.asString()      // "com.example.User"
classDecl.packageName.asString()         // "com.example"
classDecl.classKind                      // CLASS, INTERFACE, ENUM_CLASS, OBJECT, ANNOTATION_CLASS
classDecl.modifiers                      // [DATA, PUBLIC]
classDecl.superTypes                     // 父类和接口列表
classDecl.typeParameters                 // 泛型参数
classDecl.getAllProperties()             // 所有属性（包括继承的）
classDecl.getAllFunctions()              // 所有函数
classDecl.primaryConstructor             // 主构造函数
classDecl.annotations                    // 所有注解
classDecl.containingFile                 // 所在的源文件
```

### KSPropertyDeclaration（属性声明）

```kotlin
prop.simpleName.asString()               // "name"
prop.type.resolve()                      // KSType → String
prop.type.resolve().isMarkedNullable     // 是否可空 (String?)
prop.isMutable                           // 是 var 还是 val
prop.hasBackingField                     // 是否有 backing field
```

### KSFunctionDeclaration（函数声明）

```kotlin
func.simpleName.asString()               // "greet"
func.parameters                          // 参数列表 List<KSValueParameter>
func.returnType?.resolve()               // 返回类型
func.modifiers                           // [SUSPEND, PUBLIC, OVERRIDE]
```

### KSAnnotation（注解）

```kotlin
annotation.shortName.asString()          // "Route"
annotation.annotationType.resolve()      // 注解的完整类型
annotation.arguments                     // 注解参数列表

// 读取注解参数值
annotation.arguments.forEach { arg ->
    val name = arg.name?.asString()      // 参数名，如 "path"
    val value = arg.value                // 参数值，如 "/home"
}
```

### CodeGenerator（代码生成器）—— 写出新文件

```kotlin
codeGenerator.createNewFile(
    dependencies = Dependencies(
        aggregating = false,             // 是否是聚合式处理（后面解释）
        classDecl.containingFile!!       // 依赖的源文件（用于增量编译）
    ),
    packageName = "com.example",
    fileName = "GeneratedFile",          // 不需要 .kt 后缀
    extensionName = "kt"                 // 默认就是 kt
)
```

---

## 第六章：增量编译与多轮处理

### 增量编译 (Incremental Processing)

KSP 支持增量编译——只有被修改的文件才会重新处理，大幅提升编译速度。

关键在于 `Dependencies` 参数：
```kotlin
// aggregating = false：隔离式（推荐）
// 意味着每个输出文件只依赖特定的输入文件
// 如果 User.kt 改了，只重新生成 User 相关的代码
Dependencies(false, classDecl.containingFile!!)

// aggregating = true：聚合式
// 意味着输出文件依赖所有输入文件
// 任何文件改了，都要重新生成所有代码
Dependencies(true, *resolver.getAllFiles().toList().toTypedArray())
```

### 多轮处理 (Multiple Rounds)

`process()` 方法的返回值 `List<KSAnnotated>` 表示"这些符号我这一轮处理不了，请下一轮再给我"。

```kotlin
override fun process(resolver: Resolver): List<KSAnnotated> {
    val symbols = resolver.getSymbolsWithAnnotation("com.example.AutoToString")
    
    // validate() 检查符号的所有类型引用是否已解析完毕
    val valid = symbols.filter { it.validate() }
    val deferred = symbols.filterNot { it.validate() }.toList()
    
    valid.forEach { /* 处理 */ }
    
    return deferred  // 返回延迟处理的符号
}
```

为什么需要多轮？因为一个 KSP 处理器生成的代码，可能被另一个处理器需要。第一轮时那些类型还不存在，需要等生成后再处理。

---

## 第七章：进阶——使用 KotlinPoet 生成代码

手动拼接字符串容易出错。实际项目中，我们用 **KotlinPoet** 这个库来优雅地生成 Kotlin 代码：

```kotlin
// processor/build.gradle.kts
dependencies {
    implementation("com.squareup:kotlinpoet:1.15.3")
    implementation("com.squareup:kotlinpoet-ksp:1.15.3")  // KSP 集成
}
```

```kotlin
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

private fun generateToString(classDecl: KSClassDeclaration) {
    val className = classDecl.simpleName.asString()
    val packageName = classDecl.packageName.asString()
    val properties = classDecl.getAllProperties().toList()
    
    // 用 KotlinPoet 构建函数
    val funSpec = FunSpec.builder("toFormattedString")
        .receiver(ClassName(packageName, className))  // 扩展函数的接收者
        .returns(String::class)
        .addStatement("return buildString {")
        .addStatement("  append(%S)", "$className(")
        .apply {
            properties.forEachIndexed { i, prop ->
                val name = prop.simpleName.asString()
                val sep = if (i < properties.size - 1) ", " else ""
                addStatement("  append(%S + %L + %S)", "$name=", name, sep)
            }
        }
        .addStatement("  append(%S)", ")")
        .addStatement("}")
        .build()
    
    // 用 KotlinPoet 构建文件
    val fileSpec = FileSpec.builder(packageName, "${className}_AutoToString")
        .addFunction(funSpec)
        .build()
    
    // 写出文件（KotlinPoet-KSP 集成方法）
    fileSpec.writeTo(codeGenerator, Dependencies(false, classDecl.containingFile!!))
}
```

KotlinPoet 的优势：
- 自动处理 import
- 类型安全（不会拼错类名）
- 自动格式化缩进
- 支持泛型、注解、可见性修饰符等复杂场景

---

**核心资源**：
- 官方文档：https://kotlinlang.org/docs/ksp-overview.html
- 官方示例：https://github.com/google/ksp/tree/main/examples
- KotlinPoet：https://square.github.io/kotlinpoet/