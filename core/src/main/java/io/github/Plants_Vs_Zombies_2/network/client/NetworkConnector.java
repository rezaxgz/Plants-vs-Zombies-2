package io.github.Plants_Vs_Zombies_2.network.client;

import java.io.IOException;
import java.net.Socket;

@FunctionalInterface
interface NetworkConnector {
    Socket connect(String host, int port, int timeoutMillis) throws IOException;
}
