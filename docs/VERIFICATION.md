# Verification

## Configurable selection ramp — 0.6.1 (2026-10-29)

- Added a versioned, addon-owned floor-ramp JSON file. Defaults use uniform random four/order on floors 1–4; progressively stronger probabilistic scoring and less shuffle on 5–8; maximum score with exact-tie variation on 9 and beyond. Main RANDOM policy remains an override. No player-build information or move-AI changes were introduced.
- The disposable pinned-runtime floor-9 check logged heuristic=true, zero margin and no shuffle, then verified cached NPC order in the actual 4v4 actor and preservation of all six player party identities/order.
- Thirty-nine unit tests passed, covering all boundaries, continuation beyond floor 10, zero scorer calls on early floors, seeded reproducibility, mixed/shuffled middle tiers, maximum-only late selection, JSON round trip, shipped example consistency, malformed-file preservation and strict config bounds. Existing selector/packet privacy tests remain passing.

## NPC four and opener selection — 0.6.0 (2026-10-29)

- Inspected the pinned Tower trainer factory and RCT evaluator/configuration. Solo Tower supplies RCTBattleAI; Wild Battle API wild logic is separate. Basic status/support evaluators exist, but no team-intent contract or proven setter/redirection coordination was found.
- Added a pure 90-candidate heuristic with public-only opposing type/base-speed projections, own move coverage (including backup coverage), passive lead pairing, friendly-fire penalties, soft exact-roster hints and seeded near-best variation. Closed and open formats use the same input budget; player submission is absent from the selection API. RANDOM baseline and exception fallback remain available.
- Thirty-one unit tests passed: legal enumeration/order, seed reproducibility, coverage, weather pairing/conflict, friendly-fire immunity, hint bounds/identity matching, unsupported strategy rejection, and existing packet privacy/mode tests. All 324 base type relationships were independently checked against the installed Showdown type chart.
- First runtime attempt exposed standard forms being absent from the alternate-form list; random fallback preserved battle initiation. Corrected the adapter to include the standard form. The subsequent pinned-runtime test logged 90 heuristic candidates with no fallback, cached the chosen order before player submission, and asserted the same four UUIDs/opening pair in the actual NPC battle actor. Player GUI order, 4v4 rosters and all six persistent party identities/order also passed.

This verifies integration, not improved battle strength or coordinated strategy execution. No full-game win-rate comparison, actual Trick Room/redirection trial or curated hints for all built-in teams has been completed. These remain follow-up work; see [architecture and metadata schema](NPC-SELECTION.md).

## Per-run Open Team Sheets — 0.5.0 (2026-10-29)

- Added Doubles VGC Open to the existing format cycle without extending original Tower indices. Closed VGC remains the previous choice. The original lock transaction now persists a separate open flag; missing old-save keys default closed.
- Separate capability-gated lock/state/open-preview packets retain the v1 closed protocol unchanged. Open sheets allow only ability, held-item identifier, up to four move identifiers and configured Tera type for each of six opponents. No selected-four/leads, EVs/IVs/nature/stats/NBT or current/boosted PP fields exist in that schema.
- Compact opponent hover/focus cards reuse the existing tooltip interaction. The initially prototyped opponent modal was removed following feedback.
- An actual 0.4.0 client against the 0.5.0 server was rejected for open lock/start and legacy downgrade of a saved open run, then completed closed GUI selection into ordered 4v4 with persistent party preservation.
- All twenty unit tests passed, including open codec bounds, schema allowlists, closed protocol separation, state/lock round trips and capability-dependent selector cycles.
- Production-runtime menu test: five choices cycled correctly, ordinary Doubles overrode legacy opt-in, Open VGC relocked and survived original NBT serialization/reload, a fresh Tower screen restored the Open label, old-save sheets defaulted closed, and a legacy lock could not downgrade the open run. Open preview confirmed into ordered 4v4; returning to ordinary Doubles produced 6v6 and preserved all six party identities/order.

Limits: NBT reload was exercised, not a full server restart during an active open run. Complete multi-floor playthrough, open-preview disconnect/rejoin and hosted-server play remain manual checks; the open preview uses the existing cancellation/recovery lifecycle without a new persistence subsystem.

## Friendly references and action order — 0.4.0 (2026-10-29)

