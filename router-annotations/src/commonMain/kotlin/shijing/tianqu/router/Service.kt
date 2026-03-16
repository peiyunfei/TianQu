package shijing.tianqu.router

/**
 * 标记一个类为服务提供者，用于模块间通信。
 * 被此注解标记的类必须至少实现一个接口，框架会在编译期间自动将该类绑定到其实现的首个接口上，
 * 以便在其他模块中通过 `ServiceManager.getService<YourInterface>()` 获取该类的实例。
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Service
