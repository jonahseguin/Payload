package com.jonahseguin.payload.profile.profile;

/**
 * Created by Jonah on 11/15/2017.
 * Project: Payload
 *
 * @ 8:54 PM
 */
public interface ProfileInstantiator<T extends PayloadProfile> {

    /**
     * Used to instantiate a profile
     * @param username Username
     * @param uniqueId UUID
     * @return The instantiated profile
     */
    T instantiate(String username, String uniqueId);

}
