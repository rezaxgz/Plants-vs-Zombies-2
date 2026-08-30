package io.github.Plants_Vs_Zombies_2.server;

import java.io.IOException;

public final class ServerLauncher {
    private ServerLauncher() {
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        String host = System.getProperty("pvz.server.host", GameServer.DEFAULT_HOST);
        int port = Integer.getInteger("pvz.server.port", GameServer.DEFAULT_PORT);
        for (String argument : args) {
            if (argument.startsWith("--host=")) {
                host = argument.substring("--host=".length());
            } else if (argument.startsWith("--port=")) {
                port = Integer.parseInt(argument.substring("--port=".length()));
            } else {
                throw new IllegalArgumentException("Unknown argument: " + argument);
            }
        }

        GameServer server = new GameServer(host, port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "pvz2-server-shutdown"));
        server.start();
        server.awaitShutdown();
    }
}
