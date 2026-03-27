package org.okunev.customhorns.util;

import lombok.Getter;
import org.okunev.customhorns.CustomHorns;
import org.okunev.customhorns.config.CHConfig;

import java.util.List;
import java.util.function.Function;

@Getter
public enum RemoteServices {
    YOUTUBE("youtube",
            config -> config.getRemoteFilterYoutube(),
            config -> config.getRemoteCustomModelDataYoutube()),
    SOUNDCLOUD("soundcloud",
            config -> config.getRemoteFilterSoundcloud(),
            config -> config.getRemoteCustomModelDataSoundcloud());

    private final String id;
    private final Function<CHConfig, List<String>> filterProvider;
    private final Function<CHConfig, Integer> modelDataProvider;

    RemoteServices(String id, Function<CHConfig, List<String>> filterProvider,
                   Function<CHConfig, Integer> modelDataProvider) {
        this.id = id;
        this.filterProvider = filterProvider;
        this.modelDataProvider = modelDataProvider;
    }

    public int getCustomModelData() {
        return modelDataProvider.apply(CustomHorns.getInstance().getChConfig());
    }

    public static RemoteServices fromUrl(String url) {
        CHConfig config = CustomHorns.getInstance().getChConfig();

        for (RemoteServices service : values()) {
            if (matchesAny(url, service.filterProvider.apply(config))) {
                return service;
            }
        }

        return null;
    }

    private static boolean matchesAny(String url, List<String> patterns) {
        return patterns.stream().anyMatch(url::contains);
    }
}