package algorithm;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import datastructure.Glyph;
import datastructure.QuadTree;
import datastructure.events.Event;
import datastructure.events.GlyphMerge;
import datastructure.growfunction.GrowFunction;
import utils.Constants.B;
import utils.Constants.I;
import utils.Utils;
import utils.Utils.Timers;

/**
 * This class can be used to record the first merge(s) that will occur with a
 * glyph. More than one merge may be recorded, and glyphs can be
 * {@link Glyph#record(GlyphMerge) made aware} of the merges that are happening
 * to them. This class respects {@link Glyph#MAX_MERGES_TO_RECORD}.
 */
public class FirstMergeRecorder {

    /**
     * The logger of this class, that is instantiated only when logging is
     * enabled and {@link Level#FINE} messages are loggable. This is done because
     * there are some heavy-to-construct logging parameters, which are guarded by
     * cheap {@code LOGGER != null} checks. This is more efficient than checking
     * repeatedly whether the message is loggable.
     */
    private static final Logger LOGGER;
    static {
        Logger l;
        if (B.LOGGING_ENABLED.get() && (l = Logger.getLogger(
                FirstMergeRecorder.class.getName())).isLoggable(Level.FINER)) {
            LOGGER = l;
        } else {
            LOGGER = null;
        }
    }


    /**
     * Return a reference to the singleton instance of this class, creating an
     * instance when not done before. The instance will use the given
     * {@link GrowFunction}, and when an instance was created before then the
     * grow function being used by that instance is changed before a reference
     * to it is returned. <b>Note</b> that that will change the grow function
     * being used for all users of the singleton instance!
     *
     * The reason that this class has a singleton instance is that it internally
     * uses the Stream API with instances of a private inner class. Other
     * instances of {@link FirstMergeRecorder} may, via the Stream API, use
     * instances of the private inner class that have a different parent. This
     * goes, as one might expect, horribly wrong. Using a singleton instance is
     * a quick and easy way around this problem.
     *
     * @param g Grow function to use when determining which merges occur first.
     * @return A reference to the singleton instance of this class.
     */
    public static FirstMergeRecorder getInstance(GrowFunction g) {
        INSTANCE.g = g;
        return INSTANCE;
    }


    /**
     * Collector for stream operations.
     */
    private static Collector<Glyph, FirstMerge, FirstMerge> collector;


    /**
     * Singleton instance.
     */
    private static final FirstMergeRecorder INSTANCE = new FirstMergeRecorder(null);
    /**
     * Used in {@link FirstMerge#combine(FirstMerge)}.
     */
    private static final ThreadLocal<FirstMerge> COMBINE_RESULT =
            ThreadLocal.withInitial(() -> INSTANCE.new FirstMerge());
    private static final Deque<FirstMerge> REUSABLE_RECORDS = new ArrayDeque<>();
    private static FirstMerge firstReusedRecord = null;


    /**
     * Returns an instance of {@link FirstMerge} that is {@link FirstMerge#reset()}
     * and ready to accept and combine. This method may cache instances and reuse
     * them. {@link #REUSABLE_RECORDS} is used to this end.
     */
    private synchronized static FirstMerge newInstance() {
        // attempt to use cache
        if (REUSABLE_RECORDS.size() > 0 && (firstReusedRecord == null ||
                REUSABLE_RECORDS.getLast() != firstReusedRecord)) {
            FirstMerge record = REUSABLE_RECORDS.pollLast();
            REUSABLE_RECORDS.addFirst(record);
            if (firstReusedRecord == null) {
                firstReusedRecord = record;
            }
            return record;
        }
        // we are forced to create a new instance, do so
        FirstMerge record = INSTANCE.new FirstMerge();
        REUSABLE_RECORDS.addFirst(record);
        if (firstReusedRecord == null) {
            firstReusedRecord = record;
        }
        return record;
    }


    /**
     * Glyph with which merges are recorded.
     */
    private Glyph from;
    /**
     * Function to determine when merges occur.
     */
    private GrowFunction g;
    /**
     * Container that records when the first merge(s) occur(s), and which glyph(s)
     * will merge with {@link from} at that point in time.
     */
    private FirstMerge merge;


