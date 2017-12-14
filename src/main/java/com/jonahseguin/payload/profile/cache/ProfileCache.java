package com.jonahseguin.payload.profile.cache;

import com.jonahseguin.payload.profile.profile.Profile;
import com.jonahseguin.payload.profile.type.ProfileCriteria;

import java.util.UUID;

import org.bukkit.entity.Player;

/**
 *
 * This is the API-frontend partition of the Purified Caching system.
 * This is what is 'publicly' accessible via the API;
 * however the internals are easily accessible as well.
 *
 * Created by Jonah on 10/19/2017.
 * Project: purifiedCore
 *
 * @ 5:50 PM
 */
public interface ProfileCache<T extends Profile> {

    /**
     * Get a profile by a obj object
     * It is important to note that it is possible for this to return a *Temporary Profile*, denoted when the Profile
     * has the 'temporary' field set to true.  This means the profile was still being loaded when it was requested
     * by this method, and it created a blank profile for the obj based on their username and uuid.
     * This is a synchronous method that has the possibility to make a database call.  Async wrapping is suggested
     * to prevent slowing of the server.
     * If you only want the Local Profile (no possibility of database activity), call {@link #getLocalProfile(Player)}
     * Note that this method effectively calls {@code return this.getProfile(obj.getUniqueId()); }
     * Thus it just uses the Player's UUID to get the Profile using {@link #getProfile(String)}
     *
     * @param player Player
     * @return The profile, or none if it does not exist anywhere (temporary joining cache -> local -> redis -> mongo)
     */
    T getProfile(Player player);

    /**
     * Returns a Profile for the given obj; only attempting to get from the local cache (no database calls)
     *
     * @param player Player
     * @return The respective Profile if cached, or null if not in the cache
     */
    T getLocalProfile(Player player);

    /**
     * The same as {@link #getProfile(Player)}, but uses the uniqueId as the query criteria rather than the Player object.
     * This method, like the {@link #getProfile(Player)} method, has the possibility of making database calls;
     * so Async wrapping is suggested.
     * This method also has the possibility of returning
     *
     * @param uniqueId The obj's {@link UUID#toString()}
     * @return The profile, or none if it does not exist anywhere (temporary joining cache -> local -> redis -> mongo)
     */
    T getProfile(String uniqueId);

    /**
     * Attempts to find a profile using the obj's username as the query criteria.
     * Can make database calls, same as {@link #getProfile(String)} and {@link #getProfile(Player)}, but using the
     * username rather than UUID or Player.
     * Async wrapping suggested.
     * Not recommended for important data storage as usernames can be changed.
     * Note: the Profile's username is updated whenever they login (all internal caching systems are based on UUIDs)
     *
     * @param username The obj's username
     * @return The profile, or none if it does not exist anywhere (temporary joining cache -> local -> redis -> mongo)
     */
    T getProfileByUsername(String username);

    /**
     * Checks if the profile is available anywhere
     *
     * @param criteria The Profile Criteria
     * @return True if available, False if null in all caches.
     */
    boolean hasProfileAvailable(ProfileCriteria criteria);

    /**
     * Saves the give Profile *EVERYWHERE* (Locally, Redis, & MongoDB)
     * Sync call.  Async wrapping is suggested.
     * Could possibly produce errors; checking is suggested (for null, etc.)
     *
     * @param profile The Profile to save
     */
    void save(T profile);

}
