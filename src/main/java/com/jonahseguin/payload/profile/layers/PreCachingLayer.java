package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.Profile;
import com.jonahseguin.payload.profile.profile.ProfilePassable;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PreCachingLayer<X extends Profile> extends ProfileCacheLayer<X, CachingProfile<X>, ProfilePassable> {

    private final Map<String, CachingProfile<X>> cachingProfiles = new HashMap<>(); // <UUID, CachingProfile>

    public PreCachingLayer(PayloadProfileCache<X> cache, CacheDatabase database) {
        super(cache, database);
    }

    @Override
    public CachingProfile<X> provide(ProfilePassable profilePassable) {
        CachingProfile<X> cachingProfile = new CachingProfile<>(getCache(), profilePassable.getName(), profilePassable.getUniqueId(),
                System.currentTimeMillis());
        cachingProfile.setStage(PCacheStage.INIT);
        cachingProfile.setLoadingSource(PCacheSource.PRE_CACHING);
        save(cachingProfile);
        return cachingProfile;
    }

    @Override
    public CachingProfile<X> get(String uniqueId) {
        return cachingProfiles.get(uniqueId);
    }

    @Override
    public boolean save(CachingProfile<X> cachingProfile) {
        cachingProfiles.put(cachingProfile.getUniqueId(), cachingProfile);
        return true;
    }

    @Override
    public boolean has(String uniqueId) {
        return cachingProfiles.containsKey(uniqueId);
    }

    @Override
    public boolean remove(String uniqueId) {
        return cachingProfiles.remove(uniqueId) != null;
    }

    @Override
    public boolean init() {
        // No init necessary
        return true;
    }

    @Override
    public boolean shutdown() {
        this.cachingProfiles.clear();
        return true;
    }

    @Override
    public PCacheSource source() {
        return PCacheSource.PRE_CACHING;
    }

    @Override
    public int cleanup() {
        Set<String> toRemove = new HashSet<>();
        for (String key : cachingProfiles.keySet()) {
            CachingProfile<X> cachingProfile = cachingProfiles.get(key);
            if (cachingProfile != null) {
                if (cachingProfile.getStage() == PCacheStage.DONE || cachingProfile.getStage() == PCacheStage.LOADED) {
                    // Is done
                    Player player = cachingProfile.tryToGetPlayer();
                    if (player == null || !player.isOnline()) {
                        toRemove.add(key);
                    }
                }
            } else {
                toRemove.add(key);
            }
        }
        toRemove.forEach(cachingProfiles::remove);
        return toRemove.size();
    }

    @Override
    public int clear() {
        int sizeBeforeClear = this.cachingProfiles.size();
        this.cachingProfiles.clear();
        return sizeBeforeClear;
    }
}
