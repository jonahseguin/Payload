package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.exception.CachingException;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.profile.ProfilePassable;
import com.jonahseguin.payload.profile.type.PCacheSource;

public class ProfileCreationLayer<T extends PayloadProfile> extends ProfileCacheLayer<T, T, ProfilePassable> {

    private final PayloadProfileCache<T> profileCache;

    public ProfileCreationLayer(PayloadProfileCache<T> cache, CacheDatabase database) {
        super(cache, database);
        this.profileCache = cache;
    }

    @Override
    public T provide(ProfilePassable passable) {
        try {
            return createNewProfile(passable.getName(), passable.getUniqueId());
        }
        catch (Exception ex) {
            profileCache.getDebugger().error(new CachingException("Could not create a new profile for obj " + passable.getName()));
            return null;
        }
    }

    @Override
    public T get(String uniqueId) {
        throw new UnsupportedOperationException("Cannot get PayloadProfile from creation layer");
    }

    @Override
    public boolean save(T profilePassable) {
        throw new UnsupportedOperationException("Cannot save PayloadProfile to creation layer");
    }

    @Override
    public boolean has(String uniqueId) {
        return true;
    }

    @Override
    public boolean remove(String uniqueId) {
        throw new UnsupportedOperationException("Cannot remove PayloadProfile from creation layer");
    }

    @Override
    public boolean init() {
        return true;
    }

    @Override
    public boolean shutdown() {
        return true;
    }

    @Override
    public PCacheSource source() {
        return PCacheSource.NEW_PROFILE;
    }

    @Override
    public int cleanup() {
        return 0;
    }

    @Override
    public int clear() {
        return 0;
    }

    public T createNewProfile(String name, String uuid) {
        return profileCache.getSettings().getProfileInstantiator().instantiate(name, uuid);
    }

}
