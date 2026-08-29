package io.github.Plants_Vs_Zombies_2.model.auth;

import io.github.Plants_Vs_Zombies_2.model.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);

    List<User> findAll();

    boolean addIfUsernameAvailable(User user);
}
