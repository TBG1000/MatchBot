package me.tbg.match.bot

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.javacord.api.entity.message.embed.EmbedBuilder
import tc.oc.pgm.api.match.event.MatchStartEvent
import java.awt.Color
import java.time.Instant
import java.util.Locale

class MatchStartListener(private val bot: DiscordBot?) : Listener {
    @EventHandler
    fun onMatchStart(event: MatchStartEvent) {
        val match = event.match
        val map = match.map
        val matchStartEmbed = EmbedBuilder()
            .setColor(Color.WHITE)
            .setTitle("Match #" + match.id + " has started!")
            .setThumbnail(bot!!.getMapImageUrl(map))
            .setDescription(
                "Started at <t:"
                        + Instant.now().epochSecond
                        + ":f> with **"
                        + match.players.size
                        + (if (match.players.size == 1) " player" else " players")
                        + "** online."
            )
            .addInlineField("Map", map.name)
            .addInlineField("Version", map.version.toString())
            .addInlineField("Gamemodes", bot.getMapGamemodes(match).uppercase(Locale.getDefault()))
            .addInlineField("Created by", bot.getMapAuthors(match))
            .addInlineField("Pools", bot.getMapPools(match))
            .addField("Objective", map.description)
            .addInlineField("Participants", match.participants.size.toString())
            .addInlineField(
                "Observers", match.defaultParty.players.size.toString()
            )
            .addInlineField("Staff", bot.getOnlineStaffCount(match).toString())
            .setFooter("Map tags: " + map.tags.toString())
        bot.sendMatchEmbed(matchStartEmbed, event.match)
    }
}