package dev.cobbleverse.tower.ai;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class SelectionRampTest {
    private List<NpcSelector.Candidate> candidates(){return List.of(
        new NpcSelector.Candidate(List.of(1,2,3,4),Map.of("score",1d)),
        new NpcSelector.Candidate(List.of(3,4,5,6),Map.of("score",.9)),
        new NpcSelector.Candidate(List.of(5,6,1,2),Map.of("score",.1)));}
    @Test void defaultBoundariesAndBeyondTen() {
        var ramp=SelectionRamp.defaults();
        for(int f=1;f<=4;f++)assertEquals(0,ramp.at(f).heuristicChance());
        assertEquals(.7,ramp.at(5).heuristicChance());assertEquals(.8,ramp.at(6).heuristicChance());
        assertEquals(.9,ramp.at(7).heuristicChance());assertEquals(.95,ramp.at(8).heuristicChance());
        for(int f:new int[]{9,10,11,100,Integer.MAX_VALUE}) {
            assertEquals(1,ramp.at(f).heuristicChance());assertEquals(0,ramp.at(f).scoreMargin());assertEquals(0,ramp.at(f).shuffleChance());
        }
    }
    @Test void earlyFloorsNeverInvokeScorerOrHintsAndRandomizeLeads() {
        var seen=new HashSet<List<Integer>>();
        for(int seed=0;seed<100;seed++) {
            var choice=SelectionRamp.defaults().choose(4,seed,()->{fail("Early floors must not score");return null;});
            assertFalse(choice.heuristic());assertNull(choice.scored());seen.add(choice.slots());
        }
        assertTrue(seen.size()>30);
    }
    @Test void topFloorsAlwaysPickActualMaximumWithoutShuffling() {
        for(int seed=0;seed<100;seed++) {
            var choice=SelectionRamp.defaults().choose(9,seed,this::candidates);
            assertTrue(choice.heuristic());assertFalse(choice.shuffled());assertEquals(List.of(1,2,3,4),choice.slots());
        }
        var ties=new ArrayList<>(candidates());ties.add(new NpcSelector.Candidate(List.of(6,5,4,3),Map.of("score",1d)));
        var seen=new HashSet<List<Integer>>();var seeds=new Random(123);for(int i=0;i<100;i++)seen.add(SelectionRamp.defaults().choose(10,seeds.nextLong(),()->ties).slots());
        assertEquals(2,seen.size());
    }
    @Test void middleTierMixesPoliciesAndShufflesButRemainsSeeded() {
        int heuristic=0,shuffle=0;var ramp=SelectionRamp.defaults();var random=new Random(77);
        for(int i=0;i<1000;i++) {
            long seed=random.nextLong();var choice=ramp.choose(5,seed,this::candidates);
            assertEquals(choice,ramp.choose(5,seed,this::candidates));
            if(choice.heuristic()){heuristic++;assertTrue(choice.scored().score()>=.5);}
            if(choice.shuffled())shuffle++;
        }
        assertTrue(heuristic>600 && heuristic<800);assertTrue(shuffle>100 && shuffle<250);
    }
    @Test void customBandsAndRoundTripAreSupported() {
        var ramp=new SelectionRamp(List.of(new SelectionRamp.Band(1,0,0,0),new SelectionRamp.Band(3,1,.3,1)));
        assertEquals(ramp,SelectionRamp.parse(ramp.json()));
        var choice=ramp.choose(3,5,this::candidates);assertTrue(choice.heuristic());assertTrue(choice.shuffled());
    }
    @Test void malformedRampDoesNotOverwriteFile(@TempDir Path directory)throws Exception {
        var file=directory.resolve("ramp.json");assertEquals(SelectionRamp.defaults(),SelectionRamp.load(file,m->fail(m)));
        Files.writeString(file,"invalid");var warnings=new ArrayList<String>();
        assertEquals(SelectionRamp.defaults(),SelectionRamp.load(file,warnings::add));assertEquals("invalid",Files.readString(file));assertEquals(1,warnings.size());
    }
    @Test void shippedExampleMatchesDefaults()throws Exception {
        assertEquals(SelectionRamp.defaults(),SelectionRamp.parse(Files.readString(Path.of("docs/examples/cobbleverse-competitive-tower-ramp.json"))));
    }
    @Test void invalidBandsAndMissingOrMistypedFieldsAreRejected() {
        assertThrows(IllegalArgumentException.class,()->new SelectionRamp.Band(1,Double.NaN,0,0));
        assertThrows(IllegalArgumentException.class,()->new SelectionRamp.Band(1,1,-1,0));
        assertThrows(IllegalArgumentException.class,()->new SelectionRamp(List.of(new SelectionRamp.Band(2,1,0,0))));
        assertThrows(IllegalArgumentException.class,()->new SelectionRamp(List.of(new SelectionRamp.Band(1,0,0,0),new SelectionRamp.Band(1,1,0,0))));
        assertThrows(IllegalArgumentException.class,()->SelectionRamp.parse("{\"version\":1,\"bands\":[{\"fromFloor\":1}]}"));
        assertThrows(IllegalArgumentException.class,()->SelectionRamp.parse(SelectionRamp.defaults().json().replace("\"heuristicChance\": 0.0","\"heuristicChance\": \"0\"")));
    }
}
