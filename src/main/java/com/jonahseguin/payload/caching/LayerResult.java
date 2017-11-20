package com.jonahseguin.payload.caching;

import com.jonahseguin.payload.profile.ProfilePassable;
import lombok.Data;

/**
 * Created by Jonah on 11/18/2017.
 * Project: Payload
 *
 * @ 8:11 PM
 */
@Data
public class LayerResult<T extends ProfilePassable> {

    private final boolean success; // if it was able to provide
    private final boolean errors; // if any errors occurred
    private final T result;

}
