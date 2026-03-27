package org.okunev.customhorns.api;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class HornEntry {
    private final ItemStack horn;
    private final Component name;
    private final String identifier;
    private final boolean local;
    private final long duration;

    public HornEntry(ItemStack horn, Component name, String identifier, boolean local, long duration) {
        this.horn = horn;
        this.name = name;
        this.identifier = identifier;
        this.local = local;
        this.duration = duration;
    }

    @NotNull
    public ItemStack getHorn() {
        return horn;
    }

    @NotNull
    public Component getName() {
        return name;
    }

    @NotNull
    public String getIdentifier() {
        return identifier;
    }

    public boolean isLocal() {
        return local;
    }

    public long getDuration() {
        return duration;
    }
}