package net.createmod.catnip.data;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import org.junit.Test;

public class CatnipDataTest {
    @Test public void uniqueListMaintainsIndexAcrossBulkAndIteratorMutations() {
        UniqueLinkedList<String> list=new UniqueLinkedList<String>();
        assertTrue(list.addAll(Arrays.asList("a","b","a","c")));
        assertEquals(Arrays.asList("a","b","c"),list);
        Iterator<String> iterator=list.iterator();assertEquals("a",iterator.next());iterator.remove();
        assertFalse(list.contains("a"));assertTrue(list.add("a"));
        assertEquals(3,list.size());
    }
    @Test(expected=IllegalArgumentException.class) public void uniqueListRejectsDuplicateReplacement(){
        UniqueLinkedList<String> list=new UniqueLinkedList<String>();list.addAll(Arrays.asList("a","b"));list.set(0,"b");
    }
    @Test public void coupleIteratorObeysIteratorContract(){
        Iterator<Integer> iterator=Couple.create(1,2).iterator();assertEquals(Integer.valueOf(1),iterator.next());assertEquals(Integer.valueOf(2),iterator.next());assertFalse(iterator.hasNext());
        try{iterator.next();fail("Expected NoSuchElementException");}catch(NoSuchElementException expected){}
    }
    @Test public void globProducesAnchoredPattern(){
        Pattern pattern=Pattern.compile(Glob.toRegexPattern("ponder:{scene_?,demo*}"));
        assertTrue(pattern.matcher("ponder:scene_1").matches());assertTrue(pattern.matcher("ponder:demo_world").matches());assertFalse(pattern.matcher("xponder:scene_1").matches());
    }
}
