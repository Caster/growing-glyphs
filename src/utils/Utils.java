package utils;

import java.awt.geom.Rectangle2D;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Locale.Category;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A collection of static utility functions.
 */
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
     * the given rectangle. This will in particular return -1 when the given
     * point is contained in the rectangle.
     */
    public static double chebyshev(Rectangle2D rect, double px, double py) {
        if (rect.contains(px, py)) {
            return -1;
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
     * Returns the Euclidean distance between two points {@code p} and {@code q}.
     */
    public static double euclidean(double px, double py, double qx, double qy) {
        double dx = qx - px;
        double dy = qy - py;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns the minimum Euclidean distance between a point and any point in
     * the given rectangle. This will in particular return -1 when the given
     * point is contained in the rectangle.
     */
    public static double euclidean(Rectangle2D rect, double px, double py) {
        if (rect.contains(px, py)) {
            return -1;
        }
        // determine the distance between the point and the point projected
        // onto the rectangle, or clamped into it, so to say
        return euclidean(px, py, clamp(px, rect.getMinX(), rect.getMaxX()),
                clamp(py, rect.getMinY(), rect.getMaxY()));
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
     *
     * @see {@link #openIntervalsOverlap(double[], double[])
     */
    public static boolean intervalsOverlap(double[] a, double[] b) {
        return (a[1] >= b[0] && a[0] <= b[1]);
    }

    /**
     * Join a bunch of strings, ignoring empty strings, with a custom glue.
     *
     * @param glue String to insert between non-empty strings.
     * @param strings Strings to join.
     */
    public static String join(String glue, String... strings) {
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (sb.length() > 0 && !string.isEmpty()) {
                sb.append(glue);
            }
            sb.append(string);
        }
        return sb.toString();
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

    /**
     * Given two intervals (min, max), return whether they overlap. This method
     * uses at most two comparisons and no branching.
     *
     * @see {@link #intervalsOverlap(double[], double[])
     */
    public static boolean openIntervalsOverlap(double[] a, double[] b) {
        return (a[1] > b[0] && a[0] < b[1]);
    }

    /**
     * Swap two objects from two lists.
     *
     * @param listA First list.
     * @param indexA Index of item in first list to swap with second.
     * @param listB Second list.
     * @param indexB Index of item in second list to swap with first.
     */
    public static <T> void swap(List<T> listA, int indexA, List<T> listB, int indexB) {
        T tmp = listA.get(indexA);
        listA.set(indexA, listB.get(indexB));
        listB.set(indexB, tmp);
    }


    /**
     * Static utility functions related to double precision arithmetic.
     */
    public static class Double {

        /**
         * Returns whether two double values are equal, up to a difference
         * of {@link Utils#EPS}. This accounts for inaccuracies.
         */
        public static boolean eq(double a, double b) {
            return (Math.abs(a - b) <= Utils.EPS);
        }

        /**
         * Returns whether two double values are equal, meaning their
         * difference is greater than {@link Utils#EPS}.
         */
        public static boolean neq(double a, double b) {
            return !eq(a, b);
        }

    }


    /**
     * Static utility functions related to locales.
     */
    public static class Locales {

        /**
         * The top of this stack maintains the current locale at all times. The
         * stack is initialized to have the current default locale on it, and
         * that value is never popped from the stack.
         */
        private static final Stack<Locale> STACK = new Stack<>();
        static {
            STACK.push(Locale.getDefault(Category.FORMAT));
        }


        /**
         * Returns the last set locale, which is the top of the stack.
         */
        public static Locale get() {
            return STACK.peek();
        }

        /**
         * Sets the locale to the second-to-last value, popping the current
         * locale from the stack and returning it. The new value can be
         * retrieved using {@link #get()}, for example.
         */
        public static Locale pop() {
            if (STACK.size() == 1) {
                return get();
            }
            Locale prev = STACK.pop();
            set(STACK.peek());
            return prev;
        }

        /**
         * Change the {@link Locale.Category#FORMAT format} category locale to
         * the given locale, and store it on the internal stack.
         *
         * @param locale Locale to change format locale into.
         */
        public static void push(Locale locale) {
            STACK.push(set(locale));
        }


        private static Locale set(Locale locale) {
            Locale.setDefault(Category.FORMAT, locale);
            return locale;
        }

    }


    /**
     * Static utility functions related to statistics.
     */
    public static class Stats {

        private static final Pattern TAG_REGEX = Pattern.compile("^\\[[a-z]+\\]\\s+");


        /**
         * Map of stat names to objects recording full stat information.
         */
        private static Map<String, Stat> stats = new HashMap<>();


        public static void count(String name) {
            record("[count] " + name, 1);
        }

        public static Stat get(String name) {
            if (!stats.containsKey(name)) {
                stats.put(name, new Stat(0));
            }
            return stats.get(name);
        }

        public static void log(String name, Logger logger) {
            if (!stats.containsKey(name)) {
                return;
            }
            stats.get(name).log(logger, name);
        }

        public static void logAll(Logger logger) {
            logger.log(Level.FINE, "");
            logger.log(Level.FINE, "STATS");
            int padTo = stats.keySet().stream()
                    .map((n) -> TAG_REGEX.matcher(n).replaceAll(""))
                    .max(Comparator.comparingInt(String::length)).get().length();
            String f = "%1$-" + padTo + "s";
            stats.entrySet().stream()
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach((e) -> {
                    if (TAG_REGEX.matcher(e.getKey()).find()) {
                        e.getValue().logCount(logger, String.format(f,
                                TAG_REGEX.matcher(e.getKey()).replaceAll("")));
                    } else {
                        e.getValue().log(logger, String.format(f, e.getKey()));
                    }
                });
        }

        public static void record(String name, double value) {
            if (stats.containsKey(name)) {
                stats.get(name).record(value);
            } else {
                Stat stat = new Stat(value);
                stats.put(name, stat);
            }
        }

        public static void remove(String name) {
            stats.remove(name);
        }

        public static void reset() {
            stats.clear();
        }

    }


    /**
     * Static utility functions related to wall clock timing.
     */
    public static class Timers {

        public static enum Units {
            NANOSECONDS (1),
            MICROSECONDS(1000),
            MILLISECONDS(1000000),
            SECONDS     (1000000000);

            /**
             * The factor to divide nanoseconds by to get to this unit of time.
             */
            private final long factor;

            private Units(long factor) {
                this.factor = factor;
            }
        }


        /**
         * Map of timer names to objects recording full timer information.
         */
        private static Map<String, Timer> timers = new HashMap<>();


        /**
         * Returns how much time has passed between all start and stop events on
         * the given timer. When the timer is currently running, that time is
         * <em>not</em> included in this value.
         *
         * Use {@link #in(long, Units)} to convert the value returned by this
         * function to a specific time unit.
         *
         * @param name Name of the timer.
         * @see #in(long, Units)
         */
        public static long elapsed(String name) {
            if (!timers.containsKey(name)) {
                return -1;
            }
            return timers.get(name).getElapsedTotal();
        }

        /**
         * Returns how much time has passed since the given timer was last
         * started.
         *
         * @param name Name of the timer.
         * @see #in(long, Units)
         */
        public static long elapsing(String name) {
            if (!timers.containsKey(name)) {
                return -1;
            }
            return timers.get(name).getElapsed();
        }

        /**
         * Returns the given timespan in a given unit.
         *
         * @param timeSpan Timespan in nanoseconds.
         * @param units Unit to transform into.
         */
        public static double in(long timeSpan, Units units) {
            return (timeSpan / ((double) units.factor));
        }

        /**
         * Log the time that elapsed to the given logger. This will
         * {@link Timer#stop() stop} the timer and log its {@link
         * Timer#getElapsedTotal() total elapsed time}.
         *
         * This will log at level {@link Level#FINE}.
         *
         * @param name Name of the timer.
         * @param logger Logger to log to.
         * @see Utils.Timers#start(String)
         */
        public static void log(String name, Logger logger) {
            log(name, logger, Level.FINE);
        }

        /**
         * Log the time that elapsed to the given logger. This will
         * {@link Timer#stop() stop} the timer and log its {@link
         * Timer#getElapsedTotal() total elapsed time}.
         *
         * @param name Name of the timer.
         * @param logger Logger to log to.
         * @param level Level to log at.
         * @see Utils.Timers#start(String)
         */
        public static void log(String name, Logger logger, Level level) {
            if (!timers.containsKey(name)) {
                return;
            }
            timers.get(name).log(logger, name, level);
        }

        /**
         * Log the time that elapsed on all timers recorded so far. This will
         * {@link Timer#stop() stop} the timers and log their {@Link
         * Timer#getElapsedTotal() total elapsed time}.
         *
         * @param logger Logger to log to.
         */
        public static void logAll(Logger logger) {
            logger.log(Level.FINE, "");
            logger.log(Level.FINE, "TIMERS");
            int padTo = timers.keySet().stream().max(
                    Comparator.comparingInt(String::length)).get().length();
            String f = "%1$-" + padTo + "s";
            // log entries without section
            timers.entrySet().stream()
                .filter((e) -> !e.getKey().startsWith("["))
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .forEach((e) ->
                    e.getValue().log(logger, String.format(f, e.getKey())));
            // log entries in a section
            Object[] timersInSections = timers.entrySet().stream()
                .filter((e) -> e.getKey().startsWith("["))
                .sorted((a, b) -> a.getKey().compareTo(b.getKey()))
                .toArray();
            String lastSection = "";
            for (int i = 0; i < timersInSections.length; ++i) {
                @SuppressWarnings("unchecked")
                Entry<String, Timer> e = (Entry<String, Timer>) timersInSections[i];
                int offset = e.getKey().indexOf(']') + 1;
                String section = e.getKey().substring(0, offset);
                if (!section.equals(lastSection)) {
                    lastSection = section;
                    logger.log(Level.FINE, "");
                    logger.log(Level.FINE, lastSection);
                }
                e.getValue().log(logger, String.format(f,
                        e.getKey().substring(offset + 1)));
            }
        }

        /**
         * Returns a timestamp that can be used to measure elapsed time.
         */
        public static long now() {
            return System.nanoTime();
        }

        /**
         * Start a new timer with the given name. Overwrites any existing timer
         * with the same name, so can be used to restart timers too.
         *
         * @param name Name of the timer. Used when reading off elapsed time.
         * @see Utils.Timers#log(String, Logger)
         */
        public static void start(String name) {
            if (timers.containsKey(name)) {
                timers.get(name).start();
            } else {
                timers.put(name, new Timer());
            }
        }

        /**
         * Record time passed since last start event on the given timer. This
         * time will now be included in {@code getElapsedTotal}. Stopping a
         * stopped timer has no effect.
         *
         * @param name Name of the timer to stop.
         */
        public static void stop(String name) {
            timers.get(name).stop();
        }

        public static void reset() {
            timers.clear();
        }

    }

}
