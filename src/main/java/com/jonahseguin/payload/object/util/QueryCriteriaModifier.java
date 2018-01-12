package com.jonahseguin.payload.object.util;

import com.jonahseguin.payload.object.obj.ObjectCacheable;
import org.mongodb.morphia.query.Query;

/**
 * Created by Jonah on 1/5/2018.
 * Project: Payload
 *
 * @ 3:50 PM
 */
public interface QueryCriteriaModifier<X extends ObjectCacheable> {

    void apply(Query<X> query);

}
