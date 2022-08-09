package me.tbg.match.bot

import org.bukkit.plugin.java.JavaPlugin

class MatchBot : JavaPlugin() {
    private var bot: DiscordBot? = null
    private var config: BotConfig? = null
    override fun onEnable() {
        saveDefaultConfig()
        reloadConfig()
        config = BotConfig(getConfig())
        bot = DiscordBot(config!!, logger)
        registerListeners()
    }

    private fun registerListeners() {
        server.pluginManager.registerEvents(MatchStartListener(bot), this)
        server.pluginManager.registerEvents(MatchFinishListener(bot!!), this)
    }

    fun reloadBotConfig() {
        reloadConfig()
        config!!.reload(getConfig())
        bot!!.reload()
    }
}