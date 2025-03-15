package platform

fun main() = if (isWeChatEnv()) weChatMain() else webMain()