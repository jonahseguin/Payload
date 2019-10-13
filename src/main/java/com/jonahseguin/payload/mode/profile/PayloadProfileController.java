/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.base.type.PayloadController;
import com.jonahseguin.payload.server.PayloadServer;
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

    private boolean login = true; // whether this controller is being used during a login operation

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
    private int handshakeTimeoutAttempts = 0;
    // --                                             --


    public PayloadProfileController(ProfileCache<X> cache, ProfileData data) {
        this.cache = cache;
        this.data = data;
    }


    public void reset() {
        this.denyJoin = false;
        this.payload = null;
        this.failure = false;

        this.handshaking = false;
        this.loadFromServer = null;
        this.serverFound = false;
        this.handshakeComplete = false;
        this.timedOut = false;
        this.abortHandshakeNotCached = false;
        this.handshakeStartTime = -1L;
    }

    @Override
    public X cache() {
        this.reset();

        if (this.data.getUniqueId() != null && this.data.getUsername() != null) {
            // Map their UUID to Username
            PayloadPlugin.get().saveUUID(this.data.getUsername(), this.data.getUniqueId());
        }

        if (this.login) {
            if (this.cache.getSettings().isDenyJoinDatabaseDown()) {
                if (!this.cache.getPayloadDatabase().getState().canCacheFunction(this.cache)) {
                    this.denyJoin = true;
                    this.joinDenyReason = this.cache.getLangController().get(PLang.DENY_JOIN_DATABASE_DOWN, this.cache.getName());
                    return null;
                }
            }
        }

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
                            this.cache.getErrorHandler().debug(this.cache, "Loading payload " + this.getData().getUniqueId() + " from layer " + layer.layerName());
                            prePayload = layer.get(this.data);
                            if (prePayload != null) {
                                break;
                            }
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
            if (this.login) {
                // Only make a new profile if they are logging in
                this.getCache().getErrorHandler().debug(this.cache, "Creating a new profile for Payload " + this.data.getUsername());
                // Otherwise make a new profile
                payload = cache.getInstantiator().instantiate(this.data);
                payload.setLoadingSource("New Profile");
                payload.setOnline(true);
                cache.getPool().submit(() -> cache.save(payload));
            }
            // If they aren't logging in (getting a payload by UUID/username) and it wasn't found, return null as they don't exist.
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

        this.cache.getErrorHandler().debug(this.cache, "Caching Payload [network-node] " + this.getData().getIdentifier() + "(login: " + this.login + ")");

        X prePayload = attemptCache(this.login); // only load from database-only if they are logging in

        if (prePayload != null) {
            if (prePayload.isOnlineThisServer()) {
                this.cache.getErrorHandler().debug(this.cache, "Payload " + this.getData().getIdentifier() + " loaded from local cache (online this server)");
                // They are cached locally and online this server
                // No need to do anything else
                payload = prePayload;
            } else {
                if (prePayload.getLastSeenServer() != null && prePayload.isOnline()) {
                    // begin a handshake to get the server they were last seen on to save their profile before we continue to load their data

                    if (prePayload.getLastSeenServer().equalsIgnoreCase(PayloadAPI.get().getPayloadID())) {
                        // They were last seen on this server
                        // Use the payload we already loaded
                        payload = prePayload;
                        this.cache.getErrorHandler().debug(this.cache, "Recent server for Payload " + this.data.getIdentifier() + " is this server, using Payload we already loaded");
                    } else {
                        PayloadServer from = this.cache.getPayloadDatabase().getServerManager().getServer(prePayload.getLastSeenServer());

                        if (from == null || !from.isOnline()) {
                            // The server they are connecting from is not online
                            // To avoid having to wait for the handshake to timeout, instead we will just use the
                            // Payload we already loaded from the database, as it's most likely the most recent data
                            // For the payload
                            this.cache.getErrorHandler().debug(this.cache, "Not handshaking for " + this.data.getIdentifier() + ", the server " + prePayload.getLastSeenServer() + " is not online.  Using Payload from database");
                            payload = prePayload;
                        } else {
                            this.handshaking = true;

                            this.cache.getErrorHandler().debug(this.cache, "Beginning handshake for " + this.data.getIdentifier() + " from server " + prePayload.getLastSeenServer());

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
                                    payload = prePayload;
                                } else {
                                    // Handshake completed, cache normally
                                    payload = cacheStandalone();
                                }
                                this.cache.getErrorHandler().debug(this.cache, "Handshake completed & cached for Payload " + this.data.getIdentifier());

                            } else {
                                // They timed out
                                // The connecting server could be unresponsive, offline, or the player might not be connecting from
                                // another server at all and their data has simply glitched.
                                // we need to add a safety, so that after X retries after timeout (config. in settings), we just
                                // load using the prePayload from the database.

                                this.handshakeTimeoutAttempts++;

                                if (this.handshakeTimeoutAttempts >= this.cache.getSettings().getHandshakeTimeOutAttemptsAllowJoin()) {
                                    // They attempted enough times, just cache using the prePayload
                                    this.cache.getErrorHandler().debug(this.cache, "Max handshake timeout attempts exceeded for Payload " + this.data.getIdentifier() + ", using Payload from database");
                                    payload = prePayload;
                                } else {
                                    this.cache.getErrorHandler().debug(this.cache, "Handshake timed out for Payload " + this.data.getIdentifier());
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
                            }
                        }
                    }
                } else {
                    // They haven't been seen on a server recently (aren't switching servers on a network)
                    // Just use the object we loaded from the database as it's up to date.
                    this.cache.getErrorHandler().debug(this.cache, "No recent server found for Payload " + this.data.getIdentifier() + ", using Payload from database");
                    payload = prePayload;
                }
            }
        } else {
            // They don't have any object stored in any database (Redis or MongoDB)
            if (failure) {
                // The reason they don't have an object is that the database couldn't be reached or another error occurred.
                if (!this.cache.getSettings().isDenyJoinOnHandshakeFailDatabase()) {
                    this.cache.getErrorHandler().debug(this.cache, "No Payload stored in database (with failure), failure handling for " + this.data.getIdentifier());
                    // Start failure handling them to re-attempt to check for their lastSeenServer to avoid data loss due to database being down
                    if (!this.cache.getFailureManager().hasFailure(this.data)) {
                        this.cache.getFailureManager().fail(data);
                    }
                } else {
                    this.cache.getErrorHandler().debug(this.cache, "No Payload stored in database (with failure), denying join for " + this.data.getIdentifier());
                    this.denyJoin = true;
                    this.joinDenyReason = this.cache.getLangController().get(PLang.DENY_JOIN_DATABASE_DOWN, this.cache.getName());
                }

                return null;
            } else {
                this.cache.getErrorHandler().debug(this.cache, "New profile created for " + this.data.getIdentifier());
                // There were no errors - they just don't have an object stored in the database
                // Just create them a new one and call it a day.
                if (this.login) {
                    payload = cache.getInstantiator().instantiate(this.data);
                    payload.setLoadingSource("New Profile");
                    payload.setOnline(true);
                    cache.getPool().submit(() -> cache.save(payload));
                }
            }
        }

        if (payload != null) {
            if (this.login) {
                if (this.data.getIp() != null) {
                    payload.setLoginIp(this.data.getIp());
                }
                if (this.data.getUsername() != null) {
                    payload.setUsername(this.data.getUsername()); // Update their username
                }
            }
            if (this.cache.getSettings().isAlwaysCacheOnLoadNetworkNode() || this.login) {
                // Only cache them if they are logging in,
                this.cache.cache(payload);
            }

            this.cache.getErrorHandler().debug(this.cache, "Finished caching payload " + this.data.getIdentifier() + ", from: " + payload.getLoadingSource());
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
            payload.setSwitchingServers(false);
            payload.setLastSeenServer(PayloadAPI.get().getPayloadID());
            payload.setOnline(true);
            payload.setCachedTimestamp(System.currentTimeMillis());
            payload.setLastSeenTimestamp(System.currentTimeMillis());
            this.cache.getPool().submit(() -> {
                this.cache.save(payload);
            });
        }
    }

}
