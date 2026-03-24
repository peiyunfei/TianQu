package shijing.tianqu.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotated

/**
 * 代码生成策略接口（策略模式）
 * 用于将不同类型注解的代码生成逻辑解耦
 */
interface CodeGenerationStrategy<T : KSAnnotated> {
    fun generate(symbols: List<T>, codeGenerator: CodeGenerator, logger: KSPLogger)
}
