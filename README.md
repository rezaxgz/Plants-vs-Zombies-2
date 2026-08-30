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

Server properties are `pvz.server.host`, `pvz.server.port`, optionally
`pvz.server.users.database`, and `pvz.server.invitation.expiration.seconds`
(30 seconds by default). Client properties are `pvz.client.server.host` and
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

## Headless matchmaking foundation

Authenticated clients can use the typed `MatchmakingClient` exposed by
`RemoteAccountSession`. It reuses the session's existing `NetworkClient` socket;
it does not open a second connection. Its asynchronous API supports direct
invitations (accept, reject, cancel, and expiration) and a FIFO random queue.
Successful pairing delivers typed `MatchAssignment` values with one `PLANTS`
role, one `ZOMBIES` role, a shared match ID, creation time, and `PRE_GAME` status.

The wire protocol uses correlated request/response pairs for invitation and
queue commands. `INVITATION_RECEIVED`, `INVITATION_RESULT`,
`QUEUE_STATUS_CHANGED`, `MATCH_FOUND`, and `MATCH_CANCELLED` are unsolicited
server events routed only to the authenticated account's active connection.
Matchmaking listener callbacks run on the `NetworkClient` reader thread. UI code
must dispatch from that callback to the LibGDX render thread before changing
actors or screens. Listener failures are isolated, and listeners can be removed
with `removeListener`.

Matchmaking state is intentionally transient. Logout, disconnect, or server
shutdown clears invitations, queue membership, and pre-game matches. Actual
I, Zombie gameplay, board synchronization, and graphical matchmaking screens
are not part of this foundation.

## Headless multiplayer I, Zombie sessions

Every matchmaking assignment now creates one canonical server session. Players
move from `PRE_GAME` to `READY` when one participant is ready and to `ACTIVE`
only after both are ready. The server derives the authenticated username and
fixed `PLANTS`/`ZOMBIES` role from the connection; clients never submit either
as authoritative command data. Leaving, logging out, or disconnecting cancels
the session, notifies the remaining player, and releases both accounts back to
matchmaking.

Stage 5 uses a deterministic, empty 5-by-9 `FIRST_BITE` board. Plants may be
placed in columns 0 through 3 and zombies in columns 4 through 8. The plant side
starts with 500 resource and the zombie side with 300; existing plant costs and
`IZombieLevel` zombie-card costs are authoritative. The balances are independent,
and removing a plant provides no refund. The server generates and stores the
match seed. Multiplayer setup does not use the single-player `IZombie` random
plant defense or its automatic sun-producer zombies.

Each immutable snapshot has a monotonic revision beginning at 0. Ready changes
and accepted placement/removal commands increment it once. Reads and rejected
commands do not. Mutation commands must supply the exact expected revision.
Network entity IDs are server-generated, stable for the life of an entity, and
never reused within a match.

`MultiplayerGameClient`, exposed by `RemoteAccountSession`, shares the existing
`NetworkClient` socket and provides typed ready, state, plant, zombie, removal,
and leave operations. Its listener callbacks run on the network reader thread,
are exception-isolated, and must be dispatched before graphical UI updates.
The Stage 5 baseline is intentionally headless; the Stage 6 section below adds
movement, combat, projectiles, real-time ticks, and periodic authoritative
snapshots without adding graphical multiplayer screens.

### Stage 6 authoritative simulation

When both participants are ready the server advances each active I, Zombie
match at a fixed rate. `pvz.server.multiplayer.tick.rate` controls that rate and
defaults to 20 ticks/second. One shared scheduler advances all matches; each
match serializes its own mutations, and socket publication happens after match
locks are released. Tests use the same fixed-step path directly, so combat and
timer behavior do not depend on sleeps or wall-clock scheduling.

The match duration defaults to 120 seconds and can be changed with
`pvz.server.multiplayer.match.duration.seconds`. Zombies move toward their lane's
brain, stop to eat blocking plants, and are damaged by plant projectiles. Plants
win with `TIME_EXPIRED` when time reaches zero while a brain remains. Zombies win
with `ALL_BRAINS_EATEN` when the final brain is consumed. A normal result is
chosen once, transitions the match to `FINISHED`, stops future simulation, and
releases both accounts for matchmaking. Leave, disconnect, and server shutdown
are cancellations (`PLAYER_LEFT`, `PLAYER_DISCONNECTED`, `SERVER_SHUTDOWN`), not
victories.

Snapshots now separate automatic `simulationTick` progression from the
optimistic-lock `revision`: simulation ticks never make a valid placement command
stale; only accepted player/lifecycle mutations increment `revision`. Snapshots
also carry elapsed/remaining time, health, precise zombie positions, projectiles,
brains, and terminal winner/reason. The server sends `MATCH_STATE_UPDATED`
periodically (up to five times per second) and sends `MATCH_FINISHED` once to
each participant for a normal terminal result.

`MultiplayerGameClient` consumes those unsolicited typed events on the existing
`NetworkClient` reader thread, ignores older simulation snapshots, isolates
listener exceptions, and supports removable listeners. Callbacks must never
call LibGDX UI APIs directly; graphical code must dispatch to the render thread.
Graphical multiplayer UI/interpolation, reactions, leaderboard support, and
gameplay-progress persistence remain later-stage work.
