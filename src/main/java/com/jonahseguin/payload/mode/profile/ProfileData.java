/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Data;

import java.util.UUID;

@Data
public class ProfileData implements PayloadData {

    private final String username;
    private final UUID uniqueId;
    private final String ip;

    public String getIdentifier() {
        if (this.username != null) {
            return this.username;
        }
        return this.uniqueId.toString();
    }

}
