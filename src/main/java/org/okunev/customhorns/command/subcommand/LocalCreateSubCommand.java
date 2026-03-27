package org.okunev.customhorns.command.subcommand;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
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

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LocalCreateSubCommand extends AbstractSubCommand {
    private final CustomHorns plugin = CustomHorns.getInstance();

    public LocalCreateSubCommand() {
        super("local");

        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());

        this.withArguments(new StringArgument("filename").replaceSuggestions(ArgumentSuggestions.stringCollection((sender) -> {
            File soundsFolder = plugin.getSoundsFolder();
            if (!soundsFolder.isDirectory()) {
                return List.of();
            }

            File[] files = soundsFolder.listFiles();
            if (files == null) {
                return List.of();
            }

            return Arrays.stream(files)
                    .filter(file -> !file.isDirectory() && HornUtils.isValidAudioFile(file))
                    .map(File::getName)
                    .toList();
        })));
        this.withArguments(new TextArgument("song_name").replaceSuggestions(quotedArgument(null)));

        this.executesPlayer(this::executePlayer);
        this.executes(this::execute);
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.create.local.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.create.local.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customhorns.create.local");
    }

    @Override
    public void executePlayer(Player player, CommandArguments arguments) {
        if (!hasPermission(player)) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
            return;
        }

        if (!isHornInHand(player)) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.error.not-holding-horn"));
            return;
        }

        String filename = getArgumentValue(arguments, "filename", String.class);
        if (filename.contains("../") || filename.contains("..\\")) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.command.invalid-filename"));
            return;
        }

        String customName = getArgumentValue(arguments, "song_name", String.class);

        if (customName.isEmpty()) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.command.horn-name-empty"));
            return;
        }

        File soundsFolder = plugin.getSoundsFolder();
        File soundFile = new File(soundsFolder, filename);

        if (!soundFile.exists()) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.file.not-found"));
            return;
        }

        if (!HornUtils.isValidAudioFile(soundFile)) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.command.unknown-extension"));
            return;
        }

        CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("command.create.messages.checking-duration"));

        CompletableFuture<Long> durationFuture = HornUtils.getAudioDuration(soundFile.getPath());

        plugin.getSchedulers().async.runNow(task -> {
            try {
                long duration = durationFuture.get(10, TimeUnit.SECONDS);

                if (duration > plugin.getChConfig().getMaxHornDuration() * 1000) {
                    CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.horn.too-long",
                            String.valueOf(plugin.getChConfig().getMaxHornDuration())));
                    return;
                }

                // Передаем soundFile в createHorn
                File finalSoundFile = soundFile;
                plugin.getSchedulers().global.runNow(globalTask -> {
                    createHorn(player, finalSoundFile, customName, duration);
                });

            } catch (Exception e) {
                CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.duration-check-failed"));
                CustomHorns.error("Failed to check audio duration", e);
            }
        });
    }

    @SuppressWarnings("UnstableApiUsage")
    private void createHorn(Player player, File soundFile, String customName, long duration) {
        ItemStack horn = new ItemStack(player.getInventory().getItemInMainHand());

        ItemMeta meta = horn.getItemMeta();

        meta.displayName(plugin.getLanguage().component("horn-name.simple")
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

        // Используем полный путь к файлу
        data.set(Keys.LOCAL_HORN.key(), Keys.LOCAL_HORN.dataType(), soundFile.getAbsolutePath());
        data.set(Keys.HORN_DURATION.key(), Keys.HORN_DURATION.dataType(), duration);
        data.set(Keys.HORN_NAME.key(), Keys.HORN_NAME.dataType(), customName);

        horn.setItemMeta(meta);
        player.getInventory().setItemInMainHand(horn);

        CustomHorns.sendMessage(player, plugin.getLanguage().component("command.create.messages.file", soundFile.getName()));
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