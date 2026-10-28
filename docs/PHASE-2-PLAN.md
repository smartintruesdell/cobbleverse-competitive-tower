# Phase 2 — Player UI implementation plan

## Scope and baseline

Build the visual player interface on the tested 0.1.1 battle flow. User playtests confirmed a completed round, a fresh round-two preview, timeout forfeiture and Save-and-Quit recovery. Preserve those paths and random NPC selection; intelligent NPC subsets/openers are Phase 3.

## 1. Integration and rendering

The installed Cobblemon 1.7.3 BattleSwitchPokemonSelection constructor requires BattleGUI and SingleActionRequest. PartySelectGUI is also tied to Cobblemon callbacks rather than our ordered four-slot protocol. Use a dedicated Minecraft Screen and native focusable buttons, reusing Cobblemon PokemonItem species/form icons. No copied Tower/Cobblemon implementation or textures. Client classes are referenced only from a client entrypoint; dedicated-server startup must continue to work.

## 2. Protocol and privacy

Versioned Fabric payload IDs. Server-to-client preview contains a per-preview token, floor, remaining milliseconds, and exactly six entries per side. Each entry contains only species identifier/name and form name. Do not send Pokemon objects, NBT, held items, abilities, stats, moves, EVs, IVs, Tera, opponent selection, or arbitrary Pokémon aspects. Client resolves form appearance from its own species registry. Icons must use species/form-only construction.

Client-to-server confirm has token plus exactly four slot integers; cancellation has token only. Server checks the sender's existing pending preview and routes into the existing choose/cancel code. Fixed list sizes and bounded strings prevent oversized allocations. UI capability detection uses versioned channels; old/no-addon clients retain chat. Stale responses must never close a newer preview.

## 3. Layout and interaction

A centered dark panel, title/floor and countdown, six opponent cards, six selectable own-party cards, four numbered ordered slots, and explicit Confirm / Clear / Use chat / Forfeit buttons. Lead slots are distinguished by both text and color. Clicking a party card toggles selection; selected-slot controls allow removal and adjacent reorder. Confirm is enabled only for four unique slots. Preserve selection across resize and same-token refresh.

Native buttons support Tab/Shift-Tab and Enter/Space. Display species/form text even if icon resolution fails. Use a compact responsive layout for the minimum normal Minecraft scaled viewport. Countdown is advisory; the server deadline is authoritative and is never extended by reopening. Screen must not pause singleplayer. Closing with Esc or Forfeit asks for confirmation, clearly stating that it ends the run. Use chat hides only the screen; the timer keeps running and existing chat commands remain usable.

## 4. State transitions

Preview opens automatically after Tower start. Confirm disables interaction while awaiting server acknowledgement. Recoverable rejection (such as Tower initiation cooldown) unlocks the same selection and displays the reason. Success or server forfeiture closes the matching screen. Timeout closes only after server confirmation, with an awaiting-server state at zero. Disconnect clears all client state. Use chat does not cancel. Never use Screen.removed() as a cancellation trigger: resize, battle UI transitions and server closure must not forfeit a battle.

## 5. Server changes

Keep one authoritative choose implementation for chat and GUI. Add GUI responses around that function, not a second battle path. Notify matching GUI clients when previews end through commands, timeout or original Tower forfeit. Keep server player/token ownership, party/build/snapshot/position/settings/cooldown checks. Round progression, arena behavior, normalization, rewards and NPC generation remain owned by Tower. No automatic opt-in or config changes.

## 6. Validation

Build and run existing selection tests. Add meaningful tests for ordered UI selection, removal/compaction, reordering, four-slot capacity and reset; add packet round-trip/bounds tests where Minecraft test initialization permits. Inspect the packaged runtime JAR for metadata and absence of proprietary dependencies. Start a disposable dedicated server to catch accidental client-only class loading and injection regressions.

In-client acceptance: automatic opening, all six species/forms on both sides, lead ordering, keyboard navigation, resize, clear/reorder, duplicate confirm protection, cooldown rejection recovery, stale token isolation, Esc confirmation/back, Use chat, command cancellation, timeout, disconnect, next-floor reopening and actual battle leads. Inspect screenshots when a safe test client is available; do not claim a visual/gameplay check merely from compilation. Retain explicit pending checks if the running user game cannot be restarted safely.

## 7. Delivery and rollback

Version 0.2.0-experimental. Produce runtime JAR and addon-only source ZIP, update README/verification. Preserve the installed 0.1.1 JAR as a disabled backup, install 0.2.0 in the local profile without altering user worlds or preferences. User saves and fully restarts Minecraft to load it. Dedicated multiplayer needs 0.2.0 on server and clients for the GUI; old clients use chat. Restore the old JAR to roll back; pending preview cleanup continues through existing Tower recovery.

## Phase 3 boundary

Do not change NPC random-four or lead choice in this phase. Future work will inspect actual move AI capabilities, then evaluate subsets/lead pairs using the NPC's own builds and only publicly visible player species/forms.

## Player feedback refinement — 0.2.1

Use red-tinted opponent cards, hide the Normal form label, and use card numbers instead of a separate lead/reserve row. Move the order controls to scroll-over-selected-card and focused-card Left/Right. Friendly hover/focus popouts show the local player’s synced moves, requiring no new server payload fields. Center artwork in the area above the labels, with a target sprite size twice 0.2.0 and responsive reduction on small screens.

Friendly held-item badges use the same local party source, with item names included in the friendly popout. Opponent item information is never fetched or transmitted. Item art is overlaid in the lower-right of the sprite region; floating Pokémon art is clipped within that region.
