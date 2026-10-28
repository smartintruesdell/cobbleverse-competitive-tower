package dev.cobbleverse.tower;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
class OrderedSelectionTest {
    @Test void clickOrderDeterminesLeadsAndRemovalCompacts() {
        var s=new OrderedSelection(); for(int x:new int[]{5,2,6,1})s.toggle(x);
        assertArrayEquals(new int[]{5,2,6,1},s.confirmed());
        s.toggle(2); assertEquals(List.of(5,6,1),s.slots());
        assertThrows(IllegalArgumentException.class,s::confirmed);
    }
    @Test void respectsCapacityAndReordersWithoutDuplication() {
        var s=new OrderedSelection();for(int i=1;i<=6;i++)s.toggle(i);
        assertEquals(List.of(1,2,3,4),s.slots());s.move(2,-1);
        assertArrayEquals(new int[]{1,3,2,4},s.confirmed());
        s.move(0,-1);s.move(3,1);assertArrayEquals(new int[]{1,3,2,4},s.confirmed());
        s.clear();assertFalse(s.complete());assertTrue(s.slots().isEmpty());
    }
    @Test void invalidSlotsCannotEnterDraft() {
        var s=new OrderedSelection();assertThrows(IllegalArgumentException.class,()->s.toggle(0));
        assertThrows(IllegalArgumentException.class,()->s.toggle(7));
    }
}