- Inspected the installed Cobblemon 1.7.3 APIs for ability translation keys, move metadata, current/max PP, party stats and held-item tooltip translations. The client uses only its existing own-party data; opponent preview packet schemas are unchanged.
- Added compact ability/item/move hover summaries and a right-click/R reference panel with descriptions, move metadata, explicitly labeled party stats, scrolling and keyboard paging. Missing item descriptions are reported rather than invented. No normalized-stat prediction or Open Team Sheets format is included.
- Reordered actions to Forfeit / Clear / Use chat / Confirm, including keyboard traversal.
- Inspected final screenshots at the normal scale and a smaller GUI viewport: [full reference](screenshots/friendly-reference-v040.png), [paged reference](screenshots/friendly-reference-scrolled-v040.png).
- Offline build and all fifteen existing unit tests passed. The optional production-runtime harness exercises right-click, paging, Escape, selection reordering, GUI confirmation, ordered four-member teams on both sides, and preservation of all six persistent party identities/order.
- The earlier legacy UI fixture needed an explicit per-run VGC flag after the 0.3 mode changes. Another harness attempt needed Screen casts for inherited Minecraft methods during remapping. Both failures were confined to the disposable test environment. Screenshot inspection also caught and corrected item models drawing over the reference panel.

Manual follow-up: test the panel with your actual team, long translated names, and descriptions from other installed item mods. Items without a translated `.tooltip` description show an explicit fallback. Stats and PP describe the synced party, not a simulated normalized battle state.

## Tower format selector — 0.3.0

Added a virtual Doubles VGC choice to the original Tower format cycle. A new bounded lock payload includes the competitive flag with the original Tower settings. The server validates it, runs the original lock handler, and commits an explicit per-run flag before the original Tower save. A nullable NBT field preserves older command-based saves and restores the selected mode on reopening. Ordinary Doubles explicitly overrides an old command preference.

Completed checks:

* Fifteen unit tests passed, including mode-cycle bounds and competitive/non-competitive lock/state payload round trips.
* Dedicated-server startup and all required common/client Mixins applied in the pinned production environment. Loom reports name-remapping warnings for explicit intermediary client method names, but the runtime injected targets were exercised successfully.
* Real original Tower UI cycling: Singles → Doubles → Doubles VGC → Co-op → Singles → Doubles.
* Server rejected a five-member VGC party and a malformed VGC/Singles request without locking it or changing the previous run flag.
* GUI ordinary Doubles lock overrode an existing `/ctower on` preference and persisted false through original Tower NBT serialization/reload.
* GUI VGC selection/relock persisted true through original Tower serialization/reload. A fresh Tower screen restored **Format: Doubles VGC**; screenshot: `screenshots/tower-format-vgc-v030.png`.
* The original Battle button opened the pick-four UI. Immediate confirmation hit the normal Tower cooldown; retrying the same ordered choice after cooldown succeeded. The server asserted a four-Pokémon roster per side and the requested 5,2,6,1 order.
* A following explicit ordinary Doubles lock/start produced six Pokémon per side despite the old global command preference. All six persistent party UUIDs/order survived the regression sequence.
* Test log markers: MODE_VALIDATION_PASS, MODE_SELECTOR_CYCLE_PASS, MODE_NORMAL_LOCK_PASS, MODE_VGC_PERSIST_PASS, MODE_REOPEN_PASS, MODE_VGC_BATTLE_PASS, MODE_SIX_V_SIX_PASS.

The first runtime attempt failed in the separate test fixture because it tried PartyStore.set(slot,null); changing the fixture to remove/restore the sixth Pokémon resolved that. A second attempt confirmed lock/save/reopen but submitted before the cooldown expired; the completed run tested the retry as well. These test failures were confined to the disposable local server, not the hosted server.

Remaining manual checks: full match completion/next-floor flow through the new menu, actual co-op/boss encounters with multiple players, server restart with a locked VGC run, and mixed-version multiplayer behavior. The automated test exercised NBT reload and fresh-screen restoration, not a full server restart while a player remained locked. Hosted deployment/restart status is tracked separately in deployment records.


## UI feedback revision — 0.2.1

The first red-card screenshot was captured before the later artwork enlargement, which caused confusion during review. Treat the original `preview-moves.png` captures as intermediate views, not evidence for the final size/position change. The final capture has a distinct name: `screenshots/preview-large-clipped-v021.png`.

Final implementation: red opponent cards; no Normal form text on cards/popouts; no separate lead/reserve row; numbers show the order; scroll or focused-card Left/Right reorders. Friendly move references read only the player's already-synced client party, without adding network fields. The item artwork targets 64 GUI pixels instead of 32, centers above the name, and clips to the card's artwork region. Small viewports reduce the scale as necessary.

