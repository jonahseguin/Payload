/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.inject.Inject;
import com.jonahseguin.payload.base.type.Payload;
import dev.morphia.annotations.Id;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

import javax.annotation.Nonnull;

@Getter
@Setter
public abstract class PayloadObject implements Payload<String> {

    protected transient final ObjectCache cache;

    protected String payloadId;
    @Id
    protected ObjectId objectId = new ObjectId();
    protected transient long cachedTimestamp = System.currentTimeMillis();
    protected transient long handshakeStartTimestamp = 0;

    @Inject
    public PayloadObject(ObjectCache cache) {
        this.cache = cache;
        if (cache != null && cache.getApi().getPayloadID() != null) {
            this.payloadId = cache.getApi().getPayloadID();
        }
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
    public boolean save() {
        return this.getCache().save(this);
    }

    @Nonnull
    @Override
    public ObjectCache getCache() {
        return cache;
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
