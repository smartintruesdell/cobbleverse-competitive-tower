package dev.cobbleverse.tower.ai;
import java.util.*;

/** Base Gen 6+ type relationships. No ability, item, Tera or battle-state prediction. */
public final class TypeMatchups {
    private TypeMatchups() {}
    private static final Map<String,List<String>> STRONG=rows(
        "normal:","fire:grass ice bug steel","water:fire ground rock","electric:water flying",
        "grass:water ground rock","ice:grass ground flying dragon","fighting:normal ice rock dark steel",
        "poison:grass fairy","ground:fire electric poison rock steel","flying:grass fighting bug",
        "psychic:fighting poison","bug:grass psychic dark","rock:fire ice flying bug","ghost:psychic ghost",
        "dragon:dragon","dark:psychic ghost","steel:ice rock fairy","fairy:fighting dragon dark");
    private static final Map<String,List<String>> WEAK=rows(
        "normal:rock steel","fire:fire water rock dragon","water:water grass dragon","electric:electric grass dragon",
        "grass:fire grass poison flying bug dragon steel","ice:fire water ice steel","fighting:poison flying psychic bug fairy",
        "poison:poison ground rock ghost","ground:grass bug","flying:electric rock steel",
        "psychic:psychic steel","bug:fire fighting poison flying ghost steel fairy","rock:fighting ground steel",
        "ghost:dark","dragon:steel","dark:fighting dark fairy","steel:fire water electric steel","fairy:fire poison steel");
    private static final Map<String,List<String>> IMMUNE=rows("normal:ghost","electric:ground","fighting:ghost",
        "poison:steel","ground:flying","psychic:dark","ghost:normal","dragon:fairy");
    private static Map<String,List<String>> rows(String... rows) {
        Map<String,List<String>> result=new HashMap<>();
        for(String row:rows){var parts=row.split(":",-1);result.put(parts[0],parts[1].isBlank()?List.of():List.of(parts[1].split(" ")));}
        return Map.copyOf(result);
    }
    public static double effectiveness(String attack,List<String> defending) {
        double value=1;
        for(String type:defending) {
            if(IMMUNE.getOrDefault(attack,List.of()).contains(type))return 0;
            if(STRONG.getOrDefault(attack,List.of()).contains(type))value*=2;
            if(WEAK.getOrDefault(attack,List.of()).contains(type))value*=.5;
        }
        return value;
    }
}
