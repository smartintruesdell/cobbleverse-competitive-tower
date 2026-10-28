package dev.cobbleverse.tower;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SelectionTest {
    @Test void orderedLeadsDoNotReorderPersistentParty() {
        var party = new ArrayList<>(List.of("a", "b", "c", "d", "e", "f"));
        assertEquals(List.of("e", "b", "f", "a"), Selection.pick(party, new int[]{5, 2, 6, 1}));
        assertEquals(List.of("a", "b", "c", "d", "e", "f"), party);
    }
    @Test void rejectsInvalidRosters() {
        for (int[] bad : new int[][]{{1,1,2,3},{0,2,3,4},{1,2,3,7},{1,2,3},{1,2,3,4,5}})
            assertThrows(IllegalArgumentException.class, () -> Selection.validate(bad));
        assertThrows(IllegalArgumentException.class, () -> Selection.pick(List.of(1,2,3,4), new int[]{1,2,3,4}));
    }
    @Test void npcSelectionIsUniqueAndCanUseEverySlot() {
        var random = new Random(123); var seen = new HashSet<Integer>();
        for (int i = 0; i < 100; i++) for (int slot : Selection.validate(Selection.randomFour(random))) seen.add(slot);
        assertEquals(Set.of(1,2,3,4,5,6), seen);
    }
    @Test void rejectsOtherPlayersExpiredTokensAndChangedParty() {
        UUID owner = UUID.randomUUID(); var party = List.of(UUID.randomUUID(), UUID.randomUUID());
        assertTrue(Selection.accepts(owner, owner, "a", "a", 10, 9, party, party));
        assertFalse(Selection.accepts(owner, UUID.randomUUID(), "a", "a", 10, 9, party, party));
        assertFalse(Selection.accepts(owner, owner, "a", "b", 10, 9, party, party));
        assertFalse(Selection.accepts(owner, owner, "a", "a", 10, 10, party, party));
        assertFalse(Selection.accepts(owner, owner, "a", "a", 10, 9, party, List.of(party.get(1),party.get(0))));
    }
}
