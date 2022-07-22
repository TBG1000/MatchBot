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

// import tc.oc.pgm.stats.PlayerStats;
// import tc.oc.pgm.stats.StatsMatchModule;
// import tc.oc.pgm.stats.TeamStats;
// import tc.oc.pgm.api.player.MatchPlayer;
// import tc.oc.pgm.api.Datastore;
// import tc.oc.pgm.api.PGM;

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
    /*
    StatsMatchModule statsModule = match.getModule(StatsMatchModule.class);
    Map<UUID, String> matchPlayer = new HashMap<>();

    Map<Map<UUID, String>, Integer> allKills = new HashMap<>();
    Map<Map<UUID, String>, Integer> allStreaks = new HashMap<>();
    Map<Map<UUID, String>, Integer> allDeaths = new HashMap<>();
    Map<Map<UUID, String>, Integer> allShots = new HashMap<>();
    Map<Map<UUID, String>, Double> allDamage = new HashMap<>();

    for (MatchPlayer player : match.getPlayers()) {
      if (statsModule != null) {
        UUID playerUUID = player.getId();
        String playerName = player.getNameLegacy();
        PlayerStats playerStats = statsModule.getPlayerStat(player.getId());
        matchPlayer.put(playerUUID, playerName);
        allKills.put(matchPlayer, playerStats.getKills());
        allStreaks.put(matchPlayer, playerStats.getMaxKillstreak());
        allDeaths.put(matchPlayer, playerStats.getDeaths());
        allShots.put(matchPlayer, playerStats.getLongestBowKill());
        allDamage.put(matchPlayer, playerStats.getDamageDone());
      }
    }

    Map.Entry<Map<UUID, String>, Integer> highestKills = bot.sortStats(allKills);
    Map.Entry<Map<UUID, String>, Integer> highestStreak = bot.sortStats(allStreaks);
    Map.Entry<Map<UUID, String>, Integer> mostDeaths = bot.sortStats(allDeaths);
    Map.Entry<Map<UUID, String>, Integer> longestShot = bot.sortStats(allShots);
    Map.Entry<Map<UUID, String>, Double> highestDamage = bot.sortStatsDouble(allDamage);

    EmbedBuilder matchStats =
        new EmbedBuilder()
            .setTitle("Match #" + match.getId() + " Stats")
            .setColor(Color.BLACK)
            .setDescription(
                "Match ended at <t:"
                    + Instant.now().getEpochSecond()
                    + ":f> with **"
                    + match.getPlayers().size()
                    + (match.getPlayers().size() == 1 ? " player" : " players")
                    + "** online.\nPlayed on **"
                    + map.getName()
                    + " (v"
                    + map.getVersion()
                    + ")**.");
    if (scoreModule != null) {
      if (teamModule != null) {
        Map<String, Integer> teamScores = new HashMap<>();
        for (Competitor team : teams) {
          teamScores.put(team.getNameLegacy(), (int) scoreModule.getScore(team));
        }
        matchStats.addField(
            "Scores :trophy:",
            teamScores.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n")));
      } else {
        Map<String, Integer> playerScores = new HashMap<>();
        for (Competitor player : match.getCompetitors()) {
          playerScores.put(player.getNameLegacy(), (int) scoreModule.getScore(player));
        }
        matchStats.addField(
            "Podium :trophy:",
            playerScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(e -> ":medal: " + e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n")));
      }
    }

    matchStats.addField(
        "Kills :dagger:", highestKills.getValue() + " by " + bot.getPlayerName(highestKills));
    matchStats.addField(
        "Deaths :coffin:", mostDeaths.getValue() + " by " + bot.getPlayerName(mostDeaths));
    matchStats.addField(
        "Killstreak :bar_chart:",
        highestStreak.getValue() + " by " + bot.getPlayerName(highestStreak));
    matchStats.addField(
        "Longest shot :bow_and_arrow:",
        longestShot.getValue() + " blocks by " + bot.getPlayerName(longestShot));
    matchStats.addField(
        "Damage :heart:", highestDamage.getValue() + " by " + bot.getPlayerName(highestDamage));

    EmbedBuilder teamStats =
        new EmbedBuilder()
            .setColor(Color.CYAN)
            .setTitle("Match #" + match.getId() + " Team Stats")
            .setDescription(
                "Finished at <t:"
                    + Instant.now().getEpochSecond()
                    + ":f> with **"
                    + match.getPlayers().size()
                    + (match.getPlayers().size() == 1 ? " player" : " players")
                    + "** online on **"
                    + map.getName()
                    + " (v"
                    + map.getVersion()
                    + ")**");

    if (teamModule != null) {
      for (Competitor team : teams) {
        teamStats.addField(
            team.getNameLegacy() + ": " + team.getPlayers().size(),
            team.getPlayers().isEmpty()
                ? "_No players_"
                : team.getPlayers().stream()
                    .map(MatchPlayer::getNameLegacy)
                    .collect(Collectors.joining(", ")));
        TeamStats teamStatsTracker = new TeamStats(team, statsModule);
        teamStats.addInlineField("Kills", ":dagger: " + teamStatsTracker.getTeamKills());
        teamStats.addInlineField("Deaths", ":coffin: " + teamStatsTracker.getTeamDeaths());
        teamStats.addInlineField("K/D", ":bar_chart: " + teamStatsTracker.getTeamKD());
        teamStats.addInlineField(
            "DMG Dealt",
            ":crossed_swords: "
                + teamStatsTracker.getDamageDone()
                + "\n:bow_and_arrow: "
                + teamStatsTracker.getBowDamage());
        teamStats.addInlineField(
            "DMG Received",
            ":crossed_swords: "
                + teamStatsTracker.getDamageTaken()
                + "\n:bow_and_arrow: "
                + teamStatsTracker.getBowDamageTaken());
        teamStats.addInlineField(
            "Bow hits",
            ":dart: "
                + teamStatsTracker.getShotsHit()
                + "/"
                + teamStatsTracker.getShotsTaken()
                + "\n:bar_chart: "
                + teamStatsTracker.getTeamBowAcc()
                + "%");
      }
    } else {
      teamStats = null;
    }
    */

    String winner = "";
    for (Competitor competitor : match.getCompetitors()) {
      if (event.getWinners().contains(competitor)) {
        if (event.getWinners().size() == 1) {
          winner = competitor.getNameLegacy();
        } else {
          winner = "Tie";
        }
      }
    }
    EmbedBuilder matchInfo =
        new EmbedBuilder()
            .setColor(Color.RED)
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
            "Scores :trophy:",
            teamScores.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n")));
      } else {
        Map<String, Integer> playerScores = new HashMap<>();
        for (Competitor player : match.getCompetitors()) {
          playerScores.put(player.getNameLegacy(), (int) scoreModule.getScore(player));
        }
        matchInfo.addInlineField(
            "Podium :trophy:",
            playerScores.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(3)
                .map(e -> ":medal: " + e.getKey() + ": " + e.getValue() + " points")
                .collect(Collectors.joining("\n")));
      }
    } else {
      matchInfo.addInlineField("\u200E", "\u200E");
    }
    matchInfo
        .addInlineField("Map", map.getName())
        .addInlineField("Version", map.getVersion().toString())
        .addInlineField("Gamemodes", bot.getMapGamemodes(match).toUpperCase())
        .addInlineField("Created by", bot.getMapAuthors(match))
        .addInlineField("Pools", bot.getMapPools(match))
        .addField("Objective", map.getDescription())
        .addInlineField("Participants", String.valueOf(match.getParticipants().size()))
        .addInlineField("Observers", String.valueOf(match.getDefaultParty().getPlayers().size()))
        .addInlineField("Staff", String.valueOf(bot.getOnlineStaffCount(match)))
        .setFooter("Map tags: " + map.getTags().toString());
    bot.sendMatchEmbed(matchInfo, match);
    // bot.sendMatchFinishEmbeds(matchInfo, teamStats, matchStats);
  }
}
