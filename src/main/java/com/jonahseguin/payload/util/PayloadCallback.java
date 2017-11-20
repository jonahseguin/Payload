package com.jonahseguin.payload.util;

/**
 * Created by Jonah on 10/15/2017.
 * Project: purifiedCore
 *
 * @ 9:07 PM
 */
public interface PayloadCallback<V> {

    void call(V v);

}
