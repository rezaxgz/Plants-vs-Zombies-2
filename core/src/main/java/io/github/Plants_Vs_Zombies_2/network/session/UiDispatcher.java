package io.github.Plants_Vs_Zombies_2.network.session;

@FunctionalInterface
public interface UiDispatcher {
    void dispatch(Runnable runnable);
}
