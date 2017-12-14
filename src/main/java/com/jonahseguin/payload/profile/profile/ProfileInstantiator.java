package com.jonahseguin.payload.profile.profile;

/**
 * Created by Jonah on 11/15/2017.
 * Project: Payload
 *
 * @ 8:54 PM
 */
public interface ProfileInstantiator<T extends Profile> {

    T instantiate(String username, String uniqueId);

}
