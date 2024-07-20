package me.tbg.match.bot;

import java.awt.*;
import java.io.IOException;
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

    String winner = getWinner(event, match.getCompetitors());
    Color winnerColor = getWinnerColor(event, match.getCompetitors());

    EmbedBuilder matchInfo = createMatchInfoEmbed(match, map, winner, winnerColor);

    addScoresToEmbed(matchInfo, match, scoreModule, teamModule);

    addAdditionalInfoToEmbed(matchInfo, match, map);

    try {
      matchInfo.setThumbnail(bot.getMapImage(map));
    } catch (IOException e) {
      System.out.println("Unable to get map image for " + map.getName());
    }

    bot.sendMatchEmbed(matchInfo, match);
  }

  private String getWinner(MatchFinishEvent event, Collection<Competitor> teams) {
    if (event.getWinners().size() == 1) {
      return event.getWinners().iterator().next().getNameLegacy();
    } else if (event.getWinners().isEmpty()) {
      return "_No winner_";
    } else {
      return "Tie";
    }
  }

  private Color getWinnerColor(MatchFinishEvent event, Collection<Competitor> teams) {
    if (event.getWinners().size() == 1) {
      return new Color(event.getWinners().iterator().next().getFullColor().asRGB());
    } else {
      return Color.RED;
    }
  }

  private EmbedBuilder createMatchInfoEmbed(Match match, MapInfo map, String winner, Color winnerColor) {
    return new EmbedBuilder()
            .setColor(winnerColor)
            .setTitle("Match #" + match.getId() + " has finished!")
            .setDescription(
                    "Finished at <t:" + Instant.now().getEpochSecond() + ":f> with **"
                            + match.getPlayers().size() + (match.getPlayers().size() == 1 ? " player" : " players") + "** online.")
            .addInlineField("Winner", winner)
            .addInlineField("Time", bot.parseDuration(match.getDuration()));
  }

  private void addScoresToEmbed(EmbedBuilder embed, Match match, ScoreMatchModule scoreModule, TeamMatchModule teamModule) {
    if (scoreModule != null) {
      if (teamModule != null) {
        Map<String, Integer> teamScores = match.getCompetitors().stream()
                .collect(Collectors.toMap(Competitor::getNameLegacy, team -> (int) scoreModule.getScore(team)));
        embed.addInlineField("Scores", formatScores(teamScores));
      } else {
        Map<String, Integer> playerScores = match.getCompetitors().stream()
                .collect(Collectors.toMap(Competitor::getNameLegacy, player -> (int) scoreModule.getScore(player)));
        embed.addInlineField("Podium", formatPodium(playerScores));
      }
    } else {
      embed.addInlineField("\u200E", "\u200E");
    }
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

  private void addAdditionalInfoToEmbed(EmbedBuilder embed, Match match, MapInfo map) {
    embed.addInlineField("Map", map.getName())
            .addInlineField("Version", map.getVersion().toString())
            .addInlineField("Gamemodes", bot.getMapGamemodes(match).toUpperCase())
            .addInlineField("Participants", String.valueOf(match.getParticipants().size()))
            .addInlineField("Observers", String.valueOf(match.getDefaultParty().getPlayers().size()))
            .addInlineField("Staff", String.valueOf(bot.getOnlineStaffCount(match)))
            .setFooter("Map tags: " + map.getTags().toString());
  }
}
