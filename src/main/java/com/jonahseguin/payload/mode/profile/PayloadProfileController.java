package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.base.type.PayloadController;
import com.jonahseguin.payload.mode.profile.pubsub.HandshakingPayload;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class PayloadProfileController<X extends PayloadProfile> implements PayloadController<X> {

    private final ProfileCache<X> cache;
    private final ProfileData data;

    private X payload = null;
    private Player player = null;

    private HandshakingPayload handshakingPayload = null;
    private boolean handshakeDone = false;

    public PayloadProfileController(ProfileCache<X> cache, ProfileData data) {
        this.cache = cache;
        this.data = data;
    }

    @Override
    public X cache() {
        // Map their UUID to Username
        PayloadPlugin.get().getUUIDs().put(this.data.getUsername(), this.data.getUniqueId());

        if (cache.getMode() == PayloadMode.STANDALONE) {
            return this.cacheStandalone();
        } else if (cache.getMode() == PayloadMode.NETWORK_NODE) {
            return this.cacheNetworkNode();
        } else {
            throw new UnsupportedOperationException("Unknown cache mode: " + cache.getMode().toString());
        }
    }

    private X cacheStandalone() {
        boolean failure = false; // failure?

        // Iterate each layer in order
        for (PayloadLayer<UUID, X, ProfileData> layer : this.cache.getLayerController().getLayers()) {
            try {
                if (layer.has(this.data)) {
                    payload = layer.get(this.data);
                }
            } catch (Exception ex) {
                payload = null;
                this.cache.getErrorHandler().exception(this.cache, ex, "Failed to load profile " + this.data.getUsername() + " from layer " + layer.layerName());
                failure = true;
            }
        }

        if (payload == null) {
            // Failed to load from all layers

            // If there was a failure/error, start failure handling instead of making a new profile
            if (failure || cache.getState().isLocked() || !cache.getPayloadDatabase().getState().canCacheFunction(cache)) {
                // start failure handling
                if (!this.cache.getFailureManager().hasFailure(data)) {
                    this.cache.getFailureManager().fail(data);
                }
            }

            // Otherwise make a new profile
            payload = cache.getInstantiator().instantiate(this.data);
            cache.getPlugin().getServer().getScheduler().runTaskAsynchronously(cache.getPlugin(), () -> cache.save(payload));
        } else {
            // Cache the Payload if successful
            this.cache.cache(payload);
        }

        return payload;
    }

    private X cacheNetworkNode() {
        this.handshakingPayload = this.cache.getHandshakeManager().beginHandshake(this.data);
        if (this.cache.getHandshakeManager().waitForHandshake(this.handshakingPayload)) {
            if (this.handshakingPayload.isTimedOut()) {
                // Timed out.  Wasn't successful
                // No servers responded in time with the data for this Payload

            } else {
                // Handshake completed successfully
                this.handshakeDone = true;
                this.handshakingPayload.setHandshakeComplete(true);
                this.cache.getHandshakeManager().removeHandshake(this.handshakingPayload.getUuid());
                return this.cacheStandalone();
            }
        }
        // If the handshake didn't complete successfully, the error would've been handled in that method (waitForHandshake())
        return payload;
    }

    public void initializeOnJoin(Player player) {
        this.player = player;
        if (this.payload != null) {
            this.payload.initializePlayer(player);
        }
        this.updatePayloadAfterJoin();
    }

    private void updatePayloadAfterJoin() {
        if (this.payload != null) {
            payload.setLastSeenServer(PayloadAPI.get().getPayloadID());
            payload.setOnline(true);
            payload.setCachedTimestamp(System.currentTimeMillis());
            this.cache.getPool().submit(() -> {
                this.cache.save(payload);
            });
        }
    }

}
