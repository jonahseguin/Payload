package com.jonahseguin.payload.util;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.CacheDebugger;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfileInstantiator;
import com.jonahseguin.payload.type.CacheSettings;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 7:24 PM
 */
public class CacheBuilder<T extends Profile> {

    private final JavaPlugin javaPlugin;
    private final CacheSettings<T> settings;
    private Class<T> profileClass;

    public CacheBuilder(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        this.settings = new CacheSettings<>(javaPlugin);
    }

    public CacheBuilder<T> withProfileClass(Class<T> clazz) {
        this.profileClass = clazz;
        return this;
    }

    public CacheBuilder<T> withDatabase(CacheDatabase cacheDatabase) {
        this.settings.setDatabase(cacheDatabase);
        return this;
    }

    public CacheBuilder<T> withDebugger(CacheDebugger debugger) {
        this.settings.setDebugger(debugger);
        return this;
    }

    public CacheBuilder<T> withInstantiator(ProfileInstantiator<T> instantiator) {
        this.settings.setProfileInstantiator(instantiator);
        return this;
    }

    public CacheBuilder<T> withCacheLocalExpiryMinutes(int minutes) {
        this.settings.setCacheLocalExpiryMinutes(minutes);
        return this;
    }

    public CacheBuilder<T> withCacheRemoveOnLogout(boolean removeLogout) {
        this.settings.setCacheRemoveOnLogout(removeLogout);
        return this;
    }

    public CacheBuilder<T> withHaltListenerEnabled(boolean enabled) {
        this.settings.setEnableHaltListener(enabled);
        return this;
    }

    public CacheBuilder<T> withCacheLogoutSaveDatabase(boolean saveLogout) {
        this.settings.setCacheLogoutSaveDatabase(saveLogout);
        return this;
    }

    public CacheBuilder<T> withCacheFailRetryIntervalSeconds(int seconds) {
        this.settings.setCacheFailRetryIntervalSeconds(seconds);
        return this;
    }

    public ProfileCache<T> build() {
        return new ProfileCache<>(settings, profileClass);
    }


}
