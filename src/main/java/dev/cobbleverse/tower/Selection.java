package dev.cobbleverse.tower;

import java.util.*;

/** Pure selection policy. No Minecraft or Pokémon build data enters this class. */
public final class Selection {
    private Selection() {}
    public static int[] validate(int... slots) {
        if (slots.length != 4) throw new IllegalArgumentException("Choose exactly four slots.");
        Set<Integer> seen = new HashSet<>();
        for (int slot : slots) {
            if (slot < 1 || slot > 6 || !seen.add(slot))
                throw new IllegalArgumentException("Choose four different slots from 1 to 6.");
        }
        return slots.clone();
    }
    public static int[] randomFour(Random random) {
        List<Integer> slots = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6));
        Collections.shuffle(slots, random);
        return slots.subList(0, 4).stream().mapToInt(Integer::intValue).toArray();
    }
    public static <T> List<T> pick(List<T> six, int[] slots) {
        if (six.size() != 6) throw new IllegalArgumentException("Exactly six Pokémon are required.");
        return Arrays.stream(validate(slots)).mapToObj(i -> six.get(i - 1)).toList();
    }
    public static boolean accepts(UUID owner, UUID sender, String token, String submitted,
                                  long deadline, long now, List<UUID> before, List<UUID> current) {
        return owner.equals(sender) && token.equals(submitted) && now < deadline && before.equals(current);
    }
}
