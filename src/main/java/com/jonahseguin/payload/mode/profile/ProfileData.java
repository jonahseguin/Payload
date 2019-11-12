/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.google.inject.Inject;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class ProfileData extends PayloadData {

    private String username;
    private UUID uniqueId;
    private String ip;

    @Inject
    public ProfileData() {
    }

    public ProfileData(String username, UUID uniqueId, String ip) {
        this.username = username;
        this.uniqueId = uniqueId;
        this.ip = ip;
    }

    public String getIdentifier() {
        if (this.username != null) {
            return this.username;
        }
        return this.uniqueId.toString();
    }

}
