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
`pvz.server.users.database`, `pvz.server.invitation.expiration.seconds`
(30 seconds by default), `pvz.server.multiplayer.tick.rate`,
`pvz.server.multiplayer.match.duration.seconds`, and
`pvz.server.multiplayer.reaction.cooldown.millis`. Client properties are
`pvz.client.server.host` and `pvz.client.server.port`.

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
progress is synchronized as described below.

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

### Stage 7 graphical multiplayer flow

The main menu keeps the existing single-player **Play** route and adds a separate
**Multiplayer I, Zombie** entry. It uses the authenticated remote `AccountSession`
and never authenticates or matchmakes through `UserManager`. A user can invite a
specific username (and cancel/retry after recoverable errors) or join/leave the
random queue. `RemoteAccountSession`'s existing `MatchmakingClient` and
`MultiplayerGameClient` are adapted behind small controller interfaces, so no
extra socket or client-to-client connection is opened.

One application-scoped invitation bridge owns incoming invitation events across
ordinary menu changes. It displays the inviter and expiration, accepts/rejects
once, clears stale notifications on logout/disconnect, and routes `MATCH_FOUND`
exactly once to the pre-game screen. The pre-game screen displays the server
match ID, opponent and fixed `PLANTS`/`ZOMBIES` assignment, tracks both ready
states, submits local ready once, and enters the live screen only on the
authoritative `MATCH_STARTED` event.

The live screen renders immutable `MatchStateSnapshot` data: board dimensions,
red-line boundary, server resources, remaining time, brains, plants, zombies,
projectiles and health. Stable server entity/projectile IDs key graphical actors.
Older simulation ticks are ignored. Plant/zombie placement and plant removal use
the newest server mutation revision and never create optimistic entities or
spend resources locally. Controls are role-specific while the server continues
to enforce every permission and placement rule.

Normal `MATCH_FINISHED` results show the winner plus stable reasons such as
`ALL_BRAINS_EATEN` or `TIME_EXPIRED` over the final snapshot. Leave, disconnect
and shutdown cancellation are presented separately and declare no winner. Every
screen/controller removes listeners during disposal and ignores late callbacks.
Network events and future completions cross the injected `UiDispatcher`; the
LibGDX implementation uses `Gdx.app.postRunnable(...)`, keeping Scene2D mutation
and navigation on the render thread.

### Stage 10 match reactions

An active multiplayer match has exactly six predefined reactions. The three
text choices are `GOOD_LUCK` (**Good luck!**), `NICE_MOVE` (**Nice move!**), and
`WELL_PLAYED` (**Well played!**). The three emoji identities are `SMILE`,
`LAUGH`, and `ANGRY`; the graphical screen deliberately renders the readable
fallback labels **Smile**, **Laugh**, and **Angry** because no new emoji assets
or font assumptions are required. There is no text field, free-form chat,
custom emoji, URL, or markup path.

`SEND_MATCH_REACTION_REQUEST` carries only the match ID and stable reaction
identifier over the application-scoped `NetworkClient`. The correlated receipt
contains the server sequence and time, while `MATCH_REACTION_RECEIVED` pushes
the same server-confirmed event to both participants. The sender is derived from
the authenticated current connection; client-supplied sender, ordering, time,
or extra fields are rejected. Reactions from other matches and non-participants
are never routed to a client.

Each match owns an independent monotonic reaction sequence and independent
per-player cooldown. The default is one accepted reaction per player every
1,000 milliseconds and is configured with
`pvz.server.multiplayer.reaction.cooldown.millis`. Tests inject a clock, so rate
limits require no timing sleeps. Rejected reactions do not consume sequence
numbers, one player's cooldown does not block the opponent or gameplay, and
reactions never change the gameplay revision, simulation tick, or snapshot.

