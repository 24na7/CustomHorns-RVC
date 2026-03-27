package org.okunev.customhorns.command.subcommand;

import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.command.AbstractSubCommand;

public class ReloadSubCommand extends AbstractSubCommand {
    private final CustomHorns plugin = CustomHorns.getInstance();

    public ReloadSubCommand() {
        super("reload");

        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());

        this.executes(this::execute);
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.reload.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.reload.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customhorns.reload");
    }

    @Override
    public void execute(CommandSender sender, CommandArguments arguments) {
        if (!hasPermission(sender)) {
            CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
            return;
        }

        plugin.getChConfig().load();
        plugin.getLanguage().load();
        CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("command.reload.messages.successfully"));
    }
}