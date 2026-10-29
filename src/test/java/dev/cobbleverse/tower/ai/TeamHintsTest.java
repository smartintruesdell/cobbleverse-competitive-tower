package dev.cobbleverse.tower.ai;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
class TeamHintsTest {
    private String entry(){return """
        {"trainer":"Example trainer", "roster":["a|normal","b|normal","c|normal","d|normal","e|normal","f|normal"],
         "archetype":"trick_room", "preferred_leads":[[1,2]], "preferred_backline":[3,4]}
        """;}
    private String wrap(String entries){return "{\"version\":1,\"teams\":["+entries+"]}";}
    @Test void hintsRequireBothTrainerAndExactOrderedRoster() {
        var entries=TeamHints.parse(wrap(entry()),m->fail(m));var hint=entries.get(0);
        assertTrue(hint.matches("EXAMPLE TRAINER",hint.roster()));
        assertFalse(hint.matches("Other trainer",hint.roster()));
        var shuffled=new ArrayList<>(hint.roster());Collections.swap(shuffled,0,1);
        assertFalse(hint.matches(hint.trainer(),shuffled));
    }
    @Test void malformedAndUnsupportedActionPoliciesAreRejectedPerEntry() {
        var warnings=new ArrayList<String>();
        String strategy=entry().replace("\"archetype\":","\"strategy\":{\"protect_setter\":true},\"archetype\":");
        var entries=TeamHints.parse(wrap(strategy+","+entry()),warnings::add);
        assertEquals(1,entries.size());assertEquals(1,warnings.size());assertTrue(warnings.get(0).contains("strategy"));
        assertEquals(0,TeamHints.parse(wrap(entry().replace("[1,2]","[1,7]")),warnings::add).size());
    }
    @Test void versionAndDuplicateKeysCannotSilentlyOverrideHints() {
        assertThrows(IllegalArgumentException.class,()->TeamHints.parse("{\"version\":2,\"teams\":[]}",m->{}));
        var warnings=new ArrayList<String>();assertEquals(1,TeamHints.parse(wrap(entry()+","+entry()),warnings::add).size());assertEquals(1,warnings.size());
    }
}
