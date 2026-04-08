package shijing.tianqu.router

/**
 * 标记一个类为 ViewModel。
 * 框架会在编译期间自动将该类注册为服务提供者。
 * 当需要时，可通过 `tianQuViewModelInject<YourViewModel>()` 获取实例。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class InjectViewModel
