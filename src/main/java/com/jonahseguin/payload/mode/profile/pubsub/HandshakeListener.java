package com.jonahseguin.payload.mode.profile.pubsub;

import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadPermission;
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
            // Message is json
            Document data = null;
            try {
                data = Document.parse(message);
            } catch (Exception ex) {
                PayloadPlugin.get().alert(PayloadPermission.ADMIN, "&cFailed to parse data for Redis Handshake event: " + ex.getMessage());
                if (PayloadPlugin.get().isDebug()) {
                    ex.printStackTrace();
                }
            }
            if (data != null) {
                final String payloadId = data.getString("payloadId");
                final String cacheName = data.getString("cacheName");
                final String uniqueId = data.getString("uniqueId"); // Payload Profile's UUID
                if (cacheName.equalsIgnoreCase(this.cache.getName())) {
                    final UUID uuid;
                    try {
                        uuid = UUID.fromString(uniqueId);
                    } catch (IllegalArgumentException ex) {
                        PayloadPlugin.get().alert(PayloadPermission.ADMIN, "&cFailed to parse UUID '" + uniqueId + "' for Redis Handshake event: " + ex.getMessage());
                        if (PayloadPlugin.get().isDebug()) {
                            ex.printStackTrace();
                        }
                        return;
                    }

                    if (event.equals(HandshakeEvent.JOIN_INIT)) {
                        if (!payloadId.equalsIgnoreCase(PayloadPlugin.get().getLocal().getPayloadID())) {
                            // Not from this server
                            if (cache.getLocalLayer().has(uuid)) {
                                X payload = cache.getLocalLayer().get(uuid);
                                if (payload.isOnline()) {
                                    // We have the payload requested on this server
                                    // It is now our responsibility to emit PROFILE_SERVER_FOUND and save the profile


                                }
                            }
                        }
                    } else if (event.equals(HandshakeEvent.PROFILE_SERVER_FOUND)) {

                    } else if (event.equals(HandshakeEvent.PROFILE_JOIN_PROCEED)) {

                    }
                }
            }
        }
    }

    private HandshakeEvent fromChannel(String channel) {
        return HandshakeEvent.fromString(channel);
    }

}
