package com.jonahseguin.payload.profile.type;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.common.util.DefaultPayloadSettings;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.profile.ProfileInstantiator;
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
public class ProfileCacheSettings<T extends PayloadProfile> {

    private final JavaPlugin plugin;
    private CacheDatabase database = new DefaultPayloadSettings.EmptyCacheDatabase();
    private CacheDebugger debugger = new DefaultPayloadSettings.EmptyDebugger();
    private Class<T> profileClass = null;
    private ProfileInstantiator<T> profileInstantiator = null;

    private int cacheLocalExpiryMinutes = 60; // Time after logout a profile is removed from local cache
    private boolean cacheLogoutSaveDatabase = false; // Whether to save a profile to the database (mongo) when logging out
    private boolean cacheRemoveOnLogout = false; // Whether to remove a profile from the local cache when logging out
    private boolean enableHaltListener = true; // Whether to register the halt listener that prevents obj actions while halted
    private int cacheFailRetryIntervalSeconds = 30; // How frequently (seconds) to attempt to load failed profile
    private String redisKeyPrefix = "payload"; // Ensure this is changed if using Payload on the same database with multiple plugins!
    private boolean enableAsyncCaching = true; // Whether to Async. caching in a new thread from the AsyncPlayerPreLoginEvent... i.e allow them to login before being fully cached

}
