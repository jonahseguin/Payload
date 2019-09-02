package com.jonahseguin.payload.mode.profile.pubsub;

import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import com.jonahseguin.payload.mode.profile.ProfileData;
import lombok.Getter;
import org.bson.Document;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class HandshakeManager<X extends PayloadProfile> {

    public static final String SOURCE_PAYLOAD_ID = "source-payloadId";
    public static final String TARGET_PAYLOAD_ID = "target-payloadId";
    public static final String CACHE_NAME = "cacheName";
    public static final String PLAYER_UUID = "uniqueId";

    private final ProfileCache<X> cache;
    private final ConcurrentMap<UUID, HandshakingPayload> handshakes = new ConcurrentHashMap<>();
    private final HandshakeTimeoutTask<X> timeoutTask;

    public HandshakeManager(ProfileCache<X> cache) {
        this.cache = cache;
        this.timeoutTask = new HandshakeTimeoutTask<>(this);

        // TODO: handshake timeout task
    }

    public boolean isHandshakingPayload(UUID uuid) {
        return this.handshakes.containsKey(uuid);
    }

    public HandshakingPayload getHandshakingPayload(UUID uuid) {
        return this.handshakes.get(uuid);
    }

    public void removeHandshake(UUID uuid) {
        this.handshakes.remove(uuid);
    }

    public boolean waitForHandshake(HandshakingPayload handshakingPayload) {
        // Wait
        while (!handshakingPayload.isHandshakeComplete() && !handshakingPayload.isTimedOut()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                this.cache.getErrorHandler().exception(this.cache, ex, "Exception while waiting for handshake for Payload with UUID " + handshakingPayload.getUuid().toString());
                return false;
            }
        }
        // Once done (if either handshake succeeds or times out) -- continue.
        return true;
    }

    public HandshakingPayload beginHandshake(ProfileData profileData) {
        // ** Should be called async only **
        final String thisPayloadId = PayloadPlugin.get().getLocal().getPayloadID();
        HandshakingPayload handshakingPayload = new HandshakingPayload(profileData, profileData.getUniqueId(), this.cache.getName());

        Document data = new Document();
        data.append(SOURCE_PAYLOAD_ID, thisPayloadId);
        data.append(CACHE_NAME, this.cache.getName());
        data.append(PLAYER_UUID, profileData.getUniqueId().toString());

        handshakingPayload.setHandshakeStartTime(System.currentTimeMillis());

        this.cache.getPublisherJedis().publish(HandshakeEvent.JOIN_INIT.getName(), data.toJson());

        return handshakingPayload;
    }

    public void emitProfileServerFound(final UUID uuid, final String cacheName, final String targetPayloadID) {
        final String payloadId = PayloadPlugin.get().getLocal().getPayloadID();
        if (this.cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            final HandshakeEvent event = HandshakeEvent.PROFILE_SERVER_FOUND;

            Document data = new Document();
            data.append(SOURCE_PAYLOAD_ID, payloadId);
            data.append(TARGET_PAYLOAD_ID, targetPayloadID);
            data.append(CACHE_NAME, cacheName);
            data.append(PLAYER_UUID, uuid.toString());

            this.cache.getPool().submit(() -> {
                this.cache.getPublisherJedis().publish(event.getName(), data.toJson());
            });
        } else {
            throw new UnsupportedOperationException("Cannot emit handshake events unless cache is in NETWORK_NODE mode");
        }
    }

    public void handleProfileServerFound(final UUID uuid, final String cacheName, final String sourcePayloadID, final String targetPayloadID) {
        if (targetPayloadID.equalsIgnoreCase(PayloadPlugin.get().getLocal().getPayloadID())) {
            // The target is this server, proceed
            if (this.isHandshakingPayload(uuid)) {
                HandshakingPayload handshakingPayload = this.getHandshakingPayload(uuid);
                handshakingPayload.setServerFound(true); // To stop them from timing out
                // Now we wait for PROFILE_JOIN_PROCEED...
            } else {
                // The target was this server, but we aren't handshaking this Payload... debug error
                this.cache.getErrorHandler().debug(this.cache, "Not handshaking Payload with UUID " + uuid + " (target was this server/cache)");
            }
        }
    }

    public void emitProfileJoinProceed(final UUID uuid, final String cacheName, final String targetPayloadID) {
        if (this.cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            final HandshakeEvent event = HandshakeEvent.PROFILE_JOIN_PROCEED;

            Document data = new Document();
            data.append(SOURCE_PAYLOAD_ID, PayloadPlugin.get().getLocal().getPayloadID());
            data.append(TARGET_PAYLOAD_ID, targetPayloadID);
            data.append(CACHE_NAME, cacheName);
            data.append(PLAYER_UUID, uuid.toString());

            this.cache.getPool().submit(() -> {
                this.cache.getPublisherJedis().publish(event.getName(), data.toJson());
            });
        } else {
            throw new UnsupportedOperationException("Cannot emit handshake events unless cache is in NETWORK_NODE mode");
        }
    }

    public void handleProfileJoinProceed(final UUID uuid, final String cacheName, final String sourcePayloadID, final String targetPayloadID) {
        if (targetPayloadID.equalsIgnoreCase(PayloadPlugin.get().getLocal().getPayloadID())) {
            // The target is this server, proceed
            if (this.isHandshakingPayload(uuid)) {
                HandshakingPayload handshakingPayload = this.getHandshakingPayload(uuid);
                // We are handshaking this Payload and their data is ready from their source server.
                // Now we can signal back to the cache that this player is ready to be cached
                handshakingPayload.setHandshakeComplete(true);
            } else {
                // The target was this server, but we aren't handshaking this Payload... debug error
                this.cache.getErrorHandler().debug(this.cache, "Not handshaking Payload with UUID " + uuid + " (target was this server/cache)");
            }
        }
    }

}
