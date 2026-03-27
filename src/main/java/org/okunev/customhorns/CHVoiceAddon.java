package org.okunev.customhorns;

import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.VolumeCategory;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import lombok.Getter;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.Enumeration;
import java.util.concurrent.CompletableFuture;

@Getter
public class CHVoiceAddon implements VoicechatPlugin {
    public static final String HORN_CATEGORY = "custom_horns";

    private VoicechatServerApi voicechatApi;
    private VolumeCategory hornsCategory;
    private static CHVoiceAddon instance;
    private final CompletableFuture<Void> apiReadyFuture = new CompletableFuture<>();

    public synchronized static CHVoiceAddon getInstance() {
        if (instance == null) instance = new CHVoiceAddon();
        return instance;
    }

    @Override
    public String getPluginId() {
        return CustomHorns.getInstance().getName().toLowerCase();
    }

    @Override
    public void initialize(VoicechatApi api) {
        this.voicechatApi = (VoicechatServerApi) api;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            this.voicechatApi = event.getVoicechat();
            hornsCategory = voicechatApi.volumeCategoryBuilder()
                    .setId(HORN_CATEGORY)
                    .setName("Custom Horns")
                    .setIcon(getHornIcon())
                    .build();
            voicechatApi.registerVolumeCategory(hornsCategory);
            apiReadyFuture.complete(null);
            CustomHorns.info("VoiceChat API initialized successfully");
        });
    }

    private int[][] getHornIcon() {
        try {
            Enumeration<URL> resources = this.getClass().getClassLoader().getResources("horn_category.png");
            while (resources.hasMoreElements()) {
                BufferedImage bufferedImage = ImageIO.read(resources.nextElement().openStream());
                if (bufferedImage.getWidth() != 16 || bufferedImage.getHeight() != 16) continue;

                int[][] image = new int[16][16];
                for (int x = 0; x < bufferedImage.getWidth(); x++) {
                    for (int y = 0; y < bufferedImage.getHeight(); y++) {
                        image[x][y] = bufferedImage.getRGB(x, y);
                    }
                }
                return image;
            }
        } catch (Throwable e) {
            CustomHorns.error("Error getting horn icon: ", e);
        }
        return null;
    }

    public boolean isApiReady() {
        return voicechatApi != null;
    }
}