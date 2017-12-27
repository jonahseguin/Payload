package com.jonahseguin.payload.profile.util;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.profile.ProfileInstantiator;
import com.jonahseguin.payload.profile.type.ProfileCacheSettings;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 7:24 PM
 */
public class ProfileCacheBuilder<T extends PayloadProfile> {

    private final JavaPlugin javaPlugin;
    private final ProfileCacheSettings<T> settings;
    private Class<T> profileClass;

    public ProfileCacheBuilder(JavaPlugin javaPlugin) {
        this.javaPlugin = javaPlugin;
        this.settings = new ProfileCacheSettings<>(javaPlugin);
    }

    public ProfileCacheBuilder<T> withProfileClass(Class<T> clazz) {
        this.profileClass = clazz;
        return this;
    }

    public ProfileCacheBuilder<T> withDatabase(CacheDatabase cacheDatabase) {
        this.settings.setDatabase(cacheDatabase);
        return this;
    }

    public ProfileCacheBuilder<T> withEnableAsyncCaching(boolean asyncCaching) {
        this.settings.setEnableAsyncCaching(asyncCaching);
        return this;
    }

    public ProfileCacheBuilder<T> withDebugger(CacheDebugger debugger) {
        this.settings.setDebugger(debugger);
        return this;
    }

    public ProfileCacheBuilder<T> withInstantiator(ProfileInstantiator<T> instantiator) {
        this.settings.setProfileInstantiator(instantiator);
        return this;
    }

    public ProfileCacheBuilder<T> withCacheLocalExpiryMinutes(int minutes) {
        this.settings.setCacheLocalExpiryMinutes(minutes);
        return this;
    }

    public ProfileCacheBuilder<T> withCacheRemoveOnLogout(boolean removeLogout) {
        this.settings.setCacheRemoveOnLogout(removeLogout);
        return this;
    }

    public ProfileCacheBuilder<T> withHaltListenerEnabled(boolean enabled) {
        this.settings.setEnableHaltListener(enabled);
        return this;
    }

    public ProfileCacheBuilder<T> withCacheLogoutSaveDatabase(boolean saveLogout) {
        this.settings.setCacheLogoutSaveDatabase(saveLogout);
        return this;
    }

    public ProfileCacheBuilder<T> withCacheFailRetryIntervalSeconds(int seconds) {
        this.settings.setCacheFailRetryIntervalSeconds(seconds);
        return this;
    }

    public PayloadProfileCache<T> build() {
        return new PayloadProfileCache<>(settings, profileClass);
    }


}
