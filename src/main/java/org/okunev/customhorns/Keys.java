package org.okunev.customhorns;

import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;

public class Keys {
    public static final Key<String> LOCAL_HORN = Key.create("local", PersistentDataType.STRING);
    public static final Key<String> REMOTE_HORN = Key.create("remote", PersistentDataType.STRING);
    public static final Key<Long> HORN_DURATION = Key.create("duration", PersistentDataType.LONG);
    public static final Key<String> HORN_NAME = Key.create("name", PersistentDataType.STRING);

    @Deprecated(forRemoval = true)
    public static final Key<String> LEGACY_LOCAL_HORN = Key.create("customhorn", PersistentDataType.STRING);
    @Deprecated(forRemoval = true)
    public static final Key<String> LEGACY_REMOTE_HORN = Key.create("remote-customhorn", PersistentDataType.STRING);
    @Deprecated(forRemoval = true)
    public static final Key<String> LEGACY_YOUTUBE_HORN = Key.create("customhornyt", PersistentDataType.STRING);
    @Deprecated(forRemoval = true)
    public static final Key<String> LEGACY_SOUNDCLOUD_HORN = Key.create("customhornsc", PersistentDataType.STRING);

    public record Key<T>(NamespacedKey key, PersistentDataType<T, T> dataType) {
        public static <Z> Key<Z> create(String key, PersistentDataType<Z, Z> dataType) {
            return new Key<>(new NamespacedKey(CustomHorns.getInstance(), key), dataType);
        }
    }
}