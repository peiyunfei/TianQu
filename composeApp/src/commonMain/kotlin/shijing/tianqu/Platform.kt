package shijing.tianqu

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform