package org.okunev.customhorns.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.okunev.customhorns.command.subcommand.*;

public class CustomHornsCommand extends CommandAPICommand {
    public CustomHornsCommand() {
        super("customhorns");

        this.withAliases("ch", "horns");
        this.withFullDescription("Main command of CustomHorns plugin.");

        this.withSubcommand(new HelpSubCommand(this));
        this.withSubcommand(new ReloadSubCommand());
        this.withSubcommand(new DownloadSubCommand());
        this.withSubcommand(new CreateSubCommand());
        this.withSubcommand(new DistanceSubCommand());

        this.executes(this::execute);
    }

    public void execute(CommandSender sender, CommandArguments arguments) {
        findHelpCommand().execute(sender, arguments);
    }

    @NotNull
    private AbstractSubCommand findHelpCommand() {
        AbstractSubCommand subCommand = null;

        for (CommandAPICommand caSubCommand : getSubcommands()) {
            if (caSubCommand.getName().equals("help")) {
                subCommand = (AbstractSubCommand) caSubCommand;
                break;
            }
        }

        if (subCommand == null)
            throw new IllegalStateException("Command help doesn't exists");

        return subCommand;
    }
}