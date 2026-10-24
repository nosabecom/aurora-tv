package com.gathin.tvhome;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** A durable shuffle bag. Only successfully displayed images are marked; additions join this cycle. */
final class PhotoCycle {
    final Set<String> seen;
    private final Random random;

    PhotoCycle(Set<String> seen, Random random) {
        this.seen = new HashSet<>(seen);
        this.random = random;
    }

    String next(List<String> eligible, String previous) {
        seen.retainAll(new HashSet<>(eligible));
        List<String> remaining = new ArrayList<>();
        for (String id : eligible) if (!seen.contains(id)) remaining.add(id);
        if (remaining.isEmpty()) {
            seen.clear();
            remaining.addAll(eligible);
            if (remaining.size() > 1) remaining.remove(previous);
        }
        return remaining.isEmpty() ? null : remaining.get(random.nextInt(remaining.size()));
    }

    void displayed(String id) { seen.add(id); }

    static int columns(int value) { return Math.max(3, Math.min(8, value)); }

    static String greeting(int hour) {
        return hour >= 5 && hour < 12 ? "Good morning"
                : hour >= 12 && hour < 17 ? "Good afternoon" : "Good evening";
    }
}
