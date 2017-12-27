package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.event.PayloadProfilePreSaveEvent;
import com.jonahseguin.payload.profile.event.PayloadProfileSavedEvent;
import com.jonahseguin.payload.profile.profile.CachedProfile;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.profile.type.PCacheStage;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
@Setter
public class PLocalLayer<T extends PayloadProfile> extends ProfileCacheLayer<T, T, CachingProfile<T>> {

    private final ConcurrentMap<String, CachedProfile<T>> localCache = new ConcurrentHashMap<>();
    private int cacheExpiryMinutes = 30;

    public PLocalLayer(PayloadProfileCache<T> cache, CacheDatabase database) {
        super(cache, database);
    }

    @Override
    public T provide(CachingProfile<T> cachingProfile) {
        try {
            cachingProfile.setLoadingSource(this.source());
            cachingProfile.setStage(PCacheStage.LOADED);
            return get(cachingProfile.getUniqueId());
        } catch (Exception ex) {
            return getFailureHandler().providerException(this, cachingProfile, ex);
        }
    }

    @Override
    public boolean save(T profile) {
        try {
            Validate.notNull(profile);

            // Call Pre-Save Event
            PayloadProfilePreSaveEvent<T> preSaveEvent = new PayloadProfilePreSaveEvent<>(profile, getCache(), source());
            getPlugin().getServer().getPluginManager().callEvent(preSaveEvent);
            profile = preSaveEvent.getProfile();

            CachedProfile<T> cachedProfile = new CachedProfile<>(profile, System.currentTimeMillis(), getNewCacheExpiry(), 0);
            this.localCache.put(profile.getUniqueId(), cachedProfile);

            // Call Saved Event
            PayloadProfileSavedEvent<T> savedEvent = new PayloadProfileSavedEvent<>(profile, getCache(), source());
            getPlugin().getServer().getPluginManager().callEvent(savedEvent);

            return true;
        } catch (NullPointerException ex) {
            getCache().getDebugger().error(ex, "PayloadProfile was null while saving to Local ProfileCache: " + profile.getName());
            return false;
        } catch (Exception ex) {
            getCache().getDebugger().error(ex, "An exception occurred while saving profile to the Local ProfileCache: " + profile.getName());
            return false;
        }
    }

    @Override
    public T get(String uniqueId) {
        if (localCache.containsKey(uniqueId)) {
            return localCache.get(uniqueId).getProfile();
        }
        return null;
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
            getCache().getDebugger().error(ex, "Error while trying to init Local ProfileCache Layer: null while getting cache expiry");
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
        Set<String> toRemove = new HashSet<>();
        for (String key : localCache.keySet()) {
            CachedProfile<T> cachedProfile = localCache.get(key);
            if (cachedProfile != null) {
                if (!cachedProfile.isOnline()) {
                    if (cachedProfile.isExpired()) {
                        toRemove.add(key);
                    }
                } else {
                    cachedProfile.setExpiry(getNewCacheExpiry()); // Update expiry if they're still online
                }
            } else {
                toRemove.add(key);
            }
        }
        toRemove.forEach(localCache::remove);
        return toRemove.size();
    }

    @Override
    public int clear() {
        int sizeBeforeClear = 0;
        localCache.clear();
        return sizeBeforeClear;
    }

    @Override
    public PCacheSource source() {
        return PCacheSource.LOCAL;
    }

    private long getNewCacheExpiry() {
        return System.currentTimeMillis() + (1000 * 60 * cacheExpiryMinutes);
    }

}
