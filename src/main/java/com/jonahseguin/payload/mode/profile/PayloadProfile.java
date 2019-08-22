package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.Payload;
import lombok.Getter;

// The implementing class of this abstract class must add an @Entity annotation (from MongoDB) with a collection name!
@Getter
public abstract class PayloadProfile implements Payload {

    protected String username;
    protected String uniqueId;
    protected String loginIp = null;
    protected boolean online = false;
    protected String payloadId = null; // The ID of the Payload instance that currently holds this profile
    protected long cachedTimestamp = System.currentTimeMillis();
    protected long lastInteractionTimestamp = System.currentTimeMillis();

    public PayloadProfile() {
        this.payloadId = PayloadAPI.get().getPayloadID();
    }

    public PayloadProfile(String username, String uniqueId) {
        this();
        this.username = username;
        this.uniqueId = uniqueId;
    }

    public PayloadProfile(ProfileData data) {
        this(data.getUsername(), data.getUniqueId());
        this.loginIp = data.getIp();
    }

    @Override
    public void interact() {
        this.lastInteractionTimestamp = System.currentTimeMillis();
    }
}