    /**
     * Construct a recorder that will use the given {@link GrowFunction} to
     * determine when glyphs should merge.
     */
    private FirstMergeRecorder(GrowFunction g) {
        this.from = null;
        this.g = g;
        this.merge = new FirstMerge();
    }


    /**
     * @see #addEventsTo(Queue, Logger)
     */
    public void addEventsTo(Queue<Event> q) {
        addEventsTo(q, null);
    }

    /**
     * Given the glyph {@link #from} which recording started, and all possible
     * merges that have been {@link #record(Glyph) recorded} after that, one or
     * more merge events will occur first; those are added to the given queue by
     * this method. State is maintained, although it is recommended that this is
     * not used, only {@link #from} could be used to reset state and start over.
     *
     * @param q Queue to add merge events to.
     * @param l Logger to log events to, can be {@code null}.
     */
    public void addEventsTo(Queue<Event> q, Logger l) {
        GlyphMerge[] merges;
        if (B.ROBUST.get()) {
            for (Glyph glyph : merge.getGlyphs()) {
                q.add(new GlyphMerge(from, glyph, g));
            }
            merge.getGlyphs().clear();
        } else {
            while ((merges = merge.pop()) != null) {
                for (GlyphMerge merge : merges) {
                    if (LOGGER != null) {
                        LOGGER.log(Level.FINE, "recorded {0}", merge);
                    }
                    from.record(merge);
                }
            }
            from.popMergeInto(q, l);
        }
        firstReusedRecord = null; // we can reuse all records again
    }

    public Collector<Glyph, FirstMerge, FirstMerge> collector() {
        if (collector == null) {
            collector = Collector.of(
                    FirstMergeRecorder::newInstance,
                    (m, g) -> m.accept(g),
                    (a, b) -> a.combine(b),
                    Characteristics.UNORDERED);
        }
        return collector;
    }

    /**
     * Start recording possible merges with the given glyph, forgetting about
     * all previous state.
     *
     * @param from Glyph with which merges should be recorded starting now.
     */
    public void from(Glyph from) {
        if (LOGGER != null) {
            LOGGER.log(Level.FINE, "recording merges from {0}", from);
        }
        this.from = from;
        this.merge.reset();
    }

    /**
     * {@link #record(Glyph) Record} all glyphs in the given array between the
     * given indices (including {@code from}, excluding {@code upto}). Only when
     * they are {@link Glyph#alive} and not {@link #from}, they are recorded.
     *
     * This method may use parallelization to speed up recording.
     *
     * @param glyphs Array of glyphs to look in.
     * @param from First index of glyph to record.
     * @param upto Index up to but excluding which glyphs will be recorded.
     */
    public void record(Glyph[] glyphs, int from, int upto) {
        record(Arrays.stream(glyphs, from, upto));
    }

    /**
     * {@link #record(Glyph) Record} all glyphs in the given set, as long as
     * they are {@link Glyph#alive} and not {@link #from}.
     *
     * This method may use parallelization to speed up recording.
     *
     * @param glyphs Set of glyphs to record.
     */
    public void record(List<Glyph> glyphs) {
        if (glyphs != null) {
            record(glyphs.stream());
        }
    }

    /**
     * {@link #record(Glyph) Record} all glyphs in the given stream, as long as
     * they are {@link Glyph#alive} and not {@link #from}.
     *
     * This method may use parallelization to speed up recording.
     *
     * @param glyphs Stream of glyphs to record.
     */
    public void record(Stream<Glyph> glyphs) {
        if (B.ROBUST.get()) {
            merge.getGlyphs().addAll(glyphs.parallel()
                .filter((glyph) -> glyph.isAlive() && glyph != from)
                .collect(Collectors.toSet()));
        } else {
            merge.combine(glyphs
                .filter((glyph) -> glyph.isAlive() && glyph != from)
                .collect(collector()));
        }
    }

