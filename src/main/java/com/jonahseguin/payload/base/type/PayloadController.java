package com.jonahseguin.payload.base.type;

import javax.annotation.Nonnull;
import java.util.Optional;

public interface PayloadController<X extends Payload> {

    Optional<X> cache();

    void uncache(@Nonnull X payload, boolean switchingServers);

}
