package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerLocal;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerMongo;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerRedis;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import lombok.Getter;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ProfileCache<X extends PayloadProfile> extends PayloadCache<String, X, ProfileData> {

    private final ProfileCacheSettings settings = new ProfileCacheSettings();
    private final ConcurrentMap<UUID, PayloadProfileController<X>> controllers = new ConcurrentHashMap<>();

    private final ProfileLayerLocal<X> localLayer = new ProfileLayerLocal<>(this);
    private final ProfileLayerRedis<X> redisLayer = new ProfileLayerRedis<>(this);
    private final ProfileLayerMongo<X> mongoLayer = new ProfileLayerMongo<>(this);

    private final Map<UUID, ProfileData> data = new HashMap<>();

    public ProfileCache(PayloadHook hook, String name, Class<X> type) {
        super(hook, name, String.class, type);
    }

    @Override
    protected void init() {
        this.layerController.register(this.localLayer);
        this.layerController.register(this.redisLayer);
        this.layerController.register(this.mongoLayer);
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

    public X getLocalProfile(Player player) {
        return this.localLayer.getLocalCache().get(player.getUniqueId());
    }

    public ProfileData createData(String username, UUID uniqueId, String ip) {
        ProfileData data = new ProfileData(username, uniqueId, ip);
        this.data.put(uniqueId, data);
        return data;
    }

    public ProfileData getData(UUID uuid) {
        return this.data.get(uuid);
    }

    public boolean hasData(UUID uuid) {
        return this.data.containsKey(uuid);
    }

    public void removeData(UUID uuid) {
        this.data.remove(uuid);
    }

    @Override
    public PayloadProfileController<X> controller(ProfileData data) {
        if (this.controllers.containsKey(data.getUniqueId())) {
            return this.controllers.get(data.getUniqueId());
        }
        PayloadProfileController<X> controller = new PayloadProfileController<>(this, data);
        this.controllers.put(data.getUniqueId(), controller);
        return controller;
    }

    public PayloadProfileController<X> getController(UUID uuid) {
        return this.controllers.get(uuid);
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
