package io.github.Plants_Vs_Zombies_2.network.client;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;

/**
 * Receives messages on the networking reader thread. UI consumers must later
 * forward work to LibGDX with {@code Gdx.app.postRunnable} before touching actors.
 */
public interface NetworkMessageListener {
    void onMessage(ProtocolMessage message);

    default void onDisconnected(Throwable cause) {
    }
}
