package datastructure;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

class RingTest {

    @Test
    void testRingInt() {
        try {
            Ring<Integer> r = new Ring<>(0);
            assertEquals(0, r.size());

            r = new Ring<>(12);
            assertEquals(0, r.size());
        } catch (Exception e) {
            fail(e.getCause());
        }
    }

    @Test
    void testRingListOfE() {
        try {
            List<Integer> l = new ArrayList<>(3);
            l.add(12);
            l.add(42);
            l.add(45);
            Ring<Integer> r = new Ring<>(l);
            assertEquals(3, r.size());
        } catch (Exception e) {
            fail(e.getCause());
        }
    }

    @Test
    void testSize() {
        Ring<Integer> r = new Ring<>(3);
        assertEquals(0, r.size());

        r.add(0);
        assertEquals(1, r.size());

        r.add(1);
        assertEquals(2, r.size());

        r.add(2);
        assertEquals(3, r.size());

        r.add(3);
        assertEquals(3, r.size());
    }

    @Test
    void testIsEmpty() {
        Ring<Integer> r = new Ring<>(3);
        assertEquals(true, r.isEmpty());

        r.add(0);
        assertEquals(false, r.isEmpty());

        r.add(1);
        assertEquals(false, r.isEmpty());

        r.pop();
        r.pop();
        assertEquals(true, r.isEmpty());
    }

    @Test
    void testGet() {
        Ring<Integer> r = new Ring<>(3);
        r.add(14);
        r.add(15);
        r.add(16);
        assertEquals(14, r.get(0).intValue());
        assertEquals(15, r.get(1).intValue());
        assertEquals(16, r.get(2).intValue());

        r.add(17);
        assertEquals(15, r.get(0).intValue());
        assertEquals(16, r.get(1).intValue());
        assertEquals(17, r.get(2).intValue());

        r.add(18);
        assertEquals(16, r.get(0).intValue());
        assertEquals(17, r.get(1).intValue());
        assertEquals(18, r.get(2).intValue());

        assertEquals(16, r.shift().intValue());
        assertEquals(17, r.get(0).intValue());
        assertEquals(18, r.get(1).intValue());
        assertEquals(16, r.get(2).intValue());

        ArrayIndexOutOfBoundsException e = assertThrows(
                ArrayIndexOutOfBoundsException.class, () -> r.get(3));
        assertTrue(e.getMessage().contains("out of range: 3"));

        e = assertThrows(ArrayIndexOutOfBoundsException.class, () -> r.get(-1));
        assertTrue(e.getMessage().contains("out of range: -1"));
    }

    @Test
    void testIterator() {
        Ring<Integer> r = new Ring<>(5);
        r.add(42);
        r.add(43);
        r.add(44);
        r.add(45);
        r.add(46);
        Iterator<Integer> it = r.iterator();
        assertTrue(it.hasNext());
        assertEquals(42, it.next().intValue());
        assertTrue(it.hasNext());
        assertEquals(43, it.next().intValue());
        assertTrue(it.hasNext());
        assertEquals(44, it.next().intValue());
        assertTrue(it.hasNext());
        assertEquals(45, it.next().intValue());
        assertTrue(it.hasNext());
        assertEquals(46, it.next().intValue());
        assertFalse(it.hasNext());
    }

    @Test
    void testSet() {
        Ring<Integer> r = new Ring<>(5);
        r.add(42);
        r.add(43);
        r.add(44);
        r.add(45);
        r.add(46);

        assertEquals(44, r.get(2).intValue());
        r.set(2, 48);
        assertEquals(48, r.get(2).intValue());

        ArrayIndexOutOfBoundsException e = assertThrows(
                ArrayIndexOutOfBoundsException.class, () -> r.set(5, 1));
        assertTrue(e.getMessage().contains("out of range: 5"));

        e = assertThrows(ArrayIndexOutOfBoundsException.class, () -> r.set(-1, 1));
        assertTrue(e.getMessage().contains("out of range: -1"));
    }

    @Test
    void testShiftAndPop() {
        Ring<Integer> r = new Ring<>(2);
        assertEquals(0, r.size());
        r.shift();
        assertEquals(0, r.size());

        r.add(12);
        r.add(13);
        assertEquals(2, r.size());
        assertEquals(12, r.shift().intValue());
        assertEquals(2, r.size());
        assertEquals(13, r.shift().intValue());
        assertEquals(2, r.size());

        assertEquals(12, r.pop().intValue());
        assertEquals(1, r.size());
        assertEquals(13, r.pop().intValue());
        assertEquals(0, r.size());
    }

    @Test
    void testClear() {
        Ring<Integer> r = new Ring<>(2);
        assertEquals(0, r.size());
        r.clear();
        assertEquals(0, r.size());

        r.add(2);
        r.add(3);
        r.add(4);
        assertEquals(2, r.size());
        r.clear();
        assertEquals(0, r.size());
    }

}
