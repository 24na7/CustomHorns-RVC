package org.okunev.customhorns.command.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.command.AbstractSubCommand;
import org.okunev.customhorns.command.CustomHornsCommand;

import java.util.List;

public class HelpSubCommand extends AbstractSubCommand {
    private final CustomHorns plugin = CustomHorns.getInstance();
    private final CustomHornsCommand chCommand;

    public HelpSubCommand(CustomHornsCommand chCommand) {
        super("help");
        this.chCommand = chCommand;
        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());
        this.executes(this::execute);
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.help.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.help.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customhorns.help");
    }

    @Override
    public void execute(CommandSender sender, CommandArguments arguments) {
        if (!hasPermission(sender)) {
            CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
            return;
        }

        CustomHorns.sendMessage(sender, plugin.getLanguage().component("command.help.messages.header"));
        printHelp(sender, chCommand.getSubcommands());
//        CustomHorns.sendMessage(sender, plugin.getLanguage().component("command.help.messages.footer"));
    }

    private void printHelp(CommandSender sender, List<CommandAPICommand> commands) {
        for (CommandAPICommand caSubCommand : commands) {
            if (!(caSubCommand instanceof AbstractSubCommand subCommand)) continue;

            if (subCommand.hasPermission(sender)) {
                if (!subCommand.getSubcommands().isEmpty()) {
                    printHelp(sender, subCommand.getSubcommands());
                } else {
                    CustomHorns.sendMessage(sender, plugin.getLanguage().component("command.help.messages.format",
                            subCommand.getSyntax(), subCommand.getDescription()));
                }
            }
        }
    }
}