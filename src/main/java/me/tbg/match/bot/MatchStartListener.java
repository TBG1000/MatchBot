package me.tbg.match.bot;

import java.awt.Color;
import java.time.Instant;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import net.dv8tion.jda.api.EmbedBuilder;
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

        bot.setEmbedThumbnail(map, matchStartEmbed, bot);
        bot.sendMatchEmbed(matchStartEmbed, match);
        bot.storeMatchStartData(
                Long.parseLong(match.getId()),
                Instant.now().getEpochSecond(),
                match.getPlayers().size());
    }

    private EmbedBuilder createMatchStartEmbed(Match match, MapInfo map) {
        return new EmbedBuilder()
                .setColor(Color.WHITE.getRGB())
                .setTitle("Match #" + match.getId() + " has started!")
                .setDescription(bot.getMatchDescription(match))
                .addField("Map", map.getName(), true)
                .addField("Version", map.getVersion().toString(), true)
                .addField("Gamemodes", bot.getMapGamemodes(match).toUpperCase(), true)
                .addField("Created by", bot.getMapAuthors(match), true)
                .addField("Pools", bot.getMapPools(match), true)
                .addField("Objective", map.getDescription(), false)
                .addField("Participants", String.valueOf(match.getParticipants().size()), true)
                .addField("Observers", String.valueOf(match.getDefaultParty().getPlayers().size()), true);
    }
}
