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
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.AttachedFile;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
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

    private JDA api;
    private BotConfig config;
    private Logger logger;

    private Map<Long, Long> matchMessageMap = new HashMap<>();
    private Map<Long, Long> matchStartTimestamps = new HashMap<>();
    private Map<Long, Integer> matchStartPlayers = new HashMap<>();

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
            try {
                this.api = JDABuilder.createDefault(config.getToken())
                        .enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS)
                        .setActivity(Activity.playing("Starting up..."))
                        .build();
                this.api.awaitReady();
                logger.info("Discord Bot (MatchBot) is now active!");
            } catch (Exception e) {
                logger.info("Failed to login to Discord: " + e.getMessage());
            }
        }
    }

    private void setAPI(JDA api) {
        this.api = api;
    }

    public void disable() {
        if (this.api != null) {
            this.api.shutdown();
        }
        this.api = null;
    }

    public void sendMatchEmbed(EmbedBuilder embed, Match match) {
        if (api != null) {
            api.getPresence().setActivity(Activity.playing("Playing " + match.getMap().getName() + " on " + config.getServerName()));
            if (config.getServerName().isEmpty()) {
                api.getPresence().setActivity(Activity.playing("Playing " + match.getMap().getName()));
            }
            Guild guild = api.getGuildById(config.getServerId());
            if (guild != null) {
                TextChannel textChannel = guild.getTextChannelById(config.getMatchChannel());
                if (textChannel != null) {
                    File imgFile = new File(match.getMap().getSource().getAbsoluteDir().toFile(), "map.png");
                    MessageCreateAction messageAction = (imgFile.exists()) ? textChannel.sendFiles(FileUpload.fromData(imgFile, "map.png")).setEmbeds(embed.build())
                            : textChannel.sendMessageEmbeds(embed.build());
                    messageAction.queue(message -> {
                        matchMessageMap.put(Long.valueOf(match.getId()), message.getIdLong());
                    });
                }
            }
        }
    }

    public void editMatchEmbed(long matchId, EmbedBuilder newEmbed) {
        if (api != null && matchMessageMap.containsKey(matchId)) {
            Guild guild = api.getGuildById(config.getServerId());
            if (guild != null) {
                TextChannel textChannel = guild.getTextChannelById(config.getMatchChannel());
                if (textChannel != null) {
                    long messageId = matchMessageMap.get(matchId);
                    textChannel.retrieveMessageById(messageId).queue(message -> {
                        message.editMessageEmbeds(newEmbed.build()).queue();
                    });
                }
            }
        }
    }

    public EmbedBuilder setEmbedThumbnail(MapInfo map, EmbedBuilder embed, DiscordBot bot) {
        File imgFile = new File(map.getSource().getAbsoluteDir().toFile(), "map.png");
        if (!imgFile.exists()) {
            if (!bot.getConfig().getMapImageNotFound().isEmpty()) {
                embed.setThumbnail(bot.getConfig().getMapImageNotFound());
                return embed;
            }
            return embed;
        }
        embed.setThumbnail("attachment://map.png");
        return embed;
    }

    public String parseDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

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

    public long getOnlineStaffCount(Match match) {
        // Adapted from
        // https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/command/MapCommand.java
        return match.getPlayers().stream()
                .filter(player -> (player.getBukkit().hasPermission(Permissions.STAFF)
                        && !Integration.isVanished(player.getBukkit())))
                .count();
    }

    public void storeMatchStartData(long matchId, Long startTimestamp, Integer players) {
        matchStartTimestamps.put(matchId, startTimestamp);
        matchStartPlayers.put(matchId, players);
    }

    public Long getMatchStartTimestamp(long matchId) {
        return matchStartTimestamps.get(matchId);
    }

    public Integer getMatchStartPlayers(long matchId) {
        return matchStartPlayers.get(matchId);
    }

    public void reload() {
        if (this.api != null && !config.isEnabled()) {
            disable();
        } else if (this.api == null && config.isEnabled()) {
            enable();
        }
    }
}
