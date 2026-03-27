package org.okunev.customhorns.config;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;
import org.okunev.customhorns.CustomHorns;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Getter
public class CHData {
    private final YamlFile yaml = new YamlFile();
    private final File dataFile;

    private ScheduledTask autosaveTask;

    private final Map<UUID, Integer> playerHornDistanceMap = new HashMap<>();
    private final Map<UUID, Long> playerCooldownMap = new HashMap<>();

    public CHData() {
        this.dataFile = new File(CustomHorns.getInstance().getDataFolder(), "data.yml");
    }

    public void load() {
        if (dataFile.exists()) {
            try {
                yaml.load(dataFile);
            } catch (IOException e) {
                CustomHorns.error("Error while loading data: ", e);
            }
        }

        loadPlayerDistances();
        loadPlayerCooldowns();
    }

    public void save() {
        ConfigurationSection distanceSection = yaml.createSection("players.distance");
        playerHornDistanceMap.forEach((uuid, distance) ->
                distanceSection.set(uuid.toString(), distance));

        ConfigurationSection cooldownSection = yaml.createSection("players.cooldown");
        playerCooldownMap.forEach((uuid, cooldown) ->
                cooldownSection.set(uuid.toString(), cooldown));

        try {
            yaml.save(dataFile);
        } catch (IOException e) {
            CustomHorns.error("Error saving data: ", e);
        }
    }

    public void startAutosave() {
        if (autosaveTask != null)
            throw new IllegalStateException("Autosave data task already exists");

        autosaveTask = CustomHorns.getInstance().getSchedulers().async.runAtFixedRate(
                task -> save(),
                60, 60, TimeUnit.SECONDS
        );
    }

    public void stopAutosave() {
        if (autosaveTask != null) {
            autosaveTask.cancel();
            autosaveTask = null;
        }
    }

    public int getPlayerHornDistance(UUID playerId, int defaultValue) {
        return playerHornDistanceMap.getOrDefault(playerId, defaultValue);
    }

    public void setPlayerHornDistance(UUID playerId, int distance) {
        playerHornDistanceMap.put(playerId, distance);
    }

    public long getPlayerCooldown(UUID playerId) {
        return playerCooldownMap.getOrDefault(playerId, 0L);
    }

    public void setPlayerCooldown(UUID playerId, long cooldownEnd) {
        playerCooldownMap.put(playerId, cooldownEnd);
    }

    public boolean isOnCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldownEnd = getPlayerCooldown(playerId);
        return System.currentTimeMillis() < cooldownEnd;
    }

    public long getRemainingCooldown(Player player) {
        UUID playerId = player.getUniqueId();
        long cooldownEnd = getPlayerCooldown(playerId);
        return Math.max(0, (cooldownEnd - System.currentTimeMillis()) / 1000);
    }

    private void loadPlayerDistances() {
        ConfigurationSection section = yaml.getConfigurationSection("players.distance");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                int distance = section.getInt(key);
                playerHornDistanceMap.put(uuid, distance);
            } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadPlayerCooldowns() {
        ConfigurationSection section = yaml.getConfigurationSection("players.cooldown");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(key);
                long cooldown = section.getLong(key);
                playerCooldownMap.put(uuid, cooldown);
            } catch (IllegalArgumentException ignored) {}
        }
    }
}