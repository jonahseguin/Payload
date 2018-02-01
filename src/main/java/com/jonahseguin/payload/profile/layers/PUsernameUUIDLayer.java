package com.jonahseguin.payload.profile.layers;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.profile.ProfilePassable;
import com.jonahseguin.payload.profile.type.PCacheSource;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PUsernameUUIDLayer<X extends PayloadProfile> extends ProfileCacheLayer<X, ProfilePassable, ProfilePassable> {

    private final BiMap<String, String> usernameCache = HashBiMap.create(); // <UniqueID, Username>

    public PUsernameUUIDLayer(PayloadProfileCache<X> cache, CacheDatabase database) {
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
                return PUsernameUUIDLayer.this.getUsername(uniqueId.toLowerCase());
            }

            @Override
            public String getUniqueId() {
                return uniqueId.toLowerCase();
            }

            @Override
            public String getLoginIp() {
                return null;
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
    public PCacheSource source() {
        return PCacheSource.USERNAME_UUID;
    }
}
