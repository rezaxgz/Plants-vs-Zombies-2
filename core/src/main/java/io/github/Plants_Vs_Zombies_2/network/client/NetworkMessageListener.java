package io.github.Plants_Vs_Zombies_2.network.client;

import io.github.Plants_Vs_Zombies_2.network.protocol.ProtocolMessage;

public interface NetworkMessageListener {
    void onMessage(ProtocolMessage message);

    default void onDisconnected(Throwable cause) {
    }
}
