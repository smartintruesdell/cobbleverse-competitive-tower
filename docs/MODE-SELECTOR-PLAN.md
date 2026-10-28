# Tower format selector — 0.3.0

## Goal

Expose Doubles VGC in the original Holo Tower format cycle alongside Singles, Doubles and Co-op. The new option chooses the existing bring-six/pick-four preview flow without requiring a command. Ordinary Doubles explicitly selects six-versus-six.

## Integration

Keep all original Tower format indices (0–2) and BattleMode enum values. A client-only Mixin introduces a virtual fourth choice and changes only the Doubles label when that choice is selected. The existing draw code computes the clickable width from the resulting text. Original Singles, Doubles, Co-op and boss machinery retain valid enum values and array indices.

A versioned addon payload carries normal Tower lock settings plus the competitive boolean together. The server validates the payload, checks VGC enablement and six healthy party members, then invokes the original Tower lock handler. A scoped intent is committed into PlayerTowerData exactly where the original lock commits its snapshot. The flag is included in the original Tower save before its normal data sync. Failed locks preserve the previous run flag.

The flag is nullable for compatibility: old saves without it retain the existing command-preference behavior. Every successful new lock stores an explicit true/false override; this prevents an old /ctower on preference from accidentally turning an explicit ordinary Doubles selection into VGC. No enum migration or separate rewards/stats is added.

Addon mode state is synchronized with original Tower data. Locked/active screens restore the correct VGC label, including newly opened screens. The extra option is available only when the server advertises the new lock channel and reports the addon enabled. Older servers keep the existing three-choice screen; commands remain available.

## Validation

Unit tests cover the four-choice cycle without extending original indices, invalid combinations, bounded mode packets, and atomic competitive/non-competitive lock fields. Runtime testing must drive the original screen, test insufficient party rejection, explicitly lock normal Doubles despite an old command preference, switch to VGC, serialize/reload each flag, reopen the screen, and start ordered 4v4. A subsequent explicit normal Doubles lock must start 6v6. Verify persistent party membership/order after both.

## Rollout

Update the local addon, preserving 0.2.1 as a disabled backup and preserving configuration. Both client and server need 0.3.0 for the selector option. Hosted deployment is a separate file update with read-back verification and an inactive backup of the previous JAR. Restart is separate and must be coordinated; an already-running server continues using the loaded version until restarted.
