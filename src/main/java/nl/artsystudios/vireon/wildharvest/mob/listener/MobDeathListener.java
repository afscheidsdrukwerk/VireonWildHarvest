package nl.artsystudios.vireon.wildharvest.mob.listener;

import nl.artsystudios.vireon.wildharvest.mob.DropDefinition;
import nl.artsystudios.vireon.wildharvest.mob.MobConversion;
import nl.artsystudios.vireon.wildharvest.mob.MobConversionService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

/**
 * Replaces vanilla drops on converted mobs with the config-defined drops.
 * Vanilla XP and equipment are left alone; only the item drop list is touched.
 */
public final class MobDeathListener implements Listener {

    private final MobConversionService service;

    public MobDeathListener(MobConversionService service) {
        this.service = service;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent event) {
        Optional<MobConversion> conv = service.getConversionOf(event.getEntity());
        if (conv.isEmpty()) return;

        MobConversion c = conv.get();
        if (c.isSuppressVanillaDrops()) {
            event.getDrops().clear();
        }

        for (DropDefinition drop : c.getDrops()) {
            int count = service.rollDropAmount(drop);
            if (count <= 0) continue;
            ItemStack stack = service.buildDropItem(drop);
            stack.setAmount(count);
            event.getDrops().add(stack);
        }
    }
}
