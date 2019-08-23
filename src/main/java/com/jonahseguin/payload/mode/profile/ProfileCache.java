package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerLocal;

public class ProfileCache<X extends PayloadProfile> extends PayloadCache<String, X, ProfileData> {

    public ProfileCache(PayloadHook hook, String name, Class<X> type) {
        super(hook, name, String.class, type);
    }

    @Override
    protected void init() {
        // somehow register layers ....
        this.layerController.register(new ProfileLayerLocal<>(this));
    }

    @Override
    protected void shutdown() {
        // close layers in order, save all objects, etc.
    }

    @Override
    protected X get(String key) {
        return null;
    }

    @Override
    public boolean requireRedis() {
        return true;
    }

    @Override
    public boolean requireMongoDb() {
        return true;
    }
}
