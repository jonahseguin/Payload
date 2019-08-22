package com.jonahseguin.payload.mode.profile.layer;

import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProfileLayerLocal<X extends PayloadProfile> extends ProfileCacheLayer<X> {

    private final ConcurrentMap<String, X> localCache = new ConcurrentHashMap<>();

    public ProfileLayerLocal(ProfileCache<X> cache) {
        super(cache);
    }

    @Override
    public X get(ProfileData data) throws PayloadLayerCannotProvideException {
        if (!this.has(data)) {
            throw new PayloadLayerCannotProvideException("Cannot provide (does not have) in local layer for Profile username:" + data.getUsername(), this.cache);
        }
        X x = this.localCache.get(data.getUniqueId());
        x.interact();
        return x;
    }

    @Override
    public boolean save(X payload) {
        payload.interact();
        this.localCache.put(payload.getUniqueId(), payload);
        return true;
    }

    @Override
    public boolean has(ProfileData data) {
        return this.localCache.containsKey(data.getUniqueId());
    }

    @Override
    public boolean has(X payload) {
        payload.interact();
        return this.localCache.containsKey(payload.getUniqueId());
    }

    @Override
    public void remove(ProfileData data) {
        this.localCache.remove(data.getUniqueId());
    }

    @Override
    public void remove(X payload) {
        this.localCache.remove(payload.getUniqueId());
    }

    @Override
    public int cleanup() {
        return 0; // TODO: If last time a profile was interacted with exceeds a configurable time, remove the object
    }

    @Override
    public int clear() {
        int i = this.localCache.size();
        this.localCache.clear();
        return i;
    }

    @Override
    public void init() {

    }

    @Override
    public void shutdown() {
        this.clear(); // For memory purposes
    }
}
