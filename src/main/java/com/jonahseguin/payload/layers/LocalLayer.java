package com.jonahseguin.payload.layers;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.CachedProfile;
import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.CacheSource;
import com.jonahseguin.payload.type.CacheStage;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
@Setter
public class LocalLayer<T extends Profile> extends CacheLayer<T, T, CachingProfile<T>> {

    private final ConcurrentMap<String, CachedProfile<T>> localCache = new ConcurrentHashMap<>();
    private int cacheExpiryMinutes = 30;

    public LocalLayer(ProfileCache cache, CacheDatabase database) {
        super(cache, database);
    }

    @Override
    public T provide(CachingProfile<T> cachingProfile) {
        try {
            cachingProfile.setLoadingSource(this.source());
            cachingProfile.setStage(CacheStage.LOADED);
            return get(cachingProfile.getUniqueId());
        } catch (Exception ex) {
            return getFailureHandler().providerException(this, cachingProfile, ex);
        }
    }

    @Override
    public boolean save(T profile) {
        try {
            Validate.notNull(profile);
            CachedProfile<T> cachedProfile = new CachedProfile<>(profile, System.currentTimeMillis(), getNewCacheExpiry());
            this.localCache.put(profile.getUniqueId(), cachedProfile);
            return true;
        } catch (NullPointerException ex) {
            getCache().getDebugger().error(ex, "Profile was null while saving to Local Cache: " + profile.getName());
            return false;
        } catch (Exception ex) {
            getCache().getDebugger().error(ex, "An exception occurred while saving profile to the Local Cache: " + profile.getName());
            return false;
        }
    }

    @Override
    public T get(String uniqueId) {
        return localCache.get(uniqueId).getProfile();
    }

    @Override
    public boolean has(String uniqueId) {
        return localCache.containsKey(uniqueId);
    }

    @Override
    public boolean remove(String uniqueId) {
        return localCache.remove(uniqueId) != null;
    }

    @Override
    public boolean init() {
        try {
            this.cacheExpiryMinutes = getCache().getSettings().getCacheLocalExpiryMinutes();
            return true;
        } catch (NullPointerException ex) {
            // If config is not properly init'd
            getCache().getDebugger().error(ex, "Error while trying to init Local Cache Layer: null while getting cache expiry");
            return false;
        }
    }

    @Override
    public boolean shutdown() {
        clear();
        return true;
    }

    @Override
    public int cleanup() {
        return 0; // TODO
    }

    @Override
    public int clear() {
        int sizeBeforeClear = 0;
        localCache.clear();
        return sizeBeforeClear;
    }

    @Override
    public CacheSource source() {
        return CacheSource.LOCAL;
    }

    private long getNewCacheExpiry() {
        return System.currentTimeMillis() + (1000 * 60 * cacheExpiryMinutes);
    }

}
