package com.jonahseguin.payload.simple.cache;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jonahseguin.payload.simple.event.PlayerCacheListener;
import com.jonahseguin.payload.simple.simple.PlayerCacheable;
import com.jonahseguin.payload.simple.task.SimpleCacheCleanupTask;
import com.jonahseguin.payload.simple.type.SimpleCacheSettings;
import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by Jonah on 12/17/2017.
 * Project: Payload
 *
 * @ 5:27 PM
 */
@Getter
public class PayloadSimpleCache<X extends PlayerCacheable> {

    private final String cacheID = UUID.randomUUID().toString();
    private final Plugin plugin;
    private final SimpleCacheSettings<X> settings;
    private final BiMap<String, String> usernameUUIDCache = HashBiMap.create(); // <Username, UUID>
    private final ConcurrentMap<String, X> cache = new ConcurrentHashMap<>(); // <UUID, PlayerCacheable>
    private final Map<String, Long> expiry = new HashMap<>();
    private final PlayerCacheListener<X> listener;
    private final SimpleCacheCleanupTask<X> cleanupTask;

    public PayloadSimpleCache(Plugin plugin, SimpleCacheSettings<X> settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.listener = new PlayerCacheListener<>(this);
        this.cleanupTask = new SimpleCacheCleanupTask<>(this);
    }

    public X cache(Player player) {
        Validate.notNull(player);
        usernameUUIDCache.put(player.getName().toLowerCase(), player.getUniqueId().toString());
        if (cache.containsKey(player.getUniqueId().toString())) {
            return cache.get(player.getUniqueId().toString());
        } else {
            X x = settings.getInstantiator().instantiate(player);
            cache.put(player.getUniqueId().toString(), x);
            return x;
        }
    }

    public void removeFromCache(Player player) {
        if (has(player)) {
            cache.remove(player.getUniqueId().toString());
        }
    }

    public void removeFromCache(String uuid) {
        if (cache.containsKey(uuid)) {
            cache.remove(uuid);
        }
    }

    public void initExpiry(Player player) {
        if (has(player)) {
            expiry.put(player.getUniqueId().toString(),
                    (System.currentTimeMillis() + (settings.getExpiryMinutesAfterLogout() * 60 * 1000)));
        }
    }

    public X get(Player player) {
        if (cache.containsKey(player.getUniqueId().toString())) {
            return cache.get(player.getUniqueId().toString());
        }
        return null;
    }

    public boolean has(Player player) {
        return cache.containsKey(player.getUniqueId().toString());
    }

    public X getByUsername(String username) {
        if (usernameUUIDCache.containsKey(username.toLowerCase())) {
            String uuid = usernameUUIDCache.get(username.toLowerCase());
            return getByUUID(uuid);
        } else {
            Player player = Bukkit.getPlayerExact(username);
            if (player != null) {
                return get(player);
            }
        }
        return null;
    }

    public X getByUUID(String uniqueId) {
        if (cache.containsKey(uniqueId)) {
            return cache.get(uniqueId);
        }
        return null;
    }

    public final void init() {
        // Register events, etc.
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        cleanupTask.start();
    }

    public final void shutdown() {
        HandlerList.unregisterAll(listener);
        usernameUUIDCache.clear();
        cache.clear();
        cleanupTask.stop();
    }

}
