package dev.cobbleverse.tower.ai;
import java.util.*;
import java.nio.file.Path;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.cobblemon.mod.common.battles.MoveTarget;
import dev.cobbleverse.tower.*;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

/** Server adapter: the opposing side is deliberately projected from public preview entries only. */
public final class NpcSelectionPolicy {
    private NpcSelectionPolicy() {}
    private static final org.slf4j.Logger LOG=LoggerFactory.getLogger("competitive-tower-selection");
    private static List<TeamHints.Entry> hints=List.of();
    public static void load(Path path){hints=TeamHints.load(path,LOG::warn);}
    public static int[] select(Pokemon[] own,List<PreviewNetwork.Entry> opposing,String trainer,String policy,boolean diagnostics) {
        long seed=new Random().nextLong();
        if("RANDOM".equals(policy)) {
            int[] selected=Selection.randomFour(new Random(seed));
            if(diagnostics)LOG.info("NPC random selection seed={} four={}",seed,Arrays.toString(selected));
            return selected;
        }
        try {
            if(!"HEURISTIC".equals(policy))throw new IllegalArgumentException("Unknown npcSelection policy "+policy);
            var roster=Arrays.stream(own).map(NpcSelectionPolicy::identity).toList();
            var intent=hints.stream().filter(e->e.matches(trainer,roster)).map(TeamHints.Entry::hints).findFirst().orElse(NpcSelector.Hints.EMPTY);
            var result=NpcSelector.select(Arrays.stream(own).map(NpcSelectionPolicy::member).toList(),publicMembers(opposing),intent,seed);
            if(diagnostics)LOG.info("NPC selection seed={} trainer={} roster={} archetype={} candidates={} four={} components={}",
                seed,trainer,roster,intent.archetype(),result.candidates(),result.chosen().slots(),result.chosen().components());
            return Selection.validate(result.chosen().orderedSlots());
        } catch(RuntimeException e) {
            LOG.warn("NPC heuristic failed; using random four (seed={})",seed,e);
            return Selection.randomFour(new Random(seed));
        }
    }
    private static String identity(Pokemon p){return p.getSpecies().getResourceIdentifier()+"|"+p.getForm().getName().toLowerCase(Locale.ROOT);}
    private static NpcSelector.Member member(Pokemon p) {
        List<String> types=new ArrayList<>();p.getTypes().forEach(t->types.add(t.getName()));
        var moves=p.getMoveSet().getMoves().stream().map(m->{
            boolean damage=!m.getDamageCategory().getName().equalsIgnoreCase("status");
            var target=m.getTemplate().getTarget();
            boolean friendly=damage && (target==MoveTarget.all || target==MoveTarget.allAdjacent);
            return new NpcSelector.Move(m.getName(),m.getType().getName(),damage?(m.getPower()>0?m.getPower():50):0,friendly);
        }).toList();
        return new NpcSelector.Member(identity(p),types,p.getForm().getBaseStats().getOrDefault(Stats.SPEED,0),p.getAbility().getName(),moves);
    }
    public static List<NpcSelector.PublicMember> publicMembers(List<PreviewNetwork.Entry> opposing) {
        return opposing.stream().map(e->{
            var species=PokemonSpecies.getByIdentifier(Identifier.of(e.species()));
            if(species==null)throw new IllegalArgumentException("Unknown public species");
            var form=java.util.stream.Stream.concat(java.util.stream.Stream.of(species.getStandardForm()),species.getForms().stream()).filter(f->f.getName().equalsIgnoreCase(e.form())).findFirst().orElseThrow();
            List<String> types=new ArrayList<>();form.getTypes().forEach(t->types.add(t.getName()));
            return new NpcSelector.PublicMember(types,form.getBaseStats().getOrDefault(Stats.SPEED,0));
        }).toList();
    }
}
