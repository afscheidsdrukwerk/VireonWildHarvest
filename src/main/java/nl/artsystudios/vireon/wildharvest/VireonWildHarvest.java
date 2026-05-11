package nl.artsystudios.vireon.wildharvest;

import nl.artsystudios.vireon.wildharvest.command.VireonCommand;
import nl.artsystudios.vireon.wildharvest.config.ConfigManager;
import nl.artsystudios.vireon.wildharvest.forge.ForgeService;
import nl.artsystudios.vireon.wildharvest.forge.listener.PackJoinListener;
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
 * <p>v0.3.0 adds Vireon Forge: an embedded resource pack builder + HTTP host
 * that auto-generates custom item models for drops, served to joining players
 * over an in-plugin web server. Mob visual replacement comes in v0.3.1.</p>
 */
public final class VireonWildHarvest extends JavaPlugin {

    private static VireonWildHarvest instance;

    private ConfigManager configManager;
    private MobConversionService mobConversionService;
    private ForgeService forgeService;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        this.configManager        = new ConfigManager(this);
        this.mobConversionService = new MobConversionService(this);
        this.mobConversionService.loadFromConfig();

        // Forge must come AFTER MobConversionService so it can discover required model ids.
        this.forgeService = new ForgeService(this);
        this.forgeService.initialize();

        registerCommands();
        registerListeners();

        getLogger().info("───────────────────────────────");
        getLogger().info(" Vireon Wild Harvest v" + getDescription().getVersion());
        getLogger().info(" ArtsyStudios — vireon series");
        getLogger().info(" " + mobConversionService.all().size() + " mob conversion(s) loaded.");
        getLogger().info(" Forge hosting: " + (forgeService.isHosting() ? "ON" : "OFF"));
        getLogger().info("───────────────────────────────");
    }

    @Override
    public void onDisable() {
        if (forgeService != null) forgeService.shutdown();
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
        pm.registerEvents(new PackJoinListener(this),                        this);
    }

    public void reloadAll() {
        reloadConfig();
        configManager.reload();
        mobConversionService.loadFromConfig();
        if (forgeService != null) forgeService.reload();
    }

    public static VireonWildHarvest get()                  { return instance; }
    public ConfigManager getConfigManager()                { return configManager; }
    public MobConversionService getMobConversionService()  { return mobConversionService; }
    public ForgeService getForgeService()                  { return forgeService; }
}
