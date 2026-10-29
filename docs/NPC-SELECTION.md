# NPC selection architecture

Target: the pinned Tower 1.11.23 / RCT API 0.15.2-beta / Cobblemon 1.7.3 installation. This addon selects four and their opening pair; it does not replace the in-battle AI.

## Inspection and feasibility

Local JAR inspection confirms that Tower's solo trainer factory supplies RCTBattleAI, with floor-dependent tuning. BattleManager passes that trainer AI to its battle actor. Wild Battle API's wild AI is a different path; its presence does not make Tower trainers use it. Tower's 171 team definitions are constructed in Java, not loaded from editable team-composition JSON. TeamResult exposes the generated six and trainer name, not an original definition ID; random mode can mix members from different source teams.

RCT has basic evaluators for Trick Room, Tailwind, Follow Me, Rage Powder, Helping Hand and Protect, as well as damage, switching and other actions. It does not expose a pre-battle four/opening-pair API or an explicit per-team strategy contract. Basic move recognition is not proof of coordinated execution, especially with deliberately weaker early-floor tuning. Actual turn-by-turn trials are necessary before claiming a strategy works.

## Initial selector

Enumerate all 15 subsets and six opening pairs per subset (90 candidates). Score own damaging coverage against all six public species/forms, defensive type overlap, opening coverage and passive partner effects. Penalize obvious friendly-fire lead combinations. Base typing/speed and move metadata are approximations, not a damage simulation or win-probability model. Do not assume an opponent's ability, item, moves, nature, EVs, IVs or selected four. Initially use this same public information budget even with Open Team Sheets; do not silently exploit server access.

Choose from candidates near the best score using a per-preview seed. Keep that result fixed for the preview and initiation retries. Retain RANDOM as a selectable baseline and fail safely to random if projection/scoring fails. Diagnostic output is server-side only and includes seed, selection and score components for reproducibility.

## Optional team intent

Keep hints in an addon-owned sidecar, without altering the proprietary JAR. Match a trainer name AND the exact ordered generated species/form roster; never apply a strategy just because a randomly assembled team borrowed a trainer's display name. Lead/backline slots are unambiguous even with repeated species/forms. Invalid hints must be reported and ignored without disabling battles.

Archetype is descriptive context. Preferred leads/backline are modest, defeasible score bonuses. `prioritize_trick_room` / `protect_setter` are action-policy requests, not selection hints: reject them until a separately tested RCT move-policy integration implements them. A hint must not pretend to force a move. Unknown or incomplete metadata cannot invalidate a legal six.

## Validation and limits

Use pure fixtures for unique four/order, candidate count, seeded reproducibility, coverage, passive lead synergy, conflicting weather and friendly fire, optional hints and malformed metadata. Exercise actual 4v4 battle initiation and persistent-party preservation in the disposable pinned runtime. Compare heuristic and random choices on the same fixtures without equating the score used to select them with measured battle strength.

A later strategy milestone should log observed public turns, verify setter/redirection coordination, and run matched seeded battle trials. No claim of improved win rate is justified by selector unit tests alone. Best-of-three remains a separate subsequent milestone.

## Configuration in 0.6.0

The server (or singleplayer profile) config `config/cobbleverse-competitive-tower.json` accepts:

```json
{
  "npcSelection": "HEURISTIC",
  "npcSelectionDiagnostics": false
}
```

Add those fields to the existing config; retain its enabled/timeout/player preferences. Missing fields use the values above. `RANDOM` restores the old uniform four-without-replacement policy. Unknown policy values or projection failures warn and fall back to random. Restart the game/server after changing configuration. Only competitive previews use this policy; original Tower modes are unchanged.

Optional hints live in `config/cobbleverse-competitive-tower-teams.json`, created as an empty version-1 file. Enable diagnostics for a local test, then copy the exact trainer and six ordered roster keys from its server log. The following is a schema example; replace the placeholder trainer and roster with that generated data:

```json
{
  "version": 1,
  "teams": [
    {
      "trainer": "YOUR EXACT TRAINER NAME",
      "roster": [
        "namespace:species1|form", "namespace:species2|form",
        "namespace:species3|form", "namespace:species4|form",
        "namespace:species5|form", "namespace:species6|form"
      ],
      "archetype": "trick_room",
      "preferred_leads": [[1, 2]],
      "preferred_backline": [3, 4]
    }
  ]
}
```

Slots are 1–6 in the registered NPC six, not the player's party. A preferred lead pair is unordered; reserve preferences favor membership, not reserve order. These are bonuses, not forced selections. An archetype label does not activate a strategy. Unsupported fields (including `strategy`) reject that entry with a warning rather than being silently accepted. Duplicate entries are rejected. Nothing in this sidecar is sent to the client.

Diagnostics expose the seed, generated roster identity, chosen four, 90-candidate count and score components to the server log only. They intentionally reveal no player hidden builds or submitted selection. Keep diagnostics off for normal play. No curated strategy hints for the proprietary built-in teams are shipped.

The heuristic uses base speed as a small tempo proxy and move power/type for coverage, without predicting accurate damage, actual turn order, ability-dependent opponent immunities, Tera choices, held-item effects or setup execution. Custom types unknown to the base chart are neutral. Friendly-fire avoidance is a penalty, not a guarantee the existing move AI will avoid using such moves. Passive weather bonuses do not predict activation order or weather duration. These limitations should guide playtesting and the next strategy milestone.
