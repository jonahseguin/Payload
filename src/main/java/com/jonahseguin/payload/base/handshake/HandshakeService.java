package com.jonahseguin.payload.base.handshake;

import javax.annotation.Nonnull;

public interface HandshakeService {

    <H extends HandshakeController> void subscribe(@Nonnull Class<H> type);

    <H extends HandshakeController> HandshakeHandler<H> publish(@Nonnull H packet);

    void receiveReply(@Nonnull String channel, @Nonnull HandshakeData data);

    void receive(@Nonnull String channel, @Nonnull HandshakeData data);

}
