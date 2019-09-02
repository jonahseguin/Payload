package com.jonahseguin.payload.mode.profile.pubsub;

import com.jonahseguin.payload.mode.profile.ProfileData;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Data
@Getter
@Setter
public class HandshakingPayload {

    private final ProfileData data;
    private final UUID uuid;
    private final String cacheName;
    private volatile boolean serverFound = false;
    private volatile boolean handshakeComplete = false;
    private volatile boolean timedOut = false;
    private long handshakeStartTime = -1L;

}
