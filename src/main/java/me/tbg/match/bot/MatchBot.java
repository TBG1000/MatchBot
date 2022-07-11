package me.tbg.match.bot;

import org.bukkit.plugin.java.JavaPlugin;

public class MatchBot extends JavaPlugin {

  private DiscordBot bot;
  private BotConfig config;

  @Override
  public void onEnable() {
    this.saveDefaultConfig();
    this.reloadConfig();
    this.config = new BotConfig(getConfig());
    this.bot = new DiscordBot(config, getLogger());
    this.registerListeners();
  }

  private void registerListeners() {
    this.getServer().getPluginManager().registerEvents(new MatchListener(bot), this);
  }

  public void reloadBotConfig() {
    this.reloadConfig();
    config.reload(getConfig());
    bot.reload();
  }
}
