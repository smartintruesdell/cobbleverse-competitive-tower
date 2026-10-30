# Cobbleverse Competitive Tower

**Experimental Fabric addon. Phase 2 adds a visual team-selection screen to the user-tested singleplayer battle flow. See the dated verification notes for automated checks and outstanding playtests.**

Read [architecture and feasibility](docs/ARCHITECTURE.md) first, then [verification and outstanding tests](docs/VERIFICATION.md).

Adds **Doubles VGC** and **Doubles VGC Open** to the Holo Tower format selector for bring-six/pick-four solo doubles. Preview shows all six opponent species/forms. Choose four unique party slots in order; the first two lead. The NPC ranks four-member rosters and opening pairs before your selection, using its own moves and public species/form matchups. A random baseline/fallback remains available. Its chosen four and leads remain server-side during preview. Ordinary **Doubles VGC** keeps all opponent build details private; **Doubles VGC Open** opts this run into the sheets described below.

The default interface is a visual selection screen, with private chat commands retained as a fallback. In 0.6.2, routine addon chat is quiet by default; **Use chat** requests the team lists and selection command when needed. It supports **singleplayer (the integrated server) and dedicated servers**. For singleplayer, install it in your client profile. Multiplayer needs the addon on the server and each client that wants the screen; clients without the UI use the existing chat workflow. See [the Phase 2 implementation plan](docs/PHASE-2-PLAN.md).

## Open Team Sheets (per run)

Choose **Doubles VGC Open** in the existing Holo Tower Format cycle before starting a run. The locked choice persists across floors and reloads. Existing saves, command-based opt-ins and the original **Doubles VGC** choice remain closed. Choosing another format before the run starts explicitly relocks it through the original Tower validation; an active run cannot change its sheet format.

In an open preview, hover an opponent card to see its ability, held item, Tera type and move names in the same compact tooltip style as your own team. Tab can focus opponent cards for the same view. Held-item badges appear on their sprites. There is no separate opponent sheet dialog. Species/forms remain visible on all six cards; the chosen four and leads remain hidden.

The open packet contains only six sets of those declared fields. No EVs, IVs, nature, stats, current/boosted PP, raw Pokémon data or NBT are sent. Tera is the configured Tera type; this format does not enable battle gimmicks or change the Tower's rules. Existing after-battle Battle Builds reports are separate. NPC selection uses the same public species/form information in closed and open formats and gains no access to your submitted four/leads.

Both client and server need **0.5.0+** for Open Team Sheets. An older server offers the existing choices only. Older clients can still use closed runs on a new server, but cannot lock/start an open run or silently downgrade a previously locked open run. Use an updated client, or explicitly forfeit that run. Closed preview packet schemas remain unchanged. Preview timeout, disconnect and cancellation still forfeit the run using the existing recovery path.

## Chat verbosity

In the server or singleplayer profile's `config/cobbleverse-competitive-tower.json`, `"verboseChatMessages": false` suppresses automatic addon team lists, pick-four instructions, command suggestions and the routine battle-start confirmation for updated UI clients. Set it to `true` to restore those messages. Restart the game/server after changing configuration.

**Use chat** or `/ctower chat` explicitly prints the pending selection details and clickable command without extending the deadline. `/ctower preview` reopens the screen. Errors, cancellation/timeout notices and mode-command confirmations remain visible. Older clients and clients without the GUI retain automatic chat output so their existing fallback remains usable; upgrade both client and server to 0.6.2 for quiet UI behavior. This setting does not mute original Battle Tower messages, other mods' reports or server diagnostics.

## NPC selection

Version 0.6.1 scales selection by floor: random on 1–4, progressively stronger on 5–8, and maximum-scoring from floor 9 onward. Edit `config/cobbleverse-competitive-tower-ramp.json` to tune the ramp; [default JSON](docs/examples/cobbleverse-competitive-tower-ramp.json). The experimental heuristic scores all 90 four-member/opening-pair combinations. It favors coverage, passive partner synergy and safer leads, with controlled variation among close choices. Optional addon-owned team hints can favor lead pairs and reserves. [Configuration, metadata schema, inspected AI capabilities and limitations](docs/NPC-SELECTION.md) explain the supported behavior. This selects teams; it does not coach the existing move AI or establish an improved win rate.

## Pinned target

| Component | Required version |
| --- | --- |
| Minecraft | 1.21.1 |
| Java runtime | 21 |
| Fabric Loader | 0.19.5 |
| Fabric API | 0.116.14+1.21.1 |
| Cobblemon | 1.7.3+1.21.1 |
| Battle Tower | 1.11.23 |
| RCT API | 0.15.2-beta |

