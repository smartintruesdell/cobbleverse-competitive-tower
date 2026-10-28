# Architecture and feasibility — inspected 27 October 2026

## Evidence and scope

Inspected the installed Modrinth profile's `cobblemon_battle_tower-fabric-1.11.23.jar`: Fabric metadata, resource inventory, Mixin configuration, and locally decompiled battle-start, team-generation, snapshot, result, and disconnect logic. Inspected RCT API 0.15.2-beta's battle construction as well. Temporary decompilations are outside this project and are not deliverables. The private investigation recorded a full local mod inventory. The repository includes only required build dependency metadata and hashes in `dependency-pins.json`; those pins alone are not proof of any remote server's inventory. Minecraft 1.21.1 and Cobblemon 1.7.3 are present. Cobbleverse 1.7.42 is the requested pack label, not independently verified from launcher metadata.

The earlier conversation is accessible, but its assistant messages resolve only to content-reference placeholders. This design is based on the explicit request and local binaries.

## Findings

* `BattleSettings.BattleMode.DOUBLES` already selects RCT's GEN_9_DOUBLES; its maximum party size is six. No new battle format or enum extension is necessary.
* `BattleTowerManager.startBattle` validates the locked snapshot, starts the run, handles cooldowns, and dispatches to `startSoloBattle`. Solo entry is before arena selection, teleport, NPC creation, level adjustment, and battle construction.
* `startSoloBattle` generates a team through `BattleTowerTeams`, creates the original Tower trainer, then calls RCT `BattleManager.startBattle(List,List,BattleFormatProvider,BattleRules)`. The remainder registers the Tower session, tracked Pokémon, arena reservation, and mode.
* RCT `toBattlePokemons(Trainer,boolean)` reads `Trainer.getTeam()` twice (owner lookup and iteration), constructs battle-local clones for normalized battles, and preserves team order. Narrowing those reads during this one Tower start avoids changing the persistent party or Tower snapshot.
* Standard team generation marks the selected team used in `TeamTracker`. A preview must reuse the exact generated Pokémon, not generate again after confirmation. Canceling forfeits the run rather than permitting free rerolls.
* Tower win/loss/flee handlers and its disconnect cleanup own rewards, streaks, arena release, NPC cleanup and restoration. Keep these paths in place.
* Resources include translations, Holo block/model/recipe, boss scripts and moves. The inspected config has no bring-six/pick-four option. It currently disables NPC Mega/Tera/Dynamax, has a one-second initiation cooldown, and no explicit arenas.

## Proposed narrow integration

Separate Fabric addon, disabled by default, exact dependency versions. The interface is server chat commands and private species/form text, usable with the existing client. A graphical client screen is deferred until the core flow is verified in-game. Version 0.1.1 permits both client/integrated-server and dedicated-server loading. Server lifecycle hooks reset all transient world state before each world starts and after it stops; recovery journals remain world-local.

1. Explicit per-player opt-in before locking a run. Only solo DOUBLES is eligible. Other modes and opted-out runs take the original path.
2. At solo entry, verify six healthy Pokémon, capture their UUID order and the Tower snapshot identity, generate one six-Pokémon opponent through the original generator, and pause before any arena/NPC side effect.
3. Show only species and form names. Never send Pokémon NBT, moves, ability, item, stats, EVs, IVs, Tera type, or opponent selection. Server-only random selection chooses four distinct slots once; this baseline is deliberately independent of hidden player builds.
4. Accept four unique slots in order with a per-preview token. Revalidate owner, expiry, party identities, run, settings, terminal and cooldown. Resume the original public start method with a narrowly scoped context. Reuse the generated team through targeted call-site hooks; narrow RCT team reads to the selected four. First two are leads.
5. Leave the complete opponent team in the Tower session and trainer; only the battle actor roster is narrowed. Original NPC AI, normalization, bag rules, gimmicks, rewards and result handlers continue to run.
6. Timeout, explicit cancel, pending disconnect and orderly shutdown forfeit through the original Tower method. A small recovery journal marks pending previews for forfeiture on next login after a crash. No party members are moved or removed. Active battles remain governed by original Tower cleanup.

## Feasibility and limits

Technically feasible with version-specific hooks. This is VGC-style roster selection, not an official VGC regulation implementation: no new clauses, bans, timers, bag-item restrictions or separate leaderboard. Competitive wins use the existing DOUBLES progression and rewards.

Compatibility is not established by compilation. Battle Extras, Battle Positions, Mega Showdown, RCT, and the local Battle Builds addon require integrated testing, particularly selected lead order and battle result behavior. Inspection of Battle Builds 1.0.0 showed post-battle build reporting, not build modification: it retains full builds server-side during battle, then sends them after battle end. That existing disclosure is preserved. Startup failure behavior also needs scrutiny: the Tower creates its visual trainer before RCT returns success. The prototype captures that trainer for failed-start disposal, invokes original stale-battle recovery on exceptions, and restores pre-attempt position/levels/XP; these paths still need real-player fault tests.

Ship only after testing cancellation, reconnect, restart, stale submissions, changed parties, consecutive floors, normal 6v6, normalization/restoration, wins/losses, and NPC/entity/arena cleanup on a copied instance. Never experiment against the live world first.

## License boundary

The installed metadata declares **All Rights Reserved**, authors Raguto & Yemagh. This project does not patch or redistribute that JAR, bundle its classes/resources, or include decompiled implementation. Private use is not treated as permission; applicable license terms and any required author permission remain a separate issue. This is a record of the observed license and implementation boundary, not a legal determination.

## Phase 2 update

Version 0.2.0 adds a client-only Screen with species/form icons and a bounded, versioned Fabric preview/submit/cancel/status protocol. The original choose path remains authoritative for both GUI and commands. See PHASE-2-PLAN.md for the full plan. NPC choice remains random. The screen is optional for multiplayer clients; capability checks select GUI delivery, with chat still available.
