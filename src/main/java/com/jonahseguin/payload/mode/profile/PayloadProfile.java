/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.mode.profile.util.MsgBuilder;
import dev.morphia.annotations.*;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang3.Validate;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.annotation.Nonnull;
import java.util.UUID;

// The implementing class of this abstract class must add an @Entity annotation (from MongoDB) with a collection name!
@Getter
@Setter
@Indexes({
        @Index(fields = @Field("username")),
        @Index(fields = @Field("uniqueId"))
})
public abstract class PayloadProfile implements Payload<UUID> {

    protected transient final ProfileService<PayloadProfile> cache;

    @Id
    protected ObjectId objectId = new ObjectId();
    @Indexed
    protected String username;
    @Indexed
    protected String uniqueId;
    protected String loginIp = null; // IP the profile logged in with
    protected String payloadId; // The ID of the Payload instance that currently holds this profile
    protected transient UUID uuid = null;
    protected transient long cachedTimestamp = System.currentTimeMillis();
    protected transient long lastInteractionTimestamp = System.currentTimeMillis();
    protected transient long lastSaveTimestamp = 0;
    protected transient boolean saveFailed = false; // If the player's profile failed to auto-save/save on shutdown,
    // This will be set to true, and we will notify the player once their
    // Profile has been saved successfully
    protected transient String loadingSource = null;
    protected transient Player player = null;
    protected transient long handshakeStartTimestamp = 0;

    @Inject
    public PayloadProfile(ProfileService<PayloadProfile> cache) {
        this.cache = cache;
        this.payloadId = cache.getApi().getPayloadID();
    }

    public PayloadProfile(ProfileService<PayloadProfile> cache, String username, UUID uniqueId, String loginIp) {
        this(cache);
        this.username = username;
        this.uuid = uniqueId;
        this.uniqueId = uniqueId.toString();
        this.loginIp = loginIp;
    }

    public PayloadProfile(ProfileService<PayloadProfile> cache, ProfileData data) {
        this(cache, data.getUsername(), data.getUniqueId(), data.getIp());
    }

    @PostLoad
    private void onPostPayloadLoad() {
        this.uuid = UUID.fromString(this.uniqueId);
    }

    @Override
    public boolean hasValidHandshake() {
        if (handshakeStartTimestamp > 0) {
            long ago = System.currentTimeMillis() - handshakeStartTimestamp;
            long seconds = ago / 1000;
            return seconds < 10;
        }
        return false;
    }

    @Nonnull
    @Override
    public ProfileService<PayloadProfile> getCache() {
        return cache;
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
        this.init();
    }

    public void uninitializePlayer() {
        this.uninit();
        this.player = null;
    }

    abstract void init();

    abstract void uninit();

    public boolean isOnline() {
        if (this.player == null) {
            Player player = Bukkit.getPlayer(this.getUUID());
            if (player != null && player.isOnline()) {
                this.player = player;
                return true;
            }
        }
        return this.player != null && this.player.isOnline();
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

    public void msg(String msg, Object... args) {
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
    public void save() {
        this.cache.save(this);
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

    private boolean canEqual(Object other) {
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
                    if (this.uniqueId != null && other.uniqueId != null) {
                        return this.objectId.equals(other.objectId)
                                && this.uniqueId.equalsIgnoreCase(other.uniqueId);
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        }
    }

}
