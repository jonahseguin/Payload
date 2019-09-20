package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.base.type.Payload;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

@Getter
@Setter
public abstract class PayloadObject implements Payload {

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
}
