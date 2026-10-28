package dev.cobbleverse.tower;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ModeChoiceTest {
    @Test void cyclesFourChoicesWithoutExtendingTowerIndices() {
        var singles=new ModeChoice(0,false);var doubles=singles.next();var vgc=doubles.next();var coop=vgc.next();
        assertEquals(new ModeChoice(1,false),doubles);assertEquals(new ModeChoice(1,true),vgc);
        assertEquals(new ModeChoice(2,false),coop);assertEquals(singles,coop.next());
    }
    @Test void rejectsCompetitiveSinglesAndInvalidIndices() {
        assertThrows(IllegalArgumentException.class,()->new ModeChoice(0,true));
        assertThrows(IllegalArgumentException.class,()->new ModeChoice(2,true));
        assertThrows(IllegalArgumentException.class,()->new ModeChoice(3,false));
    }
}
