package nl.artsystudios.vireon.wildharvest.mob;

import org.bukkit.Material;

import java.util.Collections;
import java.util.List;

/**
 * One drop entry under a {@link MobConversion}.
 * Holds the item material, custom name (with {@code &}-codes), lore lines,
 * count range, drop chance, and a stable Vireon item id used in PDC tagging.
 */
public final class DropDefinition {

    private final Material material;
    private final String itemId;
    private final String displayName;
    private final List<String> lore;
    private final int min;
    private final int max;
    private final double chance;

    public DropDefinition(Material material, String itemId, String displayName,
                          List<String> lore, int min, int max, double chance) {
        this.material = material;
        this.itemId = itemId;
        this.displayName = displayName;
        this.lore = lore != null ? lore : Collections.emptyList();
        this.min = Math.max(0, min);
        this.max = Math.max(this.min, max);
        this.chance = Math.max(0.0, Math.min(1.0, chance));
    }

    public Material getMaterial()    { return material; }
    public String getItemId()        { return itemId; }
    public String getDisplayName()   { return displayName; }
    public List<String> getLore()    { return lore; }
    public int getMin()              { return min; }
    public int getMax()              { return max; }
    public double getChance()        { return chance; }
}
