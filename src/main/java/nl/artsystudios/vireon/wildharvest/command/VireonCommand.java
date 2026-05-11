package nl.artsystudios.vireon.wildharvest.command;

import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;
import org.bukkit.ChatColor;
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
 * Uses Bukkit's classic ChatColor so the plugin works on Spigot, Paper, and
 * any CraftBukkit fork without depending on Adventure being on the classpath.
 */
public class VireonCommand implements CommandExecutor, TabCompleter {

    private static final String PREFIX =
            ChatColor.DARK_GRAY + "[" + ChatColor.GREEN.toString() + ChatColor.BOLD + "Vireon" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET;

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
            default        -> sender.sendMessage(PREFIX + ChatColor.RED + "Unknown subcommand. Try /vireon help");
        }
        return true;
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("vireon.admin")) {
            sender.sendMessage(PREFIX + ChatColor.RED + "You do not have permission.");
            return;
        }
        long start = System.currentTimeMillis();
        plugin.reloadConfig();
        plugin.getConfigManager().reload();
        long elapsed = System.currentTimeMillis() - start;
        sender.sendMessage(PREFIX + ChatColor.GREEN + "Configuration reloaded in " + elapsed + "ms.");
    }

    private void sendVersion(CommandSender sender) {
        sender.sendMessage(PREFIX + ChatColor.AQUA + "Vireon Wild Harvest v" + plugin.getDescription().getVersion());
        sender.sendMessage(PREFIX + ChatColor.GRAY + "Part of the Vireon plugin series by ArtsyStudios.");
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage("");
        sender.sendMessage(" " + ChatColor.GREEN + ChatColor.BOLD + "Vireon Wild Harvest");
        sender.sendMessage("   " + ChatColor.AQUA + "/vireon version " + ChatColor.GRAY + "- show plugin version");
        sender.sendMessage("   " + ChatColor.AQUA + "/vireon reload  " + ChatColor.GRAY + "- reload configuration");
        sender.sendMessage("   " + ChatColor.AQUA + "/vireon help    " + ChatColor.GRAY + "- show this help");
        sender.sendMessage("");
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
