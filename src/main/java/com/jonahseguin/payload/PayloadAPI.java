package com.jonahseguin.payload;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.exception.runtime.PayloadProvisionException;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Getter;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Getter
public class PayloadAPI {

    private static PayloadAPI instance;
    private final PayloadPlugin plugin;
    private final ConcurrentMap<String, PayloadHook> hooks = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, PayloadCache> caches = new ConcurrentHashMap<>();
    private final Set<String> requested = new HashSet<>();

    private List<PayloadCache> _sortedCaches = null;

    protected PayloadAPI(PayloadPlugin plugin) throws IllegalAccessException {
        this.plugin = plugin;
        if (PayloadAPI.instance != null) {
            throw new IllegalAccessException("PayloadAPI can only be created by the internal Payload plugin.");
        }
        PayloadAPI.instance = this;
    }

    /**
     * Get the local unique ID associated with this server's instance of Payload
     * @return String unique ID
     */
    public String getPayloadID() {
        return this.plugin.getLocal().getPayloadID();
    }

    /**
     * Check if a hook is valid for a plugin
     * @param plugin Plugin
     * @param hook PayloadHook
     * @return True if valid, else false
     */
    public final boolean validateHook(Plugin plugin, PayloadHook hook) {
        return this.isProvisioned(plugin) && getHook(plugin).equals(hook);
    }

    /**
     * Get the singleton instance of the Payload API
     * @return {@link PayloadAPI}
     */
    public static PayloadAPI get() {
        return PayloadAPI.instance;
    }

    public static String convertCacheName(String name) {
        return name.toLowerCase().replace(" ", "");
    }

    /**
     * Check if a hook has been provisioned for a plugin
     * @param plugin {@link Plugin}
     * @return True if provisioned, else false
     */
    public boolean isProvisioned(Plugin plugin) {
        return this.hooks.containsKey(plugin.getName());
    }


    /**
     * Get the {@link PayloadHook} for a {@link Plugin}
     * @param plugin {@link Plugin}
     * @return {@link PayloadHook} if the plugin is provisioned ({@link #isProvisioned(Plugin)})
     */
    public PayloadHook getHook(Plugin plugin) {
        if (!this.isProvisioned(plugin)) {
            throw new PayloadProvisionException("Cannot get a hook that is not yet provisioned.  Use requestProvision() first.");
        }
        return this.hooks.get(plugin.getName());
    }

    /**
     * Request a provision for a Plugin, async.
     * This is the method for obtaining an {@link PayloadHook} for a {@link Plugin} / JavaPlugin instance,
     * for an external hooking plugin.
     * @param plugin {@link Plugin} the hooking plugin
     * @return A {@link CompletableFuture<PayloadHook>} that will be completed once provisioning is complete,
     * might also throw an exception which can be handled within the CompletableFuture
     */
    public CompletableFuture<PayloadHook> requestProvision(final Plugin plugin) {
        if (this.hooks.containsKey(plugin.getName())) {
            throw new IllegalStateException("Hook has already been provisioned for plugin " + plugin.getName());
        }
        if (this.requested.contains(plugin.getName())) {
            throw new IllegalStateException("Hook is already awaiting provisioning for plugin " + plugin.getName());
        }
        if (plugin instanceof PayloadPlugin) {
            CompletableFuture<PayloadHook> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Plugin requesting provision cannot be of instance PayloadPlugin"));
            return future;
        }
        this.requested.add(plugin.getName());
        return CompletableFuture.supplyAsync(() -> {
            while (PayloadPlugin.get().isLocked()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    throw new PayloadProvisionException("Interrupted while waiting for provision for plugin " + plugin.getName(), ex);
                }
            }
            this.requested.remove(plugin.getName());
            PayloadHook hook = new PayloadHook(plugin);
            this.hooks.putIfAbsent(plugin.getName(), hook);
            return hook;
        });
    }

    /**
     * Register a cache w/ a hook
     *
     * @param cache {@link PayloadCache}
     * @param hook  {@link PayloadHook}
     */
    public final void saveCache(PayloadCache cache, PayloadHook hook) {
        if (!this.validateHook(hook.getPlugin(), hook)) {
            throw new IllegalStateException("Hook is not valid for cache to save in PayloadAPI");
        }
        this.caches.put(convertCacheName(cache.getName()), cache);
    }

    /**
     * Get a cache by name
     * @param name Name of the cache
     * @param <K> Key type (i.e String for uuid)
     * @param <X> Value type (object to cache; i.e Profile)
     * @return The Cache
     */
    @SuppressWarnings("unchecked") // bad, oops
    public <K, X extends Payload, D extends PayloadData> PayloadCache<K, X, D> getCache(String name) {
        return (PayloadCache<K, X, D>) this.caches.get(convertCacheName(name));
    }

    public List<PayloadCache> getSortedCachesByDepends() {
        if (this._sortedCaches != null) {
            if (!this.hasBeenModified()) {
                return this._sortedCaches;
            }
        }
        this._sortedCaches = new ArrayList<>(this.caches.values()).stream().sorted().collect(Collectors.toList());
        Collections.reverse(this._sortedCaches);
        return this._sortedCaches;
    }

    private boolean hasBeenModified() {
        return this.caches.size() != this._sortedCaches.size();
    }

}
