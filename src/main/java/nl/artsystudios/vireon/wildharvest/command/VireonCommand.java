package nl.artsystudios.vireon.wildharvest.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import nl.artsystudios.vireon.wildharvest.mob.MobConversion;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Root /vireon command. Subcommands: version, reload, list, spawn, help.
 */
public final class VireonCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("version", "reload", "list", "spawn", "forge", "help");
    private static final List<String> FORGE_SUBCOMMANDS = Arrays.asList("status", "reload");

    private final VireonWildHarvest plugin;

    public VireonCommand(VireonWildHarvest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "version" -> sendVersion(sender);
            case "reload"  -> handleReload(sender);
            case "list"    -> handleList(sender);
            case "spawn"   -> handleSpawn(sender, args);
            case "forge"   -> handleForge(sender, args);
            case "help"    -> sendHelp(sender);
            default        -> sender.sendMessage(prefix()
                    .append(Component.text("Unknown subcommand. Try /vireon help", NamedTextColor.RED)));
        }
        return true;
    }

    private void handleForge(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vireon.admin")) {
            sender.sendMessage(prefix().append(Component.text("You do not have permission.", NamedTextColor.RED)));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(prefix().append(Component.text("Usage: /vireon forge <status|reload>", NamedTextColor.YELLOW)));
            return;
        }
        var forge = plugin.getForgeService();
        if (forge == null) {
            sender.sendMessage(prefix().append(Component.text("Forge service is not available.", NamedTextColor.RED)));
            return;
        }
        switch (args[1].toLowerCase(Locale.ROOT)) {
            case "status" -> {
                sender.sendMessage(Component.empty());
                sender.sendMessage(Component.text(" Vireon Forge status", NamedTextColor.GREEN, TextDecoration.BOLD));
                sender.sendMessage(Component.text("   hosting: ", NamedTextColor.GRAY)
                        .append(Component.text(forge.isHosting() ? "ON" : "OFF",
                                forge.isHosting() ? NamedTextColor.GREEN : NamedTextColor.RED)));
                sender.sendMessage(Component.text("   pack:    ", NamedTextColor.GRAY)
                        .append(Component.text(forge.getPackFile().getName(), NamedTextColor.AQUA))
                        .append(Component.text(" (" + forge.getPackFile().length() + " bytes)", NamedTextColor.DARK_GRAY)));
                String url = forge.getPublicPackUrl();
                sender.sendMessage(Component.text("   url:     ", NamedTextColor.GRAY)
                        .append(Component.text(url != null ? url : "(public-address not configured)",
                                url != null ? NamedTextColor.AQUA : NamedTextColor.YELLOW)));
                String hash = forge.getPackHash();
                sender.sendMessage(Component.text("   sha1:    ", NamedTextColor.GRAY)
                        .append(Component.text(hash != null ? hash.substring(0, Math.min(16, hash.length())) + "…" : "n/a", NamedTextColor.DARK_GRAY)));
                sender.sendMessage(Component.text("   models:  ", NamedTextColor.GRAY)
                        .append(Component.text(String.valueOf(forge.getModelRegistry().snapshot().size()), NamedTextColor.AQUA))
                        .append(Component.text(" registered", NamedTextColor.GRAY)));
                sender.sendMessage(Component.empty());
            }
            case "reload" -> {
                long t0 = System.currentTimeMillis();
                forge.reload();
                long dt = System.currentTimeMillis() - t0;
                sender.sendMessage(prefix().append(Component.text(
                        "Vireon Forge rebuilt in " + dt + "ms.", NamedTextColor.GREEN)));
                String url = forge.getPublicPackUrl();
                if (url != null) {
                    sender.sendMessage(prefix().append(Component.text(
                            "Players will receive the new pack on next join.", NamedTextColor.GRAY)));
                }
            }
            default -> sender.sendMessage(prefix().append(Component.text(
                    "Unknown forge subcommand. Try status or reload.", NamedTextColor.RED)));
        }
    }

    // ─── Subcommands ───────────────────────────────────────────

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("vireon.admin")) {
            sender.sendMessage(prefix().append(Component.text("You do not have permission.", NamedTextColor.RED)));
            return;
        }
        long start = System.currentTimeMillis();
        plugin.reloadAll();
        long elapsed = System.currentTimeMillis() - start;
        sender.sendMessage(prefix().append(Component.text("Configuration reloaded in " + elapsed + "ms.", NamedTextColor.GREEN)));
        sender.sendMessage(prefix().append(Component.text(
                "Active conversions: " + plugin.getMobConversionService().all().size(), NamedTextColor.GRAY)));
    }

    private void handleList(CommandSender sender) {
        if (!sender.hasPermission("vireon.admin")) {
            sender.sendMessage(prefix().append(Component.text("You do not have permission.", NamedTextColor.RED)));
            return;
        }
        var all = plugin.getMobConversionService().all();
        if (all.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("No conversions registered.", NamedTextColor.GRAY)));
            return;
        }
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text(" Registered Vireon mobs:", NamedTextColor.GREEN, TextDecoration.BOLD));
        for (MobConversion c : all) {
            sender.sendMessage(Component.text("  • ", NamedTextColor.DARK_GRAY)
                    .append(Component.text(c.getId(), NamedTextColor.AQUA))
                    .append(Component.text("  (from " + c.getVanillaType() + ", "
                            + c.getDrops().size() + " drops, sunImmune=" + c.isSunImmune() + ")", NamedTextColor.GRAY)));
        }
        sender.sendMessage(Component.empty());
    }

    private void handleSpawn(CommandSender sender, String[] args) {
        if (!sender.hasPermission("vireon.admin")) {
            sender.sendMessage(prefix().append(Component.text("You do not have permission.", NamedTextColor.RED)));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(prefix().append(Component.text("This subcommand must be run by a player.", NamedTextColor.RED)));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(prefix().append(Component.text("Usage: /vireon spawn <id> [count]", NamedTextColor.YELLOW)));
            return;
        }

        String id = args[1];
        Optional<MobConversion> opt = plugin.getMobConversionService().byId(id);
        if (opt.isEmpty()) {
            sender.sendMessage(prefix().append(Component.text("No conversion with id '" + id + "'. Try /vireon list.", NamedTextColor.RED)));
            return;
        }

        int count = 1;
        if (args.length >= 3) {
            try { count = Math.max(1, Math.min(50, Integer.parseInt(args[2]))); }
            catch (NumberFormatException ex) {
                sender.sendMessage(prefix().append(Component.text("Count must be a number 1–50.", NamedTextColor.RED)));
                return;
            }
        }

        Block target = player.getTargetBlockExact(40);
        Location spawnLoc = target != null
                ? target.getLocation().add(0.5, 1, 0.5)
                : player.getLocation();

        for (int i = 0; i < count; i++) {
            LivingEntity le = plugin.getMobConversionService().spawnConverted(spawnLoc, opt.get());
            // Slight offset so they don't all stack on one tile.
            if (count > 1 && le != null) {
                le.teleport(spawnLoc.clone().add(
                        (Math.random() - 0.5) * 2,
                        0,
                        (Math.random() - 0.5) * 2));
            }
        }

        sender.sendMessage(prefix()
                .append(Component.text("Spawned ", NamedTextColor.GREEN))
                .append(Component.text(count + "× ", NamedTextColor.AQUA))
                .append(Component.text(opt.get().getId(), NamedTextColor.GOLD))
                .append(Component.text(" at your target.", NamedTextColor.GREEN)));
    }

    private void sendVersion(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text(
                "Vireon Wild Harvest v" + plugin.getDescription().getVersion(), NamedTextColor.AQUA)));
        sender.sendMessage(prefix().append(Component.text(
                "Part of the Vireon plugin series by ArtsyStudios.", NamedTextColor.GRAY)));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text(" Vireon Wild Harvest", NamedTextColor.GREEN, TextDecoration.BOLD));
        sender.sendMessage(line("/vireon version",          "show plugin version"));
        sender.sendMessage(line("/vireon reload",           "reload configuration"));
        sender.sendMessage(line("/vireon list",             "list registered conversions"));
        sender.sendMessage(line("/vireon spawn <id> [n]",   "spawn a converted mob at your target"));
        sender.sendMessage(line("/vireon forge status",     "show resource pack host status"));
        sender.sendMessage(line("/vireon forge reload",     "rebuild + re-host the resource pack"));
        sender.sendMessage(line("/vireon help",             "show this help"));
        sender.sendMessage(Component.empty());
    }

    private Component line(String cmd, String desc) {
        return Component.text("   ", NamedTextColor.DARK_GRAY)
                .append(Component.text(cmd, NamedTextColor.AQUA))
                .append(Component.text("  — " + desc, NamedTextColor.GRAY));
    }

    private Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("Vireon", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

    // ─── Tab completion ────────────────────────────────────────

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String sub : SUBCOMMANDS) {
                if (sub.startsWith(prefix)) out.add(sub);
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("spawn")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (MobConversion c : plugin.getMobConversionService().all()) {
                if (c.getId().startsWith(prefix)) out.add(c.getId());
            }
            return out;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("forge")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            List<String> out = new ArrayList<>();
            for (String sub : FORGE_SUBCOMMANDS) {
                if (sub.startsWith(prefix)) out.add(sub);
            }
            return out;
        }
        return Collections.emptyList();
    }
}
