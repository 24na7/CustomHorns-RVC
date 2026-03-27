package org.okunev.customhorns.command.subcommand;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.okunev.customhorns.CustomHorns;

import java.util.ArrayList;
import java.util.List;

public class FallbackCustomHornsCommand extends Command implements CommandExecutor, TabExecutor {
    private final CustomHorns plugin = CustomHorns.getInstance();

    public FallbackCustomHornsCommand() {
        super("customhorns");
        this.setAliases(List.of("ch", "horns"));
        this.setDescription("Main command of CustomHorns plugin");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String commandLabel, @NotNull String[] args) {
        return onCommand(sender, this, commandLabel, args);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("customhorns.reload")) {
                    plugin.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
                    return true;
                }
                plugin.getChConfig().load();
                plugin.getLanguage().load();
                plugin.sendMessage(sender, plugin.getLanguage().PComponent("command.reload.messages.successfully"));
                break;

            case "help":
                sendHelp(sender);
                break;

            default:
                plugin.sendMessage(sender, plugin.getLanguage().PComponent("error.command.unknown"));
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        plugin.sendMessage(sender, plugin.getLanguage().component("command.help.messages.header"));
        plugin.sendMessage(sender, plugin.getLanguage().component("command.help.messages.format",
                "/customhorns reload", plugin.getLanguage().string("command.reload.description")));
//        plugin.sendMessage(sender, plugin.getLanguage().component("command.help.messages.footer"));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            if (sender.hasPermission("customhorns.reload")) {
                completions.add("reload");
            }
            completions.add("help");
        }

        return completions;
    }
}