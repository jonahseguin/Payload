package com.jonahseguin.payload;

import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.Set;

public class PayloadHook {

    private final Plugin plugin;
    private final Set<String> cacheIDs = new HashSet<>();

    protected PayloadHook(Plugin plugin) {
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    @SuppressWarnings("unchecked")
    public <X extends Payload> ProfileCache<X> createProfileCache(PayloadDatabase database, String name, Class<X> type) {
        ProfileCache<X> cache = database.loadCache(ProfileCache.class, name);
        if (cache == null) {
            cache = new ProfileCache<>(this, name, type);
        }
        database.hookCache(cache);
        this.cacheIDs.add(cache.getCacheId());
        PayloadAPI.get().saveCache(cache, this);
        return cache;
    }

    public final boolean isValid() {
        return PayloadAPI.get().validateHook(this.plugin, this);
    }

    public Set<String> getCacheIDs() {
        return this.cacheIDs;
    }
}
