package com.jonahseguin.payload.base.type;


import dev.morphia.query.Query;

public interface PayloadQueryModifier<X extends Payload> {

    void apply(Query<X> query);

}
