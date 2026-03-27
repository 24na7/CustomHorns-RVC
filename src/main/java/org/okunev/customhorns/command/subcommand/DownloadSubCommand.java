package org.okunev.customhorns.command.subcommand;

import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.command.AbstractSubCommand;
import org.okunev.customhorns.util.HornUtils;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

public class DownloadSubCommand extends AbstractSubCommand {
    private final CustomHorns plugin = CustomHorns.getInstance();

    public DownloadSubCommand() {
        super("download");

        this.withFullDescription(getDescription());
        this.withUsage(getSyntax());

        this.withArguments(new TextArgument("url"));
        this.withArguments(new StringArgument("filename"));

        this.executes(this::execute);
    }

    @Override
    public String getDescription() {
        return plugin.getLanguage().string("command.download.description");
    }

    @Override
    public String getSyntax() {
        return plugin.getLanguage().string("command.download.syntax");
    }

    @Override
    public boolean hasPermission(CommandSender sender) {
        return sender.hasPermission("customhorns.download");
    }

    @Override
    public void execute(CommandSender sender, CommandArguments arguments) {
        if (!hasPermission(sender)) {
            CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("error.command.no-permission"));
            return;
        }

        plugin.getSchedulers().async.runNow(task -> {
            try {
                URL fileURL = new URL(getArgumentValue(arguments, "url", String.class));
                String filename = getArgumentValue(arguments, "filename", String.class);

                if (filename.contains("../") || filename.contains("..\\")) {
                    CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("error.command.invalid-filename"));
                    return;
                }

                if (!HornUtils.isValidAudioFile(new File(filename))) {
                    CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("error.command.unknown-extension"));
                    return;
                }

                CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.downloading"));

                Path downloadPath = Path.of(plugin.getSoundsFolder().getPath(), filename);
                File downloadFile = new File(downloadPath.toUri());

                URLConnection connection = fileURL.openConnection();

                if (connection != null) {
                    long size = connection.getContentLengthLong() / 1048576;
                    if (size > plugin.getChConfig().getMaxDownloadSize()) {
                        CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.error.file-too-large",
                                String.valueOf(plugin.getChConfig().getMaxDownloadSize())));
                        return;
                    }
                }

                FileUtils.copyURLToFile(fileURL, downloadFile);

                CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.successfully"));
                CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.create-tooltip",
                        plugin.getLanguage().string("command.create.syntax")));

            } catch (Throwable e) {
                CustomHorns.error("Error while download music: ", e);
                CustomHorns.sendMessage(sender, plugin.getLanguage().PComponent("command.download.messages.error.while-download"));
            }
        });
    }
}