package shijing.tianqu.runtime

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import shijing.tianqu.runtime.service.ServiceManager
import kotlin.reflect.KClass

/**
 * 获取与当前页面 (StackEntry) 生命周期绑定的 ViewModel。
 *
 * 它的作用域被限制在当前的路由节点中。当该页面被从导航栈（Navigator.backStack）
 * 以及后台缓存栈中彻底移除时，该 ViewModel 的 onCleared() 将被自动调用，释放相关资源。
 *
 * **⚠️ 注意：此无参方法仅适用于 Android 平台。**
 * 在 iOS 等 Kotlin Native 平台上，由于缺乏运行时反射支持，调用此方法会导致崩溃。
 * 跨平台开发请务必使用带有 `factory` 参数的重载方法。
 */
@Composable
inline fun <reified T : ViewModel> tianQuViewModel(): T {
    return viewModel<T>()
}

/**
 * 获取与当前页面 (StackEntry) 生命周期绑定的 ViewModel，
 * 并支持通过自定义工厂创建带参数的 ViewModel。
 *
 * **🌟 推荐：此方法适用于所有跨端平台 (Android / iOS / Desktop)。**
 * 通过显式提供工厂闭包，避免了底层反射实例化，是 KMP 项目中获取 ViewModel 的最佳实践。
 */
@Composable
inline fun <reified T : ViewModel> tianQuViewModel(
    crossinline factory: () -> T
): T {
    return viewModel<T>(
        factory = object : ViewModelProvider.Factory {
            override fun <VM : ViewModel> create(modelClass: KClass<VM>, extras: CreationExtras): VM {
                @Suppress("UNCHECKED_CAST")
                return factory() as VM
            }
        }
    )
}

/**
 * 通过天衢的 ServiceLocator 机制，自动获取并注入跨平台 ViewModel 实例。
 *
 * 您只需在 ViewModel 类上添加 `@InjectViewModel`，
 * 框架在编译期会自动生成工厂方法并在运行时为您自动注入（免反射）。
 *
 * **🌟 推荐：这是一种优雅且 100% 兼容多端的依赖注入方式。**
 */
@Composable
inline fun <reified T : ViewModel> tianQuViewModelInject(): T {
    // 这里使用 crossinline factory 将闭包透传进去，
    // 获取注册在 ServiceManager 中的工厂方法，并调用实例化，然后交由底层 viewModel() 去绑定生命周期
    return tianQuViewModel(factory = {
        val factoryFunction = ServiceManager.getViewModelFactory(T::class)
            ?: throw IllegalStateException("未找到 ${T::class.simpleName} 的注入工厂。请确保该 ViewModel 已添加 @InjectViewModel 注解并重新编译项目。")
        factoryFunction.invoke()
    })
}

