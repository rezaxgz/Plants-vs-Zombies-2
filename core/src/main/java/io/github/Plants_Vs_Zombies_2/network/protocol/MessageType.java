package io.github.Plants_Vs_Zombies_2.network.protocol;

public enum MessageType {
    CLIENT_HELLO,
    SERVER_HELLO,
    PING,
    PONG,
    REGISTER_REQUEST,
    REGISTER_RESPONSE,
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    LOGOUT_REQUEST,
    LOGOUT_RESPONSE,
    GET_PROFILE_REQUEST,
    GET_PROFILE_RESPONSE,
    ERROR
}
