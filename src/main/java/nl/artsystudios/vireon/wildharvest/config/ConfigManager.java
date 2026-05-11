package nl.artsystudios.vireon.wildharvest.config;

import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Thin wrapper over Bukkit's FileConfiguration that caches the values we
 * look up frequently. Future modules (mob conversion, resource pack host,
 * Vireon Forge) will hang their own getters off here.
 */
public class ConfigManager {

    private final VireonWildHarvest plugin;

    private FileConfiguration config;
    private boolean debug;
    private String language;
    private boolean mobsEnabled;
    private boolean resourcePackEnabled;
    private String resourcePackHost;
    private int resourcePackPort;
    private String resourcePackPublicAddress;
    private boolean resourcePackRequired;
    private String resourcePackPrompt;

    public ConfigManager(VireonWildHarvest plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        this.config = plugin.getConfig();

        this.debug = config.getBoolean("debug", false);
        this.language = config.getString("language", "en");
        this.mobsEnabled = config.getBoolean("mobs.enabled", true);

        this.resourcePackEnabled       = config.getBoolean("resource-pack.enabled", false);
        this.resourcePackHost          = config.getString("resource-pack.host", "0.0.0.0");
        this.resourcePackPort          = config.getInt("resource-pack.port", 8200);
        this.resourcePackPublicAddress = config.getString("resource-pack.public-address", "");
        this.resourcePackRequired      = config.getBoolean("resource-pack.required", true);
        this.resourcePackPrompt        = config.getString("resource-pack.prompt", "");

        if (debug) {
            plugin.getLogger().info("[debug] Configuration reloaded.");
            plugin.getLogger().info("[debug]  language=" + language + ", mobsEnabled=" + mobsEnabled);
            plugin.getLogger().info("[debug]  resource-pack enabled=" + resourcePackEnabled
                    + ", port=" + resourcePackPort
                    + ", required=" + resourcePackRequired);
        }
    }

    public boolean isDebug() { return debug; }
    public String  getLanguage() { return language; }
    public boolean isMobsEnabled() { return mobsEnabled; }

    public boolean isResourcePackEnabled() { return resourcePackEnabled; }
    public String  getResourcePackHost() { return resourcePackHost; }
    public int     getResourcePackPort() { return resourcePackPort; }
    public String  getResourcePackPublicAddress() { return resourcePackPublicAddress; }
    public boolean isResourcePackRequired() { return resourcePackRequired; }
    public String  getResourcePackPrompt() { return resourcePackPrompt; }

    public FileConfiguration raw() { return config; }
}
