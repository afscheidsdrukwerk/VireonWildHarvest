package nl.artsystudios.vireon.wildharvest.mob;

import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.List;

/**
 * Definition of a mob conversion loaded from {@code config.yml}.
 *
 * <p>A conversion attaches a "wildlife identity" to an existing vanilla
 * entity type. The vanilla entity stays in place (so AI, spawn rules,
 * and pathfinding keep working), but its name, attributes, drops, and
 * burning behaviour are overridden.</p>
 */
public final class MobConversion {

    private final String id;
    private final EntityType vanillaType;
    private final String displayName;
    private final boolean sunImmune;
    private final double health;
    private final double damage;
    private final double speedMultiplier;
    private final boolean suppressVanillaDrops;
    private final List<DropDefinition> drops;

    public MobConversion(String id,
                         EntityType vanillaType,
                         String displayName,
                         boolean sunImmune,
                         double health,
                         double damage,
                         double speedMultiplier,
                         boolean suppressVanillaDrops,
                         List<DropDefinition> drops) {
        this.id = id;
        this.vanillaType = vanillaType;
        this.displayName = displayName;
        this.sunImmune = sunImmune;
        this.health = health;
        this.damage = damage;
        this.speedMultiplier = speedMultiplier;
        this.suppressVanillaDrops = suppressVanillaDrops;
        this.drops = drops != null ? drops : Collections.emptyList();
    }

    public String getId()                   { return id; }
    public EntityType getVanillaType()      { return vanillaType; }
    public String getDisplayName()          { return displayName; }
    public boolean isSunImmune()            { return sunImmune; }
    public double getHealth()               { return health; }
    public double getDamage()               { return damage; }
    public double getSpeedMultiplier()      { return speedMultiplier; }
    public boolean isSuppressVanillaDrops() { return suppressVanillaDrops; }
    public List<DropDefinition> getDrops()  { return drops; }
}
