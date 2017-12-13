package com.jonahseguin.payload.profile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Created by jonahseguin on 2016-08-12.
 */
@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class CachedProfile<T extends Profile> implements ProfilePassable {

    private final T profile;
    private final long initialCacheTime;
    private long expiry = 0;
    private long loggedOutTime = 0;

    @Override
    public String getName() {
        return profile.getName();
    }

    @Override
    public String getUniqueId() {
        return profile.getUniqueId();
    }

    public boolean isOnline() {
        return profile != null && profile.getPlayer() != null && profile.getPlayer().isOnline();
    }

    public boolean isExpired() {
        return expiry != 0 && System.currentTimeMillis() >= expiry;
    }

}
