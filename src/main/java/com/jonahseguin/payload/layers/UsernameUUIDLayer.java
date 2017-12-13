package com.jonahseguin.payload.layers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;
import com.jonahseguin.payload.type.CacheSource;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

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
        Set<String> toRemove = new HashSet<>();
        for (String uuid : usernameCache.keySet()) {
            String username = usernameCache.get(uuid);
            if (username != null) {
                Player player = Bukkit.getPlayerExact(username); // Case insensitive
                if (player == null || !player.isOnline()) {
                    toRemove.add(uuid);
                }
            } else {
                toRemove.add(uuid);
            }
        }
        toRemove.forEach(usernameCache::remove);
        return toRemove.size();
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
