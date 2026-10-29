# Development roadmap

Agreed sequence: repository/licensing → richer reference cards → intelligent NPC selection/openers → best-of-three tournament mode. This is a development plan; later milestones are not features of 0.3.0.

## 1. Repository and permissive license

- Establish a source-only Git baseline of the tested 0.3.0 addon.
- Publish it in a public GitHub repository under the MIT license for original addon code/documentation.
- Retain upstream wrapper notices and explicitly exclude game art/dependency rights from the MIT grant.
- Ignore installed mods, runtime/build files, decompilations, private deployment scripts and server/account records.
- Keep reproducible dependency pins, local build instructions, tests and verification limits with the source.

Acceptance: clean baseline commit; no prohibited binaries or private operational material in tracked files or source archives; build/test passes; runtime license and metadata are present; public remote matches the reviewed local source.

## 2. Richer friendly reference cards

Implemented in 0.4.0: compact friendly hover/focus summaries and a scrollable right-click/R reference panel. Uses synced own-party ability/moves/stats and available held-item translation descriptions. Statistics are labeled as persistent party values, not Tower-normalized predictions. Open Team Sheets was added in 0.5.0 as **Doubles VGC Open**, with compact opponent hover/focus cards and a separate allowlisted packet. Closed preview networking remains unchanged.

Extend the existing friendly move/item popout with ability and description, held-item description, move type/category/power/accuracy/PP, and useful player-owned battle statistics. Clearly distinguish normalized battle values from persistent party values. Start with compact useful summaries; test hover/focus, viewport scaling, long names and keyboard access. Prefer existing synced player data where it is accurate; inspect installed APIs rather than infer unsupported fields.

### Stretch: optional Open Team Sheets — implemented in 0.5.0

Treat this as a format choice set before locking a run. Keep the current closed species/form preview as the default. The server controls and persists the choice, and the screen clearly identifies the format before confirmation.

An explicitly open format may disclose the opponent's moves, ability, held item and applicable declared battle-gimmick information for its six. It must not accidentally disclose EVs, IVs, raw stats, internal NBT or the opponent's selected four/leads. Specify the final field list before implementation. Clients must support the format; an older client must not silently enter a format whose required information it cannot display.

Closed-mode packets must remain free of hidden build fields; hiding those fields only in the UI is insufficient. Add tests for both packet schemas, mode persistence, mixed-version behavior and privacy separation. Existing post-battle Battle Builds reporting is a separate behavior and should be documented.

Acceptance: accurate friendly references; readable layout; no default opponent-build disclosure. The stretch goal is complete only after explicit server-side open/closed gating and packet privacy checks pass.

## 3. Intelligent NPC four and opener selection

Initial heuristic and optional roster-matched hints implemented in 0.6.0; see [NPC selection architecture](NPC-SELECTION.md). Coordinated move policies and measured battle-strength comparisons remain future work. The selector keeps the same public-only player information budget in both sheet formats.

Inspect the installed Tower/RCT/Wild Battle AI before designing scoring rules. Treat pre-battle selection as an independent module that passes an ordered four to the existing move/switch AI.

Baseline design:

- Enumerate the 15 four-member subsets and their six unordered lead pairs: 90 candidates. Handle left/right ordering separately if a supported mechanic makes it relevant.
- Score public species/form matchups, the NPC's own coverage, defensive coherence, speed control and supported partner synergies.
- Use the NPC's own builds and only the player information allowed by the chosen format. Never inspect the player's submitted four or leads. Any expansion of information under an open-sheet format must be explicit and symmetric rather than an accidental consequence of server access.
- Reward strategies only when the installed move AI can execute them reliably.
- Make scoring explainable in server-side developer diagnostics, with deterministic seeds for tests and controlled randomness among strong candidates.
- Keep random-four as a selectable baseline/fallback.

Acceptance: legal unique four, coherent lead pair, no information cheating, reproducible tests against representative team fixtures, and actual battle trials. Measure improvements rather than assume a higher heuristic score means better play.

## 4. Best-of-three tournament mode

Introduce a set that ends when either side wins two games. Keep both registered six-member teams and their builds fixed across the set; allow a fresh ordered pick-four preview before each game. Show game number and set score.

Integrate set results ahead of the normal Tower floor/reward result path: an individual game must not increment the floor or pay a completed-set reward. Set completion should trigger progression and rewards exactly once. Preserve normal single-game modes.

Required design work:

- Persist set identity, team identities/builds, game results, score and safe transition checkpoints.
- Reset battle-local health, effects and clones between games without replacing the player's persistent party.
- Reuse the generated opponent six, preserve trainer identity/arena behavior, and support fair between-game NPC adjustments using only allowed information and prior public battle events.
- Define cancellation, disconnect, Save-and-Quit and crash recovery between games and during battle. Make reward/progression writes idempotent so a reconnect/retry cannot award twice.
- Handle 2–0 and 2–1 sets, intermediate losses, timeouts, stale submissions, server restart and final cleanup.

Acceptance: complete tested 2–0 and 2–1 sets; no per-game reward/floor leaks; no repeated final payout; correct selection/reset between games; reliable cancellation/recovery; ordinary Tower modes retain their existing results and cleanup.
