package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.Map;

public class Utils {

    /**
     * Returns the Chebyshev distance between two points {@code p} and {@code q}.
     */
    public static double chebyshev(double px, double py, double qx, double qy) {
        return Math.max(Math.abs(px - qx), Math.abs(py - qy));
    }

    /**
     * Returns the minimum Chebyshev distance between a point and a rectangle.
     */
    public static double chebyshev(Rectangle2D rect, double px, double py) {
        if (rect.contains(px, py)) {
            return 0;
        }
        return minNonNeg(
                rect.getMinX() - px,
                rect.getMinY() - py,
                px - rect.getMaxX(),
                py - rect.getMaxY()
            );
    }

    /**
     * Returns the index of an object in an array, or -1 if it cannot be found.
     * Uses {@link Object#equals(Object)} to compare objects.
     */
    public static int indexOf(Object[] haystack, Object needle) {
        for (int i = 0; i < haystack.length; ++i) {
            if ((haystack[i] == null && needle == null) ||
                    (needle != null && needle.equals(haystack[i]))) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Given an array of keys and a map, return an array with all values in the
     * same order as the keys in the input array were.
     *
     * @param keys Keys to look up.
     * @param map Map to be used for mapping.
     * @param result Array to write results into. Should have appropriate length.
     */
    public static <K, V> V[] map(K[] keys, Map<K, V> map, V[] result) {
        for (int i = 0; i < keys.length; ++i) {
            result[i] = map.get(keys[i]);
        }
        return result;
    }

    /**
     * Returns the minimum non negative value amongst a number of double values.
     * When no doubles are passed, {@link Double#POSITIVE_INFINITY} is returned.
     */
    public static double minNonNeg(double... ds) {
        double min = Double.POSITIVE_INFINITY;
        for (double d : ds) {
            if (d < min && d >= 0) {
                min = d;
            }
        }
        return min;
    }

}
