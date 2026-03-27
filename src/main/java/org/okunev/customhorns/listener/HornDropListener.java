package org.okunev.customhorns.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.okunev.customhorns.HornPlayerManagerImpl;
import org.okunev.customhorns.util.HornUtils;

public class HornDropListener implements Listener {

    @EventHandler
    public void onHornDrop(PlayerDropItemEvent event) {
        if (!HornUtils.isCustomHorn(event.getItemDrop().getItemStack())) return;

        if (HornPlayerManagerImpl.getInstance().isPlaying(event.getPlayer().getUniqueId())) {
            HornPlayerManagerImpl.getInstance().stopPlaying(event.getPlayer());
        }
    }
}