Validation: ten existing tests passed after the production changes. The final disposable client asserted enlarged 108-pixel-high cards and exercised scrolling earlier/later. The server verified GUI confirmation into an ordered four-per-side battle and verified all six persistent party UUIDs/order after forfeiting. Final screenshot inspection shows the enlarged artwork inside the card boundaries and a friendly Charizard panel listing four moves. Logs report PHASE2_LAYOUT_PASS, PHASE2_REORDER_PASS, PHASE2_REAL_UI_PASS and PHASE2_CLEANUP_PASS. The user has not yet reviewed the final revision in their game. Complete keyboard/resize and full-match playtests remain outstanding.


## Phase 2 player UI — 28 October 2026

Version 0.2.0 adds automatic visual previews, six opponent species/form cards, six selectable party cards, four ordered lead/reserve slots, adjacent reordering, clearing, confirm, countdown, Esc/forfeit confirmation, and chat fallback. NPC selection is unchanged.

Completed checks:

* `test build` passed: 10 tests (four existing server-selection tests, three ordered-draft tests and three bounded packet/round-trip tests).
* Dedicated-server startup succeeded with client-only rendering classes isolated behind the client entrypoint.
* Launched a separate Minecraft 1.21.1/Fabric 0.19.5 client against the disposable localhost server. Inspected actual rendered empty/selected/forfeit screenshots, corrected background blur/double-label rendering, and reran successfully. See `screenshots/preview-selected.png` and `screenshots/preview-forfeit.png`.
* An explicit test-only harness prepared six Pokémon and a Holo Tower in the disposable world, opted the test player in, locked Doubles and invoked the original Tower start. The real server packet opened the GUI. The client selected 5,2,6,1 and pressed Confirm through the actual button/payload path.
* The server asserted that both battle actors had four Pokémon and the player's original Pokémon UUIDs were ordered 5,2,6,1. It then forfeited and asserted all six persistent party UUIDs and their original order were intact. Logs reported `PHASE2_REAL_UI_PASS` and `PHASE2_CLEANUP_PASS`.
* The test did not play battle turns or assert active lead positions after send-out animations; it verified battle actor roster/order and startup. The user already verified normal battle completion, round-two selection, timeout and Save-and-Quit on 0.1.1.
* Both disposable processes were stopped. The visual/test harness is a separate optional mod built with `-PvisualSmoke remapVisualSmoke`; it is never included in the runtime addon or installed into the user's profile.

Outstanding playtests: real singleplayer GUI match completion/next-floor presentation, minimum scaled viewport and resize, all keyboard/narrator flows, timeout while screen or confirmation is open, Use chat/command completion, disconnect, stale response isolation, cooldown rejection recovery, and a remote server with the full pack. These remain explicit checks, not claims inferred from unit tests.


## Singleplayer update — 28 October 2026

Version 0.1.1 changes Fabric environment eligibility from server-only to both environments, so the common entrypoint runs in singleplayer too. Server STARTING and STOPPED callbacks clear transient previews, recovery IDs, launch context, journal path and storage readiness between worlds. Persistent player preferences remain profile-wide; recovery journals remain world-local.

`./gradlew --offline test build` passed, including the four existing selection-policy tests. Inspected packaged metadata confirms version 0.1.1 and environment `*`; the runtime JAR contains no proprietary dependency classes. Verified the installed Cobblemon, Tower and RCT JAR hashes still match the inspected pins. Installed the byte-identical 0.1.1 runtime JAR into the local Modrinth Cobbleverse profile, with `enabled=true` and no players automatically opted in.

A client launch and actual integrated-server battle have **not yet been verified**. The user is preparing a fresh world for that test. Save, fully restart Minecraft, run `/ctower on` before locking the team, and start solo Tower Doubles. Also test leaving a pending preview, reopening the world, and switching to a second world in the same client process.

## Original dedicated-server verification — 27 October 2026

## Completed

