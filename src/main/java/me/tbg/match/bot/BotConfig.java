package me.tbg.match.bot;

import org.bukkit.configuration.Configuration;

public class BotConfig {

    private boolean enabled;

    private String token;
    private String serverId;
    private String matchChannel;
    private String fallbackMapImages;
    private String mapImageNotFound;

    public BotConfig(Configuration config) {
        reload(config);
    }

    public void reload(Configuration config) {
        this.enabled = config.getBoolean("enabled");
        this.token = config.getString("token");
        this.serverId = config.getString("server");
        this.matchChannel = config.getString("match-channel");
        this.fallbackMapImages = config.getString("fallback-map-images");
        this.mapImageNotFound = config.getString("map-image-not-found");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getToken() {
        return token;
    }

    public String getServerId() {
        return serverId;
    }

    public String getMatchChannel() {
        return matchChannel;
    }

    public String getFallbackMapImages() {
        return fallbackMapImages;
    }

    public String getMapImageNotFound() {
        return mapImageNotFound;
    }
}
