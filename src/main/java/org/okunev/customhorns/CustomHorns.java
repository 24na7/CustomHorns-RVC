package org.okunev.customhorns;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import dev.jorel.commandapi.CommandAPI;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.okunev.customhorns.api.CustomHornsAPI;
import org.okunev.customhorns.command.CustomHornsCommand;
import org.okunev.customhorns.command.subcommand.FallbackCustomHornsCommand;
import org.okunev.customhorns.config.CHConfig;
import org.okunev.customhorns.config.CHData;
import org.okunev.customhorns.language.YamlLanguage;
import org.okunev.customhorns.listener.HornDropListener;
import org.okunev.customhorns.listener.PlayerHornListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class CustomHorns extends JavaPlugin {
    private static Logger logger;
    private static Logger debugLogger;

    @Getter
    private static CustomHorns instance;

    @Getter
    private YamlLanguage language;
    @Getter
    private File soundsFolder;
    @Getter
    private CHConfig chConfig;
    @Getter
    private CHData chData;
    @Getter
    private Schedulers schedulers;

    public int hornsUsed = 0;
    private boolean voicechatAddonRegistered = false;
    private boolean libsLoaded = false;
    private CHVoiceAddon voiceAddon;

    @Override
    public void onLoad() {
        instance = this;

        this.language = new YamlLanguage();
        this.soundsFolder = new File(this.getDataFolder(), "sounds");
        this.chConfig = new CHConfig();
        this.chData = new CHData();
        this.schedulers = new Schedulers(this);

        logger = LoggerFactory.getLogger(this.getName());
        debugLogger = LoggerFactory.getLogger(this.getName() + "/Debug");

        getServer().getServicesManager().register(
                CustomHornsAPI.class,
                new CustomHornsAPIImpl(),
                this,
                ServicePriority.Normal
        );
    }

    @Override
    public void onEnable() {
        libsLoaded = System.getProperty("customhorns.loader.success", "false").equals("true");
        if (!libsLoaded) {
            getSLF4JLogger().error("Libraries failed to load: Goodbye.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        if (getServer().getPluginManager().getPlugin("CommandAPI") != null) {
            CommandAPI.onEnable();
            getSLF4JLogger().info("CommandAPI detected and enabled");
        } else {
            getSLF4JLogger().warn("CommandAPI not found - using fallback command system");
        }

        if (getDataFolder().mkdir())
            getSLF4JLogger().info("Created plugin data folder");

        chConfig.load();
        language.load();
        chData.load();
        chData.startAutosave();

        if (!soundsFolder.exists()) {
            if (soundsFolder.mkdir())
                info("Created sounds folder");
        }

        registerVoicechatHook();
        registerEvents();
        registerCommands();
        registerProtocolLib();

        schedulers.async.runNow(task -> checkUpdates());
    }

    @Override
    public void onDisable() {
        if (!libsLoaded) return;

        if (getServer().getPluginManager().getPlugin("CommandAPI") != null) {
            CommandAPI.onDisable();
        }

        HornPlayerManagerImpl.getInstance().stopPlayingAll();

        chData.stopAutosave();
        chData.save();

        if (voicechatAddonRegistered && voiceAddon != null) {
            getServer().getServicesManager().unregister(voiceAddon);
            info("Successfully disabled CustomHorns plugin");
        }

        schedulers.async.cancelTasks();
        schedulers.global.cancelTasks();
    }

    private void registerVoicechatHook() {
        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

        if (service != null) {
            voiceAddon = CHVoiceAddon.getInstance();
            service.registerPlugin(voiceAddon);
            voicechatAddonRegistered = true;
            info("Successfully registered voicechat hook");

            schedulers.async.runNow(task -> {
                try {
                    Thread.sleep(2000);
                    if (voiceAddon.isApiReady()) {
                        info("VoiceChat API initialized and ready");
                    } else {
                        warn("VoiceChat API not yet initialized, will retry when used");
                    }
                } catch (InterruptedException e) {
                    // Ignore
                }
            });
        } else {
            error("Failed to register voicechat hook - Simple Voice Chat not found");
        }
    }

    private void registerProtocolLib() {
        try {
            ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
            protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL,
                    PacketType.Play.Server.NAMED_SOUND_EFFECT) {
                @Override
                public void onPacketSending(PacketEvent event) {
                    try {
                        PacketContainer packet = event.getPacket();

                        Object soundObject = packet.getModifier().read(0);
                        String soundName = null;

                        if (soundObject != null) {
                            soundName = soundObject.toString();
                        }

                        if (soundName != null && soundName.contains("item.goat_horn")) {
                            Player player = event.getPlayer();
                            if (player != null && HornPlayerManagerImpl.getInstance().isPlaying(player.getUniqueId())) {
                                event.setCancelled(true);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
            });
            info("ProtocolLib packet listener registered");
        } catch (Exception e) {
            error("Failed to register ProtocolLib listener", e);
        }
    }

    private void checkUpdates() {
    }

    private void registerCommands() {
        if (getServer().getPluginManager().getPlugin("CommandAPI") != null) {
            new CustomHornsCommand().register("customhorns");
        } else {
            FallbackCustomHornsCommand fallbackCommand = new FallbackCustomHornsCommand();
            getServer().getCommandMap().register("customhorns", fallbackCommand);
        }
    }

    private void registerEvents() {
        getServer().getPluginManager().registerEvents(new PlayerHornListener(), this);
        getServer().getPluginManager().registerEvents(new HornDropListener(), this);
    }

    public static void sendMessage(CommandSender sender, Component component) {
        sender.sendMessage(component);
    }

    public static void debug(@NotNull String message, Object... format) {
        if (getInstance().getChConfig().isDebug()) {
            debugLogger.info(message, format);
        }
    }

    public static void info(@NotNull String message, Object... format) {
        logger.info(message, format);
    }

    public static void warn(@NotNull String message, Object... format) {
        logger.warn(message, format);
    }

    public static void error(@NotNull String message, @Nullable Throwable e, Object... format) {
        logger.error(message, format, e);
    }

    public static void error(@NotNull String message, Object... format) {
        logger.error(message, format);
    }
}