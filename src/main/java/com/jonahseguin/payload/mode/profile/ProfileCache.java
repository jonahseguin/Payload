package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerLocal;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerRedis;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import org.bukkit.entity.Player;

public class ProfileCache<X extends PayloadProfile> extends PayloadCache<String, X, ProfileData> {

    private final ProfileCacheSettings settings = new ProfileCacheSettings();

    public ProfileCache(PayloadHook hook, String name, Class<X> type) {
        super(hook, name, String.class, type);
    }

    @Override
    protected void init() {
        // somehow register layers ....
        this.layerController.register(new ProfileLayerLocal<>(this));
        this.layerController.register(new ProfileLayerRedis<>(this));
    }

    @Override
    protected void shutdown() {
        // close layers in order, save all objects, etc.
    }

    public X getProfile(Player player) {
        return this.get(player.getUniqueId().toString());
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

    @Override
    public ProfileCacheSettings getSettings() {
        return this.settings;
    }

}
