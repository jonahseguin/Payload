/*
 * Copyright (c) 2019 Jonah Seguin.  All rights reserved.  You may not modify, decompile, distribute or use any code/text contained in this document(plugin) without explicit signed permission from Jonah Seguin.
 * www.jonahseguin.com
 */

package com.jonahseguin.payload.base.handshake;

import com.jonahseguin.payload.base.Service;

import javax.annotation.Nonnull;

public interface HandshakeService extends Service {

    <H extends Handshake> void subscribe(@Nonnull H subscriber);

    <H extends Handshake> HandshakeHandler<H> publish(@Nonnull H packet);

    void receiveReply(@Nonnull String channel, @Nonnull HandshakeData data);

    void receive(@Nonnull String channel, @Nonnull HandshakeData data);

}
