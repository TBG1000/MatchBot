package me.tbg.match.bot

import org.bukkit.configuration.Configuration

class BotConfig(config: Configuration) {
    var isEnabled = false
        private set
    var token: String? = null
        private set
    var serverId: String? = null
        private set
    var matchChannel: String? = null
        private set
    var mapImagesURL: String? = null
        private set

    fun reload(config: Configuration) {
        isEnabled = config.getBoolean("enabled")
        token = config.getString("token")
        serverId = config.getString("server")
        matchChannel = config.getString("match-channel")
        mapImagesURL = config.getString("map-images-url")
    }

    init {
        reload(config)
    }
}