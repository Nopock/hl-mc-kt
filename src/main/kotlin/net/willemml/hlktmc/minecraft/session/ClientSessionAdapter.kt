package net.willemml.hlktmc.minecraft.session

import com.github.steveice10.mc.protocol.data.game.entity.player.PositionElement
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatHeaderPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerChatPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPlayerInfoPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundTabListPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityPosRotPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundMoveEntityRotPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.ClientboundTeleportEntityPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundPlayerPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundLevelChunkWithLightPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetChunkCacheCenterPacket
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.level.ClientboundSetDefaultSpawnPositionPacket
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.level.ServerboundAcceptTeleportationPacket
import com.github.steveice10.mc.protocol.packet.login.clientbound.ClientboundGameProfilePacket
import com.github.steveice10.packetlib.Session
import com.github.steveice10.packetlib.event.session.ConnectedEvent
import com.github.steveice10.packetlib.event.session.DisconnectedEvent
import com.github.steveice10.packetlib.event.session.SessionAdapter
import com.github.steveice10.packetlib.packet.Packet
import net.willemml.hlktmc.ClientConfig
import net.willemml.hlktmc.minecraft.BasicClient
import net.willemml.hlktmc.minecraft.ConnectionLogType
import net.willemml.hlktmc.minecraft.player.*
import net.willemml.hlktmc.minecraft.world.types.ChunkPos

class ClientSessionAdapter(val config: ClientConfig, val client: BasicClient) : SessionAdapter() {

    override fun connected(event: ConnectedEvent?) {
        if (config.logConnection) client.connectionLog("", ConnectionLogType.CONNECTED)
    }

    override fun packetReceived(session: Session?, packet: Packet?) {
        when (packet) {
            is ClientboundLoginPacket -> {
                client.player.entityID = packet.entityId
                client.player.gameMode = packet.gameMode
                client.sendClientSettings()
                client.joined = true
                client.player.positioning.onJoin()
                client.onJoin(packet)
            }
            is ClientboundGameProfilePacket -> {
                client.player = Player(packet.profile.name, packet.profile.id, client, client.world)
            }
            is ClientboundSetDefaultSpawnPositionPacket -> {
                // This used to be ServerSpawnPositionPacket
                val position = packet.position
                client.player.spawnPoint = Position(position.x.toDouble(), position.y.toDouble(), position.z.toDouble())
            }
            // TODO: Verify ClientBoundPlayerPositionPacket is the same as https://github.com/GeyserMC/MCProtocolLib/blob/ca928aff219721bbba305d69af16f4c952a5d36c/src/main/java/com/github/steveice10/mc/protocol/packet/ingame/server/entity/player/ServerPlayerPositionRotationPacket.java
            is ClientboundPlayerPositionPacket -> {
                val deltaX = if (packet.relative.contains(PositionElement.X)) packet.x
                else packet.x - client.player.positioning.position.x
                val deltaY = if (packet.relative.contains(PositionElement.Y)) packet.y
                else packet.y - client.player.positioning.position.y
                val deltaZ = if (packet.relative.contains(PositionElement.Z)) packet.z
                else packet.z - client.player.positioning.position.z
                val deltaYaw = if (packet.relative.contains(PositionElement.YAW)) packet.yaw
                else packet.yaw - client.player.positioning.rotation.yaw
                val deltaPitch = if (packet.relative.contains(PositionElement.PITCH)) packet.pitch
                else packet.pitch - client.player.positioning.rotation.pitch
                client.player.positioning.position = client.player.positioning.position.addDelta(PositionDelta(deltaX, deltaY, deltaZ))
                client.player.positioning.rotation = client.player.positioning.rotation.addDelta(RotationDelta(deltaYaw, deltaPitch))
                client.client.session.send(ServerboundAcceptTeleportationPacket(packet.teleportId))
            }
            is ClientboundSetHealthPacket -> {
                // This was ServerPlayerHealthPacket
                client.player.health.health = packet.health
                client.player.health.saturation = packet.saturation
                client.player.health.food = packet.food
                if (client.player.health.health <= 0) client.respawn()
            }
            is ClientboundSetChunkCacheCenterPacket -> {
                client.player.positioning.chunk = ChunkPos(packet.chunkX, packet.chunkZ)
                client.world.pruneColumns(client.player.positioning.chunk, config.chunkUnloadDistance)
            }
            is ClientboundLevelChunkWithLightPacket -> {
                client.world.addColumn(packet.column, client.player.positioning.chunk, config.chunkUnloadDistance)
            }
            is ClientboundTeleportEntityPacket -> {
                if (packet.entityId == client.player.entityID) {
                    client.player.positioning.position = Position(packet.x, packet.y, packet.z)
                    client.player.positioning.rotation = Rotation(packet.yaw, packet.pitch)
                }
            }
            is ClientboundMoveEntityPosPacket -> {
                if (packet.entityId == client.player.entityID) {
                    client.player.positioning.position =
                        client.player.positioning.position.addDelta(
                            PositionDelta(
                                packet.moveX / (128 * 32),
                                packet.moveX / (128 * 32),
                                packet.moveX / (128 * 32)
                            )
                        )
                }
            }
            is ClientboundMoveEntityRotPacket -> {
                if (packet.entityId == client.player.entityID) {
                    client.player.positioning.rotation = Rotation(packet.yaw, packet.pitch)
                }
            }
            is ClientboundMoveEntityPosRotPacket -> {
                if (packet.entityId == client.player.entityID) {
                    client.player.positioning.position =
                        client.player.positioning.position.addDelta(
                            PositionDelta(
                                packet.moveX / (128 * 32),
                                packet.moveX / (128 * 32),
                                packet.moveX / (128 * 32)
                            )
                        )
                    client.player.positioning.rotation = Rotation(packet.yaw, packet.pitch)
                }
            }
            //TODO: Investigate ClientboundPlayerChatHeaderPacket
            is ClientboundPlayerChatPacket -> {
                val parsed = client.parser.parse(packet.messagePlain)
                val message = packet.messagePlain //TODO: Check if we need to do decorated or not D:
                //val message = client.parser.parse(MessageSerializer.toJsonString(packet.message))
                if (config.logChat) client.logChat(message, packet.chatType, packet.sender, parsed)
                client.onChat(message, packet.sender, parsed)
            }
            is ClientboundPlayerInfoPacket -> {
                for (entry in packet.entries) {
                    if (entry.profile.isComplete) client.playerListEntries[entry.profile.id] = entry.profile
                }
            }
            is ClientboundTabListPacket -> {
                client.playerListHeader = packet.header.toString()
                client.playerListFooter = packet.header.toString()
            }
        }
    }

    override fun disconnected(event: DisconnectedEvent?) {
        if (config.logConnection) client.connectionLog(event?.reason ?: "".let {
            client.parser.parse(it).toRawString()
        }, ConnectionLogType.DISCONNECTED)
        println("${client.protocol.profile}, ${client.protocol.profile.name}")
        client.joined = false
        client.player.positioning.stop = true
        client.onLeave(event ?: return)
    }
}