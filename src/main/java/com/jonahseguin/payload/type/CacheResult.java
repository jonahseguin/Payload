package com.jonahseguin.payload.type;

import com.jonahseguin.payload.profile.CachingProfile;
import com.jonahseguin.payload.profile.FailedCachedProfile;
import com.jonahseguin.payload.profile.Profile;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CacheResult<T extends Profile> {

    private CachingProfile<T> cachingProfile; // Nullable
    private T profile; // Nullable
    private FailedCachedProfile<T> failedCachedProfile;
    private String uniqueId;
    private String name;
    private boolean success;
    private CacheStage cacheStage;
    private CacheSource cacheSource;
    private boolean allowedJoin;

}
