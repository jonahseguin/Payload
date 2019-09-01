package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.failsafe.FailedPayload;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerLocal;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerMongo;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerRedis;
import com.jonahseguin.payload.mode.profile.pubsub.HandshakeListener;
import com.jonahseguin.payload.mode.profile.pubsub.HandshakeManager;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Getter
public class ProfileCache<X extends PayloadProfile> extends PayloadCache<UUID, X, ProfileData> {

    private transient final ProfileCacheSettings settings = new ProfileCacheSettings();
    private transient final ConcurrentMap<UUID, PayloadProfileController<X>> controllers = new ConcurrentHashMap<>();

    // Handshaking for NETWORK_NODE mode
    private transient final HandshakeManager<X> handshakeManager = new HandshakeManager<>(this);
    private transient final HandshakeListener<X> handshakeListener = new HandshakeListener<>(this);

    private transient final ProfileLayerLocal<X> localLayer = new ProfileLayerLocal<>(this);
    private transient final ProfileLayerRedis<X> redisLayer = new ProfileLayerRedis<>(this);
    private transient final ProfileLayerMongo<X> mongoLayer = new ProfileLayerMongo<>(this);

    private transient final Map<UUID, ProfileData> data = new HashMap<>();

    private transient Jedis publisherJedis = null;
    private transient Jedis subscriberJedis = null;

    public ProfileCache(PayloadHook hook, String name, Class<X> type) {
        super(hook, name, UUID.class, type);
    }

    @Override
    protected void init() {
        if (this.mode.equals(PayloadMode.NETWORK_NODE)) {
            // Allocate Jedis resources for Publishing and Subscribing
            this.publisherJedis = this.payloadDatabase.getJedisPool().getResource();
            this.subscriberJedis = this.payloadDatabase.getJedisPool().getResource();

            this.subscriberJedis.subscribe(this.handshakeListener);
        }

        this.layerController.register(this.localLayer);
        this.layerController.register(this.redisLayer);
        this.layerController.register(this.mongoLayer);

        this.layerController.init();

    }

    @Override
    protected void shutdown() {
        // close layers in order, save all objects, etc.
        this.layerController.shutdown();
    }

    @Override
    public void cache(X payload) {
        this.localLayer.save(payload);
    }

    public X getProfileByName(String username) {
        UUID uuid = PayloadPlugin.get().getUUIDs().get(username);
        if (uuid != null) {
            return this.getProfile(uuid);
        }
        Player exact = Bukkit.getPlayerExact(username);
        if (exact != null) {
            return this.getProfile(exact);
        } else {
            // Manually get from MongoDB
            return this.mongoLayer.getByUsername(username);
        }
    }

    public X getProfile(UUID uuid) {
        return this.get(uuid);
    }

    public X getProfile(Player player) {
        return this.get(player.getUniqueId());
    }

    @Override
    protected X get(UUID uniqueId) {
        if (this.getFailureManager().hasFailure(uniqueId)) {
            // They are attempting to be cached
            FailedPayload<X, ProfileData> failedPayload = this.getFailureManager().getFailedPayload(uniqueId);
            if (failedPayload.getTemporaryPayload() == null) {
                if (failedPayload.getPlayer() != null) {
                    failedPayload.setTemporaryPayload(this.instantiator.instantiate(this.createData(failedPayload.getPlayer().getName(), uniqueId, failedPayload.getPlayer().getAddress().getAddress().getHostAddress())));
                }
            }
            return failedPayload.getTemporaryPayload();
        }

        // Handle the getting of a Profile from the first available layer
        for (PayloadLayer<UUID, X, ProfileData> layer : this.layerController.getLayers()) {
            if (layer.has(uniqueId)) {
                try {
                    return layer.get(uniqueId);
                } catch (PayloadLayerCannotProvideException e) {
                    this.getErrorHandler().exception(this, e, "Error getting Profile by UUID: " + uniqueId.toString());
                }
            }
        }

        // No profile found for said UUID
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

    @Override
    public boolean save(X payload) {
        boolean x = true;
        for (PayloadLayer<UUID, X, ProfileData> layer : this.layerController.getLayers()) {
            if (!layer.save(payload)) {
                x = false;
            }
        }
        return x;
    }

    public boolean save(Player player) {
        X x = getProfile(player);
        if (x != null) {
            return this.save(x);
        }
        return false;
    }

    public PayloadProfileController<X> getController(UUID uuid) {
        return this.controllers.get(uuid);
    }

    public void removeController(UUID uuid) {
        this.controllers.remove(uuid);
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

    @Override
    public long cachedObjectCount() {
        return this.localLayer.size();
    }
}
