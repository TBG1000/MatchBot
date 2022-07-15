package me.tbg.match.bot;

import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.MessageBuilder;
import org.javacord.api.entity.message.MessageFlag;
import org.javacord.api.entity.message.component.ActionRow;
import org.javacord.api.entity.message.component.Button;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.stats.PlayerStats;
import tc.oc.pgm.stats.StatsMatchModule;
import tc.oc.pgm.stats.TeamStats;
import tc.oc.pgm.teams.TeamMatchModule;

// import tc.oc.pgm.api.player.MatchPlayer;

public class DiscordBot {

  private DiscordApi api;
  private BotConfig config;
  private Logger logger;

  public DiscordBot(BotConfig config, Logger logger) {
    this.config = config;
    this.logger = logger;
    reload();
  }

  public BotConfig getConfig() {
    return config;
  }

  public void enable() {
    if (config.isEnabled()) {
      logger.info("Enabling DiscordBot...");
      new DiscordApiBuilder()
          .setToken(config.getToken())
          .setWaitForServersOnStartup(false)
          .setWaitForUsersOnStartup(false)
          .login()
          .thenAcceptAsync(
              api -> {
                setAPI(api);
                api.setMessageCacheSize(1, 60 * 60);
                api.addServerBecomesAvailableListener(
                    listener -> logger.info(listener.getServer().getName() + " is now available"));
                logger.info("Discord Bot (MatchBot) is now active!");
              });
    }
  }

  private void setAPI(DiscordApi api) {
    this.api = api;
  }

  public void disable() {
    if (this.api != null) {
      this.api.disconnect();
    }
    this.api = null;
  }

  private void sendEmbed(EmbedBuilder embed) {
    if (api != null) {
      api.getServerById(config.getServerId())
          .flatMap(
              server ->
                  server.getChannelById(config.getMatchChannel()).flatMap(Channel::asTextChannel))
          .ifPresent(text -> text.sendMessage(embed).exceptionally(ExceptionLogger.get()));
    }
  }

  public void getMatchFinishEmbeds(MatchFinishEvent event) {
    EmbedBuilder matchInfo = matchFinishEmbed(event);
    EmbedBuilder teamStats = teamStatsEmbed(event);
    EmbedBuilder matchStats = matchStatsEmbed(event);
    List<EmbedBuilder> matchEmbeds = new ArrayList<>();
    matchEmbeds.add(matchInfo);
    matchEmbeds.add(teamStats);
    matchEmbeds.add(matchStats);
    TextChannel textChannel =
        api.getServerById(config.getServerId())
            .flatMap(serverById -> serverById.getChannelById(config.getMatchChannel()))
            .flatMap(Channel::asTextChannel)
            .orElse(null);
    MessageBuilder embeds = new MessageBuilder();
    embeds.setEmbed(matchEmbeds.get(0));
    embeds.addComponents(
        ActionRow.of(Button.primary("team", "Team Stats"), Button.primary("match", "Match Stats")));
    if (textChannel != null) {
      embeds
          .send(textChannel)
          .thenAccept(
              message ->
                  message.addButtonClickListener(
                      buttonClickEvent -> {
                        if (buttonClickEvent.getButtonInteractionWithCustomId("team").isPresent()) {
                          buttonClickEvent
                              .getInteraction()
                              .createImmediateResponder()
                              .setFlags(MessageFlag.EPHEMERAL)
                              .addEmbed(matchEmbeds.get(1))
                              .respond();
                        } else if (buttonClickEvent
                            .getButtonInteractionWithCustomId("match")
                            .isPresent()) {
                          buttonClickEvent
                              .getInteraction()
                              .createImmediateResponder()
                              .setFlags(MessageFlag.EPHEMERAL)
                              .addEmbed(matchEmbeds.get(2))
                              .respond();
                        }
                      }));
    }
  }

