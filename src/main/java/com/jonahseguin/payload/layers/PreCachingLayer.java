package com.jonahseguin.payload.layers;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;

import java.util.HashMap;
import java.util.Map;

public class PreCachingLayer<X extends Profile> extends CacheLayer<X, CachingProfile<X>, ProfilePassable> {

    private final Map<String, CachingProfile<X>> cachingProfiles = new HashMap<>(); // <UUID, CachingProfile>

    public PreCachingLayer(ProfileCache<X> cache, CacheDatabase database) {
        super(cache, database);
    }

    @Override
    public CachingProfile<X> provide(ProfilePassable profilePassable) {
        CachingProfile<X> cachingProfile = new CachingProfile<>(getCache(), profilePassable.getName(), profilePassable.getUniqueId(),
                System.currentTimeMillis());
        cachingProfile.setStage(CacheStage.INIT);
        cachingProfile.setLoadingSource(CacheSource.PRE_CACHING);
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
    public CacheSource source() {
        return CacheSource.PRE_CACHING;
    }

    @Override
    public int cleanup() {
        // TODO: Remove profiles that are already cached.
        return 0;
    }

    @Override
    public int clear() {
        int sizeBeforeClear = this.cachingProfiles.size();
        this.cachingProfiles.clear();
        return sizeBeforeClear;
    }
}
