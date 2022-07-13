package me.tbg.match.bot;

import org.bukkit.configuration.Configuration;

public class BotConfig {

  private boolean enabled;

  private String token;
  private String serverId;
  private String matchChannel;
  private String mapImagesURL;

  public BotConfig(Configuration config) {
    reload(config);
  }

  public void reload(Configuration config) {
    this.enabled = config.getBoolean("enabled");
    this.token = config.getString("token");
    this.serverId = config.getString("server");
    this.matchChannel = config.getString("match-channel");
    this.mapImagesURL = config.getString("map-images-url");
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

  public String getMapImagesURL() {
    return mapImagesURL;
  }
}
