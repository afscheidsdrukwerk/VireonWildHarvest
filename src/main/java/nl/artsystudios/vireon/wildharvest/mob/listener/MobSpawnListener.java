package nl.artsystudios.vireon.wildharvest.mob.listener;

import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import nl.artsystudios.vireon.wildharvest.mob.MobConversion;
import nl.artsystudios.vireon.wildharvest.mob.MobConversionService;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import java.util.Optional;

/**
 * Auto-converts naturally-spawned mobs. We skip PLUGIN / COMMAND / CUSTOM
 * spawn reasons so other plugins (and our own /vireon spawn) keep control.
 */
public final class MobSpawnListener implements Listener {

    private final VireonWildHarvest plugin;
    private final MobConversionService service;

    public MobSpawnListener(VireonWildHarvest plugin, MobConversionService service) {
        this.plugin  = plugin;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!service.isAutoConvertNatural()) return;

        SpawnReason reason = event.getSpawnReason();
        if (reason == SpawnReason.CUSTOM || reason == SpawnReason.COMMAND
                || reason == SpawnReason.DEFAULT) {
            return;
        }

        LivingEntity entity = event.getEntity();
        if (service.isConverted(entity)) return;

        Optional<MobConversion> conv = service.forVanilla(entity.getType());
        if (conv.isEmpty()) return;

        service.applyConversion(entity, conv.get());

        if (plugin.getConfigManager().isDebug()) {
            plugin.getLogger().info("[debug] Converted "
                    + entity.getType() + " → " + conv.get().getId()
                    + " at " + entity.getLocation().toVector()
                    + " (reason=" + reason + ")");
        }
    }
}
