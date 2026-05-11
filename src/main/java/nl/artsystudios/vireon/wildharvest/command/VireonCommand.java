package nl.artsystudios.vireon.wildharvest.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Root /vireon command and tab completer.
 * Subcommand surface intentionally minimal in 0.1.0 — just enough to prove
 * the foundation is wired up correctly.
 */
public class VireonCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBCOMMANDS = Arrays.asList("version", "reload", "help");

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
            case "help"    -> sendHelp(sender);
            default        -> sender.sendMessage(prefix()
                    .append(Component.text("Unknown subcommand. Try /vireon help", NamedTextColor.RED)));
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("vireon.admin")) {
            sender.sendMessage(prefix().append(Component.text("You do not have permission.", NamedTextColor.RED)));
            return;
        }
        long start = System.currentTimeMillis();
        plugin.reloadConfig();
        plugin.getConfigManager().reload();
        long elapsed = System.currentTimeMillis() - start;
        sender.sendMessage(prefix().append(Component.text("Configuration reloaded in " + elapsed + "ms.", NamedTextColor.GREEN)));
    }

    private void sendVersion(CommandSender sender) {
        sender.sendMessage(prefix().append(Component.text(
                "Vireon Wild Harvest v" + plugin.getPluginMeta().getVersion(), NamedTextColor.AQUA)));
        sender.sendMessage(prefix().append(Component.text(
                "Part of the Vireon plugin series by ArtsyStudios.", NamedTextColor.GRAY)));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(Component.empty());
        sender.sendMessage(Component.text(" Vireon Wild Harvest", NamedTextColor.GREEN, TextDecoration.BOLD));
        sender.sendMessage(Component.text("   /vireon version", NamedTextColor.AQUA)
                .append(Component.text(" — show plugin version", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("   /vireon reload", NamedTextColor.AQUA)
                .append(Component.text(" — reload configuration", NamedTextColor.GRAY)));
        sender.sendMessage(Component.text("   /vireon help", NamedTextColor.AQUA)
                .append(Component.text(" — show this help", NamedTextColor.GRAY)));
        sender.sendMessage(Component.empty());
    }

    private Component prefix() {
        return Component.text("[", NamedTextColor.DARK_GRAY)
                .append(Component.text("Vireon", NamedTextColor.GREEN, TextDecoration.BOLD))
                .append(Component.text("] ", NamedTextColor.DARK_GRAY));
    }

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
        return Collections.emptyList();
    }
}
