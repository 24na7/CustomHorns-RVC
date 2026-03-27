package org.okunev.customhorns.listener;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.HornPlayerManagerImpl;
import org.okunev.customhorns.Keys;
import org.okunev.customhorns.api.HornEntry;
import org.okunev.customhorns.api.event.HornUseEvent;
import org.okunev.customhorns.util.HornUtils;

public class PlayerHornListener implements Listener {
    private final CustomHorns plugin = CustomHorns.getInstance();

    @EventHandler(priority = EventPriority.HIGH)
    public void onHornUse(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.GOAT_HORN) return;

        if (!HornUtils.isCustomHorn(item)) return;

        event.setCancelled(true);

        if (HornPlayerManagerImpl.getInstance().isPlaying(player.getUniqueId())) {
            CustomHorns.sendMessage(player, plugin.getLanguage().PComponent("error.horn.already-playing"));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();

        String localFile = data.get(Keys.LOCAL_HORN.key(), Keys.LOCAL_HORN.dataType());
        String remoteUrl = data.get(Keys.REMOTE_HORN.key(), Keys.REMOTE_HORN.dataType());
        Long duration = data.get(Keys.HORN_DURATION.key(), Keys.HORN_DURATION.dataType());
        String customName = data.get(Keys.HORN_NAME.key(), Keys.HORN_NAME.dataType());

        if (duration == null) {
            duration = plugin.getChConfig().getMaxHornDuration() * 1000L;
        }

        String identifier;
        boolean isLocal;

        if (localFile != null) {
            identifier = localFile;
            isLocal = true;
        } else if (remoteUrl != null) {
            identifier = remoteUrl;
            isLocal = false;
        } else {
            return;
        }

        Component nameComponent = customName != null ?
                Component.text(customName) :
                plugin.getLanguage().component("horn.default-name");

        HornEntry hornEntry = new HornEntry(item, nameComponent, identifier, isLocal, duration);

        HornUseEvent useEvent = new HornUseEvent(player, hornEntry);
        plugin.getServer().getPluginManager().callEvent(useEvent);

        if (useEvent.isCancelled()) return;

        HornPlayerManagerImpl.getInstance().play(
                player,
                identifier,
                duration,
                plugin.getLanguage().component("now-playing", nameComponent)
        );

        if (plugin.getChConfig().isHornCooldownEnabled()) {
            player.setCooldown(Material.GOAT_HORN, plugin.getChConfig().getHornCooldown() * 20);
        }

        plugin.hornsUsed++;
    }
}