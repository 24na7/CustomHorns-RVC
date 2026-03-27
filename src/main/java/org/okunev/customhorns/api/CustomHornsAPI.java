package org.okunev.customhorns.api;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CustomHornsAPI {
    @Nullable
    static CustomHornsAPI get() {
        RegisteredServiceProvider<CustomHornsAPI> rsp = Bukkit.getServicesManager().getRegistration(CustomHornsAPI.class);
        if (rsp == null) return null;
        return rsp.getProvider();
    }

    @NotNull
    HornPlayerManager getHornPlayerManager();

    boolean isCustomHorn(@NotNull ItemStack item);
}