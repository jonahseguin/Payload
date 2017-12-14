package com.jonahseguin.payload.common.util;

import com.jonahseguin.payload.common.cache.CacheDatabase;
import com.jonahseguin.payload.common.cache.CacheDebugger;
import com.jonahseguin.payload.profile.profile.Profile;
/**
 * Created by Jonah on 11/16/2017.
 * Project: Payload
 *
 * @ 7:35 PM
 */
public class DefaultPayloadSettings {


    public static class EmptyCacheDatabase extends CacheDatabase {
        public EmptyCacheDatabase() {
            super(null, null, null, null, null);
        }
    }


    public static class EmptyDebugger implements CacheDebugger {
        @Override
        public void debug(String message) {

        }

        @Override
        public void error(Exception ex) {

        }

        @Override
        public void error(Exception ex, String message) {

        }

        @Override
        public boolean onStartupFailure() {
            return false;
        }
    }
}
