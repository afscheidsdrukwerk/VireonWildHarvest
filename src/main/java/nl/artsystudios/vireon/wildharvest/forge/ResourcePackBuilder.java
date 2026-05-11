package nl.artsystudios.vireon.wildharvest.forge;

import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import nl.artsystudios.vireon.wildharvest.mob.DropDefinition;
import nl.artsystudios.vireon.wildharvest.mob.MobConversion;
import org.bukkit.Material;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Compiles the {@code forge/} folder into a real Minecraft resource pack zip.
 *
 * <p>Steps per build:
 * <ol>
 *   <li>Iterate every drop in every loaded {@link MobConversion} so we know which
 *       Vireon item ids are in use, which Materials they piggyback on, and which
 *       {@code CustomModelData} integer the {@link ModelRegistry} has assigned.</li>
 *   <li>For each Vireon item id, copy {@code forge/&lt;id&gt;/model.json} and
 *       {@code forge/&lt;id&gt;/texture.png} into the pack's {@code vireon}
 *       namespace.</li>
 *   <li>For each base Material, emit a vanilla item model file with overrides
 *       routing each registered CMD value to the matching Vireon model.</li>
 *   <li>Zip everything to {@code vireon-pack.zip} in the plugin data folder and
 *       compute its SHA-1 hash (Minecraft requires this for clients to cache).</li>
 * </ol></p>
 */
public final class ResourcePackBuilder {

    private final VireonWildHarvest plugin;
    private final ModelRegistry modelRegistry;
    private final File forgeFolder;
    private final File packFile;

    private String packHash;

    public ResourcePackBuilder(VireonWildHarvest plugin, ModelRegistry modelRegistry, File forgeFolder) {
        this.plugin        = plugin;
        this.modelRegistry = modelRegistry;
        this.forgeFolder   = forgeFolder;
        this.packFile      = new File(plugin.getDataFolder(), "vireon-pack.zip");
    }

    /** Discover required model ids from currently loaded conversions. */
    public Map<String, DropDefinition> collectItemModels() {
        Map<String, DropDefinition> map = new LinkedHashMap<>();
        for (MobConversion conv : plugin.getMobConversionService().all()) {
            for (DropDefinition drop : conv.getDrops()) {
                map.put(drop.getItemId(), drop);
            }
        }
        return map;
    }

    /**
     * Build {@code vireon-pack.zip} from the current state of the forge folder.
     * Updates {@link #getPackHash()} on success.
     */
    public void build() throws IOException {
        Map<String, DropDefinition> drops = collectItemModels();

        // Ensure every drop has a CMD assigned.
        for (String id : drops.keySet()) modelRegistry.getOrAssign(id);
        modelRegistry.save();

        // Group overrides by base material.
        Map<Material, Map<String, Integer>> overrides = new HashMap<>();
        for (var e : drops.entrySet()) {
            String id = e.getKey();
            Material mat = e.getValue().getMaterial();
            overrides.computeIfAbsent(mat, k -> new LinkedHashMap<>())
                     .put(id, modelRegistry.getOrAssign(id));
        }

        try (FileOutputStream fos = new FileOutputStream(packFile);
             ZipOutputStream zip = new ZipOutputStream(fos)) {

            writeEntry(zip, "pack.mcmeta", buildPackMcmeta());

            // Per-model files.
            for (String id : drops.keySet()) {
                File folder = new File(forgeFolder, id);
                File modelJson  = new File(folder, "model.json");
                File textureFile = new File(folder, "texture.png");
                if (modelJson.exists()) {
                    writeEntry(zip, "assets/vireon/models/" + id + ".json", Files.readAllBytes(modelJson.toPath()));
                }
                if (textureFile.exists()) {
                    writeEntry(zip, "assets/vireon/textures/item/" + id + ".png", Files.readAllBytes(textureFile.toPath()));
                }
            }

            // Vanilla overrides per base material.
            for (var e : overrides.entrySet()) {
                String content = buildItemOverrideJson(e.getKey(), e.getValue());
                String path    = "assets/minecraft/models/item/" + e.getKey().name().toLowerCase(Locale.ROOT) + ".json";
                writeEntry(zip, path, content.getBytes(StandardCharsets.UTF_8));
            }
        }

        this.packHash = sha1Hex(packFile);
        plugin.getLogger().info("Vireon Forge: built " + packFile.getName()
                + " (" + packFile.length() + " bytes, " + drops.size() + " models, hash " + packHash.substring(0, 8) + "…)");
    }

    public File   getPackFile() { return packFile; }
    public String getPackHash() { return packHash; }

    // ─── Pack content generators ──────────────────────────────

    private byte[] buildPackMcmeta() {
        // pack_format 46 = 1.21.4. supported_formats keeps the pack accepted on
        // a wide range of clients without manual tuning.
        String json = "{\n"
                + "  \"pack\": {\n"
                + "    \"pack_format\": 46,\n"
                + "    \"supported_formats\": [34, 999],\n"
                + "    \"description\": \"\\u00a76Vireon Wild Harvest\\u00a77 \\u2014 auto-generated by\\u00a7a ArtsyStudios\"\n"
                + "  }\n"
                + "}\n";
        return json.getBytes(StandardCharsets.UTF_8);
    }

    private String buildItemOverrideJson(Material mat, Map<String, Integer> overrides) {
        String matLower = mat.name().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"parent\": \"minecraft:item/generated\",\n");
        sb.append("  \"textures\": { \"layer0\": \"minecraft:item/").append(matLower).append("\" },\n");
        sb.append("  \"overrides\": [\n");
        boolean first = true;
        for (var e : overrides.entrySet()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("    { \"predicate\": { \"custom_model_data\": ")
              .append(e.getValue())
              .append(" }, \"model\": \"vireon:")
              .append(e.getKey())
              .append("\" }");
        }
        sb.append("\n  ]\n");
        sb.append("}\n");
        return sb.toString();
    }

    // ─── Helpers ──────────────────────────────────────────────

    private void writeEntry(ZipOutputStream zip, String name, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        zip.putNextEntry(entry);
        zip.write(content);
        zip.closeEntry();
    }

    private static String sha1Hex(File file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] buf = new byte[8192];
            try (var in = Files.newInputStream(file.toPath())) {
                int read;
                while ((read = in.read(buf)) != -1) md.update(buf, 0, read);
            }
            byte[] hash = md.digest();
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IOException("SHA-1 unavailable on this JVM", ex);
        }
    }
}
