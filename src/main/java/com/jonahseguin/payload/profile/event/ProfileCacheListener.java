package com.jonahseguin.payload.profile.event;

import com.jonahseguin.payload.Payload;
import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.caching.ProfileCachingController;
import com.jonahseguin.payload.profile.profile.Profile;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ProfileCacheListener<T extends Profile> implements Listener {

    private final PayloadProfileCache<T> profileCache;

    public ProfileCacheListener(PayloadProfileCache<T> profileCache) {
        this.profileCache = profileCache;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        if (!profileCache.isAllowJoinsMode()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST,
                    ChatColor.GREEN + "The server's cache system is still starting up, please try logging in again.");
            return;
        }
        if (profileCache.getSettings().isEnableAsyncCaching()) {
            Payload.runASync(profileCache.getPlugin(), () -> profileCache.getController(event.getName(), event.getUniqueId().toString())
                    .cache());
        }
        else {
            ProfileCachingController<T> controller = profileCache.getController(event.getName(), event.getUniqueId().toString());
            T profile = controller.cache();
            if (!controller.isJoinable()) {
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, PayloadProfileCache.FAILED_CACHE_KICK_MESSAGE);
            }
            else {
                if (profile == null) {
                    if (controller.getCachingProfile() != null) {
                        controller.getCache().getFailureHandler().startFailureHandling(controller.getCachingProfile());
                    }
                    else {
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, PayloadProfileCache.FAILED_CACHE_KICK_MESSAGE);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onProfileInitialized(PayloadProfileInitializedEvent<T> event) {
        if (event.getCache().getCacheId().equals(profileCache.getCacheId())) {
            profileCache.getDebugger().debug("Initialized profile: " + event.getProfile().getName());
        }
    }

    @EventHandler(priority = EventPriority.LOW) // To load first -- but also have a possible event that happens before (LOWEST) in case another plugin wanted that
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (profileCache.hasController(event.getPlayer().getUniqueId().toString())) {
            ProfileCachingController<T> controller = profileCache.getController(event.getPlayer());
            controller.join(event.getPlayer()); // Handle the continuation of caching after join
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onProfileCached(PayloadProfileLoadedEvent<T> event) {
        if (event.getCache().getCacheId().equals(profileCache.getCacheId())) {
            if (event.getProfile() != null) {
                if (!event.getProfile().isInitialized()) {
                    Player player = event.tryToGetPlayer();
                    if (player != null) {
                        event.getCache().initProfile(player, event.getProfile());
                    } else {
                        profileCache.addAfterJoinTask(event.getCachingProfile(), (cachingProfile, player1) -> {
                            if (player1 != null) {
                                event.getCache().initProfile(player1, event.getProfile());
                            }
                        });
                    }
                }
                event.getCache().destroyController(event.getProfile().getUniqueId()); // Get rid of their controller after they are loaded
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST) // Have it occur second last to allow flexibility of other plugins
    public void onQuitSaveProfile(final PlayerQuitEvent event) {
        profileCache.destroyController(event.getPlayer().getUniqueId().toString());
        Payload.runASync(profileCache.getPlugin(), () -> {
            T profile = profileCache.getProfile(event.getPlayer());
            if (profile != null) {
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
