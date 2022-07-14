package me.tbg.match.bot;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import tc.oc.pgm.api.match.event.MatchFinishEvent;
import tc.oc.pgm.api.match.event.MatchStartEvent;

public class MatchListener implements Listener {

  private final DiscordBot bot;

  public MatchListener(DiscordBot bot) {
    this.bot = bot;
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    bot.matchStartEmbed(event);
  }

  @EventHandler
  public void onMatchFinish(MatchFinishEvent event) {
    bot.matchFinishEmbed(event);
    bot.teamStatsEmbed(event);
    bot.matchStatsEmbed(event);
  }
}
