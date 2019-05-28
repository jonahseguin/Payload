package com.jonahseguin.payload.base;

import com.jonahseguin.payload.PayloadHook;
import com.jonahseguin.payload.PayloadPlugin;
import com.jonahseguin.payload.database.DatabaseDependent;
import com.jonahseguin.payload.database.PayloadDatabase;
import com.jonahseguin.payload.base.error.DefaultErrorHandler;
import com.jonahseguin.payload.base.error.PayloadErrorHandler;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.lang.PayloadLangController;
import com.jonahseguin.payload.base.state.CacheState;
import com.jonahseguin.payload.base.state.PayloadTaskExecutor;
import com.jonahseguin.payload.base.type.Payload;
import lombok.Getter;
import org.apache.commons.lang.Validate;
import org.bson.types.ObjectId;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.mongodb.morphia.annotations.Entity;

import javax.persistence.Id;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The abstract backbone of all Payload cache systems.
 * All Caching modes (profile, object, simple) extend this class.
 */
@Entity("payloadCache")
@Getter
public abstract class PayloadCache<K, X extends Payload> implements DatabaseDependent<K, X> {

    private final transient Plugin plugin; // The Bukkit JavaPlugin that created this cache.  non-persistent

    @Id
    protected ObjectId id; // Persist
    protected String name; // Persist

    private transient boolean debug = false; // Debug for this cache

    protected transient PayloadErrorHandler errorHandler = new DefaultErrorHandler();
    private transient PayloadDatabase payloadDatabase = null;

    private transient final ExecutorService pool = Executors.newCachedThreadPool();
    private transient final PayloadTaskExecutor<K, X> executor;
    private transient final PayloadLangController langController = new PayloadLangController();
    private transient final CacheState<K, X> state;

    private transient final Class<K> keyType;
    private transient final Class<X> valueType;
    private transient final Class<PayloadCache<K, X>> cacheType;

    public PayloadCache(final PayloadHook hook, final String name, Class<K> keyType, Class<X> valueType, Class<PayloadCache<K, X>> cacheType) {
        if (hook.getPlugin() == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (hook.getPlugin() instanceof PayloadPlugin) {
            throw new IllegalArgumentException("Plugin cannot be PayloadPlugin");
        }
        if (!hook.isValid()) {
            throw new IllegalStateException("Provided PayloadHook is not valid; cannot create cache '" + name + "'");
        }
        this.keyType = keyType;
        this.valueType = valueType;
        this.cacheType = cacheType;
        this.plugin = hook.getPlugin();
        this.name = name;
        this.executor = new PayloadTaskExecutor<>(this);
        this.state = new CacheState<>(this);
    }

    public void alert(PayloadPermission required, PLang lang, String... args) {
        Bukkit.getLogger().info(this.langController.get(lang, args));
        for (Player pl : this.plugin.getServer().getOnlinePlayers()) {
            if (required.has(pl)) {
                pl.sendMessage(this.langController.get(lang, args));
            }
        }
    }

    public final boolean isLocked() {
        return this.state.isLocked() || PayloadPlugin.get().isLocked();
    }

    public final boolean start() {
        if (this.connect()) {
            boolean load = this.load();
            if (!load) {
                // Do something todo
            }
            return load;
        } else {
            // Failed
            return false;
        }
    }

    public final boolean stop() {
        this.shutdown(); // Allow the implementing cache to do it's shutdown first
        this.pool.shutdown(); // Shutdown our thread pool
        if (this.save()) {
            return true;
        } else {
            // Failed to save
            getErrorHandler().error(this.name, "Failed to save during shutdown");
            return false;
        }

    }

    protected final boolean save() {

    }

    public final void setupDatabase(PayloadDatabase database) {
        if (this.payloadDatabase != null) {
            throw new IllegalStateException("Database has already been defined");
        }
        this.payloadDatabase = database;
    }

    /**
     * Starts up & initializes the cache.
     * Prepares everything for a fresh startup, ensures database connections, etc.
     *
     * @return boolean: true if successful, false if any errors encountered
     */
    protected abstract boolean init();

    /**
     * Shut down the cache.
     * Saves everything first, and safely shuts down
     *
     * @return boolean: true if successful, false if any errors encountered
     */
    protected abstract boolean shutdown();

    protected abstract X get(K key);

    /**
     * Randomly generated MongoDB identifier for this cache.
     * Must be unique.  A {@link com.jonahseguin.payload.base.exception.DuplicateCacheException} error will be thrown
     * if another cache exists with the same ID during creation.
     *
     * @return String: Cache ID
     */
    public final String getCacheId() {
        return this.id.toString();
    }

    /**
     * Get the name of this cache (set by the end user, should be unique)
     * A {@link com.jonahseguin.payload.base.exception.DuplicateCacheException} error will be thrown if another cache
     * exists with the same name or ID during creation.
     *
     * @return String: Cache Name
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Get the JavaPlugin controlling this cache
     * Every Payload cache must be associated with a JavaPlugin for event handling and etc.
     *
     * @return Plugin
     */
    public final Plugin getPlugin() {
        return this.plugin;
    }

    protected abstract PayloadMode mode();

}
