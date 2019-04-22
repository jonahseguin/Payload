package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.type.Payload;
import org.bukkit.plugin.Plugin;

public class ProfileCache<X extends Payload> extends PayloadCache<X> {

    public ProfileCache(Plugin plugin, String name) {
        super(plugin, name);
    }

    @Override
    protected boolean init() {
        return false;
    }

    @Override
    protected boolean shutdown() {
        return false;
    }
}
