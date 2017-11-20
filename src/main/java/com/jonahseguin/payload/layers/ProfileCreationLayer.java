package com.jonahseguin.payload.layers;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.exception.CachingException;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;
import com.jonahseguin.payload.type.CacheSource;

public class ProfileCreationLayer<T extends Profile> extends CacheLayer<T, T, ProfilePassable> {

    private final ProfileCache<T> profileCache;

    public ProfileCreationLayer(ProfileCache<T> cache, CacheDatabase database) {
        super(cache, database);
        this.profileCache = cache;
    }

    @Override
    public T provide(ProfilePassable passable) {
        try {
            return createNewProfile(passable.getName(), passable.getUniqueId());
        }
        catch (Exception ex) {
            profileCache.getDebugger().error(new CachingException("Could not create a new profile for player " + passable.getName()));
            return null;
        }
    }

    @Override
    public T get(String uniqueId) {
        throw new UnsupportedOperationException("Cannot get Profile from creation layer");
    }

    @Override
    public boolean save(T profilePassable) {
        throw new UnsupportedOperationException("Cannot save Profile to creation layer");
    }

    @Override
    public boolean has(String uniqueId) {
        return true;
    }

    @Override
    public boolean remove(String uniqueId) {
        throw new UnsupportedOperationException("Cannot remove Profile from creation layer");
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
    public CacheSource source() {
        return CacheSource.NEW_PROFILE;
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
