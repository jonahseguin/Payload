package com.jonahseguin.payload.mode.profile;

import com.jonahseguin.payload.PayloadAPI;
import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.PayloadMode;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.exception.PayloadLayerCannotProvideException;
import com.jonahseguin.payload.base.failsafe.FailedPayload;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.layer.PayloadLayer;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerLocal;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerMongo;
import com.jonahseguin.payload.mode.profile.layer.ProfileLayerRedis;
import com.jonahseguin.payload.mode.profile.pubsub.HandshakeEvent;
import com.jonahseguin.payload.mode.profile.pubsub.HandshakeListener;
import com.jonahseguin.payload.mode.profile.pubsub.HandshakeManager;
import com.jonahseguin.payload.mode.profile.settings.ProfileCacheSettings;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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

    private transient final ConcurrentMap<UUID, ProfileData> data = new ConcurrentHashMap<>();

    private transient Jedis publisherJedis = null;
    private transient Jedis subscriberJedis = null;

    public ProfileCache(PayloadHook hook, String name, Class<X> type) {
        super(hook, name, UUID.class, type);
    }

    @Override
    protected void init() {
        // Startup!
        if (this.mode.equals(PayloadMode.NETWORK_NODE)) {
            this.handshakeManager.getTimeoutTask().start();
        }

        this.layerController.register(this.localLayer);
        this.layerController.register(this.redisLayer);
        this.layerController.register(this.mongoLayer);

        this.layerController.init();
    }

    @Override
    protected void shutdown() {
        // close layers in order, save all objects, etc.

        for (Player player : this.getPlugin().getServer().getOnlinePlayers()) {
            X payload = this.getLocalProfile(player);
            if (payload != null) {
                payload.setOnline(false);
                payload.setLastSeenTimestamp(System.currentTimeMillis());
            }
        }
        // ^ they will be saved in the superclass's shutdown impl

        if (this.mode.equals(PayloadMode.NETWORK_NODE)) {
            this.handshakeManager.getTimeoutTask().stop();
        }
        this.layerController.shutdown();
        if (this.handshakeListener.isSubscribed()) {
            this.handshakeListener.unsubscribe();
        }
        if (this.publisherJedis != null) {
            this.publisherJedis.close();
        }
        if (this.subscriberJedis != null) {
            this.subscriberJedis.close();
        }
        this.publisherJedis = null;
        this.subscriberJedis = null;
        this.data.clear();
        this.controllers.clear();
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

    public boolean hasProfileLocal(UUID uuid) {
        return this.localLayer.has(uuid);
    }

    public boolean hasProfileLocal(Player player) {
        return this.hasProfileLocal(player.getUniqueId());
    }

    @Override
    protected X get(UUID uniqueId) {
        if (this.getFailureManager().hasFailure(uniqueId)) {
            // They are attempting to be cached
            FailedPayload<X, ProfileData> failedPayload = this.getFailureManager().getFailedPayload(uniqueId);
            if (failedPayload.getTemporaryPayload() == null) {
                if (failedPayload.getPlayer() != null && failedPayload.getPlayer().isOnline()) {
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

    public Set<X> getOnlineProfiles() {
        return this.localLayer.getLocalCache().values().stream()
                .filter(PayloadProfile::isPlayerOnline)
                .collect(Collectors.toSet());
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
        if (!x) {
            payload.setSaveFailed(true); // save failed
            payload.sendMessage(this.getLangController().get(PLang.SAVE_FAILED_NOTIFY_PLAYER, this.getName()));
            this.alert(PayloadPermission.ADMIN, PLang.SAVE_FAILED_NOTIFY_ADMIN, this.getName(), payload.getUsername());
            this.getErrorHandler().debug(this, "Failed to save Payload: " + payload.getUsername());
        } else {
            payload.setLastSaveTimestamp(System.currentTimeMillis());
            if (payload.isSaveFailed()) {
                // They previously had failed to save
                // but now we are successful.
                // let them know that they are free to switch servers / logout / etc. without data loss now.
                payload.sendMessage(this.getLangController().get(PLang.SAVE_SUCCESS_NOTIFY_PLAYER, this.getName()));
                this.alert(PayloadPermission.ADMIN, PLang.SAVE_SUCCESS_NOTIFY_ADMIN, this.getName(), payload.getUsername());
                this.getErrorHandler().debug(this, "Successfully saved Payload: " + payload.getUsername());
            }
            payload.setSaveFailed(false);
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

    @Override
    public int saveAll() {
        int failures = 0;
        for (Player p : this.getPlugin().getServer().getOnlinePlayers()) {
            X payload = this.getLocalProfile(p);
            if (payload != null) {
                if (!this.save(payload)) {
                    failures++;
                }
            } else {
                failures++;
            }
        }
        return failures;
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

    @Override
    public void onRedisInitConnect() {
        super.onRedisInitConnect();
        // Allocate Jedis resources for Publishing and Subscribing
        if (this.payloadDatabase != null && this.payloadDatabase.getJedisPool() != null) {
            if (this.publisherJedis == null) {
                this.publisherJedis = this.payloadDatabase.getResource();
            }
            if (this.subscriberJedis == null) {
                this.subscriberJedis = this.payloadDatabase.getResource();

                this.pool.submit(() ->
                        this.subscriberJedis.subscribe(this.handshakeListener,
                                HandshakeEvent.PAYLOAD_NOT_CACHED_CONTINUE.getName(),
                                HandshakeEvent.REQUEST_PAYLOAD_SAVE.getName(),
                                HandshakeEvent.SAVED_PAYLOAD.getName(),
                                HandshakeEvent.SAVING_PAYLOAD.getName()));
            }
        }
    }

    @Override
    public Collection<X> getCachedObjects() {
        return this.localLayer.getLocalCache().values();
    }

    @Override
    public void updatePayloadID() {
        for (X x : this.getCachedObjects()) {
            x.setPayloadId(PayloadAPI.get().getPayloadID());
            if (x.isPlayerOnline()) {
                x.setLastSeenServer(PayloadAPI.get().getPayloadID());
            }
        }
        this.pool.submit(this::saveAll);
    }
}
