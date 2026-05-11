package nl.artsystudios.vireon.wildharvest.forge.listener;

import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import nl.artsystudios.vireon.wildharvest.forge.ForgeService;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * Sends the auto-hosted Vireon resource pack to every joining player.
 *
 * <p>If the pack URL or hash are missing (host disabled, public address not
 * configured, build failed) we silently no-op — the rest of the plugin still
 * works, players just don't see custom item models.</p>
 */
public final class PackJoinListener implements Listener {

    private final VireonWildHarvest plugin;

    public PackJoinListener(VireonWildHarvest plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ForgeService forge = plugin.getForgeService();
        if (forge == null || !forge.isHosting()) return;

        String url  = forge.getPublicPackUrl();
        String hash = forge.getPackHash();
        if (url == null || hash == null) return;

        byte[] hashBytes;
        try {
            hashBytes = hexToBytes(hash);
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Invalid pack hash, skipping send: " + ex.getMessage());
            return;
        }

        String prompt = ChatColor.translateAlternateColorCodes('&',
                plugin.getConfigManager().getResourcePackPrompt());
        boolean required = plugin.getConfigManager().isResourcePackRequired();

        try {
            event.getPlayer().setResourcePack(url, hashBytes, prompt, required);
        } catch (Throwable ex) {
            // Older Paper signatures vary; fall back to the simplest one.
            try {
                event.getPlayer().setResourcePack(url, hashBytes);
            } catch (Throwable fatal) {
                plugin.getLogger().warning("Could not send resource pack to "
                        + event.getPlayer().getName() + ": " + fatal.getMessage());
            }
        }
    }

    private static byte[] hexToBytes(String hex) {
        if (hex.length() % 2 != 0) throw new IllegalArgumentException("odd-length hex string");
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("non-hex char in pack hash");
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
