package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.base.type.PayloadController;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

@Getter
@Setter
public class PayloadProfileController<X extends PayloadProfile> implements PayloadController<X> {

    private final ProfileCache<X> cache;
    private final ProfileData data;

    private boolean denyJoin = false;
    private String joinDenyReason = ChatColor.RED + "A caching error occurred.  Please try again.";

    private X payload = null;
    private Player player = null;
    private boolean failure = false;

    // -- Handshaking (network-node mode) properties: --
    private boolean handshaking = false;
    private volatile String loadFromServer = null; // Payload ID of server to load from
    private volatile boolean serverFound = false;
    private volatile boolean handshakeComplete = false;
    private volatile boolean timedOut = false;
    private volatile boolean abortHandshakeNotCached = false; // Set to true by handshakeManager if the requesting server didn't have the profile anymore
    private long handshakeStartTime = -1L;
    // --                                             --


    public PayloadProfileController(ProfileCache<X> cache, ProfileData data) {
        this.cache = cache;
        this.data = data;
    }

    @Override
    public X cache() {
        // Map their UUID to Username
        PayloadPlugin.get().getUUIDs().put(this.data.getUsername(), this.data.getUniqueId());

        if (cache.getMode().equals(PayloadMode.STANDALONE)) {
            return this.cacheStandalone();
        } else if (cache.getMode().equals(PayloadMode.NETWORK_NODE)) {
            return this.cacheNetworkNode();
        } else {
            throw new UnsupportedOperationException("Unknown cache mode: " + cache.getMode().toString());
        }
    }

    private X attemptCache(boolean databaseOnly) {
        X prePayload = null;
        if (this.cache.getPayloadDatabase().getState().isDatabaseConnected() || !databaseOnly) {
            for (PayloadLayer<UUID, X, ProfileData> layer : this.cache.getLayerController().getLayers()) {
                if (layer.isDatabase() || !databaseOnly) { // Only for database layers in network-node mode (i.e redis/mongo)
                    try {
                        if (layer.has(this.data)) {
                            prePayload = layer.get(this.data);
                        }
                    } catch (Exception ex) {
                        prePayload = null;
                        this.cache.getErrorHandler().exception(this.cache, ex, "Failed to load profile " + this.data.getUsername() + " from layer " + layer.layerName());
                        failure = true;
                    }
                }
            }
        } else {
            // Database is down, assume failure.
            failure = true;
            this.cache.getErrorHandler().debug(this.cache, "Failing caching profile " + this.data.getUsername() + " because the database is not connected");
        }

        return prePayload;
    }

    private X cacheStandalone() {
        // Iterate each layer in order

        payload = attemptCache(false);

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
            cache.getPool().submit(() -> cache.save(payload));
        } else {
            // Update their login ip
            payload.setLoginIp(this.data.getIp());

            // Cache the Payload if successful
            this.cache.cache(payload);
        }

