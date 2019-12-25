/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.update;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.base.Cache;
import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.database.DatabaseService;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.bson.Document;

import javax.annotation.Nonnull;

public class PayloadUpdater<K, X extends Payload<K>> implements Service {

    private static final String KEY_SOURCE_SERVER = "sourceServer";
    private static final String KEY_IDENTIFIER = "identifier";

    private final Cache<K, X> cache;
    private final DatabaseService database;
    private RedisPubSubReactiveCommands<String, String> reactive = null;
    private boolean running = false;
    private final String channel;

    public PayloadUpdater(Cache<K, X> cache, DatabaseService database) {
        this.cache = cache;
        this.database = database;
        this.channel = "payload-update-" + cache.getName();
    }

    @Override
    public boolean start() {
        Preconditions.checkState(!running, "Payload Updater is already running for cache: " + cache.getName());
        boolean sub = subscribe();
        if (!sub) {
            cache.getErrorService().capture("Failed to subscribe to channel " + this.channel + " in PayloadUpdater for cache: " + cache.getName());
        }
        running = true;
        return sub;
    }

    @Override
    public boolean shutdown() {
        Preconditions.checkState(running, "Payload Updater is not running for cache: " + cache.getName());
        if (reactive != null) {
            reactive.unsubscribe(channel);
        }
        running = false;
        return false;
    }

    private boolean subscribe() {
        try {
            StatefulRedisPubSubConnection<String, String> connection = database.getRedisPubSub();
            reactive = connection.reactive();

            reactive.subscribe(channel).subscribe();

            reactive.observeChannels()
                    .filter(pm -> !pm.getChannel().equals(channel))
                    .doOnNext(patternMessage -> {
                        if (patternMessage != null && patternMessage.getMessage() != null) {
                            receiveUpdateRequest(patternMessage.getMessage());
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error subscribing in Payload Updater");
            return false;
        }
    }

    private void receiveUpdateRequest(@Nonnull String msg) {
        Preconditions.checkNotNull(msg, "Message (packet) cannot be null in PayloadUpdater for receiveUpdateRequest");
        try {
            Document document = Document.parse(msg);
            if (document != null) {
                String sourceServerString = document.getString(KEY_SOURCE_SERVER);
                String identifierString = document.getString(KEY_IDENTIFIER);
                if (sourceServerString != null && identifierString != null) {
                    if (!sourceServerString.equalsIgnoreCase(database.getServerService().getThisServer().getName())) {
                        // As long as the source server wasn't us
                        K identifier = cache.keyFromString(identifierString);
                        if (cache.isCached(identifier)) {
                            cache.runAsync(() -> {
                                cache.getFromDatabase(identifier).ifPresent(payload -> {
                                    cache.cache(payload);
                                    payload.onReceiveUpdate();
                                });
                            });
                        }
                    }
                } else {
                    cache.getErrorService().capture("Source Server or Identifier were null during receiveUpdateRequest in PayloadUpdater for packet: " + msg);
                }
            } else {
                cache.getErrorService().capture("Document parsed was null during receiveUpdateRequest in PayloadUpdater for packet: " + msg);
            }
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error with received update request in PayloadUpdater for packet: " + msg);
        }
    }

    public boolean pushUpdate(@Nonnull X payload) {
        try {
            Preconditions.checkNotNull(payload, "Payload cannot be null in PayloadUpdater (pushUpdate)");
            final Document document = new Document();
            document.append(KEY_SOURCE_SERVER, database.getServerService().getThisServer().getName());
            document.append(KEY_IDENTIFIER, cache.keyToString(payload.getIdentifier()));
            final String json = document.toJson();
            cache.runAsync(() -> database.getRedis().async().publish(channel, json));
            return true;
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Failed to push update from PayloadUpdater for Payload: " + cache.keyToString(payload.getIdentifier()));
            return false;
        }
    }


    @Override
    public boolean isRunning() {
        return running;
    }
}
