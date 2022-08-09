package me.tbg.match.bot

import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.event.server.ServerBecomesAvailableEvent
import org.javacord.api.entity.message.embed.EmbedBuilder
import org.javacord.api.entity.activity.ActivityType
import org.javacord.api.entity.channel.ServerChannel
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.server.Server
import org.javacord.api.util.logging.ExceptionLogger
import tc.oc.pgm.api.PGM
import tc.oc.pgm.api.Permissions
import tc.oc.pgm.rotation.MapPoolManager
import tc.oc.pgm.rotation.pools.MapPool
import java.util.stream.Collectors
import tc.oc.pgm.api.map.Contributor
import tc.oc.pgm.api.map.Gamemode
import tc.oc.pgm.api.map.MapInfo
import tc.oc.pgm.api.match.Match
import tc.oc.pgm.api.player.MatchPlayer
import java.time.Duration
import java.util.logging.Logger

class DiscordBot(private val config: BotConfig, private val logger: Logger) {
    private var api: DiscordApi? = null
    private fun enable() {
        if (config.isEnabled) {
            logger.info("Enabling DiscordBot...")
            DiscordApiBuilder()
                .setToken(config.token)
                .setWaitForServersOnStartup(false)
                .setWaitForUsersOnStartup(false)
                .login()
                .thenAcceptAsync { api: DiscordApi ->
                    setAPI(api)
                    api.setMessageCacheSize(1, 60 * 60)
                    api.addServerBecomesAvailableListener { listener: ServerBecomesAvailableEvent ->
                        logger.info(
                            listener.server.name + " is now available"
                        )
                    }
                    logger.info("Discord Bot (MatchBot) is now active!")
                }
        }
    }

    private fun setAPI(api: DiscordApi) {
        this.api = api
    }

    private fun disable() {
        if (api != null) {
            api!!.disconnect()
        }
        api = null
    }

    fun sendMatchEmbed(embed: EmbedBuilder?, match: Match) {
        if (api != null) {
            api!!.updateActivity(ActivityType.PLAYING, match.map.name)
            api!!.getServerById(config.serverId)
                .flatMap { server: Server ->
                    server.getChannelById(
                        config.matchChannel
                    ).flatMap { obj: ServerChannel -> obj.asTextChannel() }
                }
                .ifPresent { text: TextChannel -> text.sendMessage(embed).exceptionally(ExceptionLogger.get()) }
        }
    }

    fun parseDuration(duration: Duration): String {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() - hours * 60
        val seconds = duration.seconds - hours * 60 * 60 - minutes * 60
        if (hours > 0) {
            return (hours
                .toString() + (if (hours == 1L) " hour " else " hours ")
                    + minutes
                    + (if (minutes == 1L) " minute " else " minutes ")
                    + seconds
                    + if (seconds == 1L) " second" else " seconds")
        } else if (minutes > 0) {
            return (minutes
                .toString() + (if (minutes == 1L) " minute " else " minutes ")
                    + seconds
                    + if (seconds == 1L) " second" else " seconds")
        } else if (seconds > 0) {
            return seconds.toString() + if (seconds == 1L) " second" else " seconds"
        }
        return "_Unavailable_"
    }

    fun getMapPools(match: Match): String {
        // Extracted from
        // https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/command/MapCommand.java
        // Line #253
        if (PGM.get().mapOrder is MapPoolManager) {
            val mapPools = (PGM.get().mapOrder as MapPoolManager)
                .mapPools.stream()
                .filter { pool: MapPool -> pool.maps.contains(match.map) }
                .map { obj: MapPool -> obj.name }
                .collect(Collectors.joining(", "))
            if (mapPools.isNotEmpty()) {
                return mapPools
            }
        }
        return "_No pools present_"
    }

    fun getMapAuthors(match: Match): String {
        return match.map.authors.stream()
            .map { obj: Contributor -> obj.nameLegacy }
            .collect(Collectors.joining(", "))
    }

    fun getMapGamemodes(match: Match): String {
        return match.map.gamemodes.stream()
            .map { obj: Gamemode -> obj.id }
            .collect(Collectors.joining(", "))
    }

    fun getMapImageUrl(map: MapInfo): String {
        val repo = config.mapImagesURL
        val mapName = map.name.replace(":", "").replace(" ", "%20")
        val png = "/map.png"
        return repo + mapName + png
    }

    fun getOnlineStaffCount(match: Match): Long {
        // Adapted from
        // https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/command/MapCommand.java
        // Line 88
        return match.players.stream()
            .filter { player: MatchPlayer -> player.bukkit.hasPermission(Permissions.STAFF) && !player.isVanished }
            .count()
    }

    fun reload() {
        if (api != null && !config.isEnabled) {
            disable()
        } else if (api == null && config.isEnabled) {
            enable()
        }
    }

    init {
        reload()
    }
}