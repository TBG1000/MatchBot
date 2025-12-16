# MatchBot (Deprecated - Javacord library)
A Minecraft to Discord bot, interacting with [PGM](https://github.com/PGMDev/PGM/) Match events to display match information live.

This project's structure is a modified version of [Bolty](https://github.com/applenick/Bolty), developed by [applenick](https://github.com/applenick). Similarly, the project is also directly inspired by the `#match-status` channel located at the [Overcast Community](https://oc.tc) Discord server.

## Description

MatchBot will listen to [`MatchStartEvent`](https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/api/match/event/MatchStartEvent.java) and [`MatchFinishEvent`](https://github.com/PGMDev/PGM/blob/dev/core/src/main/java/tc/oc/pgm/api/match/event/MatchFinishEvent.java) to populate a Discord [embed](https://javacord.org/wiki/basic-tutorials/embeds.html#creating-an-embed) with information about a started or finished match.

MatchBot is built with [Javacord](https://javacord.org/), an awesome Java library for Discord bots. Javacord is no longer supported. Feel free to use JDA on the main branch or port the bot to your preferred library.

This bot runs on a single Minecraft server, and is not designed with proxies, networks, or multiple servers in mind.

## Building

1. First, [clone](https://docs.github.com/en/repositories/creating-and-managing-repositories/cloning-a-repository) or download the project's source code.
2. Optionally, make your desired changes.
3. Run the code formatter, following Google's [code style.](https://google.github.io/styleguide/javaguide.html)
```bash
mvn com.coveo:fmt-maven-plugin:format
```
5. Compile the project.
```bash
mvn package
```

You'll find the bot's `.jar` file inside the `target` folder of the project's root directory.

## Installing

MatchBot depends on [PGM](https://github.com/PGMDev/PGM/) directly to work. Make sure your server has it installed.

1. Drop the plugin's `.jar` in your server's `plugins` folder.
2. Restart the server to automatically generate the bot's required files (`config.yml`, `plugin.yml`).
3. Fill in the blanks of the configuration file (`config.yml`). To do this, you'll need the following:
    - A token for your Discord bot which you can get at the [Discord Developer Portal](https://discord.com/developers/docs)
    - The ID of the server in which the bot will be functioning.
    - The ID of the channel in which match embeds will be sent.
4. Restart the server once again for the changes to take place. Once your bot goes online, match embeds will be sent to the designated channel as soon as matches start or end.

You may look at a sample of the configuration file [below](https://github.com/TBG1000/MatchBot#config).
You can also find out how to get server, role or channel IDs [here](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID).


## Sample images
![Match start sample](https://i.imgur.com/JsbQFDV.png)
![Match finish sample](https://i.imgur.com/N5bG46T.png)

## Config
    
```yaml
# Discord Stuff
enabled: true # Enable discord bot?
token: "" # Discord bot token
server: "" # ID of discord server

# ID of channel where match embeds will be sent
match-channel: ""
```

