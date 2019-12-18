/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

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

    public Document append(String key, Object value) {
        return document.append(key, value);
    }

}
