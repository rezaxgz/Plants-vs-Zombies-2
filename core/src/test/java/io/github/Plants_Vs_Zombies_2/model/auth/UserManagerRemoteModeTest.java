package io.github.Plants_Vs_Zombies_2.model.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.github.Plants_Vs_Zombies_2.model.enums.Gender;
import io.github.Plants_Vs_Zombies_2.model.user.User;

class UserManagerRemoteModeTest {
    @Test
    void remoteOnlyModeCannotWriteOrMutateLegacyDatabase() throws Exception {
        Path database = UserManager.getDatabasePath().toAbsolutePath().normalize();
        boolean existed = Files.exists(database);
        byte[] before = existed ? Files.readAllBytes(database) : null;
        UserManager.useRemoteOnlyMode();
        try {
            assertFalse(UserManager.isPersistenceEnabled());
            UserManager.saveAllUsers();
            assertThrows(IllegalStateException.class, () ->
                    UserManager.addUserToDatabase(new User(
                            "remote-guard", "GoodPass1!", "Remote Guard",
                            "remote-guard@example.com", Gender.MALE)));
            if (existed) {
                assertArrayEquals(before, Files.readAllBytes(database));
            } else {
                assertFalse(Files.exists(database));
            }
        } finally {
            UserManager.restoreLocalModeForTesting();
        }
    }
}
