package com.jonahseguin.payload;

/**
 * There are two modes: STANDALONE, or NETWORK_NODE
 *      * In standalone mode, Payload functions as it's own entity and will use login/logout events to handle caching normally
 *      * In contrast, in network node mode, Payload functions as a node in a BungeeCord/etc. proxied network,
 *      * where in such logins are handled before logouts, data is transferred through a handshake via Redis pub/sub if
 *      * a player is already logged into another node.
 */
public enum PayloadMode {

    STANDALONE,
    NETWORK_NODE

}