  private String parseDuration(Duration duration) {
    long hours = duration.toHours();
    long minutes = duration.toMinutes() - (hours * 60);
    long seconds = duration.getSeconds() - (hours * 60 * 60) - (minutes * 60);
    if (hours > 0) {
      return hours
          + (hours == 1 ? " hour " : " hours ")
          + minutes
          + (minutes == 1 ? " minute " : " minutes ")
          + seconds
          + (seconds == 1 ? " second" : " seconds");
    } else if (minutes > 0) {
      return minutes
          + (minutes == 1 ? " minute " : " minutes ")
          + seconds
          + (seconds == 1 ? " second" : " seconds");
    } else if (seconds > 0) {
      return seconds + (seconds == 1 ? " second" : " seconds");
    }
    return "_Unavailable_";
  }

  public String getMapPools(Match match) {
    // Extracted from
    // https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/command/MapCommand.java
    // Line #253
    if (PGM.get().getMapOrder() instanceof MapPoolManager) {
      String mapPools =
          ((MapPoolManager) PGM.get().getMapOrder())
              .getMapPools().stream()
                  .filter(pool -> pool.getMaps().contains(match.getMap()))
                  .map(MapPool::getName)
                  .collect(Collectors.joining(", "));
      if (!mapPools.isEmpty()) {
        return mapPools;
      }
    }
    return "_No pools present_";
  }

  public String getMapAuthors(Match match) {
    return match.getMap().getAuthors().stream()
        .map(Contributor::getNameLegacy)
        .collect(Collectors.joining(", "));
  }

  public String getMapGamemodes(Match match) {
    return match.getMap().getGamemodes().stream()
        .map(Gamemode::getId)
        .collect(Collectors.joining(", "));
  }

  public String getMapImageUrl(MapInfo map) {
    String repo = config.getMapImagesURL();
    String mapName = map.getName().replace(":", "").replace(" ", "%20");
    String png = "/map.png";
    return repo + mapName + png;
  }

  public long getOnlineStaffCount(Match match) {
    // Adapted from
    // https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/command/MapCommand.java
    // Line 88
    return match.getPlayers().stream()
        .filter(
            player -> (player.getBukkit().hasPermission(Permissions.STAFF) && !player.isVanished()))
        .count();
  }

  public void matchStartEmbed(MatchStartEvent event) {
    Match match = event.getMatch();
    MapInfo map = match.getMap();
    api.updateActivity(ActivityType.PLAYING, map.getName());
    EmbedBuilder matchStartEmbed =
        new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("Match #" + match.getId() + " has started!")
            .setThumbnail(getMapImageUrl(map))
            .setDescription(
                "Started at <t:"
                    + Instant.now().getEpochSecond()
                    + ":f> with "
                    + match.getPlayers().size()
                    + (match.getPlayers().size() == 1 ? " player" : " players")
                    + " online.")
            .addInlineField("Map", map.getName())
            .addInlineField("Version", map.getVersion().toString())
            .addInlineField("Gamemodes", getMapGamemodes(match).toUpperCase())
            .addInlineField("Created by", getMapAuthors(match))
            .addInlineField("Pools", getMapPools(match))
            .addField("Objective", map.getDescription())
            .addInlineField("Participants", String.valueOf(match.getPlayers().size()))
            .addInlineField(
                "Observers", String.valueOf(match.getDefaultParty().getPlayers().size()))
            .addInlineField("Staff", String.valueOf(getOnlineStaffCount(match)))
            .setFooter("Map tags: " + map.getTags().toString());
    sendEmbed(matchStartEmbed);
  }