The local profile's latest log confirms Loader 0.19.5. Do not infer that the remote server uses it. The requested pack label is Cobbleverse 1.7.42; that label was not independently confirmed. The addon requires the inspected core versions and fails startup if required hooks are missing. See `docs/dependency-pins.json` for the locally supplied dependency versions and hashes required by the build. Battle Positions' filename says 1.1.3, but its Fabric metadata reports 1.1.2.

## Build locally

Use a JDK capable of running Gradle 9.5.1; compilation targets Java 21. Python 3 prepares the dependencies from your own installed files:

```sh
python3 tools/prepare_dependencies.py "/absolute/path/to/your/Cobbleverse/mods"
./gradlew test build
```

The script requires the exact inspected SHA-256 hashes and copies dependencies into ignored `libs/`. It does not download Battle Tower. Gradle may download its public build dependencies. Result:

```text
build/libs/cobbleverse-competitive-tower-0.6.2-experimental.jar
```

Do not distribute `libs/`, development runs, smoke-server directories, transformed classes, or decompilations. The runtime addon JAR contains only this addon's classes and metadata.

## Test in singleplayer

1. Install `cobbleverse-competitive-tower-0.6.2-experimental.jar` in your Cobbleverse profile's `mods/` folder, removing any older version of this addon. Fully quit and relaunch Minecraft; returning to the title screen alone does not load new mods.
2. In the profile's `config/cobbleverse-competitive-tower.json`, set `enabled` to `true` before launching. Keep `selectionTimeoutSeconds` and `optedInPlayers` as shown below. The delivered addon still defaults to disabled.
3. Enter your fresh test world, prepare six healthy Pokémon and place a Holo Tower. No command is required with the new selector.
4. Cycle **Format** to **Doubles VGC** (closed) or **Doubles VGC Open**, lock the six, and start. The selection screen opens automatically. Click four Pokémon in order: the first two lead. The numbers on your cards show the order. Click a selected Pokémon to remove it; scroll over one to move it earlier/later, or use Left/Right while its card is focused. Hover a friendly Pokémon for ability, held item and moves. Right-click a friendly card, or focus it with Tab and press **R**, for its full reference: descriptions, move type/category/power/accuracy/current PP, and party stats. Scroll or use Up/Down/Page Up/Page Down; Esc or click returns to selection. The timer continues while reading. Party stats are explicitly not normalized battle values. Opponent cards remain species/forms only. Press **Confirm** to start.
5. **Esc / Forfeit** asks before forfeiting. **Use chat** hides the screen and requests the selection details and suggested command in chat; the timer continues. `/ctower preview` reopens the screen without extending the deadline. `/ctower cancel`, the selection timeout, or leaving during preview forfeits that run. The journal is stored in this world's save; in-memory preview/recovery state is cleared between worlds. Your `/ctower on` preference is profile-wide and persists across worlds.

A remote server still needs the addon installed and enabled itself; installing it only on your client does not change remote Tower battles.

## Try in a copied local server

1. Make a disposable copy of your matching Cobbleverse **server** setup. Keep its existing mods/configs/datapacks. Use a fresh test world and Java 21. Bind to localhost and use an unused port.
2. Put the runtime addon JAR in that server's `mods/` directory. Start once with its normal Fabric launch command; the addon creates `config/cobbleverse-competitive-tower.json` disabled by default. Stop the server.
3. Set `enabled` to `true` in that file and restart. Configuration example:

   ```json
   {
     "enabled": true,
     "selectionTimeoutSeconds": 120,
     "optedInPlayers": []
   }
   ```

4. Join with a client running 0.3.0. Bring **six healthy Pokémon**, choose **Format: Doubles VGC**, lock your team, and start using the Holo Tower as usual.
5. The visual preview opens on clients with the addon. Choose four, reorder if needed, and Confirm. Clients without the UI can close the Tower screen, read chat, and edit the suggested four numbers, e.g. `5 2 6 1`. Keep the token intact. Slots 5 and 2 lead. Stand within eight blocks of the original Holo Tower until confirmation. The original Tower arena teleport happens afterward.
6. `/ctower preview` repeats the preview. `/ctower cancel` forfeits the run. Timeout, disconnect during preview, and server shutdown also forfeit it. If the Tower's initiation cooldown is still active, retry the same selection after it expires.
7. To select ordinary 6v6 doubles, finish or forfeit the run and choose **Format: Doubles**. Singles, co-op and boss modes use the original path regardless of this preference. Server config changes require restart.

The development smoke instance lives in `build/smoke-server`, bound to `127.0.0.1:25577`; it has been stopped. It uses a subset of the profile and is not a complete pack copy. Its EULA was accepted with explicit user authorization for this disposable test server only. That original smoke test changed no live profile, world or remote server. On 28 October, version 0.1.1 was installed into the local Modrinth Cobbleverse profile and its config enabled for the requested singleplayer test; no worlds or remote server were modified.

