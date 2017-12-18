package com.jonahseguin.payload.simple.cache;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jonahseguin.payload.simple.simple.PlayerCacheable;
import com.jonahseguin.payload.simple.type.SimpleCacheSettings;
import lombok.Getter;

import java.util.UUID;

import org.bukkit.plugin.Plugin;

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
    private final BiMap<String, String> usernameUUIDCache = HashBiMap.create();

    public PayloadSimpleCache(Plugin plugin, SimpleCacheSettings<X> settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    public final boolean init() {

    }

    public final boolean shutdown() {

    }

}
