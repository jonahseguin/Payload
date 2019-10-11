/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadCallback;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.jonahseguin.payload.server.PayloadServer;
import com.mongodb.BasicDBObject;
import lombok.Getter;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class SyncManager<K, X extends Payload<K>, D extends PayloadData> {

    private final PayloadCache<K, X, D> cache;
    private Jedis jedisSubscriber = null;
    private SyncSubscriber<K, X, D> subscriber = null;

    private final ConcurrentMap<String, PayloadCallback<X>> requestedSaves = new ConcurrentHashMap<>();
    private final Set<PayloadCallback<PayloadServer>> requestedSaveAll = new HashSet<>();

    public SyncManager(PayloadCache<K, X, D> cache) {
        this.cache = cache;
    }

    public void startup() {
        this.cache.getPool().submit(() -> {
            this.subscriber = new SyncSubscriber<>(this);
            this.jedisSubscriber = this.cache.getPayloadDatabase().getResource();
            this.jedisSubscriber.subscribe(this.subscriber, this.getChannelName());
        });
    }

    public void shutdown() {
        if (this.subscriber != null) {
            if (this.subscriber.isSubscribed()) {
                this.subscriber.unsubscribe();
            }
        }
        if (this.jedisSubscriber != null) {
            this.jedisSubscriber.close();
            this.jedisSubscriber = null;
        }
    }

    public void prepareSaveAll(Runnable callback) {
        this.requestedSaveAll.add(new PayloadCallback<PayloadServer>() {
            Set<PayloadServer> done = new HashSet<>();

            @Override
            public void callback(PayloadServer object) {
                done.add(object);
                for (PayloadServer s : cache.getPayloadDatabase().getServerManager().getServers().values()) {
                    if (!done.contains(s)) {
                        return;
                    }
                }
                callback.run();
            }
        });
        this.publishRequestSaveAll();
    }

    public void prepareUpdate(X payload, PayloadCallback<X> callback) {
        if (payload.shouldPrepareUpdate()) {
            this.requestedSaves.put(payload.getIdentifier().toString(), callback);
            this.publishRequestSave(payload.getIdentifier());
        } else {
            callback.callback(payload);
        }
    }

    public void publishRequestSave(K key) {
        this.publish(key.toString(), SyncEvent.REQUEST_SAVE, key.toString());
    }

    public void publishSaveCompleted(K key) {
        this.publish(key.toString(), SyncEvent.SAVE_COMPLETED, key.toString());
    }

    public void publishRequestSaveAll() {
        this.publish("", SyncEvent.REQUEST_SAVE_ALL, "ALL");
    }

    public void publishSaveAllCompleted() {
        this.publish("", SyncEvent.SAVE_ALL_COMPLETED, "ALL");
    }

    public void publishUpdate(X payload) {
        this.publish(payload.getIdentifier().toString(), SyncEvent.UPDATE, payload.getIdentifier().toString());
    }

    public void publishUncache(K key) {
        this.publish(key.toString(), SyncEvent.UNCACHE, key.toString());
    }

    private void publish(String dataString, SyncEvent event, String identifierName) {
        BasicDBObject data = new BasicDBObject();
        data.append("source", PayloadAPI.get().getPayloadID());
        data.append("data", dataString);
        data.append("cache", this.cache.getName());
        data.append("event", event.getName());
        cache.getPool().submit(() -> {
            try (Jedis jedis = cache.getPayloadDatabase().getResource()) {
                jedis.publish(this.getChannelName(), data.toJson());
                this.cache.getErrorHandler().debug(this.cache, "Sync: Published " + event.getName() + " for Payload: " + identifierName);
            } catch (Exception ex) {
                this.cache.getErrorHandler().exception(this.cache, ex, "Sync: Failed to publish " + event.getName() + " for Payload: " + identifierName);
            }
        });
    }

    public void receiveSync(BasicDBObject object) {
        try {
            final SyncEvent event = SyncEvent.fromString(object.getString("event"));
            if (event != null) {
                final PayloadServer server = this.cache.getPayloadDatabase().getServerManager().getServer(object.getString("source"));
                final K key = this.cache.keyFromString(object.getString("data"));
                if (server != null && key != null) {
                    if (event.equals(SyncEvent.UPDATE)) {
                        if ((this.cache.getSyncMode().equals(SyncMode.UPDATE) && this.cache.isCached(key)) || this.cache.getSyncMode().equals(SyncMode.CACHE_ALL)) {
                            this.cache.getPool().submit(() -> {
                                X payload = this.cache.getFromDatabase(key);
                                if (payload != null) {
                                    if (!cache.getSettings().isServerSpecific() || payload.getPayloadServer().equalsIgnoreCase(PayloadAPI.get().getPayloadID())) {
                                        this.cache.cache(payload);
                                        this.cache.getErrorHandler().debug(this.cache, "Sync: Updated payload " + payload.getIdentifier());
                                    }
                                } else {
                                    this.cache.getErrorHandler().error(this.cache, "Sync: Payload was null when fetching update for identifier: " + key.toString());
                                }
                            });
                        }
                    } else if (event.equals(SyncEvent.UNCACHE)) {
                        this.cache.uncacheLocal(key);
                    } else if (event.equals(SyncEvent.REQUEST_SAVE)) {
                        if (this.cache.isCached(key)) {
                            X payload = this.cache.getFromCache(key);
                            if (payload.shouldSave()) {
                                this.cache.save(cache.getFromCache(key));
                                this.publishSaveCompleted(key);
                            }
                        }
                    } else if (event.equals(SyncEvent.SAVE_COMPLETED)) {
                        if (this.requestedSaves.containsKey(key.toString())) {
                            this.cache.getPool().submit(() -> {
                                X x = this.cache.getFromDatabase(key);
                                if (x != null) {
                                    this.cache.cache(x);
                                    this.requestedSaves.get(key.toString()).callback(x);
                                    this.requestedSaves.remove(key.toString());
                                } else {
                                    this.cache.getErrorHandler().error(this.cache, "Sync: Payload is null after loading from database after save completed for prepareUpdate");
                                }
                            });
                        }
                    } else if (event.equals(SyncEvent.REQUEST_SAVE_ALL)) {
                        this.cache.getPool().submit(() -> {
                            this.cache.saveAll();
                            this.publishSaveAllCompleted();
                        });
                    } else if (event.equals(SyncEvent.SAVE_ALL_COMPLETED)) {
                        this.requestedSaveAll.forEach(s -> s.callback(server));
                    } else {
                        this.cache.getErrorHandler().error(this.cache, "Sync: Unknown event received: " + event.getName());
                    }
                }
            } else {
                this.cache.getErrorHandler().error(this.cache, "Sync: Unknown event received: " + object.getString("event"));
            }
        }
        catch (Exception ex) {
            this.cache.getErrorHandler().exception(this.cache, ex, "Sync: Error mapping Payload type " + this.cache.getPayloadClass().getSimpleName() + " from json object from sync service");
        }
    }

    public String getChannelName() {
        return "payload-sync-" + cache.getName();
    }

}
