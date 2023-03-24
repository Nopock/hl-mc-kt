package net.willemml.hlktmc.minecraft

import com.github.steveice10.mc.auth.data.GameProfile
import com.github.steveice10.mc.protocol.MinecraftProtocol
import com.github.steveice10.mc.protocol.data.game.ClientCommand
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundClientCommandPacket
import com.github.steveice10.packetlib.AbstractServer
import com.github.steveice10.packetlib.Client
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import com.github.steveice10.packetlib.packet.Packet
import com.github.steveice10.packetlib.tcp.TcpClientSession
import com.github.steveice10.packetlib.tcp.TcpSessionFactory
import kotlinx.coroutines.delay
import net.daporkchop.lib.minecraft.text.component.MCTextRoot
import net.daporkchop.lib.minecraft.text.parser.AutoMCFormatParser
import net.daporkchop.lib.minecraft.text.util.TranslationSource
import net.willemml.hlktmc.ClientConfig
import net.willemml.hlktmc.minecraft.player.*
import net.willemml.hlktmc.minecraft.session.ClientSessionAdapter
import net.willemml.hlktmc.minecraft.world.WorldManager
import net.willemml.hlktmc.minecraft.world.types.ChunkPos
import java.net.Proxy
import java.util.*
import kotlin.collections.HashMap

open class BasicClient(val config: ClientConfig = ClientConfig()) {
    val protocol: MinecraftProtocol =
        if (config.password.isEmpty()) {
            println("No password")
            MinecraftProtocol(config.username)
        } else MinecraftProtocol(
            config.username,
            config.password
        )

    val client = Client(config.address, config.port, protocol, TcpSessionFactory())
    val test = TcpClientSession("127.0.0.1", 25565, Proxy.NO_PROXY)

    var joined = false

    private val hostPort = "${config.address}:${config.port}"

    val parser = AutoMCFormatParser(
        TranslationSource.ofMap(
            hashMapOf(
                Pair("chat.type.text", "<%s> %s"),
                Pair("chat.type.announcement", "[%s] %s")
            )
        )
    )

    var world = WorldManager()

    var playerListHeader = ""
    var playerListFooter = ""
    val playerListEntries = HashMap<UUID, GameProfile>()

    var player = Player(protocol.profile.name, protocol.profile.id ?: UUID.randomUUID(), client, world)

    init {
        client.session.addListener(ClientSessionAdapter(config, this))
    }

    suspend fun connect(): BasicClient {
        if (config.logConnection) connectionLog("", ConnectionLogType.CONNECTING)
        client.session.connect()
        while (!joined) {
            delay(5)
        }
        return this
    }

    fun disconnect(): BasicClient {
        if (config.logConnection) connectionLog("", ConnectionLogType.DISCONNECTING)
        client.session.disconnect("")
        return this
    }

    fun respawn(): BasicClient {
        client.session.send(ServerboundClientCommandPacket(ClientCommand.RESPAWN))
        if (config.logRespawns && joined) connectionLog("", ConnectionLogType.RESPAWN)
        return this
    }

    fun sendMessage(message: String): BasicClient {
        // TODO: COme back to review this and what to send for salt, other stuff too!
        client.session.send(ServerboundChatPacket(
            message = message,
            timeStamp = System.currentTimeMillis(),
            )
        )
        return this
    }

    fun sendPacket(packet: Packet) {
        client.session.send(packet)
    }

    fun sendClientSettings() {
        client.session.send(
            ClientSettingsPacket(
                config.locale,
                config.chunkUnloadDistance,
                config.chatVisibility,
                false,
                config.visibleParts,
                config.preferredHand
            )
        )
    }

    fun changeChunkDistance(newDistance: Int) {
        config.chunkUnloadDistance = newDistance
        sendClientSettings()
    }

    fun getNameFromID(id: UUID) = playerListEntries[id]?.name

    open fun logChat(message: String, messageType: Int, sender: UUID, rawMessage: MCTextRoot) {
        println("${getNameFromID(sender) ?: sender}@$hostPort ($messageType): $message")
    }

    open fun connectionLog(info: String, type: ConnectionLogType) {
        when (type) {
            ConnectionLogType.DISCONNECTED -> {
                if (info.isNotEmpty()) println("$hostPort Disconnected, reason: $info")
                else println("$hostPort Disconnected")
            }
            ConnectionLogType.DISCONNECTING -> println("$hostPort Disconnecting")
            ConnectionLogType.CONNECTED -> println("$hostPort Connected")
            ConnectionLogType.RESPAWN -> println("$hostPort Respawned")
            ConnectionLogType.CONNECTING -> println("$hostPort Connecting")
        }

    }

    open fun onJoin(packet: ClientboundLoginPacket) {}
    open fun onLeave(event: DisconnectedEvent) {}
    open fun onChat(message: String, sender: UUID, rawMessage: MCTextRoot) {}
}

enum class ConnectionLogType {
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    DISCONNECTING,
    RESPAWN
}

