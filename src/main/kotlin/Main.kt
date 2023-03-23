import net.willemml.hlktmc.cli.CLI
import net.willemml.hlktmc.http.server
import net.willemml.hlktmc.minecraft.bot.ChatBotManager
import net.willemml.hlktmc.minecraft.ResourceManager
import java.io.IOException

val chatBotManager = ChatBotManager("config/minecraft-bots.json")

@ExperimentalUnsignedTypes
fun main() {
    ResourceManager.apply {
        loadPaths()
        loadBlocks()
        loadItems()
        loadMaterials()
    }
    var failed = true
    var i = 8080
    while (failed) {
        try {
            server(i)
            failed = false
        } catch (_: IOException) {
            i++
        }
    }
    CLI()
}