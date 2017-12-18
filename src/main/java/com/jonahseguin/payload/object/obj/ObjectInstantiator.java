package com.jonahseguin.payload.object.obj;

/**
 * Created by Jonah on 12/17/2017.
 * Project: Payload
 *
 * @ 3:08 PM
 */
public interface ObjectInstantiator<X extends ObjectCacheable> {

    X instantiate(String id);

    X instantiate();

}
