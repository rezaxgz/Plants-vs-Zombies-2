package io.github.Plants_Vs_Zombies_2.network.session;

public enum ClientSessionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    REGISTERING,
    AUTHENTICATING,
    AUTHENTICATED,
    LOGGING_OUT,
    CLOSED
}
