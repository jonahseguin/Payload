/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.exception.runtime.PayloadProvisionException;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.database.PayloadDatabase;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
@Singleton
public class PayloadAPI {

    private final PayloadPlugin plugin;
    private final ConcurrentMap<String, PayloadCache> caches = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PayloadDatabase> databases = new ConcurrentHashMap<>();
    private final Set<String> requested = new HashSet<>();

    private List<PayloadCache> _sortedCaches = null;
    private List<PayloadCache> _sortedCachesReversed = null;

    @Inject
    PayloadAPI(PayloadPlugin plugin) {
        this.plugin = plugin;
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

    /**
     * Get a cache by name
     * @param name Name of the cache
     * @param <K> Key type (i.e String for uuid)
     * @param <X> Value type (object to cache; i.e Profile)
     * @return The Cache
     */
    @SuppressWarnings("unchecked") // bad, oops
    public <K, X extends Payload<K>, D extends PayloadData> PayloadCache<K, X, D> getCache(String name) {
        return (PayloadCache<K, X, D>) this.caches.get(convertCacheName(name));
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
            for (PayloadDatabase database : getDatabases().values()) {
                database.getServerManager().getPublisher().publishUpdateName(oldName, name);
            }
        } else {
            throw new IllegalArgumentException("Payload ID must be alphanumeric, '" + name + "' is not valid.");
        }
    }

}
