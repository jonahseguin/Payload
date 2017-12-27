package com.jonahseguin.payload.profile.cache;

import com.jonahseguin.payload.profile.profile.PayloadProfile;
import com.jonahseguin.payload.profile.type.ProfileCriteria;
import org.apache.commons.lang.Validate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * This will be the API-accessible implementation of our PayloadProfile ProfileCache.
 * It will act as a user-friendly wrapper for the cache, with the optional easy access to the internals of the cache.
 * Methods here should be well documented
 */
public class SimpleProfileCache<T extends PayloadProfile> implements ProfileCache<T> {

    private final PayloadProfileCache<T> cache; // The internal cache

    /**
     * Creates the simplistic front-end cache using an internal PayloadProfile ProfileCache
     * @param cache {@link PayloadProfileCache}
     */
    public SimpleProfileCache(PayloadProfileCache<T> cache) {
        this.cache = cache;
    }

    /**
     * Gets a profile using a not null Player
     * @param player Player
     * @return PayloadProfile or null if profile does not exist
     */
    @Override
    public T getProfile(Player player) {
        Validate.notNull(player, "Player cannot be null");
        return cache.getProfile(player);
    }

    /**
     * Saves a profile everywhere (locally, redis, + mongo)
     * @param profile The PayloadProfile to save
     */
    @Override
    public void save(T profile) {
        Validate.notNull(profile, "PayloadProfile cannot be null");
        cache.saveEverywhere(profile);
    }

    /**
     * Gets a profile from only the local cache using a not null Player
     * @param player Player
     * @return PayloadProfile or null if not locally cached
     */
    @Override
    public T getLocalProfile(Player player) {
        Validate.notNull(player, "Player cannot be null");
        return cache.getLocalProfile(player);
    }

    /**
     * Gets a profile from a uniqueId (UUID); can be online or offline: will use first available cache layer
     * @param uniqueId The obj's {@link java.util.UUID#toString()}
     * @return PayloadProfile or null if the profile does not exist
     */
    @Override
    public T getProfile(String uniqueId) {
        Validate.notNull(uniqueId, "UUID cannot be null");
        return cache.getProfile(uniqueId);
    }

    /**
     * Gets a profile from a username {@link Player#getName()}
     * Will attempt to convert it to a UUID if possible, otherwise fetch from Mongo using the username
     * @param username The obj's username
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
     * @param criteria The PayloadProfile Criteria
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
