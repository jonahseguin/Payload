package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.Payload;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.Validate;
import org.bson.types.ObjectId;
import org.bukkit.entity.Player;
import org.mongodb.morphia.annotations.Id;

import java.util.UUID;

// The implementing class of this abstract class must add an @Entity annotation (from MongoDB) with a collection name!
@Getter
@Setter
public abstract class PayloadProfile implements Payload {

    @Id
    protected ObjectId objectId;
    protected String username;
    protected String uniqueId;
    protected transient UUID uuid = null;
    protected String loginIp = null; // IP the profile logged in with

    protected String lastSeenServer = null; // The Payload ID of the server they last joined
    protected boolean online = false; // is the profile online anywhere in the network, can be true even if they aren't online on this server instance
    protected String payloadId; // The ID of the Payload instance that currently holds this profile
    protected long cachedTimestamp = System.currentTimeMillis();
    protected long lastInteractionTimestamp = System.currentTimeMillis();
    protected long redisCacheTimestamp = System.currentTimeMillis();

    protected transient boolean switchingServers = false; // set to true when an incoming handshake requests their profile be saved
    protected transient boolean saveFailed = false; // If the player's profile saved to auto-save/save on shutdown,
    // This will be set to true, and we will notify the player once their
    // Profile has been saved successfully

    protected transient Player player = null;

    public PayloadProfile() {
        this.payloadId = PayloadAPI.get().getPayloadID();
    }

    public PayloadProfile(String username, UUID uniqueId, String loginIp) {
        this();
        this.username = username;
        this.uuid = uniqueId;
        this.uniqueId = uniqueId.toString();
        this.loginIp = loginIp;
    }

    public PayloadProfile(ProfileData data) {
        this(data.getUsername(), data.getUniqueId(), data.getIp());
        this.loginIp = data.getIp();
    }

    public void initializePlayer(Player player) {
        Validate.notNull(player, "Player cannot be null for initializePlayer");
        this.player = player;
    }

    public UUID getUniqueId() {
        if (this.uuid == null) {
            this.uuid = UUID.fromString(this.uniqueId);
        }
        return this.uuid;
    }

    @Override
    public void interact() {
        this.lastInteractionTimestamp = System.currentTimeMillis();
    }

    public void interactRedis() {
        this.redisCacheTimestamp = System.currentTimeMillis();
    }

    @Override
    public String getIdentifier() {
        return this.uniqueId;
    }

    public void sendMessage(String msg) {
        if (this.isInitialized()) {
            this.player.sendMessage(msg);
        }
    }

    public boolean isInitialized() {
        return this.player != null;
    }

}
