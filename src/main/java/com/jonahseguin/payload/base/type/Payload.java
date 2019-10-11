/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.type;

import com.jonahseguin.payload.base.PayloadCache;

/**
 * A Payload is an object that can be cached, saved, or loaded within the payload system.  Is it the functional
 * object that we aim to handle.  Multiple implementations of a Payload are available for different caching strategies
 */
public interface Payload<K> {

    /**
     * Gets the identifier to base our Payload on.  For local storage in a HashMap key, for instance.
     * This identifier should be unique to an object. (For example an ObjectID or UUID)
     * @return K Identifier
     */
    K getIdentifier();

    /**
     * The field name that {@link #getIdentifier()} returns from
     * The name of the field for the identifier (i.e for MongoDB lookup)
     *
     * @return String: identifier field name
     */
    String identifierFieldName();

    /**
     * Internal method to get the cache associated with this Payload object.
     * Every Payload object must have their matching cache stored in their object locally (non-persistent / transient)
     * @return PayloadCache
     */
    PayloadCache getCache();

    /**
     * Get a timestamp of when this object was cached
     * (i.e or last interaction with this object)
     *
     * @return Timestamp of cache
     */
    long cachedTimestamp();

    /**
     * Called internally when this profile/payload/object is accessed.  Used for updating last-access times to handle cache expiry/object
     * cleanup internally.
     */
    void interact();

    /**
     * For server-specific caches
     *
     * @return The Payload ID of the server this object is stored for
     */
    String getPayloadServer();

    /**
     * For server-specific caches
     *
     * @param payloadID The Payload ID of the server this object is stored for
     */
    void setPayloadServer(String payloadID);

    /**
     * Whether this object/profile should be saved
     * I.e only return true if a profile is online
     *
     * @return True if should save
     */
    boolean shouldSave();

    /**
     * Whether this object/profile needs to prepare (i.e be saved elsewhere) before an update/save
     *
     * @return true if should prepare before an update/save
     */
    boolean shouldPrepareUpdate();

    void save();

}
