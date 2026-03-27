package org.okunev.customhorns.command.subcommand;

import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.command.AbstractSubCommand;

public class DistanceSubCommand extends AbstractSubCommand {
    private final CustomHorns plugin = CustomHorns.getInstance();

    public DistanceSubCommand() {
        super("distance");

        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());

        this.withArguments(new IntegerArgument("radius", 0,
                plugin.getChConfig().getDistanceCommandMaxDistance()));

        this.executesPlayer(this::executePlayer);
        this.executes(this::execute);
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.distance.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.distance.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customhorns.distance");
    }

    @Override
    public void executePlayer(Player player, CommandArguments arguments) {
        if (!hasPermission(player)) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
            return;
        }

        int radius = getArgumentValue(arguments, "radius", Integer.class);
        plugin.getChData().setPlayerHornDistance(player.getUniqueId(), radius);

        CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("command.distance.messages.success",
                String.valueOf(radius)));
    }

    @Override
    public void execute(CommandSender sender, CommandArguments arguments) {
        CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
    }
}