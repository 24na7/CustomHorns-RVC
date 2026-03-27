package org.okunev.customhorns.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.okunev.customhorns.api.HornEntry;

public class HornUseEvent extends Event implements Cancellable {
    private static final HandlerList HANDLER_LIST = new HandlerList();
    private boolean isCancelled;

    private final Player player;
    private final HornEntry hornEntry;

    public HornUseEvent(Player player, HornEntry hornEntry) {
        this.player = player;
        this.hornEntry = hornEntry;
        this.isCancelled = false;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @Override
    public boolean isCancelled() {
        return this.isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public HornEntry getHornEntry() {
        return hornEntry;
    }
}