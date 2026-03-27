package org.okunev.customhorns.config;

import lombok.Getter;
import org.simpleyaml.configuration.comments.CommentType;
import org.simpleyaml.configuration.file.YamlFile;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.language.Language;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

@Getter
public class CHConfig {
    private final YamlFile yaml = new YamlFile();
    private final File configFile;
    private String configVersion;

    public CHConfig() {
        this.configFile = new File(CustomHorns.getInstance().getDataFolder(), "config.yml");
    }

    public List<String> getRemoteFilterYoutube() {
        return remoteFilterYoutube;
    }

    public int getRemoteCustomModelDataYoutube() {
        return remoteCustomModelDataYoutube;
    }

    public List<String> getRemoteFilterSoundcloud() {
        return remoteFilterSoundcloud;
    }

    public int getRemoteCustomModelDataSoundcloud() {
        return remoteCustomModelDataSoundcloud;
    }

    public void load() {
        if (configFile.exists()) {
            try {
                yaml.load(configFile);
            } catch (IOException e) {
                CustomHorns.error("Error loading file: ", e);
            }
        }

        configVersion = getString("info.version", "1.0", "Don't change this value");

        for (Method method : this.getClass().getDeclaredMethods()) {
            if (Modifier.isPrivate(method.getModifiers()) &&
                    method.getReturnType().equals(Void.TYPE) &&
                    method.getName().endsWith("Settings")) {
                try {
                    method.invoke(this);
                } catch (Throwable t) {
                    CustomHorns.error("Failed to load configuration option from {}", t, method.getName());
                }
            }
        }

        save();
    }

    public void save() {
        try {
            yaml.save(configFile);
        } catch (IOException e) {
            CustomHorns.error("Error saving file: ", e);
        }
    }

    private void setComment(String key, String... comment) {
        if (yaml.contains(key) && comment.length > 0) {
            yaml.setComment(key, String.join("\n", comment), CommentType.BLOCK);
        }
    }

    private void ensureDefault(String key, Object defaultValue, String... comment) {
        if (!yaml.contains(key))
            yaml.set(key, defaultValue);
        setComment(key, comment);
    }

    private boolean getBoolean(String key, boolean defaultValue, String... comment) {
        ensureDefault(key, defaultValue, comment);
        return yaml.getBoolean(key, defaultValue);
    }

    private int getInt(String key, int defaultValue, String... comment) {
        ensureDefault(key, defaultValue, comment);
        return yaml.getInt(key, defaultValue);
    }

    private double getDouble(String key, double defaultValue, String... comment) {
        ensureDefault(key, defaultValue, comment);
        return yaml.getDouble(key, defaultValue);
    }

    private String getString(String key, String defaultValue, String... comment) {
        ensureDefault(key, defaultValue, comment);
        return yaml.getString(key, defaultValue);
    }

    private List<String> getStringList(String key, List<String> defaultValue, String... comment) {
        ensureDefault(key, defaultValue, comment);
        return yaml.getStringList(key);
    }

    private String locale = Language.ENGLISH.getLabel();
    private boolean shouldCheckUpdates = true;
    private boolean debug = false;

    private void globalSettings() {
        locale = getString("global.locale", locale,
                "Language of the plugin",
                "Supported: " + Language.getAllSeparatedComma(),
                "Unknown languages will be replaced with " + Language.ENGLISH.getLabel()
        );
        if (!Language.isExists(locale)) locale = Language.ENGLISH.getLabel();
        shouldCheckUpdates = getBoolean("global.check-updates", shouldCheckUpdates);
        debug = getBoolean("global.debug", debug);
    }

    // Horn Settings
    private int maxHornDuration = 8;
    private float hornVolume = 1.0f;
    private int defaultHornDistance = 32;
    private boolean hornCooldownEnabled = true;
    private int hornCooldown = 5;
    private String particleType = "note";
    private int maxDownloadSize = 25;

    private void hornSettings() {
        maxHornDuration = getInt("horn.max-duration", maxHornDuration,
                "Maximum duration of horn sounds in seconds (7-8 recommended)");
        hornVolume = Float.parseFloat(getString("horn.volume", String.valueOf(hornVolume),
                "The master volume of horns from 0-1"));
        defaultHornDistance = getInt("horn.default-distance", defaultHornDistance,
                "Default distance in blocks that horn sound can be heard");
        hornCooldownEnabled = getBoolean("horn.cooldown.enabled", hornCooldownEnabled,
                "Enable cooldown between horn uses");
        hornCooldown = getInt("horn.cooldown.seconds", hornCooldown,
                "Cooldown time in seconds");
        particleType = getString("horn.particle-type", particleType,
                "Particle type when using horn: note, music, spell, or none");
        maxDownloadSize = getInt("command.download.max-size", maxDownloadSize,
                "The maximum download size in megabytes");
    }

    private int localCustomModelData = 0;
    private List<String> remoteTabComplete = List.of("https://www.youtube.com/watch?v=", "https://soundcloud.com/");
    private int remoteCustomModelDataYoutube = 0;
    private List<String> remoteFilterYoutube = List.of("https://www.youtube.com/watch?v=", "https://youtu.be/");
    private int remoteCustomModelDataSoundcloud = 0;
    private List<String> remoteFilterSoundcloud = List.of("https://soundcloud.com/");
    private int distanceCommandMaxDistance = 64;

    private void commandSettings() {
        localCustomModelData = getInt("command.create.local.custom-model", localCustomModelData);
        remoteTabComplete = getStringList("command.create.remote.tabcomplete", remoteTabComplete);
        remoteCustomModelDataYoutube = getInt("command.create.remote.youtube.custom-model", remoteCustomModelDataYoutube);
        remoteFilterYoutube = getStringList("command.create.remote.youtube.filter", remoteFilterYoutube);
        remoteCustomModelDataSoundcloud = getInt("command.create.remote.soundcloud.custom-model", remoteCustomModelDataSoundcloud);
        remoteFilterSoundcloud = getStringList("command.create.remote.soundcloud.filter", remoteFilterSoundcloud);
        distanceCommandMaxDistance = getInt("command.distance.max", distanceCommandMaxDistance);

        setComment("command.create.remote.tabcomplete",
                "tabcomplete — Displaying hints when entering remote command",
                "filter — Filter for applying custom-model-data to remote horns");
    }

    private boolean youtubeOauth2 = false;
    private String youtubePoToken = "";
    private String youtubePoVisitorData = "";
    private String youtubeRemoteServer = "";
    private String youtubeRemoteServerPassword = "";

    private void youtubeSettings() {
        youtubeOauth2 = getBoolean("youtube.use-oauth2", youtubeOauth2,
                "This may help if the plugin is not working properly.",
                "When you first play after the server starts, you will see an authorization request.");

        youtubePoToken = getString("youtube.po-token.token", youtubePoToken);
        youtubePoVisitorData = getString("youtube.po-token.visitor-data", youtubePoVisitorData);

        setComment("youtube.po-token",
                "If you have oauth2 enabled, leave these fields blank.",
                "This may help if the plugin is not working properly.");

        youtubeRemoteServer = getString("youtube.remote-server.url", youtubeRemoteServer);
        youtubeRemoteServerPassword = getString("youtube.remote-server.password", youtubeRemoteServerPassword);

        setComment("youtube.remote-server",
                "A method for obtaining streaming via a remote server.",
                "Make sure Oauth2 was enabled!");
    }
}