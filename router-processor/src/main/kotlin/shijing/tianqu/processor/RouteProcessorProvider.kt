package shijing.tianqu.processor

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * KSP 处理器工厂类
 * 通过 SPI 机制被 KSP 框架发现和调用
 */
class RouteProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return RouteProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}