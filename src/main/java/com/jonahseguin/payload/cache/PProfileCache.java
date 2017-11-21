package com.jonahseguin.payload.cache;

import com.jonahseguin.payload.profile.Profile;
import com.jonahseguin.payload.type.ProfileCriteria;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * This will be the API-accessible implementation of our Profile Cache.
 * It will act as a user-friendly wrapper for the cache, with the optional easy access to the internals of the cache.
 * Methods here should be well documented
 */
public class PProfileCache<T extends Profile> implements Cache<T> {

    private final ProfileCache<T> cache; // The internal cache

    /**
     * Creates the simplistic front-end cache using an internal Profile Cache
     * @param cache {@link ProfileCache}
     */
    public PProfileCache(ProfileCache<T> cache) {
        this.cache = cache;
    }

    /**
     * Gets a profile using a not null Player
     * @param player Player
     * @return Profile or null if profile does not exist
     */
    @Override
    public T getProfile(Player player) {
        Validate.notNull(player, "Player cannot be null");
        return cache.getProfile(player);
    }

    /**
     * Saves a profile everywhere (locally, redis, + mongo)
     * @param profile The Profile to save
     */
    @Override
    public void save(T profile) {
        Validate.notNull(profile, "Profile cannot be null");
        cache.saveEverywhere(profile);
    }

    /**
     * Gets a profile from only the local cache using a not null Player
     * @param player Player
     * @return Profile or null if not locally cached
     */
    @Override
    public T getLocalProfile(Player player) {
        Validate.notNull(player, "Player cannot be null");
        return cache.getLocalProfile(player);
    }

    /**
     * Gets a profile from a uniqueId (UUID); can be online or offline: will use first available cache layer
     * @param uniqueId The player's {@link java.util.UUID#toString()}
     * @return Profile or null if the profile does not exist
     */
    @Override
    public T getProfile(String uniqueId) {
        Validate.notNull(uniqueId, "UUID cannot be null");
        return cache.getProfile(uniqueId);
    }

    /**
     * Gets a profile from a username {@link Player#getName()}
     * Will attempt to convert it to a UUID if possible, otherwise fetch from Mongo using the username
     * @param username The player's username
     * @return The profile or null if it does not exist locally, via username-->uuid reverse lookup, or in Mongo
     */
    @Override
    public T getProfileByUsername(String username) {
        Validate.notNull(username, "Username cannot be null");
        if (cache.getLayerController().getUsernameUUIDLayer().hasUniqueId(username)) {
            return getProfile(cache.getLayerController().getUsernameUUIDLayer().getUniqueId(username));
        } else {
            Player exact = Bukkit.getPlayerExact(username);
            if (exact != null) {
                return getProfile(exact);
            }
            else {
                // Manual via MongoDB
                return cache.getLayerController().getMongoLayer().getByUsername(username);
            }
        }
    }

    /**
     * Checks if a profile exists in any layer with given criteria
     * @param criteria The Profile Criteria
     * @return boolean: True if exists
     */
    @Override
    public boolean hasProfileAvailable(ProfileCriteria criteria) {
        if (criteria.getType() == ProfileCriteria.Type.USERNAME) {
            String username = criteria.getValue();
            return getProfileByUsername(username) != null;
        } else if (criteria.getType() == ProfileCriteria.Type.UUID) {
            String uniqueId = criteria.getValue();
            return getProfile(uniqueId) != null;
        } else {
            throw new IllegalArgumentException("Unknown ProfileCritera Type: " + criteria.getType().toString());
        }
    }
}