* Built with Fabric Loom 1.16.3, Gradle 9.5.1, Java release target 21. `test build` succeeded.
* Four JUnit tests passed: ordered leads without mutating the source party; invalid/duplicate/out-of-range selections; random NPC subset uniqueness and slot coverage; wrong owner/token, expiry and changed-order rejection.
* Loaded an isolated dedicated Minecraft 1.21.1 server with Java 21, Fabric Loader 0.19.5 and the addon. It reached `Done`, registered all `/ctower` commands and stopped cleanly. Repeated startup with the final build and `enabled=true`; it also reached `Done`. Console-only use of player commands is rejected. No actual player opted in during these tests.
* Inspected transformed `BattleTowerManager` and RCT `BattleManager` classes: preview/start/team-reuse/forfeit/recovery hooks and both roster-read call sites applied. Required injection counts did not fail. This validates Mixin application, not successful execution of a selected match.
* Smoke test included Tower 1.11.23, Cobblemon 1.7.3, RCT API 0.15.2-beta, RCT Mod 0.18.1-beta, Wild Battle API 1.1.2, Battle Extras 1.13.45, Battle Positions (metadata 1.1.2), Mega Showdown 1.8.4, Battle Builds 1.0.0, Fabric API 0.116.14, Architectury 13.0.8, Kotlin 1.13.13, Accessories, owo and Forge Config API Port. Loader counted 80 mods including nested dependencies.
* Confirmed the produced runtime JAR contains only `dev/cobbleverse/tower` classes and addon metadata, with no Battle Tower/RCT/Cobblemon classes, resources or embedded JARs.

Initial attempts stopped at unaccepted EULA and then restricted localhost binding. After explicit user EULA authorization and local-process approval, the server started successfully. Optional missing-class/resource warnings from the selected mod subset remain; no addon Mixin failure appeared. The tested instance was stopped, not left listening.

## Not established / release blockers

There was no real player/client in the smoke server. The preview, selection, full battle, actual leads, cancellation, active disconnect, rewards, normalization and arena cleanup paths have **not** been exercised end to end. Passing the helper tests does not establish those behaviors. The full Cobbleverse mod set and remote server inventory have not been verified. The addon remains experimental and disabled by default.

No client GUI was implemented. The interface is chat commands; the original 0.1.0 test covered a dedicated server only. The installed Battle Builds post-battle disclosure remains enabled outside this addon's privacy scope.

## Acceptance tests in a copied instance

| Scenario | Required observation |
| --- | --- |
| Disabled addon / opted-out doubles | Original six-member roster, Tower UI and rewards remain unchanged |
| Singles, co-op, bosses | No preview; original flow |
| Opt-in with 5 Pokémon or a fainted member | Refuse competitive start, no spawned trainer or arena reservation |
| Preview six including alternate forms | Exactly species/form only; no moves, items, ability, EV/IV, Tera, NPC subset or NBT in preview |
| Pick 5,2,6,1 | Four player battle members in that order; 5 and 2 lead; four distinct NPC members |
| Persistent party | All six UUIDs and order survive selection and battle |
| Invalid/stale/duplicate token or slots | No battle and no duplicated rewards |
| Changed party/build/settings/terminal/dimension | Reject stale preview and forfeit without teleporting or spawning NPC |
| Double-click Start / repeated confirm | One preview, one generated opponent, at most one battle |
| Two simultaneous players | No shared selection, team, arena or reward state |
| Cancel, timeout, disconnect before confirmation | Existing run-forfeit behavior, journal cleared, no NPC/arena leak |
| Crash during preview then reconnect | Journal recovery forfeits once; no party mutation or duplicate payout |
| Disconnect during active match | Existing Tower cleanup, level restoration and loss/forfeit behavior |
| Force RCT start rejection / exception | Trainer discarded; no stale battle; position, level and XP restored |
| Win/loss/flee; several consecutive floors | Original progression/rewards/cleanup; fresh preview each eligible floor |
| Level 50 and 100 normalization | Four actor clones normalized; persistent party restored according to Tower rules |
| Custom arena dimensions and Battle Positions | Original arena and return points; both doubles leads positioned correctly |
| Mega Showdown and Battle Extras | Existing gimmick/bag rules; no invalid actions or extra bench members |
| Battle Builds | No builds before/during preview; existing full-build report arrives only after battle end |

Capture server logs plus before/after party UUIDs, levels, XP, Tower floor/streak/BP and entity counts. Do not publish hidden opponent builds or proprietary transformed/decompiled classes with test evidence.

The held-item addition was checked with six real held items assigned to the disposable test party through Cobblemon and synced to the client. `screenshots/preview-large-items-v021.png` shows friendly badges and Charizard’s Life Orb plus four moves. The server again verified ordered 4v4 startup and six-party preservation. Opponent cards did not receive item fields or badges. Redundant instruction lines were then removed from the popout to avoid crowding it at small viewport heights; the screen footer retains the interaction instructions.
