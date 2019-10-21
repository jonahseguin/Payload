/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.type.Payload;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import dev.morphia.annotations.PostLoad;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang3.Validate;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.UUID;

// The implementing class of this abstract class must add an @Entity annotation (from MongoDB) with a collection name!
@Getter
@Setter
public abstract class PayloadProfile implements Payload<UUID> {

    @Id
    protected ObjectId objectId = new ObjectId();

    @Indexed
    protected String username;
    @Indexed
    protected String uniqueId;
    protected String loginIp = null; // IP the profile logged in with

    protected String lastSeenServer = null; // The Payload ID of the server they last joined
    protected long lastSeenTimestamp = 0;
    protected boolean online = false; // is the profile online anywhere in the network, can be true even if they aren't online on this server instance
    protected String payloadId; // The ID of the Payload instance that currently holds this profile

    protected transient UUID uuid = null;
    protected transient long cachedTimestamp = System.currentTimeMillis();
    protected transient long lastInteractionTimestamp = System.currentTimeMillis();
    protected transient long redisCacheTimestamp = System.currentTimeMillis();
    protected transient long lastSaveTimestamp = 0;
    protected transient boolean switchingServers = false; // set to true when an incoming handshake requests their profile be saved
    protected transient boolean saveFailed = false; // If the player's profile failed to auto-save/save on shutdown,
    // This will be set to true, and we will notify the player once their
    // Profile has been saved successfully
    protected transient String loadingSource = null;
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
    }

    @PostLoad
    private void onPostPayloadLoad() {
        this.uuid = UUID.fromString(this.uniqueId);
    }

    public UUID getUUID() {
        if (this.uuid == null) {
            this.uuid = UUID.fromString(this.uniqueId);
        }
        return this.uuid;
    }

    public UUID getUniqueId() {
        return this.getUUID();
    }

    public void initializePlayer(Player player) {
        Validate.notNull(player, "Player cannot be null for initializePlayer");
        this.player = player;
    }

    public void uninitializePlayer() {

    }

    public boolean isPlayerOnline() {
        if (this.player == null) {
            Player player = Bukkit.getPlayer(this.getUUID());
            if (player != null && player.isOnline()) {
                this.player = player;
            }
        }
        return this.player != null && this.player.isOnline();
    }

    public boolean isOnlineThisServer() {
        return this.isPlayerOnline();
    }

    public boolean isOnlineOtherServer() {
        return !this.isPlayerOnline() && this.online;
    }

    public String getCurrentIP() {
        if (player != null && player.isOnline()) {
            return player.getAddress().getAddress().getHostAddress();
        }
        return this.getLoginIp();
    }

    @Override
    public void interact() {
        this.lastInteractionTimestamp = System.currentTimeMillis();
    }

    public void interactRedis() {
        this.redisCacheTimestamp = System.currentTimeMillis();
    }

    @Override
    public UUID getIdentifier() {
        return this.getUniqueId();
    }

    public void sendMessage(String msg) {
        if (this.isInitialized()) {
            this.player.sendMessage(msg);
        }
    }

    public boolean isInitialized() {
        return this.player != null;
    }

    @Override
    public String identifierFieldName() {
        return "uniqueId";
    }

    @Override
    public long cachedTimestamp() {
        return this.cachedTimestamp;
    }

    public String getName() {
        return this.getUsername();
    }

    public void msg(String msg) {
        if (player != null && player.isOnline()) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', msg));
        }
    }

    public void msg(String msg, String... args) {
        this.msg(PayloadPlugin.format(msg, args));
    }

    public void msg(BaseComponent component) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(component);
        }
    }

    public void msg(BaseComponent[] components) {
        if (player != null && player.isOnline()) {
            player.spigot().sendMessage(components);
        }
    }

    public void msgBuilder(String msg, MsgBuilder builder) {
        this.msg(builder.build(new ComponentBuilder(msg)));
    }

    public ComponentBuilder msgBuilder(String msg) {
        return new ComponentBuilder(msg);
    }

    @Override
    public String getPayloadServer() {
        return this.payloadId;
    }

    @Override
    public void setPayloadServer(String payloadID) {
        this.payloadId = payloadID;
    }

    @Override
    public boolean shouldSave() {
        return this.isOnlineThisServer();
    }

    @Override
    public boolean shouldPrepareUpdate() {
        return this.isOnlineOtherServer();
    }

    @Override
    public void save() {
        this.getCache().save(this);
    }

    @Override
    public int hashCode() {
        int result = 1;
        Object $objectId = this.getObjectId();
        result = result * 59 + ($objectId == null ? 43 : $objectId.hashCode());
        Object $uniqueId = this.getUniqueId();
        result = result * 59 + ($uniqueId == null ? 43 : $uniqueId.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof PayloadProfile;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PayloadProfile)) {
            return false;
        } else {
            PayloadProfile other = (PayloadProfile) o;
            if (!other.canEqual(this)) {
                return false;
            } else {
                if (this.objectId != null && other.objectId != null) {
                    return this.objectId.equals(other.objectId);
                } else {
                    return false;
                }
            }
        }
    }

}
