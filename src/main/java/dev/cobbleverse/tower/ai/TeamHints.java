package dev.cobbleverse.tower.ai;
import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import java.io.IOException;
import java.util.function.Consumer;

/** Addon-owned metadata; exact roster matching prevents trainer-name collisions in random mode. */
public final class TeamHints {
    private TeamHints() {}
    public record Entry(String trainer,List<String> roster,NpcSelector.Hints hints) {
        public Entry {roster=List.copyOf(roster);}
        public boolean matches(String trainer,List<String> roster){return this.trainer.equalsIgnoreCase(trainer) && this.roster.equals(roster);}
    }
    public static List<Entry> load(Path file,Consumer<String> warning) {
        try {
            if(!Files.exists(file)) {Files.writeString(file,"{\n  \"version\": 1,\n  \"teams\": []\n}\n");return List.of();}
            if(Files.size(file)>131072)throw new IOException("Hints file exceeds 128 KiB");
            return parse(Files.readString(file),warning);
        } catch(Exception e){warning.accept("Team hints ignored: "+e.getMessage());return List.of();}
    }
    public static List<Entry> parse(String json,Consumer<String> warning) {
        var root=JsonParser.parseString(json).getAsJsonObject();
        keys(root,Set.of("version","teams"));
        if(root.get("version").getAsInt()!=1)throw new IllegalArgumentException("Unsupported hints version");
        var teams=root.getAsJsonArray("teams");
        if(teams.size()>512)throw new IllegalArgumentException("Too many team hints");
        List<Entry> entries=new ArrayList<>();int index=0;
        for(var element:teams) {
            int current=index++;
            try {
                var team=element.getAsJsonObject();
                keys(team,Set.of("trainer","roster","archetype","preferred_leads","preferred_backline"));
                String trainer=bounded(team.get("trainer").getAsString());
                var roster=strings(team.getAsJsonArray("roster"));if(roster.size()!=6)throw new IllegalArgumentException("Roster must have six exact species|form keys");
                List<List<Integer>> leads=new ArrayList<>();
                if(team.has("preferred_leads"))for(var pair:team.getAsJsonArray("preferred_leads"))leads.add(slots(pair.getAsJsonArray()));
                var back=team.has("preferred_backline")?slots(team.getAsJsonArray("preferred_backline")):List.<Integer>of();
                var hints=new NpcSelector.Hints(team.has("archetype")?bounded(team.get("archetype").getAsString()):"unspecified",leads,back);
                var entry=new Entry(trainer,roster,hints);
                if(entries.stream().anyMatch(e->e.matches(trainer,roster)))throw new IllegalArgumentException("Duplicate trainer/roster hints");
                entries.add(entry);
            }catch(Exception e){warning.accept("Team hints entry "+current+" ignored: "+e.getMessage());}
        }
        return List.copyOf(entries);
    }
    private static void keys(JsonObject object,Set<String> allowed) {
        for(String key:object.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Unsupported field "+key+" (strategy action policies are not implemented)");
    }
    private static String bounded(String value){if(value.length()>128 || value.isBlank())throw new IllegalArgumentException("Invalid label");return value;}
    private static List<String> strings(JsonArray array){List<String> values=new ArrayList<>();for(var e:array)values.add(bounded(e.getAsString()));return values;}
    private static List<Integer> slots(JsonArray array){List<Integer> values=new ArrayList<>();for(var e:array){int slot=e.getAsInt();if(!e.getAsString().equals(Integer.toString(slot)))throw new IllegalArgumentException("Slot must be an integer");values.add(slot);}return values;}
}
