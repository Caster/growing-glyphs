package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.Map;

public class Utils {

    /**
     * Epsilon, useful for double comparison.
     */
    public static final double EPS = 1e-5;


    /**
     * Returns the Chebyshev distance between two points {@code p} and {@code q}.
     */
    public static double chebyshev(double px, double py, double qx, double qy) {
        return Math.max(Math.abs(px - qx), Math.abs(py - qy));
    }

    /**
     * Returns the minimum Chebyshev distance between a point and any point in
     * the given rectangle. This will in particular return 0 when the given
     * point is contained in the rectangle.
     */
    public static double chebyshev(Rectangle2D rect, double px, double py) {
        if (rect.contains(px, py)) {
            return 0;
        }
        // determine the distance between the point and the point projected
        // onto the rectangle, or clamped into it, so to say
        return chebyshev(px, py, clamp(px, rect.getMinX(), rect.getMaxX()),
                clamp(py, rect.getMinY(), rect.getMaxY()));
    }

    /**
     * Clamp a given value to within the given range.
     *
     * @param value Value to clamp.
     * @param min Minimum value to return.
     * @param max Maximum value to return.
     * @return The value closest to {@code value} that is within the closed
     *         interval {@code [min, max]}.
     */
    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
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
     * Given two intervals [min, max], return whether they overlap. This method
     * uses at most two comparisons and no branching.
     */
    public static boolean intervalsOverlap(double[] a, double[] b) {
        return (a[1] >= b[0] && a[0] <= b[1]);
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
     * Given a zero-width or zero-height rectangle, return if that line segment
     * is on the border of the given rectangle.
     *
     * @param side Line segment to consider.
     * @param rect Rectangle to consider.
     */
    public static boolean onBorderOf(Rectangle2D side, Rectangle2D rect) {
        return (
            (side.getWidth() == 0 &&
                (side.getX() == rect.getMinX() || side.getX() == rect.getMaxX()) &&
                side.getMinY() >= rect.getMinY() && side.getMaxY() <= rect.getMaxY()) ||
            (side.getHeight() == 0 &&
                (side.getY() == rect.getMinY() || side.getY() == rect.getMaxY()) &&
                side.getMinX() >= rect.getMinX() && side.getMaxX() <= rect.getMaxX())
        );
    }

}