    public void recordAllPairs(QuadTree cell, Queue<Event> q, Logger logger) {
        Glyph[] glyphs = cell.getGlyphs().toArray(new Glyph[0]);
        for (int i = 0; i < glyphs.length; ++i) {
            // add events for when two glyphs in the same cell touch
            from(glyphs[i]);
            Timers.start("first merge recording 5");
            record(glyphs, i + 1, glyphs.length);
            Timers.stop("first merge recording 5");
            addEventsTo(q);
        }
    }


    /**
     * Container for collecting the first merge; when will it happen and which
     * glyphs are involved, aside from {@link FirstMergeRecorder#from}.
     */
    private class FirstMerge {

        /**
         * First times at which merge events with {@link FirstMergeRecorder#from}
         * are recorded so far. This will contain the timestamp of the first
         * event, then the timestamp of the second, et cetera.
         */
        private List<Double> at;
        /**
         * Set of glyphs that touch {@link FirstMergeRecorder#from} at time
         * {@link #at}. In practice this will almost always contain just a
         * single glyph. Similarly to {@link #at}, this is a list that tracks
         * the set for the first, second, ... merge events.
         */
        private List<List<Glyph>> glyphs;
        /**
         * Number of merges that have been recorded.
         */
        private int size;


        public FirstMerge() {
            this.at = new ArrayList<>(Collections.nCopies(
                    I.MAX_MERGES_TO_RECORD.get(), Double.POSITIVE_INFINITY));
            this.glyphs = new ArrayList<>(I.MAX_MERGES_TO_RECORD.get());
            for (int i = 0; i < I.MAX_MERGES_TO_RECORD.get(); ++i) {
                this.glyphs.add(new ArrayList<>(1));
            }
            this.size = 0;
            if (LOGGER != null) {
                LOGGER.log(Level.FINER, "constructed an empty FirstMerge #{0}",
                        hashCode());
            }
        }

        public void accept(Glyph candidate) {
            if (LOGGER != null) {
                LOGGER.log(Level.FINER, "accepting {0} into #{1}",
                        new Object[] {candidate, hashCode()});
            }
            double at = g.intersectAt(from, candidate);
            for (int i = 0; i < I.MAX_MERGES_TO_RECORD.get(); ++i) {
                if (at < this.at.get(i)) {
                    if (this.at.get(i).isInfinite()) {
                        size++;
                    }
                    // make room to shift, if needed
                    if (this.at.size() == I.MAX_MERGES_TO_RECORD.get()) {
                        this.at.remove(this.at.size() - 1);
                        this.glyphs.add(i, this.glyphs.remove(this.glyphs.size() - 1));
                    }
                    this.at.add(i, at);
                    this.glyphs.get(i).clear();
                    this.glyphs.get(i).add(candidate);
                    break;
                } else if (at == this.at.get(i)) {
                    this.glyphs.get(i).add(candidate);
                    break;
                }
                // if at > this.at.get(i), try next i...
            }
            if (LOGGER != null) {
                LOGGER.log(Level.FINER, "#{0} now has glyphs {1} at {2}",
                    new Object[] {
                        hashCode(),
                        "[" + glyphs.stream().map((glyphSet) ->
                            glyphSet.stream().map(Glyph::toString).collect(
                                Collectors.joining(", "))
                        ).collect(Collectors.joining("], [")) + "]",
                        "[" + this.at.stream().map(Object::toString).collect(
                                Collectors.joining(", ")) + "]"
                    });
            }
        }

