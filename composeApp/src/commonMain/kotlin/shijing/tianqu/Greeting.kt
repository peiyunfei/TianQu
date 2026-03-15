package shijing.tianqu

/**
 * 跨平台欢迎信息生成类。
 */
class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}