package shijing.tianqu.router

/**
 * 标记一个类为自定义路由过渡动画策略。
 * 被此注解标记的类必须实现 `TransitionStrategy` 接口（通常继承 `BaseTransitionStrategy`）。
 *
 * @param name 该动画策略在路由注解中的引用名称。
 *             此名称对应于 `@Router(enterTransition = "xxx", exitTransition = "xxx")` 中的字符串。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Transition(
    val name: String
)
