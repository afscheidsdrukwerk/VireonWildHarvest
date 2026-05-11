package nl.artsystudios.vireon.wildharvest.mob;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Core service for the Vireon Wild Harvest mob layer.
 *
 * <p>Owns the registered set of {@link MobConversion}s and is responsible
 * for tagging entities, applying attributes, and rolling drops. Listeners
 * delegate every actual mutation to this class.</p>
 */
public final class MobConversionService {

    private final VireonWildHarvest plugin;

    /** PDC key stored on a converted living entity — value is the conversion id. */
    private final NamespacedKey convertedKey;
    /** PDC key stored on a Vireon-tagged item — value is the drop's stable id. */
    private final NamespacedKey itemKey;

    private final Map<EntityType, MobConversion> byVanilla = new EnumMap<>(EntityType.class);
    private final Map<String, MobConversion>     byId      = new HashMap<>();

    private boolean autoConvertNatural = true;

    public MobConversionService(VireonWildHarvest plugin) {
        this.plugin       = plugin;
        this.convertedKey = new NamespacedKey(plugin, "converted_mob_id");
        this.itemKey      = new NamespacedKey(plugin, "item_id");
    }

    // ─── Loading ───────────────────────────────────────────────

    public void loadFromConfig() {
        byVanilla.clear();
        byId.clear();

        this.autoConvertNatural = plugin.getConfig().getBoolean("mobs.auto-convert-natural", true);

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("mobs.conversions");
        if (section == null) {
            plugin.getLogger().info("No mob conversions configured.");
            return;
        }

        for (String key : section.getKeys(false)) {
            try {
                EntityType vanilla = EntityType.valueOf(key.toUpperCase(Locale.ROOT));
                ConfigurationSection cs = section.getConfigurationSection(key);
                if (cs == null) continue;

                String id           = cs.getString("id", key.toLowerCase(Locale.ROOT));
                String displayName  = cs.getString("display-name", id);
                boolean sunImmune   = cs.getBoolean("sun-immune", false);
                double health       = cs.getDouble("health", 20.0);
                double damage       = cs.getDouble("damage", 3.0);
                double speed        = cs.getDouble("speed-multiplier", 1.0);
                boolean suppress    = cs.getBoolean("suppress-vanilla-drops", true);

                List<DropDefinition> drops = new ArrayList<>();
                for (Map<?, ?> drop : cs.getMapList("drops")) {
                    try {
                        Material mat = Material.valueOf(String.valueOf(drop.get("item")).toUpperCase(Locale.ROOT));
                        String dropId      = drop.containsKey("id") ? String.valueOf(drop.get("id")) : (id + "_" + mat.name().toLowerCase(Locale.ROOT));
                        String dropName    = drop.containsKey("name") ? String.valueOf(drop.get("name")) : null;
                        List<String> lore  = new ArrayList<>();
                        Object rawLore     = drop.get("lore");
                        if (rawLore instanceof List<?> list) {
                            for (Object line : list) lore.add(String.valueOf(line));
                        }
                        int min      = readInt(drop.get("min"), 1);
                        int max      = readInt(drop.get("max"), min);
                        double chance = readDouble(drop.get("chance"), 1.0);

                        drops.add(new DropDefinition(mat, dropId, dropName, lore, min, max, chance));
                    } catch (Exception ex) {
                        plugin.getLogger().warning("Skipping invalid drop in conversion '" + key + "': " + ex.getMessage());
                    }
                }

                MobConversion conv = new MobConversion(id, vanilla, displayName, sunImmune,
                        health, damage, speed, suppress, drops);
                byVanilla.put(vanilla, conv);
                byId.put(id.toLowerCase(Locale.ROOT), conv);

                plugin.getLogger().info("Loaded conversion: " + vanilla + " → " + id
                        + " (" + drops.size() + " drops, sunImmune=" + sunImmune + ")");
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Unknown entity type in mobs.conversions: " + key);
            }
        }
    }

    // ─── Public API ────────────────────────────────────────────

    public boolean isAutoConvertNatural() { return autoConvertNatural; }
    public NamespacedKey getConvertedKey() { return convertedKey; }
    public NamespacedKey getItemKey()      { return itemKey; }

    public Optional<MobConversion> forVanilla(EntityType type) {
        return Optional.ofNullable(byVanilla.get(type));
    }

    public Optional<MobConversion> byId(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(byId.get(id.toLowerCase(Locale.ROOT)));
    }

