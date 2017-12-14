package com.jonahseguin.payload.profile.type;

import lombok.Data;

/**
 * Created by Jonah on 10/19/2017.
 * Project: purifiedCore
 *
 * @ 9:03 PM
 */
@Data
public class ProfileCriteria {

    private final String value;
    private final Type type;

    public static ProfileCriteria fromUsername(String username) {
        return new ProfileCriteria(username, Type.USERNAME);
    }

    public static ProfileCriteria fromUUID(String uniqueId) {
        return new ProfileCriteria(uniqueId, Type.UUID);
    }

    public enum Type {
        USERNAME, UUID
    }

}
