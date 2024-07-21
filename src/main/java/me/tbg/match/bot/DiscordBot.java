package me.tbg.match.bot;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
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
import tc.oc.pgm.api.integration.Integration;

import javax.imageio.ImageIO;

public class DiscordBot {

    private DiscordApi api;
    private BotConfig config;
    private Logger logger;

    private Map<Long, Long> matchMessageMap = new HashMap<>();

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
                    .thenAcceptAsync(api -> {
                        setAPI(api);
                        api.setMessageCacheSize(1, 60 * 60);
                        api.addServerBecomesAvailableListener(
                                listener -> logger.info(listener.getServer().getName() + " is now available"));
                        logger.info("Discord Bot (MatchBot) is now active!");
                    })
                    .exceptionally(throwable -> {
                        logger.info("Failed to login to Discord: " + throwable.getMessage());
                        return null;
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
                    .flatMap(server ->
                            server.getChannelById(config.getMatchChannel()).flatMap(Channel::asTextChannel))
                    .ifPresent(textChannel -> textChannel
                            .sendMessage(embed)
                            .thenAccept(message -> matchMessageMap.put(Long.valueOf(match.getId()), message.getId()))
                            .exceptionally(ExceptionLogger.get()));
        }
    }

    public void editMatchEmbed(long matchId, EmbedBuilder newEmbed) {
        if (api != null && matchMessageMap.containsKey(matchId)) {
            long messageId = matchMessageMap.get(matchId);
            api.getServerById(config.getServerId())
                    .flatMap(server ->
                            server.getChannelById(config.getMatchChannel()).flatMap(Channel::asTextChannel))
                    .ifPresent(textChannel -> textChannel
                            .getMessageById(messageId)
                            .thenAccept(message -> message.edit(newEmbed))
                            .exceptionally(ExceptionLogger.get()));
        }
    }

    public String parseDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes();
        long seconds = duration.getSeconds();

        StringBuilder result = new StringBuilder();

        if (hours > 0) {
            result.append(hours).append(hours == 1 ? " hour " : " hours ");
        }
        if (minutes > 0) {
            result.append(minutes).append(minutes == 1 ? " minute " : " minutes ");
        }
        if (seconds > 0 || result.length() == 0) {
            result.append(seconds).append(seconds == 1 ? " second" : " seconds");
        }

        return result.length() > 0 ? result.toString().trim() : "_Unavailable_";
    }

    public String getMapPools(Match match) {
        // Extracted from
        // https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/command/MapCommand.java
        if (PGM.get().getMapOrder() instanceof MapPoolManager) {
            String mapPools = ((MapPoolManager) PGM.get().getMapOrder())
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
        return match.getMap().getGamemodes().stream().map(Gamemode::getId).collect(Collectors.joining(", "));
    }

    public String getMatchDescription(Match match) {
        int playerCount = match.getPlayers().size();
        return "Started at <t:" + Instant.now().getEpochSecond() + ":f> with **" + playerCount
                + (playerCount == 1 ? " player" : " players") + "** online.";
    }

    public BufferedImage getMapImage(MapInfo map) throws IOException {
        Path sourceDir = map.getSource().getAbsoluteDir();
        File pngFile = new File(sourceDir.toFile(), "map.png");
        return ImageIO.read(pngFile);
    }

    public long getOnlineStaffCount(Match match) {
        // Adapted from
        // https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/command/MapCommand.java
        return match.getPlayers().stream()
                .filter(player -> (player.getBukkit().hasPermission(Permissions.STAFF)
                        && !Integration.isVanished(player.getBukkit())))
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
