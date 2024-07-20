package me.tbg.match.bot;

import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import tc.oc.pgm.api.map.MapInfo;
import tc.oc.pgm.api.match.Match;
import tc.oc.pgm.api.match.event.MatchStartEvent;

public class MatchStartListener implements Listener {

  private final DiscordBot bot;

  public MatchStartListener(DiscordBot bot) {
    this.bot = bot;
  }

  @EventHandler
  public void onMatchStart(MatchStartEvent event) {
    Match match = event.getMatch();
    MapInfo map = match.getMap();
    EmbedBuilder matchStartEmbed = createMatchStartEmbed(match, map);

    try {
      matchStartEmbed.setThumbnail(bot.getMapImage(map));
    } catch (IOException e) {
      System.out.println("Unable to get map image for " + map.getName());
    }

    bot.sendMatchEmbed(matchStartEmbed, match);
  }

  private EmbedBuilder createMatchStartEmbed(Match match, MapInfo map) {
    return new EmbedBuilder()
            .setColor(Color.WHITE)
            .setTitle("Match #" + match.getId() + " has started!")
            .setDescription(bot.getMatchDescription(match))
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
  }
}
