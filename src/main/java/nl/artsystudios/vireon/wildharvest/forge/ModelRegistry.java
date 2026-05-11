package nl.artsystudios.vireon.wildharvest.forge;

import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tracks the {@code CustomModelData} integer assigned to each Vireon model id.
 *
 * <p>Assignments are persisted to {@code forge/.assignments.yml} so a model that
 * was CMD 1001 yesterday is still CMD 1001 tomorrow — otherwise every pack
 * rebuild would invalidate existing in-world items.</p>
 */
public final class ModelRegistry {

    private static final int CMD_BASE = 1000;

    private final VireonWildHarvest plugin;
    private final File assignmentsFile;
    private final Map<String, Integer> assignments = new LinkedHashMap<>();
    private int nextCmd = CMD_BASE;

    public ModelRegistry(VireonWildHarvest plugin, File forgeFolder) {
        this.plugin           = plugin;
        this.assignmentsFile  = new File(forgeFolder, ".assignments.yml");
    }

    /** Load persisted assignments from disk. Safe to call repeatedly. */
    public void load() {
        assignments.clear();
        nextCmd = CMD_BASE;

        if (!assignmentsFile.exists()) return;

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(assignmentsFile);
        nextCmd = yaml.getInt("next-cmd", CMD_BASE);

        var section = yaml.getConfigurationSection("assignments");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                assignments.put(id, section.getInt(id));
            }
        }
    }

    /** Persist assignments back to disk. */
    public void save() {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("next-cmd", nextCmd);
        for (var e : assignments.entrySet()) {
            yaml.set("assignments." + e.getKey(), e.getValue());
        }
        try {
            yaml.save(assignmentsFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save Forge model assignments: " + ex.getMessage());
        }
    }

    /** Look up the CMD for a model id, allocating a new one if first seen. */
    public int getOrAssign(String id) {
        Integer existing = assignments.get(id);
        if (existing != null) return existing;
        int cmd = nextCmd++;
        assignments.put(id, cmd);
        return cmd;
    }

    /** Look up without allocating. Returns {@code null} if id is unknown. */
    public Integer get(String id) {
        return assignments.get(id);
    }

    public Map<String, Integer> snapshot() {
        return new LinkedHashMap<>(assignments);
    }
}
