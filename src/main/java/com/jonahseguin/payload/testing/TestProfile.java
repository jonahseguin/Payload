package com.jonahseguin.payload.testing;

import com.jonahseguin.payload.base.PayloadCache;
import com.jonahseguin.payload.mode.profile.PayloadProfile;

public class TestProfile extends PayloadProfile {

    @Override
    public String getIdentifier() {
        return null;
    }

    @Override
    public PayloadCache getCache() {
        return null;
    }
}