## Recovery and behavior

Selections are validated on the server against the token, party UUID order, complete server-side party state, snapshot, floor, settings, dimension, terminal proximity and deadline. No Pokémon is removed from persistent party storage. Replayed and stale selections cannot start another battle.

The existing Tower starts the run before the preview checkpoint. Therefore preview cancellation uses its normal forfeit path and pays only whatever that path already awards for completed floors. It does not count the preview as a win. This prevents opponent-preview rerolls within the same run.

Pending previews are journaled in the world root at `competitive-tower-pending.json`. A reconnect after an interrupted preview forfeits that run through the Tower. Normal active-battle disconnect handling stays with the Tower; crash recovery for an already established battle is **not newly implemented** here. Failed starts request original cleanup, discard the captured visual trainer, and restore the pre-attempt return position, levels and experience.

## Limits and privacy

* No new VGC clauses, ban list, timer, battle-bag restrictions or leaderboard. Original Tower bag/gimmick rules and doubles rewards/streaks remain in force. This is VGC-style roster selection, not official regulation compliance.
* No strategic NPC subset heuristic yet. Random four is the intentional baseline; it has no access to the player's hidden build for selection.
* The preview never serializes opponent Pokémon or build data to clients. The installed **Battle Builds 1.0.0** separately sends full builds **after battle end**. That pre-existing behavior is preserved; this addon does not promise to hide builds after the match or override other mods' disclosures.
* The full remote-server setup and every GUI recovery/resize/keyboard edge case still need the acceptance tests. The user confirmed singleplayer battle progression, repeated selection, timeout and Save-and-Quit recovery on 0.1.1; see verification notes for the 0.2.0 checks.

## License and third-party boundary

Our original addon code and documentation are licensed under [MIT](LICENSE). See [third-party notices](THIRD_PARTY_NOTICES.md) for the Gradle wrapper, game screenshots and dependencies.

Battle Tower's embedded metadata declares **All Rights Reserved** (Raguto & Yemagh). This addon leaves its JAR unchanged and does not redistribute its implementation. Private use does not automatically grant permission. Applicable terms and any needed author permission remain unresolved; this project does not make a legal determination.

Local inspection used [CFR 0.152](https://www.benf.org/other/cfr/) and `javap`. Their temporary output is not included in the addon or source bundle.

## Phase 2 test status and rollback

0.2.0 has passed a real client/server preview → GUI confirm → ordered 4v4 startup test, as well as screenshot inspection and ten automated tests. Full battle turns through the GUI and every recovery edge case remain playtest items. Restart Minecraft fully after installing. The local 0.1.1 JAR is retained with a `.disabled` suffix when installing 0.2.0; to roll back, quit Minecraft, disable/remove 0.2.0 and restore the old filename. Keep only one enabled addon version.

## UI refinement — 0.2.1

Opponent cards are red-tinted; the default Normal form label is hidden. The separate lead/reserve button row is removed: card numbers show selection order, with 1–2 opening the battle. Scroll a selected card to reorder it, or use Left/Right while it is focused. Click it to deselect. Friendly hover/focus panels list that Pokémon’s current moves and held item from the already-synced client party. Held items appear as small badges at the lower-right of each friendly sprite; empty held-item slots show no badge. Opponent previews still contain only species/forms. Artwork is centered above the labels and targets double the previous sprite size, shrinking as needed on smaller viewports.

## Tower selector — 0.3.0

The format cycle is Singles → Doubles → Doubles VGC → Co-op → Singles. VGC is offered when both client and server have 0.3.0 and the server enables the addon. VGC requires six healthy Pokémon before lock-in. The chosen flag is saved with the original Tower team/run data, restored when reopening the screen, and submitted together with the normal lock settings. An explicit ordinary Doubles lock selects 6v6 even if the player previously used `/ctower on`.

Older saves without a per-run flag retain their prior command-based behavior. `/ctower on/off` remain fallback preferences for older/no-GUI clients; they are not needed for the new selector. Older servers retain their original choices until updated. Existing doubles progression/rewards are shared; no new leaderboard or official VGC regulations are introduced.

See [mode-selector design](docs/MODE-SELECTOR-PLAN.md) and the dated verification notes. Both Minecraft client and dedicated server must be fully restarted after updating for the new selector to appear. An uploaded JAR does not update a running server until restart.

## Development roadmap

See [ROADMAP](docs/ROADMAP.md) for the agreed sequence: source repository and permissive licensing; richer friendly reference cards with an optional Open Team Sheets format; intelligent NPC selection/openers; then best-of-three tournament mode.

For building, testing and source-only publishing checks, see [CONTRIBUTING](CONTRIBUTING.md). Private deployment scripts, server records, installed mod binaries, game saves and decompilations are intentionally excluded from this repository and source archives.
