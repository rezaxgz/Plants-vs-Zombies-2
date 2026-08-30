package io.github.Plants_Vs_Zombies_2.server;

import io.github.Plants_Vs_Zombies_2.network.protocol.MessageType;

record MatchmakingEvent(String username, MessageType type, Object payload) { }
