package com.jonahseguin.payload.base.type;

public interface PayloadInstantiator<X extends Payload, D extends PayloadData> {

    X instantiate(D data);

}
