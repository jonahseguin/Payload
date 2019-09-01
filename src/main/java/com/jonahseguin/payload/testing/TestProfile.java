package com.jonahseguin.payload.testing;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.mode.profile.PayloadProfile;
import com.jonahseguin.payload.mode.profile.ProfileData;

import java.util.UUID;

public class TestProfile extends PayloadProfile {

    public TestProfile() {
    }

    public TestProfile(String username, UUID uniqueId, String loginIp) {
        super(username, uniqueId, loginIp);
    }

    public TestProfile(ProfileData data) {
        super(data);
    }

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public PayloadCache getCache() {
        return null;
    }
}
