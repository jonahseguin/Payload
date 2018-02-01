package com.jonahseguin.payload.profile.event;

import com.jonahseguin.payload.profile.cache.PayloadProfileCache;
import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.PayloadProfile;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 8:07 PM
 */
public class PayloadProfileLoadedEvent<X extends PayloadProfile> extends Event {

    private static final HandlerList handlerList = new HandlerList();

    private final CachingProfile<X> cachingProfile;
    private final PayloadProfileCache<X> cache;
    private final X profile;
    private final String ip;
    private boolean joinable = true;
    private String joinDenyMessage = PayloadProfileCache.FAILED_CACHE_KICK_MESSAGE;

    public PayloadProfileLoadedEvent(CachingProfile<X> cachingProfile, PayloadProfileCache<X> cache, X profile, String ip) {
        super(true);
        this.cachingProfile = cachingProfile;
        this.cache = cache;
        this.profile = profile;
        this.ip = ip;
    }

    public static HandlerList getHandlerList() {
        return handlerList;
    }

    public Player tryToGetPlayer() {
        return Bukkit.getPlayerExact(profile.getName());
    }

    public String getIp() {
        return ip;
    }

    public String getJoinDenyMessage() {
        return joinDenyMessage;
    }

    public void setJoinDenyMessage(String joinDenyMessage) {
        this.joinDenyMessage = joinDenyMessage;
    }

    public boolean isJoinable() {
        return joinable;
    }

    public void setJoinable(boolean joinable) {
        this.joinable = joinable;
    }

    @Override
    public HandlerList getHandlers() {
        return handlerList;
    }

    public CachingProfile<X> getCachingProfile() {
        return cachingProfile;
    }

    public PayloadProfileCache<X> getCache() {
        return cache;
    }

    public X getProfile() {
        return profile;
    }
}
