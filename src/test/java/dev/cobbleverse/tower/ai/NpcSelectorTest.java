package dev.cobbleverse.tower.ai;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import dev.cobbleverse.tower.Selection;
class NpcSelectorTest {
    private NpcSelector.Member member(String name,String type,String ability,String moveType,boolean spread) {
        return new NpcSelector.Member(name,List.of(type),80,ability,List.of(new NpcSelector.Move("attack",moveType,100,spread)));
    }
    private List<NpcSelector.Member> uniform(){return Collections.nCopies(6,member("member","normal","","normal",false));}
    private List<NpcSelector.PublicMember> targets(String type){return Collections.nCopies(6,new NpcSelector.PublicMember(List.of(type),60));}
    @Test void enumeratesAllSubsetsAndLeadPairsOnce() {
        var cs=NpcSelector.candidates(uniform(),targets("normal"),NpcSelector.Hints.EMPTY);
        assertEquals(90,cs.size());assertEquals(90,cs.stream().map(c->c.slots()).distinct().count());
        for(var c:cs)assertArrayEquals(c.orderedSlots(),Selection.validate(c.orderedSlots()));
        assertEquals(15,cs.stream().map(c->new HashSet<>(c.slots())).distinct().count());
    }
    @Test void seedIsReproducibleAndVariesNearTiedChoices() {
        var first=NpcSelector.select(uniform(),targets("normal"),NpcSelector.Hints.EMPTY,42);
        assertEquals(first,NpcSelector.select(uniform(),targets("normal"),NpcSelector.Hints.EMPTY,42));
        Set<List<Integer>> choices=new HashSet<>();
        for(int seed=0;seed<30;seed++)choices.add(NpcSelector.select(uniform(),targets("normal"),NpcSelector.Hints.EMPTY,seed).chosen().slots());
        assertTrue(choices.size()>1);
    }
    @Test void strongPublicCoverageGetsBroughtAndLed() {
        var own=new ArrayList<>(uniform());own.set(5,member("fire","fire","","fire",false));
        for(int seed=0;seed<30;seed++) {
            var result=NpcSelector.select(own,targets("grass"),NpcSelector.Hints.EMPTY,seed);
            assertTrue(result.chosen().slots().subList(0,2).contains(6));
        }
    }
    @Test void passiveWeatherPairScoresAboveUnrelatedOrConflictingWeather() {
        var own=new ArrayList<>(uniform());
        own.set(0,member("rain","water","drizzle","water",false));
        own.set(1,member("swimmer","water","swiftswim","water",false));
        own.set(2,member("sun","fire","drought","fire",false));
        var cs=NpcSelector.candidates(own,targets("normal"),NpcSelector.Hints.EMPTY);
        assertTrue(component(cs,1,2,"passivePair")>component(cs,1,4,"passivePair"));
        assertTrue(component(cs,1,3,"passivePair")<0);
    }
    @Test void spreadPartnerImmunityAvoidsFriendlyFirePenalty() {
        var own=new ArrayList<>(uniform());
        own.set(0,member("quake","ground","","ground",true));
        own.set(1,member("ally","normal","levitate","normal",false));
        var cs=NpcSelector.candidates(own,targets("normal"),NpcSelector.Hints.EMPTY);
        assertEquals(0,component(cs,1,2,"friendlyFire"),1e-9);
        assertTrue(component(cs,1,3,"friendlyFire")<0);
    }
    @Test void hintsAreSoftAndNeverReplaceLegalRosterValidation() {
        var hints=new NpcSelector.Hints("trick_room",List.of(List.of(5,6)),List.of(3,4));
        var cs=NpcSelector.candidates(uniform(),targets("normal"),hints);
        assertTrue(component(cs,5,6,"hints")>component(cs,1,2,"hints"));
        assertThrows(IllegalArgumentException.class,()->new NpcSelector.Hints("x",List.of(List.of(1,1)),List.of()));
        assertThrows(IllegalArgumentException.class,()->NpcSelector.select(uniform().subList(0,5),targets("normal"),hints,1));
    }
    @Test void publicOpponentModelHasNoBuildOrSelectionFields() {
        assertEquals(List.of("types","baseSpeed"),Arrays.stream(NpcSelector.PublicMember.class.getRecordComponents()).map(c->c.getName()).toList());
    }
    @Test void dualTypesAndImmunitiesUseBaseRelationships() {
        assertEquals(4,TypeMatchups.effectiveness("ice",List.of("ground","flying")));
        assertEquals(0,TypeMatchups.effectiveness("electric",List.of("water","ground")));
        assertEquals(.25,TypeMatchups.effectiveness("grass",List.of("fire","flying")));
        assertEquals(0,TypeMatchups.effectiveness("dragon",List.of("fairy")));
    }
    private double component(List<NpcSelector.Candidate> cs,int a,int b,String key) {
        return cs.stream().filter(c->c.slots().get(0)==Math.min(a,b) && c.slots().get(1)==Math.max(a,b)).findFirst().orElseThrow().components().get(key);
    }
}
