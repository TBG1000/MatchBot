package me.tbg.match.bot;

import java.time.Duration;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.javacord.api.DiscordApi;
import org.javacord.api.DiscordApiBuilder;
import org.javacord.api.entity.activity.ActivityType;
import org.javacord.api.entity.channel.Channel;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.util.logging.ExceptionLogger;
import tc.oc.pgm.api.PGM;
import tc.oc.pgm.api.Permissions;
import tc.oc.pgm.api.map.Contributor;
import tc.oc.pgm.api.map.Gamemode;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.rotation.MapPoolManager;
import tc.oc.pgm.rotation.pools.MapPool;

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

  public void sendMatchEmbed(EmbedBuilder embed, Match match) {
    if (api != null) {
      api.updateActivity(ActivityType.PLAYING, match.getMap().getName());
      api.getServerById(config.getServerId())
          .flatMap(
              server ->
                  server.getChannelById(config.getMatchChannel()).flatMap(Channel::asTextChannel))
          .ifPresent(text -> text.sendMessage(embed).exceptionally(ExceptionLogger.get()));
    }
  }

  public String parseDuration(Duration duration) {
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

  public void reload() {
    if (this.api != null && !config.isEnabled()) {
      disable();
    } else if (this.api == null && config.isEnabled()) {
      enable();
    }
  }
}
