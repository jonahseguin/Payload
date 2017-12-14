package com.jonahseguin.payload.profile.type;

import com.jonahseguin.payload.profile.profile.CachingProfile;
import com.jonahseguin.payload.profile.profile.FailedCachedProfile;
import com.jonahseguin.payload.profile.profile.Profile;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProfileCacheResult<T extends Profile> {

    private CachingProfile<T> cachingProfile; // Nullable
    private T profile; // Nullable
    private FailedCachedProfile<T> failedCachedProfile;
    private String uniqueId;
    private String name;
    private boolean success;
    private PCacheStage cacheStage;
    private PCacheSource cacheSource;
    private boolean allowedJoin;

}
