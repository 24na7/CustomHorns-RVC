package org.okunev.customhorns.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class HornStopEvent extends Event {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Player player;
    private final String identifier;

    public HornStopEvent(Player player, String identifier) {
        super(true);
        this.player = player;
        this.identifier = identifier;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    @NotNull
    public Player getPlayer() {
        return player;
    }

    @NotNull
    public String getIdentifier() {
        return identifier;
    }
}