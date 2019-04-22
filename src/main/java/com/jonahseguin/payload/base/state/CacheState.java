package com.jonahseguin.payload.base.state;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.type.Payload;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheState<K, X extends Payload> {

    private final PayloadCache<K, X> cache;

    private volatile boolean locked = false; // lock caching, tasks, etc.
    private volatile boolean joinable = false; // can players join

    private volatile boolean mongoConnected = false;
    private volatile boolean redisConnected = false;

    private volatile boolean mongoConnecting = false;

    public CacheState(PayloadCache<K, X> cache) {
        this.cache = cache;
    }

    /**
     * Check the connectivity of both databases
     * @return true if both are connected
     */
    public boolean isDatabaseConnected() {
        return this.mongoConnected && this.redisConnected;
    }

    /**
     * Lock the cache
     * This will prevent the Task Executor from executing further tasks, and will also prevent non-admin/operator players
     * from joining the server
     */
    public final void lock() {
        if (!this.locked) {
            this.locked = true;
            this.cache.alert(PayloadPermission.ADMIN, PLang.CACHE_LOCKED);
        }
    }

    public final void unlock() {
        if (this.locked) {
            this.locked = false;
            this.cache.alert(PayloadPermission.ADMIN, PLang.CACHE_UNLOCKED);
        }
    }

}
