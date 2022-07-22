package me.tbg.match.bot;

import java.awt.*;
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
    EmbedBuilder matchStartEmbed =
        new EmbedBuilder()
            .setColor(Color.GREEN)
            .setTitle("Match #" + match.getId() + " has started!")
            .setThumbnail(bot.getMapImageUrl(map))
            .setDescription(
                "Started at <t:"
                    + Instant.now().getEpochSecond()
                    + ":f> with "
                    + match.getPlayers().size()
                    + (match.getPlayers().size() == 1 ? " player" : " players")
                    + " online.")
            .addInlineField("Map", map.getName())
            .addInlineField("Version", map.getVersion().toString())
            .addInlineField("Gamemodes", bot.getMapGamemodes(match).toUpperCase())
            .addInlineField("Created by", bot.getMapAuthors(match))
            .addInlineField("Pools", bot.getMapPools(match))
            .addField("Objective", map.getDescription())
            .addInlineField("Participants", String.valueOf(match.getPlayers().size()))
            .addInlineField(
                "Observers", String.valueOf(match.getDefaultParty().getPlayers().size()))
            .addInlineField("Staff", String.valueOf(bot.getOnlineStaffCount(match)))
            .setFooter("Map tags: " + map.getTags().toString());
    bot.sendMatchEmbed(matchStartEmbed, event.getMatch());
  }
}
