/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import lombok.Getter;
import redis.clients.jedis.Jedis;

@Getter
public class SyncManager<K, X extends Payload<K>, D extends PayloadData> {

    private final PayloadCache<K, X, D> cache;
    private Jedis jedisSubscriber = null;
    private SyncSubscriber<K, X, D> subscriber = null;

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

    public void publishUpdate(X payload) {
        BasicDBObject object = (BasicDBObject) this.cache.getPayloadDatabase().getMorphia().toDBObject(payload);
        BasicDBObject data = new BasicDBObject();
        data.append("source", PayloadAPI.get().getPayloadID());
        data.append("data", object.toJson());
        cache.getPool().submit(() -> {
            try (Jedis jedis = cache.getPayloadDatabase().getResource()) {
                jedis.publish(this.getChannelName(), data.toJson());
                this.cache.getErrorHandler().debug(this.cache, "Sync: Published update for Payload: " + payload.getIdentifier());
            }
            catch (Exception ex) {
                this.cache.getErrorHandler().exception(this.cache, ex, "Sync: Failed to publish update for Payload: " + payload.getIdentifier());
            }
        });
    }

    public void receiveUpdate(BasicDBObject object) {
        try {
            X payload = this.cache.getPayloadDatabase().getMorphia().fromDBObject(this.cache.getPayloadDatabase().getDatastore(), this.cache.getPayloadClass(), object);
            if (payload != null) {
                if ((this.cache.getSyncMode().equals(SyncMode.UPDATE) && this.cache.isCached(payload.getIdentifier())) || this.cache.getSyncMode().equals(SyncMode.CACHE_ALL)) {
                    if (!cache.getSettings().isServerSpecific() || payload.getPayloadServer().equalsIgnoreCase(PayloadAPI.get().getPayloadID())) {
                        this.cache.cache(payload);
                        this.cache.getErrorHandler().debug(this.cache, "Sync: Updated payload " + payload.getIdentifier());
                    }
                }
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
