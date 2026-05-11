package nl.artsystudios.vireon.wildharvest;

import nl.artsystudios.vireon.wildharvest.command.VireonCommand;
import nl.artsystudios.vireon.wildharvest.config.ConfigManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Vireon Wild Harvest — entry point.
 *
 * <p>Part of the Vireon plugin series by ArtsyStudios.
 * This is the 0.1.0 foundation build: command framework, config manager,
 * lifecycle hooks. The realistic-wildlife mob conversion arrives in 0.2.0
 * and the Blockbench model loader (Vireon Forge) in 0.3.0.</p>
 */
public final class VireonWildHarvest extends JavaPlugin {

    private static VireonWildHarvest instance;
    private ConfigManager configManager;

    @Override
    public void onEnable() {
        instance = this;

        // Ensure config.yml is on disk so users can edit it.
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        registerCommands();

        getLogger().info("───────────────────────────────");
        getLogger().info(" Vireon Wild Harvest v" + getPluginMeta().getVersion());
        getLogger().info(" ArtsyStudios — vireon series");
        getLogger().info(" Foundation ready.");
        getLogger().info(" Mob conversion arrives in v0.2.0.");
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

    /** Singleton accessor for cross-package use. */
    public static VireonWildHarvest get() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }
}
