package com.jonahseguin.payload.simple.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.profile.type.PCacheSource;
import com.jonahseguin.payload.simple.cache.PayloadSimpleCache;
import com.jonahseguin.payload.simple.fail.SimpleCacheFailureHandler;
import com.jonahseguin.payload.simple.obj.SimpleCacheable;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

@Getter
public abstract class SimpleCacheLayer<X extends SimpleCacheable> {

    protected final CacheDatabase database;
    protected final PayloadSimpleCache<X> cache;
    protected final Plugin plugin;

    public SimpleCacheLayer(PayloadSimpleCache<X> cache, CacheDatabase database) {
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

    public abstract X provide(String id);

    public abstract boolean save(String id);

    public abstract boolean has(String id);

    public abstract boolean remove(String id);

    public abstract boolean init();

    public abstract boolean shutdown();

    public abstract PCacheSource source();

    public abstract int cleanup();

    public abstract int clear();

    public SimpleCacheFailureHandler getFailureHandler() {
        return cache.getFailureHandler(); // TODO
    }


}
