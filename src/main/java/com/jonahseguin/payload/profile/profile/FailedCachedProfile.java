package com.jonahseguin.payload.profile.profile;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class FailedCachedProfile<T extends PayloadProfile> implements ProfilePassable {

    private final CachingProfile<T> cachingProfile;
    private final String username;
    private final String uniqueId;
    private final String loginIp;

    public FailedCachedProfile(CachingProfile<T> cachingProfile, String username, String uniqueId, String ip) {
        this.cachingProfile = cachingProfile;
        this.username = username;
        this.uniqueId = uniqueId;
        this.loginIp = ip;
    }

    @Override
    public String getLoginIp() {
        return loginIp;
    }

    public boolean isOnline() {
        return Bukkit.getPlayerExact(username) != null;
    }

    public Player tryToGetPlayer() {
        return Bukkit.getPlayerExact(username);
    }

    public CachingProfile<T> getCachingProfile() {
        return cachingProfile;
    }

    public String getUsername() {
        return username;
    }

    public String getUniqueId() {
        return uniqueId;
    }

    @Override
    public String getName() {
        return username;
    }
}
