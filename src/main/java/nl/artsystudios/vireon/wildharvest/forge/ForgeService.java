package nl.artsystudios.vireon.wildharvest.forge;

import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import nl.artsystudios.vireon.wildharvest.mob.DropDefinition;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Top-level coordinator for Vireon Forge.
 *
 * <p>Owns the model registry, the resource pack builder, and the embedded HTTP
 * server. Initialised once on plugin enable, refreshed by {@code /vireon forge
 * reload}, and shut down on disable.</p>
 */
public final class ForgeService {

    private final VireonWildHarvest plugin;

    private final File forgeFolder;
    private final ModelRegistry modelRegistry;
    private final ResourcePackBuilder packBuilder;
    private final PackHostServer hostServer;

    public ForgeService(VireonWildHarvest plugin) {
        this.plugin       = plugin;
        this.forgeFolder  = new File(plugin.getDataFolder(), "forge");
        this.modelRegistry = new ModelRegistry(plugin, forgeFolder);
        this.packBuilder   = new ResourcePackBuilder(plugin, modelRegistry, forgeFolder);
        this.hostServer    = new PackHostServer(plugin);
    }

    public void initialize() {
        if (!forgeFolder.exists() && !forgeFolder.mkdirs()) {
            plugin.getLogger().severe("Could not create Forge folder: " + forgeFolder);
            return;
        }
        modelRegistry.load();
        ensurePlaceholderFolders();

        try {
            packBuilder.build();
        } catch (IOException ex) {
            plugin.getLogger().severe("Vireon Forge: pack build failed: " + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        if (plugin.getConfigManager().isResourcePackEnabled()) {
            startHost();
        } else {
            plugin.getLogger().info("Vireon Forge: pack built but auto-host is disabled in config.");
        }
    }

    /** Rebuild the pack from disk and reload the host server with the new file. */
    public void reload() {
        modelRegistry.load();
        ensurePlaceholderFolders();
        try {
            packBuilder.build();
            hostServer.updatePack(packBuilder.getPackFile());
        } catch (IOException ex) {
            plugin.getLogger().severe("Vireon Forge: pack rebuild failed: " + ex.getMessage());
        }

        if (plugin.getConfigManager().isResourcePackEnabled() && !hostServer.isRunning()) {
            startHost();
        } else if (!plugin.getConfigManager().isResourcePackEnabled() && hostServer.isRunning()) {
            hostServer.stop();
        }
    }

    public void shutdown() {
        hostServer.stop();
        modelRegistry.save();
    }

    // ─── Accessors ────────────────────────────────────────────

    public ModelRegistry getModelRegistry() { return modelRegistry; }
    public File          getForgeFolder()   { return forgeFolder; }
    public File          getPackFile()      { return packBuilder.getPackFile(); }
    public String        getPackHash()      { return packBuilder.getPackHash(); }
    public boolean       isHosting()        { return hostServer.isRunning(); }

    /** Public URL clients use to fetch the pack. {@code null} until host is running. */
    public String getPublicPackUrl() {
        if (!plugin.getConfigManager().isResourcePackEnabled()) return null;

        String addr = plugin.getConfigManager().getResourcePackPublicAddress();
        if (addr == null || addr.isBlank()) return null;

        // Allow "host:port", bare "host", or "http(s)://host:port" — normalise.
        String url = addr.trim();
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // If user gave only a host (no port), tack the configured port on.
            if (!url.contains(":")) url = url + ":" + plugin.getConfigManager().getResourcePackPort();
            url = "http://" + url;
        }
        if (!url.endsWith("/")) url = url + "/";
        return url + "vireon-pack.zip";
    }

    // ─── Internals ────────────────────────────────────────────

    private void startHost() {
        try {
            hostServer.start(
                    plugin.getConfigManager().getResourcePackHost(),
                    plugin.getConfigManager().getResourcePackPort(),
                    packBuilder.getPackFile());
        } catch (IOException ex) {
            plugin.getLogger().severe("Vireon Forge: failed to start pack host: " + ex.getMessage());
        }
    }

    /**
     * For every item id referenced by a loaded conversion, ensure a folder
     * exists under {@code forge/&lt;id&gt;/} with at least a {@code model.json}
     * and a placeholder {@code texture.png}. This means the plugin "just works"
     * on first install — the operator can replace these later with their own
     * Blockbench exports.
     */
    private void ensurePlaceholderFolders() {
        var drops = packBuilder.collectItemModels();
        for (var entry : drops.entrySet()) {
            String id = entry.getKey();
            DropDefinition drop = entry.getValue();
            File folder = new File(forgeFolder, id);
            if (!folder.exists() && !folder.mkdirs()) {
                plugin.getLogger().warning("Could not create forge folder for '" + id + "'");
                continue;
            }
            File modelJson = new File(folder, "model.json");
            File texture   = new File(folder, "texture.png");
            if (!modelJson.exists()) writeDefaultItemModel(modelJson, id);
            if (!texture.exists())   writePlaceholderTexture(texture);

            // Per-folder README so first-time users know what to do with it.
            File readme = new File(folder, "README.txt");
            if (!readme.exists()) writeFolderReadme(readme, id, drop);
        }
    }

    private void writeDefaultItemModel(File file, String id) {
        String json =
                "{\n" +
                "  \"parent\": \"minecraft:item/generated\",\n" +
                "  \"textures\": { \"layer0\": \"vireon:item/" + id + "\" }\n" +
                "}\n";
        try {
            Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write default model.json for '" + id + "': " + ex.getMessage());
        }
    }

    private void writePlaceholderTexture(File file) {
        // 16×16 magenta/black checker — the unmistakable "missing texture" tell.
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                boolean check = ((x >> 2) + (y >> 2)) % 2 == 0;
                img.setRGB(x, y, check ? 0xFFFF00FF : 0xFF000000);
            }
        }
        try {
            ImageIO.write(img, "png", file);
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not write placeholder texture: " + ex.getMessage());
        }
    }

    private void writeFolderReadme(File file, String id, DropDefinition drop) {
        String text =
                "Vireon Forge model folder — " + id + "\n" +
                "Material: " + drop.getMaterial().name() + "\n" +
                "\n" +
                "Replace model.json and texture.png with your Blockbench exports:\n" +
                "  • model.json   — exported via Blockbench: File → Export → Generic Model (Item)\n" +
                "  • texture.png  — the model's PNG texture\n" +
                "\n" +
                "After editing, run /vireon forge reload to rebuild the pack and re-host it.\n";
        try {
            Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
        } catch (IOException ignored) { /* not critical */ }
    }
}
