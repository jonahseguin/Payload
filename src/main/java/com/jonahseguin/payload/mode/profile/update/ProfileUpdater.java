/*
 * Copyright (c) 2020 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.update;

import com.google.common.base.Preconditions;
import com.jonahseguin.payload.base.PayloadCallback;
import com.jonahseguin.payload.base.Service;
import com.jonahseguin.payload.database.DatabaseService;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileCache;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import io.lettuce.core.pubsub.api.reactive.RedisPubSubReactiveCommands;
import org.bson.Document;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ProfileUpdater<X extends PayloadProfile> implements Service {

    private static final String KEY_SOURCE_SERVER = "sourceServer";
    private static final String KEY_TARGET_SERVER = "targetServer";
    private static final String KEY_IDENTIFIER = "identifier";
    private static final String KEY_MODE = "mode";
    private static final String MODE_REQUEST = "request";
    private static final String MODE_OK = "ok";

    private final PayloadProfileCache<X> cache;
    private final DatabaseService database;
    private RedisPubSubReactiveCommands<String, String> reactive = null;
    private boolean running = false;
    private final String channel;
    private final ConcurrentMap<String, PayloadCallback<X>> waitingReply = new ConcurrentHashMap<>();

    public ProfileUpdater(PayloadProfileCache<X> cache, DatabaseService database) {
        this.cache = cache;
        this.database = database;
        this.channel = "payload-profile-update-" + cache.getName();
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
        waitingReply.clear();
        running = false;
        return true;
    }

    private boolean subscribe() {
        try {
            StatefulRedisPubSubConnection<String, String> connection = database.getRedisPubSub();
            reactive = connection.reactive();

            reactive.subscribe(channel).subscribe();

            reactive.observeChannels()
                    .filter(pm -> pm.getChannel().equals(channel))
                    .doOnNext(patternMessage -> {
                        try {
                            String json = patternMessage.getMessage();
                            Document document = Document.parse(json);
                            String targetServerString = document.getString(KEY_TARGET_SERVER);
                            String sourceServerString = document.getString(KEY_SOURCE_SERVER);
                            if (!sourceServerString.equalsIgnoreCase(database.getServerService().getThisServer().getName()) && targetServerString.equalsIgnoreCase(database.getServerService().getThisServer().getName())) {
                                String identifierString = document.getString(KEY_IDENTIFIER);
                                String mode = document.getString(KEY_MODE);
                                if (mode.equalsIgnoreCase(MODE_REQUEST)) {
                                    receiveRequestSave(sourceServerString, identifierString);
                                } else if (mode.equalsIgnoreCase(MODE_OK)) {
                                    receiveReplyOk(sourceServerString, identifierString);
                                }
                            }
                        } catch (Exception ex) {
                            cache.getErrorService().capture(ex, "Error reading incoming packet in ProfileUpdater for packet: '" + patternMessage.getMessage() + "' in channel: " + patternMessage.getChannel());
                        }
                    }).subscribe();

            return true;
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error subscribing in Payload Updater");
            return false;
        }
    }

    private void receiveRequestSave(@Nonnull String sourceServerString, @Nonnull String identifierString) {
        try {
            Preconditions.checkNotNull(sourceServerString, "Source Server cannot be null in ProfileUpdater for receiveRequestSave");
            Preconditions.checkNotNull(identifierString, "Payload Identifier cannot be null in ProfileUpdater for receiveRequestSave");
            UUID uuid = cache.keyFromString(identifierString);
            cache.runAsync(() -> {
                cache.getFromCache(uuid).ifPresent(cache::save);
                replyOk(sourceServerString, identifierString);
            });
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error with incoming save request in ProfileUpdater from server: '" + sourceServerString + "' for Payload with identifier: '" + identifierString + "'");
        }
    }

    private void receiveReplyOk(@Nonnull String sourceServerString, @Nonnull String identifierString) {
        try {
            Preconditions.checkNotNull(sourceServerString, "Source Server cannot be null in ProfileUpdater for receiveReplyOk");
            Preconditions.checkNotNull(identifierString, "Payload Identifier cannot be null in ProfileUpdater for receiveReplyOk");
            if (waitingReply.containsKey(identifierString)) {
                PayloadCallback<X> callback = waitingReply.get(identifierString);
                waitingReply.remove(identifierString);
                UUID uuid = cache.keyFromString(identifierString);
                cache.runAsync(() -> {
                    X payload = cache.getFromDatabase(uuid).orElse(null);
                    if (payload != null) {
                        callback.callback(payload);
                        cache.getErrorService().debug("Received OK reply & called-back from save request for Payload: '" + identifierString + "' from server: '" + sourceServerString + "'");
                    } else {
                        cache.getErrorService().capture("Failed to get Payload from database in ProfileUpdater after receiving OK reply for identifier: '" + identifierString + "'");
                    }
                });
            }
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error with incoming OK reply in ProfileUpdater from server: '" + sourceServerString + "' for Payload with identifier: '" + identifierString + "'");
        }
    }

    private void replyOk(@Nonnull String sourceServerString, @Nonnull String identifierString) {
        try {
            Preconditions.checkNotNull(sourceServerString, "Source Server cannot be null in ProfileUpdater for replyOk");
            Preconditions.checkNotNull(identifierString, "Payload Identifier cannot be null in ProfileUpdater for replyOk");
            final Document document = new Document();
            document.append(KEY_TARGET_SERVER, sourceServerString);
            document.append(KEY_SOURCE_SERVER, database.getServerService().getThisServer().getName());
            document.append(KEY_MODE, MODE_OK);
            document.append(KEY_IDENTIFIER, identifierString);
            final String json = document.toJson();
            cache.runAsync(() -> database.getRedis().async().publish(channel, json));
            cache.getErrorService().debug("Replied OK for save request for Payload: '" + identifierString + "' from server: '" + sourceServerString + "'");
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Error with sending OK reply in ProfileUpdater to server: '" + sourceServerString + "' for Payload with identifier: '" + identifierString + "'");
        }
    }

    public boolean requestSave(@Nonnull X payload, @Nonnull String targetServerName, @Nonnull PayloadCallback<X> callback) {
        try {
            Preconditions.checkNotNull(payload, "Payload cannot be null in ProfileUpdater (requestSave)");
            Preconditions.checkNotNull(targetServerName, "Target Server Name cannot be null in ProfileUpdater (requestSave)");
            Preconditions.checkNotNull(callback, "Callback cannot be null in ProfileUpdater (requestSave)");
            final Document document = new Document();
            document.append(KEY_SOURCE_SERVER, database.getServerService().getThisServer().getName());
            document.append(KEY_IDENTIFIER, cache.keyToString(payload.getIdentifier()));
            document.append(KEY_MODE, MODE_REQUEST);
            document.append(KEY_TARGET_SERVER, targetServerName);
            final String json = document.toJson();
            waitingReply.put(payload.getIdentifier().toString(), callback);
            cache.runAsync(() -> database.getRedis().async().publish(channel, json));
            cache.getErrorService().debug("Requested save for Payload '" + payload.getIdentifier().toString() + "' from server: '" + targetServerName + "'");
            return true;
        } catch (Exception ex) {
            cache.getErrorService().capture(ex, "Failed to push update from ProfileUpdater for Payload: " + cache.keyToString(payload.getIdentifier()));
            return false;
        }
    }


    @Override
    public boolean isRunning() {
        return running;
    }

}
