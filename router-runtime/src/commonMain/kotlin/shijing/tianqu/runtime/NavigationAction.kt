package shijing.tianqu.runtime

/**
 * 导航动作枚举
 */
enum class NavigationAction {
    IDLE,
    PUSH,
    POP,
    POP_TO_ROOT,
    POP_UNTIL,
    REPLACE,
    CLEAR_AND_PUSH
}
