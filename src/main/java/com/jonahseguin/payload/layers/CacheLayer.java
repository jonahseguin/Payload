package com.jonahseguin.payload.layers;

import com.jonahseguin.payload.cache.CacheDatabase;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.fail.CacheFailureHandler;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.profile.ProfilePassable;
import com.jonahseguin.payload.type.CacheSource;
import lombok.Getter;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

@Getter
public abstract class CacheLayer<P extends Profile, T extends ProfilePassable, Passable extends ProfilePassable> {

    protected final CacheDatabase database;
    protected final ProfileCache<P> cache;
    protected final Plugin plugin;

    public CacheLayer(ProfileCache<P> cache, CacheDatabase database) {
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
     * Called when it is this Cache Layer's turn to provide it's part to the Cache process.
     * Each Layer provides it's own Passable subclass, respective to the role of the Cache Layer.
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

    public abstract CacheSource source();

    public abstract int cleanup();

    public abstract int clear();

    public CacheFailureHandler<P> getFailureHandler() {
        return cache.getFailureHandler();
    }

}
