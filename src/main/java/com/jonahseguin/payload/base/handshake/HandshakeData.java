package com.jonahseguin.payload.base.handshake;

import lombok.Data;
import org.bson.Document;

import java.util.UUID;

@Data
public class HandshakeData {

    public static final String ID = "id";

    private final Document document;

    public String getID() {
        return document.getString(ID);
    }

    public void writeID() {
        document.append(ID, UUID.randomUUID().toString());
    }

    public void writeChannel() {

    }

    public Document append(String key, Object value) {
        return document.append(key, value);
    }

}