Reaction history and cooldown state are transient: the controller retains at
most the latest five server events and clears them on leave, finish,
cancellation, disconnect, logout, disposal, or shutdown. Nothing is written to
the users database or restored after a match/server restart. Multiplayer
listener callbacks, including reactions and disconnect notification, initially
run on the `NetworkClient` reader thread; `LiveMatchController` routes every UI
observer callback through `UiDispatcher`. The sender also renders only the push
event, not an optimistic copy of the button click.

Stage 7 controller tests are asset-independent. The graphical integration reuses
the current skin/rendering primitives and adds no binary assets. A desktop smoke
test still requires the complete `pvz-assets` bundle in its normal runtime
location; a missing bundle is not replaced with committed placeholder assets.

Reactions/messages, Couch Play,
persistent login tokens, automatic reconnect loops and TLS remain out of scope.

### Stage 8 server-backed gameplay progress

Authenticated remote accounts now persist gameplay through the server users
database. The synchronized state includes coins, diamonds, sprouts, plant food,
pot inventory and greenhouse unlock count; chapter/level, minigame completion,
high score and games-played statistics; adventure/minigame unlock records;
plant and zombie collections, plant boosts, and daily-offer purchase state.
Daily and non-daily completed-quest counters, fixed greenhouse pot contents and
growth timers, active quest-instance progress, and bounded account news are also
synchronized. Quest definitions remain catalog-owned and are validated against
the server copy; their total completion count is always derived.
Passwords, hashes, security answers, profile fields, and connection state are
never part of gameplay responses or update requests.

Each account stores a gameplay revision in the existing backward-compatible
JSON record (legacy records begin at revision 0). A complete validated update is
written atomically and increments that revision once. Stale updates are rejected
without mutation. The client refreshes after a conflict but preserves its dirty
local compatibility state; overwriting newer server data requires an explicit
retry. Timeouts and server failures also remain dirty/recoverable and are not
blindly retried.

The application-scoped synchronizer shares `RemoteAccountSession`'s one socket.
The render loop only observes whether the compatibility `User` changed; actual
requests are asynchronous, serialized, and coalesced. Login/clean refresh
hydrates the compatibility user, successful acknowledgements update the cached
profile, and logout attempts an asynchronous flush. The compatibility user is
never inserted into `UserManager`, so local `data/users.json` is not used for
remote gameplay and another device receives the latest acknowledged server
state on login.

Saved in-progress adventure boards remain local checkpoints. The Phase 3 PDF
does not require cross-device resume for those transient battle snapshots, and
their legacy format uses Java object serialization, so they are deliberately not
accepted by the network gameplay endpoint.
There is still no offline retry queue, automatic reconnect, or persistent login
token support.

### Stage 9 server-backed graphical leaderboard

The graphical leaderboard is authenticated and server-backed. It reads an
immutable snapshot from the same server users database used by login and
gameplay synchronization, never from graphical `UserManager`/`data/users.json`
state. The terminal `LeaderboardMenuController` intentionally keeps its local
Phase 1 behavior for compatibility.

The graphical client can sort by username, last completed chapter/level,
completed minigames, completed daily quests, completed non-daily quests, total
completed quests, or high score in either direction. The primary sort is
followed by a case-insensitive ascending username tie-breaker and then exact
ascending username spelling. Ranks are sequential positions in the complete
ordered result. Requests are capped at 100 rows; the first page reports the
total player count and the authenticated user's global rank even when it falls
outside that page.

Leaderboard rows contain only rank, username, the displayed completion
counters, and high score. Passwords/hashes, email, security data, connection
identity, and gameplay revisions are not exposed. Loading and refresh are fully
asynchronous on the existing account-session socket. The screen shows loading,
empty and recoverable error states, offers explicit retry, ignores superseded
sort responses, and highlights the current account by profile username.

Controlled graphical logout and in-application exit await an asynchronous
gameplay flush. A direct window-manager/process shutdown now sends a non-blocking
best-effort final dirty snapshot before closing the socket, but delivery cannot
be guaranteed once the operating system is terminating the process. No blocking
wait or persistent retry queue is used during LibGDX disposal.
