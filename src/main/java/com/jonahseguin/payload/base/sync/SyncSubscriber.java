/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.sync;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import com.mongodb.BasicDBObject;
import redis.clients.jedis.JedisPubSub;

public class SyncSubscriber<K, X extends Payload<K>, D extends PayloadData> extends JedisPubSub {

    private final SyncManager<K, X, D> manager;

    public SyncSubscriber(SyncManager<K, X, D> manager) {
        this.manager = manager;
    }

    @Override
    public void onMessage(String channel, String json) {
        if (channel.equalsIgnoreCase(manager.getChannelName())) {
            try {
                BasicDBObject object = BasicDBObject.parse(json);
                if (object != null) {
                    String source = object.getString("source");
                    if (!source.equalsIgnoreCase(PayloadAPI.get().getPayloadID())) {
                        BasicDBObject data = BasicDBObject.parse(object.getString("data"));
                        if (data != null) {
                            this.manager.receiveUpdate(data);
                        }
                    }
                }
            }
            catch (Exception ex) {
                this.manager.getCache().getErrorHandler().exception(this.manager.getCache(), ex, "Sync: Error mapping updated Payload from sync service");
            }
        }
    }

    @Override
    public void onPong(String pattern) {
        super.onPong(pattern);
    }

    @Override
    public void ping() {
        super.ping();
    }
}
