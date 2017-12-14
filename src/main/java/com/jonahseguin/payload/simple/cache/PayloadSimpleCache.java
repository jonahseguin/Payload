package com.jonahseguin.payload.simple.cache;

import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.simple.obj.SimpleCacheable;
import com.jonahseguin.payload.simple.type.SimpleCacheSettings;
import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;

@Getter
public class PayloadSimpleCache<X extends SimpleCacheable> {

    private final String cacheID = UUID.randomUUID().toString();
    private final Plugin plugin;
    private final SimpleCacheSettings settings;
    private final CacheDebugger debugger;

    public PayloadSimpleCache(Plugin plugin, SimpleCacheSettings settings) {
        this.plugin = plugin;
        this.settings = settings;
        this.debugger = settings.getDebugger();
    }
}
