package entry

fun main() = if (isWeChatEnv()) weChatMain() else webMain()