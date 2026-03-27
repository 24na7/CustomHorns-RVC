package org.okunev.customhorns.util;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.clients.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.Keys;
import org.okunev.customhorns.api.HornEntry;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("removal")
public class HornUtils {
    private static final CustomHorns plugin = CustomHorns.getInstance();
    private static final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();

    static {
        audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());
        audioPlayerManager.registerSourceManager(new YoutubeAudioSourceManager(
                new Music(), new AndroidVr(), new Web(), new WebEmbedded(), new Tv()
        ));
    }

    public static boolean isCustomHorn(ItemStack item) {
        if (item == null || item.getType() != Material.GOAT_HORN) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();

        // Проверяем миграцию
        if (migratePDC(data)) {
            item.setItemMeta(meta);
        }

        return data.has(Keys.LOCAL_HORN.key(), Keys.LOCAL_HORN.dataType()) ||
                data.has(Keys.REMOTE_HORN.key(), Keys.REMOTE_HORN.dataType());
    }

    private static boolean migratePDC(PersistentDataContainer data) {
        String legacyLocalValue = data.get(Keys.LEGACY_LOCAL_HORN.key(), Keys.LEGACY_LOCAL_HORN.dataType());
        if (legacyLocalValue != null) {
            data.remove(Keys.LEGACY_LOCAL_HORN.key());
            data.set(Keys.LOCAL_HORN.key(), Keys.LOCAL_HORN.dataType(), legacyLocalValue);
            return true;
        }

        Keys.Key<String>[] legacyRemoteKeys = new Keys.Key[]{
                Keys.LEGACY_REMOTE_HORN,
                Keys.LEGACY_YOUTUBE_HORN,
                Keys.LEGACY_SOUNDCLOUD_HORN
        };

        for (Keys.Key<String> key : legacyRemoteKeys) {
            String legacyRemoteValue = data.get(key.key(), key.dataType());
            if (legacyRemoteValue != null) {
                data.remove(key.key());
                data.set(Keys.REMOTE_HORN.key(), Keys.REMOTE_HORN.dataType(), legacyRemoteValue);
                return true;
            }
        }

        return false;
    }

    public static HornEntry getHornEntry(ItemStack horn) {
        ItemMeta meta = horn.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        String local = data.get(Keys.LOCAL_HORN.key(), Keys.LOCAL_HORN.dataType());
        String remote = data.get(Keys.REMOTE_HORN.key(), Keys.REMOTE_HORN.dataType());
        Long duration = data.get(Keys.HORN_DURATION.key(), Keys.HORN_DURATION.dataType());
        String name = data.get(Keys.HORN_NAME.key(), Keys.HORN_NAME.dataType());

        Component nameComponent = name != null ?
                Component.text(name).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false) :
                Component.text("Unknown").color(NamedTextColor.GRAY);

        if (local != null) {
            return new HornEntry(horn, nameComponent, local, true,
                    duration != null ? duration : CustomHorns.getInstance().getChConfig().getMaxHornDuration() * 1000L);
        }

        if (remote != null) {
            return new HornEntry(horn, nameComponent, remote, false,
                    duration != null ? duration : CustomHorns.getInstance().getChConfig().getMaxHornDuration() * 1000L);
        }

        throw new IllegalArgumentException("Item is not a custom horn");
    }

    public static CompletableFuture<Long> getAudioDuration(String identifier) {
        CompletableFuture<Long> future = new CompletableFuture<>();

        audioPlayerManager.loadItem(identifier, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                CustomHorns.debug("Track loaded: {}", track.getInfo().title);
                future.complete(track.getDuration());
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                AudioTrack track = playlist.getSelectedTrack();
                if (track != null) {
                    CustomHorns.debug("Playlist track loaded: {}", track.getInfo().title);
                    future.complete(track.getDuration());
                } else if (!playlist.getTracks().isEmpty()) {
                    AudioTrack firstTrack = playlist.getTracks().get(0);
                    CustomHorns.debug("First playlist track loaded: {}", firstTrack.getInfo().title);
                    future.complete(firstTrack.getDuration());
                } else {
                    CustomHorns.debug("Playlist is empty");
                    future.complete(0L);
                }
            }

            @Override
            public void noMatches() {
                CustomHorns.debug("No matches for: {}", identifier);
                future.complete(0L);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                CustomHorns.error("Failed to load audio: {}", exception, exception.getMessage());
                future.complete(0L);
            }
        });

        return future;
    }

    public static boolean isValidAudioFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".wav") || name.endsWith(".mp3") ||
                name.endsWith(".flac") || name.endsWith(".ogg");
    }

    public static String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return index > 0 ? filename.substring(index + 1) : "";
    }

    public static Component getHornName(ItemMeta meta) {
        String name = meta.getPersistentDataContainer().get(Keys.HORN_NAME.key(), Keys.HORN_NAME.dataType());
        if (name != null) {
            return Component.text(name).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
        }

        List<Component> lore = meta.lore();
        if (lore != null && !lore.isEmpty()) {
            return lore.getFirst();
        }

        return Component.text("Unknown").color(NamedTextColor.GRAY);
    }
}