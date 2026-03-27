package org.okunev.customhorns.command.subcommand;

import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.Keys;
import org.okunev.customhorns.command.AbstractSubCommand;
import org.okunev.customhorns.util.HornUtils;
import org.okunev.customhorns.util.RemoteServices;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class RemoteCreateSubCommand extends AbstractSubCommand {
    private final CustomHorns plugin = CustomHorns.getInstance();

    public RemoteCreateSubCommand() {
        super("remote");

        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());

        this.withArguments(new TextArgument("url")
                .replaceSuggestions(quotedArgument(plugin.getChConfig().getRemoteTabComplete())));
        this.withArguments(new TextArgument("song_name")
                .replaceSuggestions(quotedArgument(null)));
        this.executesPlayer(this::executePlayer);
        this.executes(this::execute);
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.create.remote.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.create.remote.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customhorns.create.remote");
    }

    @Override
    public boolean hasPermission(CommandSender sender, RemoteServices service) {
        if (service == null) return hasPermission(sender);
        return sender.hasPermission("customhorns.create.remote." + service.getId());
    }

    @Override
    public void executePlayer(Player player, CommandArguments arguments) {
        String url = getArgumentValue(arguments, "url", String.class);
        RemoteServices service = RemoteServices.fromUrl(url);

        if (service == null || !hasPermission(player, service)) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
            return;
        }

        if (!isHornInHand(player)) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.error.not-holding-horn"));
            return;
        }

        String customName = getArgumentValue(arguments, "song_name", String.class);

        if (customName.isEmpty()) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.command.horn-name-empty"));
            return;
        }

        CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.checking-duration"));

        CompletableFuture<Long> durationFuture = HornUtils.getAudioDuration(url);

        plugin.getSchedulers().async.runNow(task -> {
            try {
                long duration = durationFuture.get(30, TimeUnit.SECONDS);

                if (duration > plugin.getChConfig().getMaxHornDuration() * 1000) {
                    CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.horn.too-long",
                            String.valueOf(plugin.getChConfig().getMaxHornDuration())));
                    return;
                }

                if (duration <= 0) {
                    CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.duration-check-failed"));
                    return;
                }

                plugin.getSchedulers().global.runNow(globalTask -> {
                    createRemoteHorn(player, url, customName, duration, service);
                });

            } catch (Exception e) {
                CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.duration-check-failed"));
                CustomHorns.error("Failed to check remote audio duration", e);
            }
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private void createRemoteHorn(Player player, String url, String customName, long duration, RemoteServices service) {
        ItemStack horn = new ItemStack(player.getInventory().getItemInMainHand());

        ItemMeta meta = horn.getItemMeta();

        meta.displayName(plugin.getLanguage().component("horn-name." + service.getId())
                .decoration(TextDecoration.ITALIC, false));

        final Component customLoreSong = Component.text(customName)
                .decoration(TextDecoration.ITALIC, false)
                .color(NamedTextColor.GRAY);

        meta.addItemFlags(ItemFlag.values());
        meta.lore(List.of(customLoreSong));

        int modelData = plugin.getChConfig().getLocalCustomModelData();
        if (modelData != 0) {
            horn.setData(DataComponentTypes.CUSTOM_MODEL_DATA,
                    CustomModelData.customModelData(modelData));
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        for (NamespacedKey key : data.getKeys()) {
            data.remove(key);
        }

        data.set(Keys.REMOTE_HORN.key(), Keys.REMOTE_HORN.dataType(), url);
        data.set(Keys.HORN_DURATION.key(), Keys.HORN_DURATION.dataType(), duration);
        data.set(Keys.HORN_NAME.key(), Keys.HORN_NAME.dataType(), customName);

        horn.setItemMeta(meta);
        player.getInventory().setItemInMainHand(horn);

        CustomHorns.sendMessage(player, plugin.getLanguage().component("command.create.messages.link", url));
        CustomHorns.sendMessage(player, plugin.getLanguage().component("command.create.messages.name", customName));
        CustomHorns.sendMessage(player, plugin.getLanguage().component("command.create.messages.duration",
                String.format("%.1f", duration / 1000.0)));
    }

    private boolean isHornInHand(Player player) {
        return player.getInventory().getItemInMainHand().getType() == Material.GOAT_HORN;
    }

    @Override
    public void execute(CommandSender sender, CommandArguments arguments) {
        CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
    }
}