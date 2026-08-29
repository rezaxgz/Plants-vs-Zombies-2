# Plants-vs-Zombies-2

A [libGDX](https://libgdx.com/) project generated with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

This project was generated with a template including simple application launchers and an `ApplicationAdapter` extension that draws libGDX logo.

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.

## Online account server and graphical client

Start the account server in one PowerShell window, then start the LibGDX client
in another:

```powershell
.\gradlew.bat server:run
.\gradlew.bat lwjgl3:run
```

The defaults are `127.0.0.1:54555` for both processes. Override the endpoint
with Java system properties passed to Gradle:

```powershell
.\gradlew.bat "-Dpvz.server.host=0.0.0.0" "-Dpvz.server.port=54556" server:run
.\gradlew.bat "-Dpvz.client.server.host=192.168.1.20" "-Dpvz.client.server.port=54556" lwjgl3:run
```

Server properties are `pvz.server.host`, `pvz.server.port`, and optionally
`pvz.server.users.database`. Client properties are `pvz.client.server.host` and
`pvz.client.server.port`.

Loopback only accepts clients on the same computer. For another computer on the
LAN, bind the server to an accessible address such as `0.0.0.0` and configure
the client with the server computer's LAN address. Configure firewall access
manually if required; the application does not change operating-system or
firewall settings.

Graphical signup, login, profile refresh, and logout use the server as their
authentication source of truth. Persistent remote login tokens and remote
password recovery are not implemented yet, so the graphical client requires a
fresh login after restart. The temporary `User` exposed to legacy gameplay is a
non-persisted compatibility snapshot with no usable local password; gameplay
progress synchronization remains out of scope for this stage.
