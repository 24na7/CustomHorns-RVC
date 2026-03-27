package org.okunev.customhorns.command.subcommand;

import org.bukkit.command.CommandSender;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.command.AbstractSubCommand;

public class CreateSubCommand extends AbstractSubCommand {
    private final CustomHorns plugin = CustomHorns.getInstance();

    public CreateSubCommand() {
        super("create");

        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());
        this.withSubcommand(new LocalCreateSubCommand());
        this.withSubcommand(new RemoteCreateSubCommand());
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.create.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.create.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customhorns.create");
    }
}