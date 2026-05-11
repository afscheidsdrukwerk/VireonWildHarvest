package nl.artsystudios.vireon.wildharvest;

import nl.artsystudios.vireon.wildharvest.command.VireonCommand;
import nl.artsystudios.vireon.wildharvest.config.ConfigManager;
import nl.artsystudios.vireon.wildharvest.mob.MobConversionService;
import nl.artsystudios.vireon.wildharvest.mob.listener.MobCombustListener;
import nl.artsystudios.vireon.wildharvest.mob.listener.MobDeathListener;
import nl.artsystudios.vireon.wildharvest.mob.listener.MobSpawnListener;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Vireon Wild Harvest — entry point.
 *
 * <p>v0.2.0 adds the mob conversion engine: natural Zombies become
 * Brown Bears with realistic drops and sun immunity. Custom models
 * (Vireon Forge) and the admin GUI come in 0.3.0 / 0.4.0.</p>
 */
public final class VireonWildHarvest extends JavaPlugin {

    private static VireonWildHarvest instance;

    private ConfigManager configManager;
    private MobConversionService mobConversionService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager        = new ConfigManager(this);
        this.mobConversionService = new MobConversionService(this);
        this.mobConversionService.loadFromConfig();

        registerCommands();
        registerListeners();

        getLogger().info("───────────────────────────────");
        getLogger().info(" Vireon Wild Harvest v" + getDescription().getVersion());
        getLogger().info(" ArtsyStudios — vireon series");
        getLogger().info(" " + mobConversionService.all().size() + " mob conversion(s) loaded.");
        getLogger().info("───────────────────────────────");
    }

    @Override
    public void onDisable() {
        getLogger().info("Vireon Wild Harvest disabled.");
        instance = null;
    }

    private void registerCommands() {
        PluginCommand cmd = getCommand("vireon");
        if (cmd == null) {
            getLogger().severe("Failed to register /vireon command — plugin.yml mismatch?");
            return;
        }
        VireonCommand handler = new VireonCommand(this);
        cmd.setExecutor(handler);
        cmd.setTabCompleter(handler);
    }

    private void registerListeners() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new MobSpawnListener(this, mobConversionService),  this);
        pm.registerEvents(new MobCombustListener(mobConversionService),      this);
        pm.registerEvents(new MobDeathListener(mobConversionService),        this);
    }

    public void reloadAll() {
        reloadConfig();
        configManager.reload();
        mobConversionService.loadFromConfig();
    }

    public static VireonWildHarvest get()              { return instance; }
    public ConfigManager getConfigManager()            { return configManager; }
    public MobConversionService getMobConversionService() { return mobConversionService; }
}
