package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.base.type.PayloadController;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
public class PayloadProfileController<X extends PayloadProfile> implements PayloadController<X> {

    private final ProfileCache<X> cache;
    private final ProfileData data;

    private X payload = null;
    private Player player = null;

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
        for (PayloadLayer<UUID, X, ProfileData> layer : this.cache.getLayerController().getLayers()) {
            if (layer.has(this.data)) {
                try {
                    payload = layer.get(this.data);
                } catch (PayloadLayerCannotProvideException ex) {
                    payload = null;
                    this.cache.getErrorHandler().exception(this.cache, ex, "Failed to load profile " + this.data.getUsername() + " from layer " + layer.layerName());
                }

            }
        }

        if (payload == null) {
            // Failed to load from all layers - start failure handling
            if (!this.cache.getFailureManager().hasFailure(data)) {
                this.cache.getFailureManager().fail(data);
            }
        }
        return payload;
    }

    private X cacheNetworkNode() {
        // TODO handle the entire caching process here with external help from the pub/sub system (HANDSHAKE)
        return null;
    }

    public void initializeOnJoin(Player player) {
        this.player = player;
        if (this.payload != null) {
            this.payload.initializePlayer(player);
        }
    }

}
