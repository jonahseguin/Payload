package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadMode;
import com.jonahseguin.payload.base.type.Payload;
import org.bukkit.plugin.Plugin;

public class ProfileCache<X extends Payload> extends PayloadCache<String, X> {

    public ProfileCache(PayloadHook hook, String name, Class<X> type) {
        super(hook, name, String.class, type);
    }

    @Override
    protected boolean init() {
        return false;
    }

    @Override
    protected boolean shutdown() {
        return false;
    }

    @Override
    protected X get(String key) {
        return null;
    }

    @Override
    protected PayloadMode mode() {
        return null;
    }

    @Override
    public void onMongoDbDisconnect() {

    }

    @Override
    public void onRedisDisconnect() {

    }

    @Override
    public void onMongoDbReconnect() {

    }

    @Override
    public void onRedisReconnect() {

    }

    @Override
    public void onMongoDbInitConnect() {

    }

    @Override
    public void onRedisInitConnect() {

    }

    @Override
    public boolean requireRedis() {
        return false;
    }

    @Override
    public boolean requireMongoDb() {
        return false;
    }
}
