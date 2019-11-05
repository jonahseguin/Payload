/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.common.base.Preconditions;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.network.NetworkPayload;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.database.DatabaseModule;
import com.jonahseguin.payload.database.DatabaseService;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
@Singleton
public class PayloadAPI {

    private final PayloadPlugin plugin;
    private final ConcurrentMap<String, PayloadCache> caches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, DatabaseService> databases = new ConcurrentHashMap<>();
    private final Set<String> requested = new HashSet<>();

    private List<PayloadCache> _sortedCaches = null;
    private List<PayloadCache> _sortedCachesReversed = null;

    PayloadAPI(PayloadPlugin plugin) {
        this.plugin = plugin;
    }

    public static PayloadModule install(@Nonnull Plugin plugin, @Nonnull DatabaseModule databaseModule) {
        Preconditions.checkNotNull(plugin);
        Preconditions.checkNotNull(databaseModule);
        return new PayloadModule(plugin, databaseModule);
    }

    public static PayloadModule install(@Nonnull Plugin plugin, @Nonnull String databaseName) {
        Preconditions.checkNotNull(databaseName);
        return install(plugin, new DatabaseModule(plugin, databaseName));
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
    public final void saveCache(PayloadCache cache) {
        this.caches.putIfAbsent(convertCacheName(cache.getName()), cache);
    }

    public void registerDatabase(DatabaseService database) {
        if (!isDatabaseRegistered(database.getName())) {
            this.databases.putIfAbsent(database.getName().toLowerCase(), database);
        } else {
            throw new IllegalArgumentException("A Payload database with the name '" + database.getName() + "' has already been registered.  Choose a different name.");
        }
    }

    public DatabaseService getDatabase(String name) {
        return this.databases.get(name.toLowerCase());
    }

    public boolean isDatabaseRegistered(String name) {
        return this.databases.containsKey(name.toLowerCase());
    }

    /**
     * Get a cache by name
     * @param name Name of the cache
     * @param <K> Key type (i.e String for uuid)
     * @param <X> Value type (object to cache; i.e Profile)
     * @return The Cache
     */
    @SuppressWarnings("unchecked") // bad, oops
    public <K, X extends Payload<K>, N extends NetworkPayload<K>, D extends PayloadData> PayloadCache<K, X, N, D> getCache(String name) {
        return (PayloadCache<K, X, N, D>) this.caches.get(convertCacheName(name));
    }

    public List<PayloadCache> getSortedCachesByDepends() {
        if (this._sortedCaches != null) {
            if (!this.hasBeenModified()) {
                return this._sortedCaches;
            }
        }
        this._sortedCaches = new ArrayList<>(this.caches.values()).stream().sorted().collect(Collectors.toList());
        Collections.reverse(this._sortedCaches);
        return this._sortedCaches;
    }

    public List<PayloadCache> getSortedCachesByDependsReversed() {
        if (this._sortedCachesReversed != null) {
            if (!this.hasBeenModifiedReversed()) {
                return this._sortedCachesReversed;
            }
        }
        this._sortedCachesReversed = new ArrayList<>(this.caches.values()).stream().sorted().collect(Collectors.toList());
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
            for (PayloadCache cache : getCaches().values()) {
                cache.updatePayloadID();
            }
            for (DatabaseService database : getDatabases().values()) {
                database.getServerService().getPublisher().publishUpdateName(oldName, name);
            }
        } else {
            throw new IllegalArgumentException("Payload ID must be alphanumeric, '" + name + "' is not valid.");
        }
    }

}
