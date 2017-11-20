package com.jonahseguin.payload.event;

import com.jonahseguin.payload.profile.Profile;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Jonah on 10/15/2017.
 * Project: purifiedCore
 *
 * @ 5:46 PM
 */
public class AsyncPlayerHaltEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    private final Profile profile;
    private boolean halted;

    public AsyncPlayerHaltEvent(Profile profile, boolean halted) {
        super(true);
        this.profile = profile;
        this.halted = halted;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public boolean isHalted() {
        return halted;
    }

    public void setHalted(boolean halted) {
        this.halted = halted;
    }

    public Profile getProfile() {
        return profile;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