        return payload;
    }

    private X cacheNetworkNode() {
        // [X] First, we check their lastSeenServer field via their Redis/Mongo Profile (if it exists)
        // [X] If they don't have a Redis or Mongo Profile, create a new Profile and stop there
        // [X] If they do have a profile in redis, but their lastSeenServer field is null: use the profile we loaded

        // [X] If we are able to acquire their lastSeenServer, begin a handshake to get that server to save their profile first.
        // [X] Then continue to cache them as normal

        // [X] If they timeout (server didn't respond in time) at any point --> what we do will depend on a config setting (allowJoinOnTimeout)
        // [X] --> if true allow their join and start failure handling (which will continue to attempt more handshakes until successful)
        // [X] --> if false we will just kick the player (deny the login) ****[will have to be handled in the ProfileListener event for e.disallow)****

        X prePayload = attemptCache(true);

        if (prePayload != null) {
            if (prePayload.getLastSeenServer() != null && !prePayload.getLastSeenServer().equalsIgnoreCase(PayloadAPI.get().getPayloadID())) {
                // begin a handshake to get the server they were last seen on to save their profile before we continue to load their data

                this.handshaking = true;

                this.cache.getErrorHandler().debug(this.cache, "Beginning handshake for " + this.data.getUsername() + " from server " + prePayload.getLastSeenServer());

                // Publish our event to request that the lastSeenServer saves the profile
                this.cache.getHandshakeManager().beginHandshake(this, prePayload.getLastSeenServer());

                // Wait for the handshake to complete
                if (!this.cache.getHandshakeManager().waitForHandshake(this)) {
                    this.timedOut = true;
                }

                // Once we get here, the handshake will be complete (or timed out)

                if (!this.timedOut) {
                    // Didn't timeout, the handshake completed successfully.
                    // We can now load their Profile data as per usual.

                    if (this.isAbortHandshakeNotCached()) {
                        // the payload wasn't on the target server
                        // just cache the prePayload we already loaded
                        this.cache.cache(prePayload);
                        payload = prePayload;
                    } else {
                        // Handshake completed, cache normally
                        payload = cacheStandalone();
                    }
                    this.cache.getErrorHandler().debug(this.cache, "Handshake completed & cached for Payload " + this.data.getUsername());

                } else {
                    this.cache.getErrorHandler().debug(this.cache, "Handshake timed out for Payload " + this.data.getUsername());
                    if (!this.cache.getSettings().isDenyJoinOnHandshakeTimeout()) {
                        // Allow join, start failure handling
                        if (!this.cache.getFailureManager().hasFailure(this.data)) {
                            this.cache.getFailureManager().fail(this.data);
                        }
                        return null;
                    } else {
                        // Deny the join
                        this.denyJoin = true;
                        this.joinDenyReason = this.cache.getLangController().get(PLang.DENY_JOIN_HANDSHAKE_TIMEOUT, this.cache.getName());
                        return null;
                    }
                }

            } else {
                // They haven't been seen on a server recently (aren't switching servers on a network)
                // Just use the object we loaded from the database as it's up to date.
                this.cache.getErrorHandler().debug(this.cache, "No recent server found for Payload " + this.data.getUsername() + ", using Payload from database");
                payload = prePayload;
                this.cache.cache(payload);
            }
        } else {
            // They don't have any object stored in any database (Redis or MongoDB)
            if (failure) {
                // The reason they don't have an object is that the database couldn't be reached or another error occurred.
                if (!this.cache.getSettings().isDenyJoinOnHandshakeFailDatabase()) {
                    this.cache.getErrorHandler().debug(this.cache, "No Payload stored in database (with failure), failure handling for " + this.data.getUsername());
                    // Start failure handling them to re-attempt to check for their lastSeenServer to avoid data loss due to database being down
                    if (!this.cache.getFailureManager().hasFailure(this.data)) {
                        this.cache.getFailureManager().fail(data);
                    }
                } else {
                    this.cache.getErrorHandler().debug(this.cache, "No Payload stored in database (with failure), denying join for " + this.data.getUsername());
                    this.denyJoin = true;
                    this.joinDenyReason = this.cache.getLangController().get(PLang.DENY_JOIN_HANDSHAKE_DATABASE_DOWN, this.cache.getName());
                }

                return null;
            } else {
                this.cache.getErrorHandler().debug(this.cache, "New profile created for " + this.data.getUsername());
                // There were no errors - they just don't have an object stored in the database
                // Just create them a new one and call it a day.
                payload = cache.getInstantiator().instantiate(this.data);
                cache.getPool().submit(() -> cache.save(payload));
            }
        }

        if (payload != null) {
            payload.setLoginIp(this.data.getIp());
        }

        return payload;
    }

    /**
     * To be called by our HandshakeManager when receiving the PAYLOAD_SAVED event or the PAYLOAD_NOT_CACHED_CONTINUE event
     */
    public void onHandshakeComplete() {
        this.handshakeComplete = true;
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
