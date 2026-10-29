package dev.cobbleverse.tower.ai;

import com.google.gson.*;
import dev.cobbleverse.tower.Selection;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/** Floor-dependent selection only. Does not alter the Tower's move AI or information budget. */
public record SelectionRamp(List<SelectionRamp.Band> bands) {
    public record Band(int fromFloor,double heuristicChance,double scoreMargin,double shuffleChance) {
        public Band {
            if(fromFloor<1 || !probability(heuristicChance) || !probability(shuffleChance)
                || !Double.isFinite(scoreMargin) || scoreMargin<0 || scoreMargin>10)
                throw new IllegalArgumentException("Invalid ramp band");
        }
        private static boolean probability(double x){return Double.isFinite(x) && x>=0 && x<=1;}
    }
    public record Choice(List<Integer> slots,boolean heuristic,boolean shuffled,NpcSelector.Candidate scored) {
        public Choice {slots=List.copyOf(slots);Selection.validate(slots.stream().mapToInt(Integer::intValue).toArray());}
        public int[] orderedSlots(){return slots.stream().mapToInt(Integer::intValue).toArray();}
    }
    public SelectionRamp {
        bands=List.copyOf(bands);
        if(bands.isEmpty() || bands.size()>64 || bands.getFirst().fromFloor()!=1)throw new IllegalArgumentException("Ramp must start at floor 1");
        int previous=0;
        for(var band:bands){if(band.fromFloor()<=previous)throw new IllegalArgumentException("Ramp floors must strictly increase");previous=band.fromFloor();}
    }
    public static SelectionRamp defaults(){return new SelectionRamp(List.of(
        new Band(1,0,0,0),new Band(5,.70,.50,.25),new Band(6,.80,.35,.20),
        new Band(7,.90,.25,.10),new Band(8,.95,.15,.05),new Band(9,1,0,0)));}
    public Band at(int floor) {
        if(floor<1)throw new IllegalArgumentException("Invalid floor");
        Band chosen=bands.getFirst();for(var band:bands){if(band.fromFloor()>floor)break;chosen=band;}return chosen;
    }
    public Choice choose(int floor,long seed,Supplier<List<NpcSelector.Candidate>> scorer) {
        var band=at(floor);var random=new Random(seed);
        if(band.heuristicChance()==0 || (band.heuristicChance()<1 && random.nextDouble()>=band.heuristicChance())) {
            return new Choice(Arrays.stream(Selection.randomFour(random)).boxed().toList(),false,false,null);
        }
        var candidates=scorer.get();double best=candidates.stream().mapToDouble(NpcSelector.Candidate::score).max().orElseThrow();
        var near=candidates.stream().filter(c->c.score()>=best-band.scoreMargin()).toList();
        var candidate=near.get(random.nextInt(near.size()));var slots=new ArrayList<>(candidate.slots());
        boolean shuffle=band.shuffleChance()>0 && random.nextDouble()<band.shuffleChance();
        if(shuffle)Collections.shuffle(slots,random);
        return new Choice(slots,true,shuffle,candidate);
    }
    public static SelectionRamp load(Path file,Consumer<String> warning) {
        try {
            if(!Files.exists(file))Files.writeString(file,defaults().json());
            if(Files.size(file)>32768)throw new IllegalArgumentException("Ramp file exceeds 32 KiB");
            return parse(Files.readString(file));
        }catch(Exception e){warning.accept("Invalid NPC ramp; using built-in defaults: "+e.getMessage());return defaults();}
    }
    public String json() {
        var root=new JsonObject();root.addProperty("version",1);var array=new JsonArray();
        for(var band:bands) {
            var item=new JsonObject();item.addProperty("fromFloor",band.fromFloor());item.addProperty("heuristicChance",band.heuristicChance());
            item.addProperty("scoreMargin",band.scoreMargin());item.addProperty("shuffleChance",band.shuffleChance());array.add(item);
        }
        root.add("bands",array);return new GsonBuilder().setPrettyPrinting().create().toJson(root)+"\n";
    }
    public static SelectionRamp parse(String json) {
        var root=JsonParser.parseString(json).getAsJsonObject();keys(root,Set.of("version","bands"));
        if(number(root,"version")!=1)throw new IllegalArgumentException("Unsupported ramp version");
        var array=root.getAsJsonArray("bands");List<Band> bands=new ArrayList<>();
        if(array.size()>64)throw new IllegalArgumentException("Too many ramp bands");
        for(var element:array) {
            var item=element.getAsJsonObject();keys(item,Set.of("fromFloor","heuristicChance","scoreMargin","shuffleChance"));
            double floor=number(item,"fromFloor");if(floor!=Math.rint(floor) || floor<1 || floor>Integer.MAX_VALUE)throw new IllegalArgumentException("Invalid fromFloor");
            bands.add(new Band((int)floor,number(item,"heuristicChance"),number(item,"scoreMargin"),number(item,"shuffleChance")));
        }
        return new SelectionRamp(bands);
    }
    private static double number(JsonObject object,String name) {
        var value=object.getAsJsonPrimitive(name);
        if(value==null || !value.isNumber())throw new IllegalArgumentException("Missing/invalid number: "+name);
        return value.getAsDouble();
    }
    private static void keys(JsonObject object,Set<String> allowed) {
        for(String key:object.keySet())if(!allowed.contains(key))throw new IllegalArgumentException("Unknown ramp field: "+key);
    }
}
