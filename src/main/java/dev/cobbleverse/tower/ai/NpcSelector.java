package dev.cobbleverse.tower.ai;
import java.util.*;
import dev.cobbleverse.tower.Selection;

/** Pure, bounded pre-battle selector. Opponent builds and player submissions cannot enter its API. */
public final class NpcSelector {
    private NpcSelector() {}
    public record Move(String name,String type,double power,boolean damagesAlly) {}
    public record Member(String identity,List<String> types,int baseSpeed,String ability,List<Move> moves) {
        public Member {types=List.copyOf(types);moves=List.copyOf(moves);}
    }
    public record PublicMember(List<String> types,int baseSpeed) {
        public PublicMember {types=List.copyOf(types);}
    }
    public record Hints(String archetype,List<List<Integer>> preferredLeads,List<Integer> preferredBackline) {
        public static final Hints EMPTY=new Hints("unspecified",List.of(),List.of());
        public Hints {
            Objects.requireNonNull(archetype);
            preferredLeads=preferredLeads.stream().map(List::copyOf).toList();preferredBackline=List.copyOf(preferredBackline);
            if(preferredLeads.size()>15 || preferredBackline.size()>4)throw new IllegalArgumentException("Too many hints");
            for(var pair:preferredLeads)if(pair.size()!=2 || pair.get(0).equals(pair.get(1)) || pair.stream().anyMatch(s->s<1||s>6))throw new IllegalArgumentException("Invalid lead hint");
            if(new HashSet<>(preferredBackline).size()!=preferredBackline.size() || preferredBackline.stream().anyMatch(s->s<1||s>6))throw new IllegalArgumentException("Invalid backline hint");
        }
    }
    public record Candidate(List<Integer> slots,Map<String,Double> components) {
        public Candidate {slots=List.copyOf(slots);components=Collections.unmodifiableMap(new LinkedHashMap<>(components));}
        public double score(){return components.values().stream().mapToDouble(Double::doubleValue).sum();}
        public int[] orderedSlots(){return slots.stream().mapToInt(Integer::intValue).toArray();}
    }
    public record Result(Candidate chosen,int candidates,long seed) {}
    public static Result select(List<Member> own,List<PublicMember> opponent,Hints hints,long seed) {
        var candidates=candidates(own,opponent,hints);
        double best=candidates.stream().mapToDouble(Candidate::score).max().orElseThrow();
        // A small fixed margin permits varied play without selecting an obviously much worse candidate.
        var near=candidates.stream().filter(c->c.score()>=best-.2).toList();
        return new Result(near.get(new Random(seed).nextInt(near.size())),candidates.size(),seed);
    }
    public static List<Candidate> candidates(List<Member> own,List<PublicMember> opponent,Hints hints) {
        if(own.size()!=6 || opponent.size()!=6)throw new IllegalArgumentException("Exactly six per side required");
        List<Candidate> candidates=new ArrayList<>();
        for(int a=0;a<3;a++)for(int b=a+1;b<4;b++)for(int c=b+1;c<5;c++)for(int d=c+1;d<6;d++) {
            int[] four={a,b,c,d};
            for(int x=0;x<3;x++)for(int y=x+1;y<4;y++) {
                int first=four[x],second=four[y];
                var back=new ArrayList<Integer>();for(int slot:four)if(slot!=first && slot!=second)back.add(slot+1);
                var slots=List.of(first+1,second+1,back.get(0),back.get(1));
                var all=Selection.pick(own,slots.stream().mapToInt(Integer::intValue).toArray());
                var leads=all.subList(0,2);Map<String,Double> scores=new LinkedHashMap<>();
                scores.put("coverage",coverage(all,opponent));scores.put("openingCoverage",coverage(leads,opponent)*.65);
                scores.put("baseSpeed",opponent.stream().filter(t->leads.stream().anyMatch(m->m.baseSpeed>t.baseSpeed)).count()*.015);
                scores.put("defensiveOverlap",defence(all,opponent));scores.put("openingSafety",defence(leads,opponent)*.4);
                scores.put("passivePair",pair(leads.get(0),leads.get(1))+pair(leads.get(1),leads.get(0)));
                scores.put("friendlyFire",-friendlyFire(leads.get(0),leads.get(1))-friendlyFire(leads.get(1),leads.get(0)));
                boolean preferred=hints.preferredLeads.stream().anyMatch(p->new HashSet<>(p).equals(Set.of(slots.get(0),slots.get(1))));
                scores.put("hints",(preferred?.35:0)+back.stream().filter(hints.preferredBackline::contains).count()*.08);
                candidates.add(new Candidate(slots,scores));
            }
        }
        return List.copyOf(candidates);
    }
    private static double coverage(List<Member> team,List<PublicMember> targets) {
        double total=0;
        for(var target:targets) {
            double best=0,backup=0;
            for(var member:team) {
                double memberBest=0;
                for(var move:member.moves) {
                    if(move.power<=0)continue;
                    double stab=member.types.contains(move.type)?1.5:1;
                    double value=Math.sqrt(Math.min(150,move.power)/100)*stab*TypeMatchups.effectiveness(move.type,target.types);
                    memberBest=Math.max(memberBest,Math.min(4,value));
                }
                if(memberBest>best){backup=best;best=memberBest;}else backup=Math.max(backup,memberBest);
            }
            // Prefer a second member able to cover the matchup, rather than relying on one attacker.
            total+=best+backup*.25;
        }
        return total/targets.size();
    }
    private static double defence(List<Member> team,List<PublicMember> targets) {
        double penalty=0;
        for(var target:targets)for(String type:target.types) {
            long vulnerable=team.stream().filter(m->TypeMatchups.effectiveness(type,m.types)>1).count();
            // Public STAB possibilities, not an assertion that the player knows those moves.
            penalty+=Math.max(0,vulnerable-1)*.08;
        }
        return -penalty/targets.size();
    }
    private static double pair(Member setter,Member partner) {
        double bonus=0;
        if(setter.ability.equals("intimidate"))bonus+=.12;
        String weather=weather(setter.ability),other=weather(partner.ability);
        if(!weather.isEmpty() && !other.isEmpty() && !weather.equals(other))bonus-=.5;
        if(weather.equals("rain") && Set.of("swiftswim","raindish","dryskin","hydration").contains(partner.ability))bonus+=.3;
        if(weather.equals("sun") && Set.of("chlorophyll","solarpower","protosynthesis").contains(partner.ability))bonus+=.3;
        if(weather.equals("sand") && Set.of("sandrush","sandforce","sandveil").contains(partner.ability))bonus+=.3;
        if(weather.equals("snow") && Set.of("slushrush","icebody","snowcloak").contains(partner.ability))bonus+=.3;
        if(setter.ability.equals("electricsurge") && partner.ability.equals("quarkdrive"))bonus+=.3;
        return bonus;
    }
    private static String weather(String ability) {
        return switch(ability){case "drizzle","primordialsea"->"rain";case "drought","desolateland"->"sun";
            case "sandstream"->"sand";case "snowwarning"->"snow";default->"";};
    }
    private static double friendlyFire(Member attacker,Member partner) {
        double worst=0;
        for(var move:attacker.moves)if(move.damagesAlly) {
            boolean immune=(move.type.equals("ground") && partner.ability.equals("levitate"))
                || (move.type.equals("electric") && Set.of("voltabsorb","lightningrod","motordrive").contains(partner.ability))
                || (move.type.equals("water") && Set.of("waterabsorb","stormdrain","dryskin").contains(partner.ability))
                || (move.type.equals("fire") && partner.ability.equals("flashfire")) || partner.ability.equals("telepathy");
            if(!immune)worst=Math.max(worst,.2*TypeMatchups.effectiveness(move.type,partner.types));
        }
        return worst;
    }
}