  public EmbedBuilder matchFinishEmbed(MatchFinishEvent event) {
    Match match = event.getMatch();
    MapInfo map = match.getMap();
    String winners = "";
    for (Competitor competitor : match.getCompetitors()) {
      if (event.getWinners().contains(competitor)) {
        winners = competitor.getNameLegacy();
      }
    }
    return new EmbedBuilder()
        .setColor(Color.RED)
        .setTitle("Match #" + match.getId() + " has finished!")
        .setThumbnail(getMapImageUrl(map))
        .setDescription(
            "Finished at <t:"
                + Instant.now().getEpochSecond()
                + ":f> with **"
                + match.getPlayers().size()
                + (match.getPlayers().size() == 1 ? " player" : " players")
                + "** online.")
        .addInlineField("Winner", winners.isEmpty() ? "_No winner_" : winners)
        .addInlineField("Time", parseDuration(match.getDuration()))
        .addInlineField("\u200E", "\u200E")
        .addInlineField("Map", map.getName())
        .addInlineField("Version", map.getVersion().toString())
        .addInlineField("Gamemodes", getMapGamemodes(match).toUpperCase())
        .addInlineField("Created by", getMapAuthors(match))
        .addInlineField("Pools", getMapPools(match))
        .addField("Objective", map.getDescription())
        .addInlineField("Participants", String.valueOf(match.getParticipants().size()))
        .addInlineField(
            "Observers", String.valueOf(match.getDefaultParty().getPlayers().size()))
        .addInlineField("Staff", String.valueOf(getOnlineStaffCount(match)))
        .setFooter("Map tags: " + map.getTags().toString());
  }

  public EmbedBuilder teamStatsEmbed(MatchFinishEvent event) {
    TeamMatchModule tmm = event.getMatch().getModule(TeamMatchModule.class);
    Collection<Competitor> teams = event.getMatch().getCompetitors();
    StatsMatchModule smm = event.getMatch().getModule(StatsMatchModule.class);

    EmbedBuilder teamStatsEmbed =
        new EmbedBuilder()
            .setColor(Color.CYAN)
            .setTitle("Match #" + event.getMatch().getId() + " Team Stats")
            .setDescription(
                "Finished at <t:"
                    + Instant.now().getEpochSecond()
                    + ":f> with **"
                    + event.getMatch().getPlayers().size()
                    + (event.getMatch().getPlayers().size() == 1 ? " player" : " players")
                    + "** online on **"
                    + event.getMatch().getMap().getName()
                    + " (v"
                    + event.getMatch().getMap().getVersion()
                    + ")**");

    if (tmm != null) {
      for (Competitor team : teams) {
        teamStatsEmbed.addField(
            team.getNameLegacy() + ": " + team.getPlayers().size(),
            team.getPlayers().isEmpty()
                ? "_No players_"
                : team.getPlayers().stream()
                    .map(MatchPlayer::getNameLegacy)
                    .collect(Collectors.joining(", ")));
        TeamStats teamStats = new TeamStats(team, smm);
        teamStatsEmbed.addInlineField("Kills", ":dagger: " + teamStats.getTeamKills());
        teamStatsEmbed.addInlineField("Deaths", ":coffin: " + teamStats.getTeamDeaths());
        teamStatsEmbed.addInlineField("K/D", ":bar_chart: " + teamStats.getTeamKD());
        teamStatsEmbed.addInlineField(
            "DMG Dealt",
            ":crossed_swords: "
                + teamStats.getDamageDone()
                + "\n:bow_and_arrow: "
                + teamStats.getBowDamage());
        teamStatsEmbed.addInlineField(
            "DMG Received",
            ":crossed_swords: "
                + teamStats.getDamageTaken()
                + "\n:bow_and_arrow: "
                + teamStats.getBowDamageTaken());
        teamStatsEmbed.addInlineField(
            "Bow hits",
            ":dart: "
                + teamStats.getShotsHit()
                + "/"
                + teamStats.getShotsTaken()
                + "\n:bar_chart: "
                + teamStats.getTeamBowAcc()
                + "%");
      }
    } else {
      teamStatsEmbed.addField(
          "Players: "
              + event.getMatch().getParticipants().size()
              + "/"
              + event.getMatch().getMaxPlayers(),
          event.getMatch().getParticipants().stream()
              .map(MatchPlayer::getNameLegacy)
              .collect(Collectors.joining(", ")));
    }
    return teamStatsEmbed;
  }

