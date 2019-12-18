/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.handshake.HandshakeService;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.database.DatabaseModule;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.server.ServerService;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
public class PayloadAPI {

    private final PayloadPlugin plugin;
    private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PayloadDatabase> databases = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ServerService> serverServices = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, HandshakeService> handshakeServices = new ConcurrentHashMap<>();
    private final Set<String> requested = new HashSet<>();

    private List<Cache> _sortedCaches = null;
    private List<Cache> _sortedCachesReversed = null;

    PayloadAPI(PayloadPlugin plugin) {
        this.plugin = plugin;
    }

    public static PayloadModule install(@Nonnull JavaPlugin plugin, @Nonnull DatabaseModule databaseModule) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(databaseModule);
        return new PayloadModule(plugin, databaseModule);
    }

    public static PayloadModule install(@Nonnull JavaPlugin plugin, @Nonnull String databaseName) {
        Preconditions.checkNotNull(databaseName);
        return install(plugin, new DatabaseModule(databaseName));
    }

    /**
     * Get the local unique ID associated with this server's instance of Payload
     * @return String unique ID
     */
    public String getPayloadID() {
        return this.plugin.getLocal().getPayloadID();
    }

    public static String convertCacheName(String name) {
        return name.toLowerCase().replace(" ", "");
    }

    /**
     * Register a cache w/ a hook
     *
     * @param cache {@link PayloadCache}
     */
    public final void saveCache(Cache cache) {
        this.caches.putIfAbsent(convertCacheName(cache.getName()), cache);
    }

    public void registerDatabase(PayloadDatabase database) {
        if (!isDatabaseRegistered(database.getName())) {
            this.databases.putIfAbsent(database.getName().toLowerCase(), database);
        } else {
            throw new IllegalArgumentException("A Payload database with the name '" + database.getName() + "' has already been registered.  Choose a different name.");
        }
    }

    public PayloadDatabase getDatabase(String name) {
        return this.databases.get(name.toLowerCase());
    }

    public boolean isDatabaseRegistered(String name) {
        return this.databases.containsKey(name.toLowerCase());
    }

    public boolean isServerServiceRegistered(String name) {
        return this.serverServices.containsKey(name.toLowerCase());
    }

    public ServerService getServerService(String name) {
        return this.serverServices.get(name.toLowerCase());
    }

    public void registerServerService(ServerService serverService) {
        if (!isServerServiceRegistered(serverService.getName())) {
            this.serverServices.put(serverService.getName(), serverService);
        } else {
            throw new IllegalArgumentException("A Payload Server Service (database) with the name '" + serverService.getName() + "' has already been registered.  Choose a different name.");
        }
    }

    public boolean isHandshakeServiceRegistered(String name) {
        return this.handshakeServices.containsKey(name.toLowerCase());
    }

    public HandshakeService getHandshakeService(String name) {
        return this.handshakeServices.get(name.toLowerCase());
    }

    public void registerHandshakeService(HandshakeService handshakeService) {
        if (!isHandshakeServiceRegistered(handshakeService.getName())) {
            this.handshakeServices.put(handshakeService.getName(), handshakeService);
        } else {
            throw new IllegalArgumentException("A Payload Server Service (database) with the name '" + handshakeService.getName() + "' has already been registered.  Choose a different name.");
        }
    }


    /**
     * Get a cache by name
     * @param name Name of the cache
     * @param <K> Key type (i.e String for uuid)
     * @param <X> Value type (object to cache; i.e Profile)
     * @return The Cache
     */
    @SuppressWarnings("unchecked") // bad, oops
    public <K, X extends Payload<K>, N extends NetworkPayload<K>> Cache<K, X, N> getCache(String name) {
        return (Cache<K, X, N>) this.caches.get(convertCacheName(name));
    }

    public Cache getCacheRaw(String name) {
        return this.caches.get(convertCacheName(name));
    }

    public boolean isCacheRegistered(String name) {
        return this.caches.containsKey(convertCacheName(name));
    }

    public List<Cache> getSortedCachesByDepends() {
        if (this._sortedCaches != null) {
            if (!this.hasBeenModified()) {
                return this._sortedCaches;
            }
        }
        this._sortedCaches = new ArrayList<>(this.caches.values()).stream().sorted().collect(Collectors.toList());
        return this._sortedCaches;
    }

    public List<Cache> getSortedCachesByDependsReversed() {
        if (this._sortedCachesReversed != null) {
            if (!this.hasBeenModifiedReversed()) {
                return this._sortedCachesReversed;
            }
        }
        this._sortedCachesReversed = new ArrayList<>(this.caches.values()).stream().sorted().collect(Collectors.toList());
        Collections.reverse(this._sortedCachesReversed);
        return this._sortedCachesReversed;
    }

    private boolean hasBeenModified() {
        return this.caches.size() != this._sortedCaches.size();
    }

    private boolean hasBeenModifiedReversed() {
        return this.caches.size() != this._sortedCachesReversed.size();
    }

    public void setPayloadID(String name) {
        if (StringUtils.isAlphanumeric(name)) {
            final String oldName = plugin.getLocal().getPayloadID();
            plugin.getLocal().savePayloadID(name);
            for (Cache cache : getCaches().values()) {
                cache.updatePayloadID();
            }
            serverServices.values().forEach(s -> s.getPublisher().publishUpdateName(oldName, name));
        } else {
            throw new IllegalArgumentException("Payload ID must be alphanumeric, '" + name + "' is not valid.");
        }
    }

}
