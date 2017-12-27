package com.jonahseguin.payload.profile.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.fail.PCacheFailureHandler;
import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.profile.ProfilePassable;
import com.jonahseguin.payload.profile.type.PCacheSource;
import lombok.Getter;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

@Getter
public abstract class ProfileCacheLayer<P extends PayloadProfile, T extends ProfilePassable, Passable extends ProfilePassable> {

    protected final CacheDatabase database;
    protected final PayloadProfileCache<P> cache;
    protected final Plugin plugin;

    public ProfileCacheLayer(PayloadProfileCache<P> cache, CacheDatabase database) {
        this.cache = cache;
        this.plugin = cache.getPlugin();
        this.database = database;
    }

    public void debug(String message) {
        getCache().getDebugger().debug(message);
    }

    public void error(Exception ex, String message) {
        getCache().getDebugger().error(ex, message);
    }

    public void error(Exception ex) {
        getCache().getDebugger().error(ex);
    }

    protected String format(String s, String... args) {
        if (args != null) {
            if (args.length > 0) {
                for (int i = 0; i < args.length; i++) {
                    if (s.contains("{" + i + "}")) {
                        s = s.replace("{" + i + "}", args[i]);
                    }
                }
            }
        }

        return ChatColor.translateAlternateColorCodes('&', s);
    }

    /**
     * Called when it is this ProfileCache Layer's turn to provide it's part to the ProfileCache process.
     * Each Layer provides it's own Passable subclass, respective to the role of the ProfileCache Layer.
     *
     * @param passable The information required to provide a ProfilePassable
     * @return The provided ProfilePassable by this class
     */
    public abstract T provide(Passable passable);

    public abstract T get(String uniqueId);

    public abstract boolean save(T profilePassable);

    public abstract boolean has(String uniqueId);

    public abstract boolean remove(String uniqueId);

    public abstract boolean init();

    public abstract boolean shutdown();

    public abstract PCacheSource source();

    public abstract int cleanup();

    public abstract int clear();

    public PCacheFailureHandler<P> getFailureHandler() {
        return cache.getFailureHandler();
    }

}