    public Collection<MobConversion> all() {
        return Collections.unmodifiableCollection(byId.values());
    }

    public boolean isConverted(Entity entity) {
        return entity.getPersistentDataContainer().has(convertedKey, PersistentDataType.STRING);
    }

    public Optional<MobConversion> getConversionOf(Entity entity) {
        String id = entity.getPersistentDataContainer().get(convertedKey, PersistentDataType.STRING);
        return byId(id);
    }

    /**
     * Apply a conversion to an existing entity. Tags it via PDC, renames it,
     * boosts its attributes, and applies type-specific tweaks.
     */
    public void applyConversion(LivingEntity entity, MobConversion conv) {
        PersistentDataContainer pdc = entity.getPersistentDataContainer();
        pdc.set(convertedKey, PersistentDataType.STRING, conv.getId());

        // Display name (translated from legacy & codes).
        String legacy = ChatColor.translateAlternateColorCodes('&', conv.getDisplayName());
        entity.customName(LegacyComponentSerializer.legacySection().deserialize(legacy)
                .decoration(TextDecoration.ITALIC, false));
        entity.setCustomNameVisible(true);

        applyAttributes(entity, conv);

        if (entity instanceof Zombie zombie) {
            zombie.setAdult();
            zombie.setShouldBurnInDay(!conv.isSunImmune());
            AttributeInstance reinforce = entity.getAttribute(Attribute.ZOMBIE_SPAWN_REINFORCEMENTS);
            if (reinforce != null) reinforce.setBaseValue(0.0);
        }
    }

    private void applyAttributes(LivingEntity entity, MobConversion conv) {
        AttributeInstance maxHealth = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(conv.getHealth());
            entity.setHealth(Math.min(conv.getHealth(), maxHealth.getValue()));
        }
        AttributeInstance attack = entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE);
        if (attack != null) attack.setBaseValue(conv.getDamage());

        AttributeInstance speed = entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(speed.getBaseValue() * conv.getSpeedMultiplier());
    }

    /**
     * Spawn a converted mob at the given location. The world spawn fires a
     * CreatureSpawnEvent with reason PLUGIN — our listener intentionally
     * skips PLUGIN spawns so this method controls conversion explicitly.
     */
    public LivingEntity spawnConverted(Location loc, MobConversion conv) {
        Entity raw = loc.getWorld().spawnEntity(loc, conv.getVanillaType());
        if (!(raw instanceof LivingEntity living)) {
            raw.remove();
            throw new IllegalStateException("Vanilla type " + conv.getVanillaType()
                    + " did not spawn as a LivingEntity");
        }
        applyConversion(living, conv);
        return living;
    }

    // ─── Drops ─────────────────────────────────────────────────

    public ItemStack buildDropItem(DropDefinition drop) {
        ItemStack stack = new ItemStack(drop.getMaterial());
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            if (drop.getDisplayName() != null) {
                String coloured = ChatColor.translateAlternateColorCodes('&', drop.getDisplayName());
                meta.displayName(LegacyComponentSerializer.legacySection().deserialize(coloured)
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (!drop.getLore().isEmpty()) {
                List<Component> lore = new ArrayList<>(drop.getLore().size());
                for (String line : drop.getLore()) {
                    String coloured = ChatColor.translateAlternateColorCodes('&', line);
                    lore.add(LegacyComponentSerializer.legacySection().deserialize(coloured)
                            .decoration(TextDecoration.ITALIC, false));
                }
                meta.lore(lore);
            }
            meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, drop.getItemId());
            stack.setItemMeta(meta);
        }
        return stack;
    }

    /** Roll a drop's chance + count. Returns 0 if the roll fails. */
    public int rollDropAmount(DropDefinition drop) {
        if (drop.getChance() < 1.0 && ThreadLocalRandom.current().nextDouble() > drop.getChance()) return 0;
        if (drop.getMax() <= drop.getMin()) return drop.getMin();
        return ThreadLocalRandom.current().nextInt(drop.getMin(), drop.getMax() + 1);
    }

    // ─── Helpers ───────────────────────────────────────────────

    private static int readInt(Object raw, int fallback) {
        if (raw instanceof Number n) return n.intValue();
        if (raw instanceof String s) try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
        return fallback;
    }

    private static double readDouble(Object raw, double fallback) {
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) {}
        return fallback;
    }
}
