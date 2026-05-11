package nl.artsystudios.vireon.wildharvest.mob.listener;

import nl.artsystudios.vireon.wildharvest.mob.MobConversion;
import nl.artsystudios.vireon.wildharvest.mob.MobConversionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustByBlockEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityCombustEvent;

import java.util.Optional;

/**
 * Cancels ambient combustion (sun-burn) for converted mobs that are flagged
 * sun-immune. Lava and fire-arrow combust events still go through because
 * we ignore those subclasses — bears still die in lava.
 */
public final class MobCombustListener implements Listener {

    private final MobConversionService service;

    public MobCombustListener(MobConversionService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCombust(EntityCombustEvent event) {
        // Skip block- and entity-caused combust (lava / fire arrows / etc.).
        if (event instanceof EntityCombustByBlockEvent)  return;
        if (event instanceof EntityCombustByEntityEvent) return;

        Optional<MobConversion> conv = service.getConversionOf(event.getEntity());
        if (conv.isPresent() && conv.get().isSunImmune()) {
            event.setCancelled(true);
        }
    }
}
