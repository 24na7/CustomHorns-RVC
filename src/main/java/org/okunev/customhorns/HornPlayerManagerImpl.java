package org.okunev.customhorns;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.Position;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.YoutubeSourceOptions;
import dev.lavalink.youtube.clients.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.okunev.customhorns.api.HornPlayerManager;
import org.okunev.customhorns.api.event.HornStartEvent;
import org.okunev.customhorns.api.event.HornStopEvent;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class HornPlayerManagerImpl implements HornPlayerManager {
    private static HornPlayerManagerImpl instance;

    private final CustomHorns plugin = CustomHorns.getInstance();
    private final AudioPlayerManager audioPlayerManager = new DefaultAudioPlayerManager();
    private final Map<UUID, HornPlayer> playerMap = new ConcurrentHashMap<>();
    private final File refreshTokenFile = new File(plugin.getDataFolder(), ".youtube-token");
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "HornPlayerExecutorThread"));
    private YoutubeAudioSourceManager youtubeSourceManager;

    private final List<ActiveHandler> allHandlers = new CopyOnWriteArrayList<>();
    private final Map<Plugin, List<ActiveHandler>> pluginMap = new ConcurrentHashMap<>();

    public synchronized static HornPlayerManagerImpl getInstance() {
        if (instance == null) instance = new HornPlayerManagerImpl();
        return instance;
    }

    private HornPlayerManagerImpl() {
        registerYoutube();
        registerSoundcloud();
        audioPlayerManager.registerSourceManager(new LocalAudioSourceManager());
    }

    private void registerYoutube() {
        YoutubeSourceOptions options = new YoutubeSourceOptions().setAllowSearch(false);

        if (!plugin.getChConfig().getYoutubeRemoteServer().isBlank()) {
            String pass = plugin.getChConfig().getYoutubeRemoteServerPassword();
            CustomHorns.debug("Setting YouTube remote-cipher");
            options.setRemoteCipher(
                    plugin.getChConfig().getYoutubeRemoteServer(),
                    pass.isBlank() ? null : pass,
                    null
            );
        }

        youtubeSourceManager = new YoutubeAudioSourceManager(options,
                new Music(), new AndroidVr(), new Web(), new WebEmbedded(), new Tv());

        if (!plugin.getChConfig().getYoutubePoToken().isBlank() &&
                !plugin.getChConfig().getYoutubePoVisitorData().isBlank()) {
            Web.setPoTokenAndVisitorData(
                    plugin.getChConfig().getYoutubePoToken(),
                    plugin.getChConfig().getYoutubePoVisitorData()
            );
        } else if (plugin.getChConfig().isYoutubeOauth2()) {
            try {
                String oauth2token = null;
                if (refreshTokenFile.exists() && refreshTokenFile.isFile()) {
                    oauth2token = Files.readString(refreshTokenFile.toPath()).trim();
                }
                youtubeSourceManager.useOauth2(oauth2token, false);
                if (oauth2token == null) listenForTokenChange(youtubeSourceManager);
            } catch (Throwable e) {
                CustomHorns.error("Failed to load YouTube oauth2 token: ", e);
            }
        }

        audioPlayerManager.registerSourceManager(youtubeSourceManager);
    }

    private void registerSoundcloud() {
        audioPlayerManager.registerSourceManager(SoundCloudAudioSourceManager.createDefault());
    }

    private VoicechatServerApi waitForVoicechatApi() {
        CHVoiceAddon addon = CHVoiceAddon.getInstance();

        if (addon.isApiReady()) {
            return addon.getVoicechatApi();
        }

        try {
            addon.getApiReadyFuture().get(5, TimeUnit.SECONDS);
            return addon.getVoicechatApi();
        } catch (Exception e) {
            CustomHorns.error("Timeout waiting for VoiceChat API", e);
            return null;
        }
    }

    private void listenForTokenChange(YoutubeAudioSourceManager source) {
        final String currentToken = source.getOauth2RefreshToken() != null
                ? source.getOauth2RefreshToken() : "null";

        AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
        ScheduledFuture<?> future = executor.scheduleAtFixedRate(() -> {
            CustomHorns.debug("Trying to handle token change.");
            String newToken = source.getOauth2RefreshToken();
            if (newToken == null) return;
            if (currentToken.equals(newToken)) return;

            saveYoutubeToken();
            futureRef.get().cancel(false);
        }, 4, 4, TimeUnit.SECONDS);
        futureRef.set(future);
    }

    private void saveYoutubeToken() {
        CustomHorns.debug("Attempting to save YouTube token");

        if (youtubeSourceManager != null) {
            String refreshToken = youtubeSourceManager.getOauth2RefreshToken();
            if (refreshToken != null) {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(refreshTokenFile))) {
                    writer.write(refreshToken);
                    CustomHorns.debug("YouTube's oauth2 token is successfully saved");
                } catch (IOException e) {
                    CustomHorns.error("Failed to save the YouTube's oauth2 token: ", e);
                }
            }
        }
    }

    @Override
    public void registerPacketHandler(@NotNull Plugin plugin, @NotNull PacketConsumer consumer) {
        ActiveHandler active = new ActiveHandler(plugin, consumer);
        allHandlers.add(active);
        pluginMap.computeIfAbsent(plugin, k -> new CopyOnWriteArrayList<>()).add(active);
    }

    @Override
    public void unregisterPacketHandlers(@NotNull Plugin plugin) {
        List<ActiveHandler> handlers = pluginMap.remove(plugin);
        if (handlers != null) {
            allHandlers.removeAll(handlers);
        }
    }

    private void removeHandler(ActiveHandler handler) {
        allHandlers.remove(handler);
        List<ActiveHandler> pluginList = pluginMap.get(handler.plugin);
        if (pluginList != null) pluginList.remove(handler);
    }

    @Override
    public void play(@NotNull Player player, @NotNull String identifier, long duration, Component actionbarComponent) {
        UUID playerId = player.getUniqueId();

        if (duration > plugin.getChConfig().getMaxHornDuration() * 1000) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.horn.too-long",
                    String.valueOf(plugin.getChConfig().getMaxHornDuration())));
            return;
        }

        if (playerMap.containsKey(playerId)) {
            CustomHorns.debug("Player {} already playing a horn", playerId);
            return;
        }

        CustomHorns.debug("Starting HornPlayer for player: {}", playerId);

        VoicechatServerApi api = waitForVoicechatApi();
        if (api == null) {
            CustomHorns.error("Voicechat API not available");
            CustomHorns.sendMessage(player, Component.text("VoiceChat API not available. Please try again later.").color(NamedTextColor.RED));
            return;
        }

        LocationalAudioChannel audioChannel = api.createLocationalAudioChannel(
                UUID.randomUUID(),
                api.fromServerLevel(player.getWorld()),
                createPlayerPosition(api, player)
        );

        if (audioChannel == null) {
            CustomHorns.error("Failed to create audio channel");
            return;
        }

        audioChannel.setCategory(CHVoiceAddon.HORN_CATEGORY);
        audioChannel.setDistance(plugin.getChData().getPlayerHornDistance(playerId,
                plugin.getChConfig().getDefaultHornDistance()));

        HornPlayer hornPlayer = new HornPlayer(player, identifier, audioChannel, playerId, duration, api);
        playerMap.put(playerId, hornPlayer);
        hornPlayer.hornPlayerThread.start();

        plugin.hornsUsed++;

        if (actionbarComponent != null) {
            player.sendActionBar(actionbarComponent);
        }
    }

    private Position createPlayerPosition(VoicechatServerApi api, Player player) {
        Location loc = player.getLocation();
        return api.createPosition(loc.getX(), loc.getY() + 1.0, loc.getZ());
    }

    @Override
    public boolean isPlaying(@NotNull UUID playerId) {
        return playerMap.containsKey(playerId);
    }

    @Override
    public void stopPlaying(@NotNull Player player) {
        stopPlaying(player.getUniqueId());
    }

    private synchronized void stopPlaying(UUID playerId) {
        HornPlayer hornPlayer = playerMap.get(playerId);
        if (hornPlayer != null && hornPlayer.isRunning) {
            CustomHorns.debug("Stopping HornPlayer: {}", playerId);

            CompletableFuture<Void> eventFuture = new CompletableFuture<>();
            executor.execute(() -> {
                try {
                    HornStopEvent event = new HornStopEvent(hornPlayer.player, hornPlayer.identifier);
                    plugin.getServer().getPluginManager().callEvent(event);
                } finally {
                    eventFuture.complete(null);
                }
            });

            try {
                eventFuture.get(2, TimeUnit.SECONDS);
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                CustomHorns.error("Event timed out for HornPlayer {}", playerId);
            }

            hornPlayer.stop();
            playerMap.remove(playerId);
        }
    }

    @Override
    public void stopPlayingAll() {
        Set.copyOf(playerMap.keySet()).forEach(this::stopPlaying);
    }

    @Override
    @Nullable
    public LocationalAudioChannel getAudioChannel(@NotNull Player player) {
        HornPlayer hornPlayer = playerMap.get(player.getUniqueId());
        return hornPlayer == null ? null : hornPlayer.audioChannel;
    }

    private class HornPlayer {
        private final Player player;
        private final String identifier;
        private final LocationalAudioChannel audioChannel;
        private final UUID playerId;
        private final long maxDuration;
        private final VoicechatServerApi api;

        private final Thread hornPlayerThread = new Thread(this::threadJob, "HornPlayerThread");
        private final CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();

        private AudioPlayer audioPlayer;
        private volatile boolean isRunning = true;

        public HornPlayer(Player player, String identifier, LocationalAudioChannel audioChannel,
                          UUID playerId, long maxDuration, VoicechatServerApi api) {
            this.player = player;
            this.identifier = identifier;
            this.audioChannel = audioChannel;
            this.playerId = playerId;
            this.maxDuration = maxDuration;
            this.api = api;
        }

        private void stop() {
            this.isRunning = false;
            hornPlayerThread.interrupt();
            this.trackFuture.complete(null);
            if (audioPlayer != null) {
                this.audioPlayer.destroy();
            }
        }

        private boolean processPacket(Player player, byte[] data) {
            for (ActiveHandler handler : allHandlers) {
                boolean allowed = handler.consumer.process(handler, player, data);
                if (!allowed) return false;
            }
            return true;
        }

        private void updatePlayerPosition() {
            if (player.isOnline() && !player.isDead()) {
                Location loc = player.getLocation();
                Position newPosition = api.createPosition(loc.getX(), loc.getY() + 1.0, loc.getZ());
                audioChannel.updateLocation(newPosition);
            }
        }

        private void threadJob() {
            try {
                HornStartEvent event = new HornStartEvent(this.player, this.identifier);
                plugin.getServer().getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    if (isRunning) stopPlaying(playerId);
                    return;
                }

                audioPlayer = audioPlayerManager.createPlayer();

                audioPlayerManager.loadItem(identifier, new AudioLoadResultHandler() {
                    @Override
                    public void trackLoaded(AudioTrack audioTrack) {
                        CustomHorns.debug("HornPlayer {} loaded track: {}", playerId, audioTrack.getInfo().title);

                        long trackDuration = audioTrack.getDuration();
                        if (trackDuration > maxDuration) {
                            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.horn.too-long",
                                    String.valueOf(plugin.getChConfig().getMaxHornDuration())));
                            if (isRunning) stopPlaying(playerId);
                            trackFuture.complete(null);
                            return;
                        }

                        trackFuture.complete(audioTrack);
                    }

                    @Override
                    public void playlistLoaded(AudioPlaylist audioPlaylist) {
                        AudioTrack selected = audioPlaylist.getSelectedTrack();
                        CustomHorns.debug("HornPlayer {} loaded track from playlist: {}", playerId, selected.getInfo().title);

                        long trackDuration = selected.getDuration();
                        if (trackDuration > maxDuration) {
                            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.horn.too-long",
                                    String.valueOf(plugin.getChConfig().getMaxHornDuration())));
                            if (isRunning) stopPlaying(playerId);
                            trackFuture.complete(null);
                            return;
                        }

                        trackFuture.complete(selected);
                    }

                    @Override
                    public void noMatches() {
                        CustomHorns.debug("HornPlayer {} didn't find the track: {}", playerId, identifier);
                        CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.play.no-matches"));
                        if (isRunning) stopPlaying(playerId);
                    }

                    @Override
                    public void loadFailed(FriendlyException e) {
                        CustomHorns.debug("HornPlayer {} failed to load track: {} - {}", playerId, identifier, e.getMessage());
                        CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.play.audio-load"));
                        if (isRunning) stopPlaying(playerId);
                        trackFuture.complete(null);
                    }
                });

                AudioTrack audioTrack;
                try {
                    audioTrack = trackFuture.get(30, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    audioTrack = null;
                    hornPlayerThread.interrupt();
                    CustomHorns.debug("HornPlayer {} got interrupt while loading", playerId);
                }

                if (audioTrack == null) {
                    CustomHorns.debug("HornPlayer {} track is null. Stopping...", playerId);
                    if (isRunning) stopPlaying(playerId);
                    return;
                }

                int volume = Math.round(plugin.getChConfig().getHornVolume() * 100);
                audioPlayer.setVolume(volume);
                audioPlayer.playTrack(audioTrack);

                try {
                    long start = System.currentTimeMillis();
                    long lastUpdate = 0;

                    while (isRunning && !hornPlayerThread.isInterrupted() &&
                            audioPlayer.getPlayingTrack() != null &&
                            audioTrack.getState() != AudioTrackState.FINISHED) {

                        // Обновляем позицию игрока каждые 500мс (полсекунды)
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastUpdate > 500) {
                            updatePlayerPosition();
                            lastUpdate = currentTime;
                        }

                        AudioFrame frame = audioPlayer.provide(20L, TimeUnit.MILLISECONDS);
                        if (frame == null) {
                            TimeUnit.MILLISECONDS.sleep(50);
                            continue;
                        }

                        byte[] data = frame.getData();
                        if (processPacket(this.player, data)) {
                            audioChannel.send(data);
                        }

                        long wait = (start + frame.getTimecode()) - System.currentTimeMillis();
                        if (wait > 0) TimeUnit.MILLISECONDS.sleep(wait);
                    }
                } catch (InterruptedException e) {
                    CustomHorns.debug("HornPlayer {} got interrupt", playerId);
                    Thread.currentThread().interrupt();
                } catch (Throwable e) {
                    CustomHorns.error("HornPlayer {} got unexpected exception: ", e, playerId);
                }

                if (isRunning) stopPlaying(playerId);

            } catch (Throwable e) {
                CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.play.while-playing"));
                CustomHorns.error("HornPlayer {} got exception: ", e, playerId);
            }
        }
    }

    private static class ActiveHandler implements HandlerRegistration {
        private final Plugin plugin;
        private final PacketConsumer consumer;

        private ActiveHandler(Plugin plugin, PacketConsumer consumer) {
            this.plugin = plugin;
            this.consumer = consumer;
        }

        @Override
        public void unregister() {
            HornPlayerManagerImpl.getInstance().removeHandler(this);
        }
    }
}