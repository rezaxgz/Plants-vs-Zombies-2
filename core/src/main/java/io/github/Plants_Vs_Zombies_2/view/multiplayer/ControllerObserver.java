package io.github.Plants_Vs_Zombies_2.view.multiplayer;

@FunctionalInterface
public interface ControllerObserver<T> {
    void changed(T state);
}
