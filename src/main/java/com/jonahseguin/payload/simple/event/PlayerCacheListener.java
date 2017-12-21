package com.jonahseguin.payload.simple.event;

import com.jonahseguin.payload.simple.cache.PayloadSimpleCache;
import com.jonahseguin.payload.simple.simple.PlayerCacheable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Created by Jonah on 12/17/2017.
 * Project: Payload
 *
 * @ 5:43 PM
 */
public class PlayerCacheListener<X extends PlayerCacheable> implements Listener {

    private final PayloadSimpleCache<X> cache;

    public PlayerCacheListener(PayloadSimpleCache<X> cache) {
        this.cache = cache;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(PlayerLoginEvent event) {
        cache.cache(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (cache.getSettings().isRemoveOnLogout()) {
            cache.removeFromCache(event.getPlayer());
        }
        else {
            cache.initExpiry(event.getPlayer());
        }
    }

}
