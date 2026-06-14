package shijing.tianqu

class OhosPlatform : Platform {
    override val name: String = "HarmonyOS"
}

actual fun getPlatform(): Platform = OhosPlatform()
