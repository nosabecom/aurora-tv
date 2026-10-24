package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.*;

public class PhotoCycleTest {
    @Test public void fullAlbumBeforeRepeatAcrossRestarts() {
        List<String> photos = new ArrayList<>();
        for(int i=0;i<678;i++) photos.add("photo-"+i);
        Set<String> saved = new HashSet<>(), displayed = new HashSet<>(); String previous="";
        for(int i=0;i<678;i++) {
            PhotoCycle restarted = new PhotoCycle(saved, new Random(i));
            String picked = restarted.next(photos, previous);
            assertTrue("A photo repeated at "+i, displayed.add(picked));
            restarted.displayed(picked); saved = new HashSet<>(restarted.seen); previous=picked;
        }
        PhotoCycle next = new PhotoCycle(saved,new Random(1));
        assertNotEquals(previous,next.next(photos,previous));
    }
    @Test public void additionsJoinWithoutResettingHistory() {
        PhotoCycle cycle = new PhotoCycle(new HashSet<>(Arrays.asList("a","b")), new Random(2));
        String next=cycle.next(Arrays.asList("a","b","c","new"),"b");
        assertTrue(next.equals("c") || next.equals("new")); cycle.displayed(next);
        assertEquals(next.equals("c") ? "new" : "c",cycle.next(Arrays.asList("a","b","c","new"),next));
    }
    @Test public void deletionsDoNotResetSurvivors() {
        PhotoCycle cycle=new PhotoCycle(new HashSet<>(Arrays.asList("a","deleted")),new Random(0));
        assertEquals("b",cycle.next(Arrays.asList("a","b"),"a"));
        assertTrue(cycle.seen.contains("a")); assertFalse(cycle.seen.contains("deleted"));
    }
    @Test public void failedDecodeDoesNotMarkSeen() {
        PhotoCycle cycle=new PhotoCycle(Collections.emptySet(),new Random(0));
        cycle.next(Arrays.asList("a","b"),""); assertTrue(cycle.seen.isEmpty());
    }
    @Test public void emptyAndSingletonAreSafe() {
        PhotoCycle cycle=new PhotoCycle(Collections.emptySet(),new Random(0));
        assertNull(cycle.next(Collections.emptyList(),""));
        assertEquals("a",cycle.next(Collections.singletonList("a"),"a"));
        cycle.displayed("a"); assertEquals("a",cycle.next(Collections.singletonList("a"),"a"));
    }
    @Test public void gridRangeAndGreetingBoundaries() {
        for(int i=3;i<=8;i++) assertEquals(i,PhotoCycle.columns(i));
        assertEquals(3,PhotoCycle.columns(-1)); assertEquals(8,PhotoCycle.columns(100));
        assertEquals("Good evening",PhotoCycle.greeting(4)); assertEquals("Good morning",PhotoCycle.greeting(5));
        assertEquals("Good morning",PhotoCycle.greeting(11)); assertEquals("Good afternoon",PhotoCycle.greeting(12));
        assertEquals("Good afternoon",PhotoCycle.greeting(16)); assertEquals("Good evening",PhotoCycle.greeting(17));
        assertEquals("Good evening",PhotoCycle.greeting(23));
    }
}
