/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.database;

public interface DatabaseDependent {

    /**
     * Does this database-dependent object depend on Redis for functionality?
     * @return True if required
     */
    boolean requireRedis();

    /**
     * Does this database-dependent object depend on MongoDB for functionality?
     * @return True if required
     */
    boolean requireMongoDb();


}
