package com.jonahseguin.payload.base.state;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.base.PayloadPermission;
import com.jonahseguin.payload.base.lang.PLang;
import com.jonahseguin.payload.base.type.Payload;
import com.jonahseguin.payload.base.type.PayloadData;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CacheState<K, X extends Payload, D extends PayloadData> {

    private final PayloadCache<K, X, D> cache;

    private volatile boolean locked = false; // lock caching, tasks, etc.
    private volatile boolean joinable = false; // can players join

    public CacheState(PayloadCache<K, X, D> cache) {
        this.cache = cache;
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

    /**
     * Unlocks the cache opposite of {@link #lock()}
     * @see #lock()
     */
    public final void unlock() {
        if (this.locked) {
            this.locked = false;
            this.cache.alert(PayloadPermission.ADMIN, PLang.CACHE_UNLOCKED);
        }
    }

}
