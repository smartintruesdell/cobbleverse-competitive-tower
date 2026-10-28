package dev.cobbleverse.tower;

import java.util.*;

/** Ordered UI draft; never a substitute for server validation. */
public final class OrderedSelection {
    private final List<Integer> slots=new ArrayList<>();
    public List<Integer> slots() { return List.copyOf(slots); }
    public void toggle(int slot) {
        if(slot<1 || slot>6) throw new IllegalArgumentException("Invalid party slot");
        if(slots.contains(slot)) slots.remove(Integer.valueOf(slot));
        else if(slots.size()<4) slots.add(slot);
    }
    public void move(int index,int delta) {
        int target=index+delta;
        if(index>=0 && index<slots.size() && target>=0 && target<slots.size()) Collections.swap(slots,index,target);
    }
    public void clear() { slots.clear(); }
    public boolean complete() { return slots.size()==4; }
    public int[] confirmed() { return Selection.validate(slots.stream().mapToInt(Integer::intValue).toArray()); }
}