  public Map.Entry<Map<UUID, String>, Integer> sortStats(Map<Map<UUID, String>, Integer> map) {
    return map.entrySet().stream().max(Comparator.comparingInt(Map.Entry::getValue)).orElse(null);
  }

  public String getPlayerName(Map.Entry<Map<UUID, String>, ?> map) {
    return map.getKey().entrySet().stream().map(Map.Entry::getValue).collect(Collectors.joining());
  }

  public Map.Entry<Map<UUID, String>, Double> sortStatsDouble(Map<Map<UUID, String>, Double> map) {
    return map.entrySet().stream()
        .max(Comparator.comparingDouble(Map.Entry::getValue))
        .orElse(null);
  }

  public EmbedBuilder matchStatsEmbed(MatchFinishEvent event) {
    StatsMatchModule smm = event.getMatch().getModule(StatsMatchModule.class);
    Map<UUID, String> matchPlayer = new HashMap<>();
    Map<Map<UUID, String>, Integer> allKills = new HashMap<>();
    Map<Map<UUID, String>, Integer> allStreaks = new HashMap<>();
    Map<Map<UUID, String>, Integer> allDeaths = new HashMap<>();
    Map<Map<UUID, String>, Integer> allShots = new HashMap<>();
    Map<Map<UUID, String>, Double> allDamage = new HashMap<>();

    for (MatchPlayer player : event.getMatch().getPlayers()) {
      if (smm != null) {
        UUID playerUUID = player.getId();
        String playerName = player.getNameLegacy();
        PlayerStats playerStats = smm.getPlayerStat(player.getId());
        matchPlayer.put(playerUUID, playerName);
        allKills.put(matchPlayer, playerStats.getKills());
        allStreaks.put(matchPlayer, playerStats.getMaxKillstreak());
        allDeaths.put(matchPlayer, playerStats.getDeaths());
        allShots.put(matchPlayer, playerStats.getLongestBowKill());
        allDamage.put(matchPlayer, playerStats.getDamageDone());
      }
    }

    Map.Entry<Map<UUID, String>, Integer> highestKills = sortStats(allKills);
    Map.Entry<Map<UUID, String>, Integer> highestStreak = sortStats(allStreaks);
    Map.Entry<Map<UUID, String>, Integer> mostDeaths = sortStats(allDeaths);
    Map.Entry<Map<UUID, String>, Integer> longestShot = sortStats(allShots);
    Map.Entry<Map<UUID, String>, Double> highestDamage = sortStatsDouble(allDamage);

    return new EmbedBuilder()
        .setTitle("Match #" + event.getMatch().getId() + " Stats")
        .setColor(Color.BLACK)
        .setDescription(
            "Match ended at <t:"
                + Instant.now().getEpochSecond()
                + ":f> with "
                + event.getMatch().getPlayers().size()
                + (event.getMatch().getPlayers().size() == 1 ? " player" : " players")
                + " online.\nPlayed on "
                + event.getMatch().getMap().getName()
                + " (v"
                + event.getMatch().getMap().getVersion()
                + ")")
        .addField("Kills", highestKills.getValue() + " by " + getPlayerName(highestKills))
        .addField("Deaths", mostDeaths.getValue() + " by " + getPlayerName(mostDeaths))
        .addField(
            "Killstreak", highestStreak.getValue() + " by " + getPlayerName(highestStreak))
        .addField("Longest shot", longestShot.getValue() + " blocks by " + getPlayerName(longestShot))
        .addField("Damage", highestDamage.getValue() + " by " + getPlayerName(highestDamage));
  }

  public void reload() {
    if (this.api != null && !config.isEnabled()) {
      disable();
    } else if (this.api == null && config.isEnabled()) {
      enable();
    }
  }
}
