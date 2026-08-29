package io.github.Plants_Vs_Zombies_2.network.session;

@FunctionalInterface
public interface SessionStateListener {
    /** Called on the thread that caused the state transition. */
    void onStateChanged(ClientSessionState previous, ClientSessionState current,
            Throwable failure);
}
