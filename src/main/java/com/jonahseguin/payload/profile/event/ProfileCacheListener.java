package com.jonahseguin.payload.profile.event;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.caching.ProfileCachingController;
import com.jonahseguin.payload.profile.profile.PayloadProfile;

import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProfileCacheListener<T extends PayloadProfile> implements Listener {

    private final PayloadProfileCache<T> profileCache;

    public ProfileCacheListener(PayloadProfileCache<T> profileCache) {
        this.profileCache = profileCache;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        final String ip = Payload.getIP(event.getAddress());
        profileCache.getDebugger().debug("Called AsyncPlayerPreLoginEvent: " + event.getName());
        if (!profileCache.isAllowJoinsMode()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    ChatColor.GREEN + "The server's cache system is still starting up, please try logging in again.");
            return;
        }
        if (profileCache.getSettings().isEnableAsyncCaching()) {
            profileCache.getDebugger().debug("Using async caching");
            Payload.runASync(profileCache.getPlugin(), () -> profileCache.getController(event.getName(), event.getUniqueId().toString(), ip)
                    .cache(ip));
        }
        else {
            profileCache.getDebugger().debug("Using sync caching");
            ProfileCachingController<T> controller = profileCache.getController(event.getName(), event.getUniqueId().toString(), ip);
            T profile = controller.cache(ip);
            if (!controller.isJoinable()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, controller.getJoinDenyMessage());
            }
            else {
                if (profile == null) {
                    if (controller.getCachingProfile() != null) {
                        controller.getCache().getFailureHandler().startFailureHandling(controller.getCachingProfile());
                    }
                    else {
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, controller.getJoinDenyMessage());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProfileInitialized(PayloadProfileInitializedEvent<T> event) {
        profileCache.getDebugger().debug("Called PayloadProfileInitializedEvent: " + event.getProfile().getName());
        if (event.getCache().getCacheId().equals(profileCache.getCacheId())) {
            profileCache.getDebugger().debug("Initialized profile: " + event.getProfile().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOW) // To load first -- but also have a possible event that happens before (LOWEST) in case another plugin wanted that
    public void onPlayerJoin(PlayerJoinEvent event) {
        profileCache.getDebugger().debug("Called PlayerJoinEvent: " + event.getPlayer().getName());
        if (profileCache.hasController(event.getPlayer().getUniqueId().toString())) {
            ProfileCachingController<T> controller = profileCache.getController(event.getPlayer());
            controller.join(event.getPlayer()); // Handle the continuation of caching after join
        }
        else {
            event.getPlayer().kickPlayer(PayloadProfileCache.FAILED_CACHE_KICK_MESSAGE);
            profileCache.getDebugger().debug("Did not have controller on join for " + event.getPlayer().getName());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Have it occur second last to allow flexibility of other plugins
    public void onQuitSaveProfile(final PlayerQuitEvent event) {
        Payload.runASync(profileCache.getPlugin(), () -> {
            profileCache.destroyController(event.getPlayer().getUniqueId().toString());
            T profile = profileCache.getProfile(event.getPlayer());
            if (profile != null) {
                PayloadProfilePreQuitSaveEvent<T> preQuitSaveEvent = new PayloadProfilePreQuitSaveEvent<>(true, profile, profileCache);
                profileCache.getPlugin().getServer().getPluginManager().callEvent(preQuitSaveEvent);
                profile.setInitialized(false);
                profileCache.getLayerController().getRedisLayer().save(profile); // Save to redis always (3 hour exp.)
                if (profileCache.getSettings().isCacheLogoutSaveDatabase()) {
                    profileCache.getLayerController().getMongoLayer().save(profile); // Save to MongoDB if enabled
                }
                profileCache.getLayerController().getUsernameUUIDLayer().remove(event.getPlayer().getUniqueId().toString()); // In case of username changes
                profileCache.getLayerController().getPreCachingLayer().remove(event.getPlayer().getUniqueId().toString()); // Always Remove from pre-caching (CachingProfile)
                if (profileCache.getSettings().isCacheRemoveOnLogout()) {
                    profileCache.getLayerController().getLocalLayer().remove(event.getPlayer().getUniqueId().toString()); // Remove them from the local cache
                }
            } else {
                profileCache.getDebugger().debug(Payload.format("&c{0} logged out with a null profile", event.getPlayer().getName()));
            }
        });
    }

}
