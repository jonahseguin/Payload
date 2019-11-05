package com.jonahseguin.payload.base.handshake;

import com.jonahseguin.payload.base.Service;

import javax.annotation.Nonnull;

public interface HandshakeService extends Service {

    <H extends Handshake> void subscribe(@Nonnull Class<H> type);

    <H extends Handshake> HandshakeHandler<H> publish(@Nonnull H packet);

    void receiveReply(@Nonnull String channel, @Nonnull HandshakeData data);

    void receive(@Nonnull String channel, @Nonnull HandshakeData data);

}
