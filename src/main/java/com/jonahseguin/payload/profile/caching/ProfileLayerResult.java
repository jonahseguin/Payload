package com.jonahseguin.payload.profile.caching;

import com.jonahseguin.payload.profile.profile.ProfilePassable;
import lombok.Data;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 8:11 PM
 */
@Data
public class ProfileLayerResult<T extends ProfilePassable> {

    private final boolean success; // if it was able to provide
    private final boolean errors; // if any errors occurred
    private final T result;

}
