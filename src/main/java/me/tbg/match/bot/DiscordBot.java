package me.tbg.match.bot;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;
import tc.oc.pgm.api.party.Competitor;
import tc.oc.pgm.api.player.MatchPlayer;
import tc.oc.pgm.rotation.MapPool;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.teams.TeamMatchModule;

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
          .ifPresent(
              server ->
                  server
                      .getChannelById(config.getMatchChannel())
                      .ifPresent(
                          channel ->
                              channel.asTextChannel().ifPresent(text -> text.sendMessage(embed))));
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

  public long getOnlineStaffCount(Match match) {
    return match.getPlayers().stream()
        .filter(
            player -> (player.getBukkit().hasPermission(Permissions.STAFF) && !player.isVanished()))
        .count();
  }

  public void matchStartEmbed(MatchStartEvent event) {
    api.updateActivity(ActivityType.PLAYING, event.getMatch().getMap().getName());
    EmbedBuilder matchStartEmbed =
        new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("Match #" + event.getMatch().getId() + " has started!")
            .setDescription("Started at <t:" + Instant.now().getEpochSecond() + ":f>")
            .addInlineField("Map", event.getMatch().getMap().getName())
            .addInlineField("Version", event.getMatch().getMap().getVersion().toString())
            .addInlineField("Gamemodes", getMapGamemodes(event.getMatch()).toUpperCase())
            .addInlineField("Created by", getMapAuthors(event.getMatch()))
            .addInlineField("Pools", getMapPools(event.getMatch()))
            .addField("Objective", event.getMatch().getMap().getDescription())
            .addInlineField("Players online", String.valueOf(event.getMatch().getPlayers().size()))
            .addInlineField("Staff online", String.valueOf(getOnlineStaffCount(event.getMatch())))
            .setFooter("Map tags: " + event.getMatch().getMap().getTags().toString());
    sendEmbed(matchStartEmbed);
  }

  public void matchFinishEmbed(MatchFinishEvent event) {
    String winners = "";
    for (Competitor competitor : event.getMatch().getCompetitors()) {
      if (event.getWinners().contains(competitor)) {
        winners = competitor.getNameLegacy();
      }
    }
    EmbedBuilder matchFinishEmbed =
        new EmbedBuilder()
            .setColor(Color.RED)
            .setTitle("Match #" + event.getMatch().getId() + " has finished!")
            .setDescription("Finished at <t:" + Instant.now().getEpochSecond() + ":f>")
            .addInlineField("Winner", winners.isEmpty() ? "_No winner_" : winners)
            .addInlineField("Time", parseDuration(event.getMatch().getDuration()))
            .addInlineField("\u200E", "\u200E")
            .addInlineField("Map", event.getMatch().getMap().getName())
            .addInlineField("Version", event.getMatch().getMap().getVersion().toString())
            .addInlineField("Gamemodes", getMapGamemodes(event.getMatch()).toUpperCase())
            .addInlineField("Created by", getMapAuthors(event.getMatch()))
            .addInlineField("Pools", getMapPools(event.getMatch()))
            .addField("Objective", event.getMatch().getMap().getDescription())
            .addInlineField("Players online", String.valueOf(event.getMatch().getPlayers().size()))
            .addInlineField("Staff online", String.valueOf(getOnlineStaffCount(event.getMatch())))
            .setFooter("Map tags: " + event.getMatch().getMap().getTags().toString());
    sendEmbed(matchFinishEmbed);
  }

  /*public void matchPlayersEmbed(MatchFinishEvent event) {
    EmbedBuilder playersEmbed =
            new EmbedBuilder()
                    .setColor(Color.YELLOW)
                    .setTitle("Stats for match #" + event.getMatch().getId());
    TeamMatchModule tmm = event.getMatch().getModule(TeamMatchModule.class);
    if (tmm != null) {
      tmm.getTeams()
              .forEach(
                      team ->
                              playersEmbed.addField(
                                      team.getNameLegacy()
                                              + ": "
                                              + team.getPlayers().size()
                                              + "/"
                                              + team.getMaxPlayers(),
                                      team.getPlayers().isEmpty()
                                              ? "_No players_"
                                              : team.getPlayers().stream()
                                              .map(MatchPlayer::getNameLegacy)
                                              .collect(Collectors.joining(", "))));
    } else {
      playersEmbed.addField(
              "Players: "
                      + event.getMatch().getParticipants().size()
                      + "/"
                      + event.getMatch().getMaxPlayers(),
              event.getMatch().getParticipants().stream()
                      .map(MatchPlayer::getNameLegacy)
                      .collect(Collectors.joining(", ")));
    }
    playersEmbed.addInlineField(
            "Observers", String.valueOf(event.getMatch().getDefaultParty().getPlayers().size()));
    playersEmbed.addInlineField(
            "Staff online",
            String.valueOf(
                    event.getMatch().getPlayers().stream()
                            .filter(
                                    player ->
                                            (player.getBukkit().hasPermission(Permissions.STAFF)
                                                    && !player.isVanished()))
                            .count()));
    playersEmbed.addInlineField(
            "Total online", String.valueOf(event.getMatch().getPlayers().size()));
    sendEmbed(playersEmbed);
  }*/

  public void reload() {
    if (this.api != null && !config.isEnabled()) {
      disable();
    } else if (this.api == null && config.isEnabled()) {
      enable();
    }
  }
}
