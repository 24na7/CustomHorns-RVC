package org.okunev.customhorns;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.okunev.customhorns.api.CustomHornsAPI;
import org.okunev.customhorns.api.HornPlayerManager;
import org.okunev.customhorns.util.HornUtils;

public class CustomHornsAPIImpl implements CustomHornsAPI {
    @Override
    public @NotNull HornPlayerManager getHornPlayerManager() {
        return HornPlayerManagerImpl.getInstance();
    }

    @Override
    public boolean isCustomHorn(@NotNull ItemStack item) {
        return HornUtils.isCustomHorn(item);
    }
}