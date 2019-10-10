/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.mode.profile.event;

import com.jonahseguin.payload.mode.profile.PayloadProfile;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PayloadProfileSwitchServersEvent extends Event {

    private static final HandlerList handlers = new HandlerList();

    private final PayloadProfile profile;

    public PayloadProfileSwitchServersEvent(PayloadProfile profile) {
        this.profile = profile;
    }

    public PayloadProfile getProfile() {
        return profile;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
