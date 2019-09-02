package com.jonahseguin.payload.mode.profile.pubsub;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileCache;
import org.bson.Document;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;

public class HandshakeListener<X extends PayloadProfile> extends JedisPubSub {

    private final ProfileCache<X> cache;

    public HandshakeListener(ProfileCache<X> cache) {
        this.cache = cache;
    }

    @Override
    public void onMessage(String channel, String message) {
        HandshakeEvent event = fromChannel(channel);
        if (event != null) {
            // Confirmed it's a handshake event, make assumption the message is JSON (Document)
            // Message is json
            Document data;
            try {
                data = Document.parse(message);
            } catch (Exception ex) {
                this.cache.getErrorHandler().exception(this.cache, ex, "Failed to parse data for Redis Handshake event: " + ex.getMessage());
                return;
            }

            if (data != null) {
                final String sourcePayloadId = data.getString(HandshakeManager.SOURCE_PAYLOAD_ID);
                final String cacheName = data.getString(HandshakeManager.CACHE_NAME);
                final String uniqueId = data.getString(HandshakeManager.PLAYER_UUID); // Payload Profile's UUID

                if (sourcePayloadId == null) {
                    this.cache.getErrorHandler().error(this.cache, "Missing field in Payload handshake: '" + HandshakeManager.SOURCE_PAYLOAD_ID + "'");
                    return;
                } else if (cacheName == null) {
                    this.cache.getErrorHandler().error(this.cache, "Missing field in Payload handshake: '" + HandshakeManager.CACHE_NAME + "'");
                    return;
                } else if (uniqueId == null) {
                    this.cache.getErrorHandler().error(this.cache, "Missing field in Payload handshake: '" + HandshakeManager.PLAYER_UUID + "'");
                    return;
                }

                if (cacheName.equalsIgnoreCase(this.cache.getName())) {
                    final UUID uuid;
                    try {
                        uuid = UUID.fromString(uniqueId);
                    } catch (IllegalArgumentException ex) {
                        this.cache.getErrorHandler().exception(this.cache, ex, "Failed to parse UUID '" + uniqueId + "' for Redis Handshake event: " + ex.getMessage());
                        return;
                    }

                    if (event.equals(HandshakeEvent.JOIN_INIT)) {
                        if (!sourcePayloadId.equalsIgnoreCase(PayloadPlugin.get().getLocal().getPayloadID())) {
                            // Not from this server
                            if (cache.getLocalLayer().has(uuid)) {
                                X payload = cache.getLocalLayer().get(uuid);
                                if (payload.isOnline()) {
                                    // We have the payload requested on this server
                                    // It is now our responsibility to emit PROFILE_SERVER_FOUND and save the profile
                                    this.cache.getErrorHandler().debug(this.cache, "Handshake init for player " + payload.getUsername());
                                    this.cache.getHandshakeManager().emitProfileServerFound(uuid, cacheName, sourcePayloadId);

                                    // We will also now begin to save the profile (async.) and then send PROFILE_JOIN_PROCEED
                                    this.cache.getPool().submit(() -> {
                                        if (this.cache.save(payload)) {
                                            // Success, now we can send PROFILE_JOIN_PROCEED
                                            this.cache.getHandshakeManager().emitProfileJoinProceed(uuid, cacheName, sourcePayloadId);
                                        } else {
                                            // Failure... how should we handle this effectively?
                                            this.cache.getErrorHandler().error(this.cache, "Failed to save Payload during handshake: " + payload.getUsername());
                                        }
                                    });

                                }
                            }
                        }
                    } else if (event.equals(HandshakeEvent.PROFILE_SERVER_FOUND)) {
                        // Check if we are handshaking a Payload with the uniqueId sent
                        final String targetPayloadID = data.getString(HandshakeManager.TARGET_PAYLOAD_ID);
                        if (targetPayloadID != null) {
                            this.cache.getHandshakeManager().handleProfileServerFound(uuid, cacheName, sourcePayloadId, targetPayloadID);
                        } else {
                            this.cache.getErrorHandler().error(this.cache, "Missing field in PROFILE_SERVER_FOUND Payload handshake: '" + HandshakeManager.TARGET_PAYLOAD_ID + "'");
                        }
                    } else if (event.equals(HandshakeEvent.PROFILE_JOIN_PROCEED)) {
                        // Check if we are handshaking a Payload with the uniqueId sent
                        final String targetPayloadID = data.getString(HandshakeManager.TARGET_PAYLOAD_ID);
                        if (targetPayloadID != null) {
                            this.cache.getHandshakeManager().handleProfileJoinProceed(uuid, cacheName, sourcePayloadId, targetPayloadID);
                        } else {
                            this.cache.getErrorHandler().error(this.cache, "Missing field in PROFILE_SERVER_FOUND Payload handshake: '" + HandshakeManager.TARGET_PAYLOAD_ID + "'");
                        }
                    }
                }
            }
        }
    }

    private HandshakeEvent fromChannel(String channel) {
        return HandshakeEvent.fromString(channel);
    }

}
