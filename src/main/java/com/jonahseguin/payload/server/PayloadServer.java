package com.jonahseguin.payload.server;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.mode.object.PayloadObject;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;

@Getter
@Setter
@Entity("payloadServers")
public class PayloadServer extends PayloadObject {

    @Id
    private ObjectId id = new ObjectId();
    private String name;
    private long lastPing = 0;
    private boolean online = false;

    public PayloadServer(String name) {
        this.name = name;
    }

    public PayloadServer(String name, long lastPing, boolean online) {
        this.name = name;
        this.lastPing = lastPing;
        this.online = online;
    }

    @Override
    public String getIdentifier() {
        return this.name;
    }

    @Override
    public String identifierFieldName() {
        return "name";
    }

    @Override
    public PayloadCache getCache() {
        return null; // TODO
    }
}
