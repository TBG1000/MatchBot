package me.tbg.match.bot

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import tc.oc.pgm.api.match.event.MatchFinishEvent
import tc.oc.pgm.score.ScoreMatchModule
import tc.oc.pgm.teams.TeamMatchModule
import org.javacord.api.entity.message.embed.EmbedBuilder
import java.awt.Color
import java.time.Instant
import java.util.Comparator
import java.util.HashMap
import java.util.stream.Collectors
import java.util.Locale

class MatchFinishListener(private val bot: DiscordBot) : Listener {
    @EventHandler
    fun onMatchFinish(event: MatchFinishEvent) {
        val match = event.match
        val map = match.map
        val scoreModule = match.getModule(ScoreMatchModule::class.java)
        val teamModule = match.getModule(TeamMatchModule::class.java)
        val teams = match.competitors
        var winner = ""
        var winnerColor: Color? = null
        for (competitor in teams) {
            if (event.winners.contains(competitor)) {
                if (event.winners.size == 1) {
                    winner = competitor.nameLegacy
                    winnerColor = Color(competitor.fullColor.asRGB())
                } else {
                    winner = "Tie"
                    winnerColor = Color.RED
                }
            }
        }
        val matchInfo = EmbedBuilder()
            .setColor(winnerColor)
            .setTitle("Match #" + match.id + " has finished!")
            .setThumbnail(bot.getMapImageUrl(map))
            .setDescription(
                "Finished at <t:"
                        + Instant.now().epochSecond
                        + ":f> with **"
                        + match.players.size
                        + (if (match.players.size == 1) " player" else " players")
                        + "** online."
            )
            .addInlineField("Winner", winner.ifEmpty { "_No winner_" })
            .addInlineField("Time", bot.parseDuration(match.duration))
        if (scoreModule != null) {
            if (teamModule != null) {
                val teamScores: MutableMap<String, Int> = HashMap()
                for (team in teams) {
                    teamScores[team.nameLegacy] = scoreModule.getScore(team).toInt()
                }
                matchInfo.addInlineField(
                    "Scores",
                    teamScores.entries.stream()
                        .map { (key, value): Map.Entry<String, Int> -> "$key: $value points" }
                        .collect(Collectors.joining("\n")))
            } else {
                val playerScores: MutableMap<String, Int> = HashMap()
                for (player in match.competitors) {
                    playerScores[player.nameLegacy] = scoreModule.getScore(player).toInt()
                }
                matchInfo.addInlineField(
                    "Podium",
                    playerScores.entries.stream()
                        .sorted(java.util.Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(3)
                        .map { (key, value): Map.Entry<String, Int> -> "$key: $value points" }
                        .collect(Collectors.joining("\n")))
            }
        } else {
            matchInfo.addInlineField("\u200E", "\u200E")
        }
        matchInfo
            .addInlineField("Map", map.name)
            .addInlineField("Version", map.version.toString())
            .addInlineField("Gamemodes", bot.getMapGamemodes(match).uppercase(Locale.getDefault()))
            .addInlineField("Participants", match.participants.size.toString())
            .addInlineField("Observers", match.defaultParty.players.size.toString())
            .addInlineField("Staff", bot.getOnlineStaffCount(match).toString())
            .setFooter("Map tags: " + map.tags.toString())
        bot.sendMatchEmbed(matchInfo, match)
    }
}