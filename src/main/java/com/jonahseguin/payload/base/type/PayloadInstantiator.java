package com.jonahseguin.payload.base.type;

public interface PayloadInstantiator<K, X extends Payload<K>, D extends PayloadData> {

    X instantiate(D data);

}
