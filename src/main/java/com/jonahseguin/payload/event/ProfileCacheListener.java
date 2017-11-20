package com.jonahseguin.payload.event;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.cache.ProfileCache;
import com.jonahseguin.payload.caching.CachingController;
import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.CacheStage;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProfileCacheListener<T extends Profile> implements Listener {

    private final ProfileCache<T> profileCache;

    public ProfileCacheListener(ProfileCache<T> profileCache) {
        this.profileCache = profileCache;
    }

    @EventHandler
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        Payload.runASync(profileCache.getPlugin(), () -> profileCache.getController(event.getName(), event.getUniqueId().toString())
                .cache());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (profileCache.hasController(event.getPlayer().getUniqueId().toString())) {
            CachingController<T> controller = profileCache.getController(event.getPlayer());
            controller.join(event.getPlayer());
        }
    }

    @EventHandler
    public void onProfileCached(PayloadPlayerLoadedEvent<T> event) {
        if (event.getCache().getCacheId().equals(profileCache.getCacheId())) {
            if (event.getProfile() != null) {
                profileCache.destroyController(event.getProfile().getUniqueId());
            }
        }
    }

    @EventHandler
    public void onQuitSaveProfile(final PlayerQuitEvent event) {
        profileCache.destroyController(event.getPlayer().getUniqueId().toString());
        Payload.runASync(profileCache.getPlugin(), () -> {
            T profile = profileCache.getProfile(event.getPlayer());
            if (profile != null) {
                profileCache.getLayerController().getRedisLayer().save(profile); // Save to redis always (3 hour exp.)
                if (profileCache.getSettings().isCacheLogoutSaveDatabase()) {
                    profileCache.getLayerController().getMongoLayer().save(profile); // Save to MongoDB if enabled
                }
                if (profileCache.getSettings().isCacheRemoveOnLogout()) {
                    profileCache.getLayerController().getLocalLayer().remove(event.getPlayer().getUniqueId().toString()); // Remove them from the local cache
                }
            } else {
                profileCache.getDebugger().debug(Payload.format("&c{0} logged out with a null profile", event.getPlayer().getName()));
            }
        });
    }

}
