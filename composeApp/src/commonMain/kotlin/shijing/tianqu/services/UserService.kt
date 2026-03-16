package shijing.tianqu.services

import shijing.tianqu.router.Service

/**
 * 示例服务接口，代表某个模块对外提供的能力。
 */
interface UserService {
    fun getUserName(): String
    fun isUserLoggedIn(): Boolean
}

/**
 * 示例服务实现类。
 * 使用 @Service 注解标记后，KSP 会自动将其注册到 ServiceRegistry 中，
 * 绑定到其实现的第一个接口 UserService 上。
 */
@Service
class UserServiceImpl : UserService {
    override fun getUserName(): String {
        return "TianQu Admin"
    }

    override fun isUserLoggedIn(): Boolean {
        return true
    }
}
