/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.object;

import com.google.inject.Inject;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ObjectData extends PayloadData {

    private String identifier;

    @Inject
    public ObjectData() {
    }

    public ObjectData(String identifier) {
        this.identifier = identifier;
    }
}
