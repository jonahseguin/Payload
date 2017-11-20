package com.jonahseguin.payload.layers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;
import com.jonahseguin.payload.type.CacheSource;

public class UsernameUUIDLayer<X extends Profile> extends CacheLayer<X, ProfilePassable, ProfilePassable> {

    private final BiMap<String, String> usernameCache = HashBiMap.create(); // <UniqueID, Username>

    public UsernameUUIDLayer(ProfileCache<X> cache, CacheDatabase database) {
        super(cache, database);
    }

    public String getUniqueId(String username) {
        return usernameCache.inverse().get(username.toLowerCase());
    }

    public String getUsername(String uniqueId) {
        return usernameCache.get(uniqueId.toLowerCase());
    }

    @Override
    public ProfilePassable provide(ProfilePassable profilePassable) {
        save(profilePassable);
        return profilePassable;
    }

    @Override
    public boolean save(ProfilePassable profilePassable) {
        this.usernameCache.put(profilePassable.getUniqueId().toLowerCase(), profilePassable.getName().toLowerCase());
        return true;
    }

    @Override
    public ProfilePassable get(String uniqueId) {
        return new ProfilePassable() {
            @Override
            public String getName() {
                return UsernameUUIDLayer.this.getUsername(uniqueId.toLowerCase());
            }

            @Override
            public String getUniqueId() {
                return uniqueId.toLowerCase();
            }
        };
    }

    @Override
    public boolean has(String uniqueId) {
        return usernameCache.containsKey(uniqueId.toLowerCase());
    }

    public boolean hasUniqueId(String username) {
        return usernameCache.inverse().containsKey(username.toLowerCase());
    }

    @Override
    public boolean remove(String uniqueId) {
        return usernameCache.remove(uniqueId.toLowerCase()) != null;
    }

    @Override
    public boolean init() {
        // No initialization required
        return true;
    }

    @Override
    public boolean shutdown() {
        this.usernameCache.clear();
        return true;
    }

    @Override
    public int cleanup() {
        // Strings aren't going to be too memory intensive, but we'll attempt to find players who have been logged out
        // for a while, and remove them from the provide
        // TODO
        // TODO: Remove any caches for players that are not online AND remove players when they logout.
        // To prevent issues caused with UUIDs when name changes happen.
        return 0;
    }

    @Override
    public int clear() {
        int sizeBefore = this.usernameCache.size();
        this.usernameCache.clear();
        return sizeBefore;
    }

    @Override
    public CacheSource source() {
        return CacheSource.USERNAME_UUID;
    }
}
