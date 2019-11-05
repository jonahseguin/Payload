/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.inject.Inject;
import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.type.Payload;
import dev.morphia.annotations.Id;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.bukkit.plugin.Plugin;

@Getter
@Setter
public abstract class PayloadObject implements Payload<String> {

    protected transient final PayloadAPI api;
    protected transient final PayloadPlugin payloadPlugin;
    protected transient final Plugin plugin;
    protected transient final ObjectCache cache;

    private String payloadId;
    @Id
    private ObjectId objectId = new ObjectId();
    private long cachedTimestamp = System.currentTimeMillis();
    private transient long handshakeStartTimestamp = 0;

    @Inject
    public PayloadObject(PayloadAPI api, PayloadPlugin payloadPlugin, Plugin plugin, ObjectCache cache) {
        this.api = api;
        this.payloadPlugin = payloadPlugin;
        this.plugin = plugin;
        this.cache = cache;
    }

    public PayloadObject(PayloadAPI api, PayloadPlugin payloadPlugin, Plugin plugin, ObjectCache cache, ObjectId objectId) {
        this(api, payloadPlugin, plugin, cache);
        this.objectId = objectId;
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

    @Override
    public long cachedTimestamp() {
        return this.cachedTimestamp;
    }

    @Override
    public void interact() {
        this.cachedTimestamp = System.currentTimeMillis();
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
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof PayloadObject;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof PayloadObject)) {
            return false;
        } else {
            PayloadObject other = (PayloadObject) o;
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
