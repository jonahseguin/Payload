/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.Payload;
import dev.morphia.annotations.Id;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
public abstract class PayloadObject implements Payload<String> {

    private String payloadId;

    @Id
    private ObjectId objectId = new ObjectId();

    private long cachedTimestamp = System.currentTimeMillis();

    public PayloadObject() {
        this.payloadId = PayloadAPI.get().getPayloadID();
    }

    public PayloadObject(ObjectId objectId) {
        this();
        this.objectId = objectId;
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
    public boolean shouldSave() {
        return this.getCache().getSettings().isEnableSync() || this.getCache().getSettings().isServerSpecific();
    }

    @Override
    public boolean shouldPrepareUpdate() {
        return !this.getCache().getSettings().isEnableSync() && !this.getCache().getSettings().isServerSpecific();
    }


    @Override
    public void save() {
        this.getCache().save(this);
    }
}
