package com.jonahseguin.payload.type;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.CacheDebugger;
import com.jonahseguin.payload.profile.ProfileInstantiator;
import com.jonahseguin.payload.profile.Profile;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jonah on 11/14/2017.
 * Project: Payload
 *
 * @ 9:36 PM
 */
@AllArgsConstructor
@Getter
@Setter
@RequiredArgsConstructor
public class CacheSettings<T extends Profile> {

    private final JavaPlugin plugin;
    private CacheDatabase database = new DefaultSettings.EmptyCacheDatabase();
    private CacheDebugger debugger = new DefaultSettings.EmptyDebugger();
    private Class<T> profileClass = null;
    private ProfileInstantiator<T> profileInstantiator = null;

    private int cacheLocalExpiryMinutes = 60; // Time after logout a profile is removed from local cache
    private boolean cacheLogoutSaveDatabase = false; // Whether to save a profile to the database (mongo) when logging out
    private boolean cacheRemoveOnLogout = false; // Whether to remove a profile from the local cache when logging out

}
