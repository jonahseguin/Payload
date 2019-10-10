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
        this.publish(object.toJson(), SyncEvent.UPDATE, payload.getIdentifier().toString());
    }

    public void publishUncache(K key) {
        this.publish(key.toString(), SyncEvent.UNCACHE, key.toString());
    }

    private void publish(String dataString, SyncEvent event, String identifierName) {
        BasicDBObject data = new BasicDBObject();
        data.append("source", PayloadAPI.get().getPayloadID());
        data.append("data", dataString);
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
            SyncEvent event = SyncEvent.fromString(object.getString("event"));
            if (event != null) {
                if (event.equals(SyncEvent.UPDATE)) {
                    BasicDBObject data = BasicDBObject.parse(object.getString("data"));
                    if (data != null) {
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
                } else if (event.equals(SyncEvent.UNCACHE)) {
                    K key = this.cache.keyFromString(object.getString("data"));
                    this.cache.uncache(key);
                } else {
                    this.cache.getErrorHandler().error(this.cache, "Sync: Unknown event received: " + event.getName());
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
