package me.tbg.match.bot;

import java.awt.Color;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.score.ScoreMatchModule;
import tc.oc.pgm.teams.TeamMatchModule;

public class MatchFinishListener implements Listener {

    private final DiscordBot bot;

    public MatchFinishListener(DiscordBot bot) {
        this.bot = bot;
    }

    @EventHandler
    public void onMatchFinish(MatchFinishEvent event) {
        Match match = event.getMatch();
        MapInfo map = match.getMap();
        ScoreMatchModule scoreModule = match.getModule(ScoreMatchModule.class);
        TeamMatchModule teamModule = match.getModule(TeamMatchModule.class);

        String winner = getWinner(event);
        Color winnerColor = getWinnerColor(event);

        EmbedBuilder matchFinishEmbed =
                createMatchFinishEmbed(match, map, winner, winnerColor, scoreModule, teamModule);

        bot.editMatchEmbed(Long.parseLong(match.getId()), matchFinishEmbed);
    }

    private String getWinner(MatchFinishEvent event) {
        if (event.getWinners().size() == 1) {
            return event.getWinners().iterator().next().getNameLegacy();
        } else if (event.getWinners().isEmpty()) {
            return "_No winner_";
        } else {
            return "Tie";
        }
    }

    private Color getWinnerColor(MatchFinishEvent event) {
        if (event.getWinners().size() == 1) {
            return new Color(event.getWinners().iterator().next().getFullColor().asRGB());
        } else {
            return Color.RED;
        }
    }

    private EmbedBuilder createMatchFinishEmbed(
            Match match,
            MapInfo map,
            String winner,
            Color winnerColor,
            ScoreMatchModule scoreModule,
            TeamMatchModule teamModule) {
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(winnerColor.getRGB())
                .setTitle("Match #" + match.getId() + " has finished!")
                .setDescription("Started at <t:" + bot.getMatchStartTimestamp(Long.parseLong(match.getId()))
                        + ":f> with **"
                        + bot.getMatchStartPlayers(Long.parseLong(match.getId()))
                        + (bot.getMatchStartPlayers(Long.parseLong(match.getId())) == 1 ? " player" : " players")
                        + "**  and finished at <t:" + Instant.now().getEpochSecond() + ":f> with **"
                        + match.getPlayers().size() + (match.getPlayers().size() == 1 ? " player" : " players")
                        + "** online.")
                .addField("Winner", winner, true)
                .addField("Time", bot.parseDuration(match.getDuration()), true);

        if (scoreModule != null) {
            if (teamModule != null) {
                Map<String, Integer> teamScores = match.getCompetitors().stream()
                        .collect(Collectors.toMap(Competitor::getNameLegacy, team -> (int) scoreModule.getScore(team)));
                embed.addField("Scores", formatScores(teamScores), true);
            } else {
                Map<String, Integer> playerScores = match.getCompetitors().stream()
                        .collect(Collectors.toMap(
                                Competitor::getNameLegacy, player -> (int) scoreModule.getScore(player)));
                embed.addField("Podium", formatPodium(playerScores), true);
            }
        } else {
            embed.addField("\u200E", "\u200E", false);
        }

        embed.addField("Map", map.getName(), true)
                .addField("Version", map.getVersion().toString(), true)
                .addField("Gamemodes", bot.getMapGamemodes(match).toUpperCase(), true)
                .addField("Participants", String.valueOf(match.getParticipants().size()), true)
                .addField("Observers", String.valueOf(match.getDefaultParty().getPlayers().size()), true)
                .addField("Staff", String.valueOf(bot.getOnlineStaffCount(match)), true)
                .setFooter("Map tags: " + map.getTags().toString());

        bot.setEmbedThumbnail(map, embed, bot);

        return embed;
    }

    private String formatScores(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n"));
    }

    private String formatPodium(Map<String, Integer> scores) {
        return scores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(e -> e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n"));
    }
}