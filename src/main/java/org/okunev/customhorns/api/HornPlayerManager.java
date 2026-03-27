package org.okunev.customhorns.api;

import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface HornPlayerManager {
    interface HandlerRegistration {
        void unregister();
    }

    @FunctionalInterface
    interface PacketConsumer {
        boolean process(@NotNull HandlerRegistration registration, @NotNull Player player, byte @NotNull [] data);
    }

    void registerPacketHandler(@NotNull Plugin plugin, @NotNull PacketConsumer consumer);
    void unregisterPacketHandlers(@NotNull Plugin plugin);

    void play(@NotNull Player player, @NotNull String identifier, long duration, @Nullable Component actionbarComponent);
    boolean isPlaying(@NotNull UUID playerId);
    void stopPlaying(@NotNull Player player);
    void stopPlayingAll();

    @Nullable
    LocationalAudioChannel getAudioChannel(@NotNull Player player);
}