        public FirstMerge combine(FirstMerge that) {
            if (LOGGER != null) {
                LOGGER.log(Level.FINER, "combining #{0} and #{1};\n#{0} has glyphs {2} at {3};\n#{1} has glyphs {4} at {5}",
                        new Object[] {hashCode(), that.hashCode(),
                            "[" + this.glyphs.stream().map((glyphSet) ->
                                glyphSet.stream().map(Glyph::toString).collect(
                                    Collectors.joining(", "))
                            ).collect(Collectors.joining("], [")) + "]",
                            "[" + this.at.stream().map(Object::toString).collect(
                                    Collectors.joining(", ")) + "]",
                            "[" + that.glyphs.stream().map((glyphSet) ->
                                glyphSet.stream().map(Glyph::toString).collect(
                                    Collectors.joining(", "))
                            ).collect(Collectors.joining("], [")) + "]",
                            "[" + that.at.stream().map(Object::toString).collect(
                                    Collectors.joining(", ")) + "]"
                        });
            }
            int thisInd = 0;
            int thatInd = 0;
            FirstMerge result = COMBINE_RESULT.get();
            for (int i = 0; i < I.MAX_MERGES_TO_RECORD.get(); ++i) {
                // need to be careful here that we don't have both lists
                // reference the same sublist; won't go well with resetting
                if (that.at.get(thatInd) < this.at.get(thisInd)) {
                    Utils.swap(result.at, i, that.at, thatInd);
                    Utils.swap(result.glyphs, i, that.glyphs, thatInd);
                    thatInd++;
                } else if (that.at.get(thatInd) == this.at.get(thisInd)) {
                    Utils.swap(result.at, i, that.at, thatInd);
                    Utils.swap(result.glyphs, i, that.glyphs, thatInd);
                    result.glyphs.get(i).addAll(this.glyphs.get(thisInd));
                    thisInd++;
                    thatInd++;
                } else { // that.at.get(thatInd > this.at.get(thisInd)
                    Utils.swap(result.at, i, this.at, thisInd);
                    Utils.swap(result.glyphs, i, this.glyphs, thisInd);
                    thisInd++;
                }
                result.size++;
            }
            if (LOGGER != null) {
                LOGGER.log(Level.FINER, "result #{0} of merging #{3} and #{4} has glyphs {1} at {2} (storing in #{3} now)",
                    new Object[] {
                        result.hashCode(),
                        "[" + result.glyphs.stream().map((glyphSet) ->
                            glyphSet.stream().map(Glyph::toString).collect(
                                Collectors.joining(", "))
                        ).collect(Collectors.joining("], [")) + "]",
                        "[" + result.at.stream().map(Object::toString).collect(
                                Collectors.joining(", ")) + "]",
                        this.hashCode(), that.hashCode()
                    });
            }
            // swap properties with result
            List<Double> tmpAt = this.at;
            this.at = result.at;
            result.at = tmpAt;
            List<List<Glyph>> tmpGlyphs = this.glyphs;
            this.glyphs = result.glyphs;
            result.glyphs = tmpGlyphs;
            // as we reset result and primitive is copied anyway, no need to swap
            this.size = result.size;
            // reset result, ready for reuse
            result.reset();
            return this;
        }

        public List<Glyph> getGlyphs() {
            return glyphs.get(0);
        }

        public void reset() {
            resizeIfNeeded();
            for (int i = 0; i < I.MAX_MERGES_TO_RECORD.get(); ++i) {
                at.set(i, Double.POSITIVE_INFINITY);
                glyphs.get(i).clear();
            }
            size = 0;
        }

        public void resizeIfNeeded() {
            int ws = I.MAX_MERGES_TO_RECORD.get();
            int cs = at.size();
            if (cs < ws) {
                for (int i = cs; i < ws; ++i) {
                    at.add(Double.POSITIVE_INFINITY);
                    glyphs.add(new ArrayList<>(1));
                }
            } else if (cs > ws) {
                for (int i = cs - 1; i >= ws; --i) {
                    at.remove(i);
                    glyphs.remove(i);
                }
            }
        }

        public GlyphMerge[] pop() {
            if (size == 0) {
                return null;
            }

            double at = this.at.get(0);
            Collections.rotate(this.at, -1);
            List<Glyph> glyphs = this.glyphs.get(0);
            Collections.rotate(this.glyphs, -1);
            size--;

            GlyphMerge[] result = new GlyphMerge[glyphs.size()];
            int i = 0;
            for (Glyph with : glyphs) {
                result[i++] = new GlyphMerge(from, with,
                    (B.ROBUST.get() ? g.intersectAt(from, with) : at));
            }
            return result;
        }

    }

}
