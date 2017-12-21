package com.jonahseguin.payload.object.layers;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.object.cache.PayloadObjectCache;
import com.jonahseguin.payload.object.obj.ObjectCacheable;
import com.jonahseguin.payload.object.type.OLayerType;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

@Getter
public abstract class ObjectCacheLayer<X extends ObjectCacheable> {

    protected final CacheDatabase database;
    protected final PayloadObjectCache<X> cache;
    protected final Plugin plugin;

    public ObjectCacheLayer(PayloadObjectCache<X> cache, CacheDatabase database) {
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

    public abstract boolean save(String id, X x);

    public abstract boolean has(String id);

    public abstract boolean remove(String id);

    public abstract boolean init();

    public abstract boolean shutdown();

    public abstract OLayerType source();

    public abstract int cleanup();

    public abstract int clear();

}
