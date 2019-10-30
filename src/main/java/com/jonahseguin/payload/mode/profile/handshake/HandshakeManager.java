/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.handshake;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.PayloadProfileController;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import lombok.Getter;
import org.bson.Document;
import redis.clients.jedis.Jedis;

@Getter
public class HandshakeManager<X extends PayloadProfile> {


    public static final String SOURCE_PAYLOAD_ID = "source-payloadId";
    public static final String TARGET_PAYLOAD_ID = "target-payloadId";
    public static final String CACHE_NAME = "cacheName";
    public static final String PLAYER_UUID = "uniqueId";

    private final PayloadAPI api;
    private final ProfileCache<X> cache;
    private final HandshakeTimeoutTask<X> timeoutTask;

    public HandshakeManager(PayloadAPI api, ProfileCache<X> cache) {
        this.api = api;
        this.cache = cache;
        this.timeoutTask = new HandshakeTimeoutTask<>(this);
    }

    public boolean waitForHandshake(PayloadProfileController<X> controller) {
        // ** Should be called async only **
        // Wait
        while (!controller.isHandshakeComplete() && !controller.isTimedOut()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                this.cache.getErrorHandler().exception(this.cache, ex, "Exception while waiting for handshake for Payload " + controller.getData().getUniqueId().toString());
                return false;
            }
        }
        // Once done (if either handshake succeeds or times out) -- continue.
        return true;
    }

    public void beginHandshake(PayloadProfileController<X> controller, String targetServerPayloadID) {
        // ** Should be called async only **
        final String thisPayloadId = api.getPayloadID();

        Document data = new Document();
        data.append(SOURCE_PAYLOAD_ID, thisPayloadId);
        data.append(CACHE_NAME, this.cache.getName());
        data.append(TARGET_PAYLOAD_ID, targetServerPayloadID);
        data.append(PLAYER_UUID, controller.getData().getUniqueId().toString());

        controller.setHandshakeStartTime(System.currentTimeMillis());

        cache.runAsync(() -> {
            try (Jedis jedis = this.cache.getDatabase().getJedisResource()) {
                jedis.publish(HandshakeEvent.REQUEST_PAYLOAD_SAVE.getName(), data.toJson());
            } catch (Exception ex) {
                controller.setTimedOut(true);
                controller.setFailure(true);
                this.cache.getErrorHandler().exception(this.cache, ex, "Failed to publish REQUEST_PAYLOAD_SAVE (start handshaking) during handshake for Payload " + controller.getData().getUniqueId().toString());
            }
        });
    }

}
