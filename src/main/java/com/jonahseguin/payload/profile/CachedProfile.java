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
    private long expiry;

    @Override
    public String getName() {
        return profile.getName();
    }

    @Override
    public String getUniqueId() {
        return profile.getUniqueId();
    }
}
