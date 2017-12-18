package com.jonahseguin.payload.simple.util;

import com.jonahseguin.payload.simple.cache.PayloadSimpleCache;
import com.jonahseguin.payload.simple.simple.PlayerCacheable;
import com.jonahseguin.payload.simple.type.SimpleCacheSettings;
import com.jonahseguin.payload.simple.type.SimpleInstantiator;
import org.bukkit.plugin.Plugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 7:24 PM
 */
public class SimpleCacheBuilder<T extends PlayerCacheable> {

    private final Plugin plugin;
    private final SimpleCacheSettings<T> settings;

    public SimpleCacheBuilder(Plugin plugin, SimpleInstantiator<T> instantiator) {
        this.plugin = plugin;
        this.settings = new SimpleCacheSettings<>(instantiator);
    }

    public SimpleCacheBuilder<T> withRemoveOnLogout(boolean removeOnLogout) {
        this.settings.setRemoveOnLogout(removeOnLogout);
        return this;
    }

    public SimpleCacheBuilder<T> withCleanupCheckIntervalMinutes(int cleanupCheckIntervalMinutes) {
        this.settings.setCacheCleanupCheckIntervalMinutes(cleanupCheckIntervalMinutes);
        return this;
    }

    public SimpleCacheBuilder<T> withExpiryMinutesAfterLogout(int expiryMinutesAfterLogout) {
        this.settings.setExpiryMinutesAfterLogout(expiryMinutesAfterLogout);
        return this;
    }

    public PayloadSimpleCache<T> build() {
        return new PayloadSimpleCache<>(plugin, settings);
    }


}
