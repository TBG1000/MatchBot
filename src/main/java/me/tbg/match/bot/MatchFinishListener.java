package me.tbg.match.bot;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.javacord.api.entity.message.embed.EmbedBuilder;
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
    Collection<Competitor> teams = match.getCompetitors();

    String winner = "";
    Color winnerColor = null;
    for (Competitor competitor : teams) {
      if (event.getWinners().contains(competitor)) {
        if (event.getWinners().size() == 1) {
          winner = competitor.getNameLegacy();
          winnerColor = new Color(competitor.getFullColor().asRGB());
        } else {
          winner = "Tie";
          winnerColor = Color.RED;
        }
      }
    }
    EmbedBuilder matchInfo =
        new EmbedBuilder()
            .setColor(winnerColor)
            .setTitle("Match #" + match.getId() + " has finished!")
            .setThumbnail(bot.getMapImageUrl(map))
            .setDescription(
                "Finished at <t:"
                    + Instant.now().getEpochSecond()
                    + ":f> with **"
                    + match.getPlayers().size()
                    + (match.getPlayers().size() == 1 ? " player" : " players")
                    + "** online.")
            .addInlineField("Winner", winner.isEmpty() ? "_No winner_" : winner)
            .addInlineField("Time", bot.parseDuration(match.getDuration()));
    if (scoreModule != null) {
      if (teamModule != null) {
        Map<String, Integer> teamScores = new HashMap<>();
        for (Competitor team : teams) {
          teamScores.put(team.getNameLegacy(), (int) scoreModule.getScore(team));
        }
        matchInfo.addInlineField(
            "Scores",
            teamScores.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n")));
      } else {
        Map<String, Integer> playerScores = new HashMap<>();
        for (Competitor player : match.getCompetitors()) {
          playerScores.put(player.getNameLegacy(), (int) scoreModule.getScore(player));
        }
        matchInfo.addInlineField(
            "Podium",
            playerScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(e -> e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n")));
      }
    } else {
      matchInfo.addInlineField("\u200E", "\u200E");
    }
    matchInfo
        .addInlineField("Map", map.getName())
        .addInlineField("Version", map.getVersion().toString())
        .addInlineField("Gamemodes", bot.getMapGamemodes(match).toUpperCase())
        .addInlineField("Participants", String.valueOf(match.getParticipants().size()))
        .addInlineField("Observers", String.valueOf(match.getDefaultParty().getPlayers().size()))
        .addInlineField("Staff", String.valueOf(bot.getOnlineStaffCount(match)))
        .setFooter("Map tags: " + map.getTags().toString());
    bot.sendMatchEmbed(matchInfo, match);
  }
}
