package com.jonahseguin.payload.mode.object;

import com.jonahseguin.payload.base.type.Payload;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Id;

@Getter
@Setter
public abstract class PayloadObject implements Payload {

    @Id
    private ObjectId objectId;

    private long cachedTimestamp = System.currentTimeMillis();

    @Override
    public long cachedTimestamp() {
        return this.cachedTimestamp;
    }

    @Override
    public void interact() {
        this.cachedTimestamp = System.currentTimeMillis();
    }
}
