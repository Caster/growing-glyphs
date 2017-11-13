package algorithm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Stream;
import java.util.stream.Collector.Characteristics;

import datastructure.Glyph;
import datastructure.events.Event;
import datastructure.events.GlyphMerge;
import datastructure.growfunction.GrowFunction;

public class FirstMergeRecorder {

    /**
     * Collector for stream operations.
     */
    private static Collector<Glyph, FirstMerge, FirstMerge> collector;


    /**
     * Glyph with which merges are recorded.
     */
    private Glyph from;
    /**
     * Function to determine when merges occur.
     */
    private GrowFunction g;
    /**
     * Container that records when the first merge occurs, and which glyph(s)
     * will merge with {@link from} at that point in time.
     */
    private FirstMerge merge;


    /**
     * Construct a recorder that will use the given {@link GrowFunction} to
     * determine when glyphs should merge.
     */
    public FirstMergeRecorder(GrowFunction g) {
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
        for (Glyph with : merge.glyphs) {
            q.add(new GlyphMerge(from, with, merge.at));
            if (l != null) {
                l.log(Level.FINEST, "-> merge at {0} with {1}",
                        new Object[] {merge.at, with});
            }
        }
    }

    public Collector<Glyph, FirstMerge, FirstMerge> collector() {
        if (collector == null) {
            collector = Collector.of(
                    () -> new FirstMerge(),
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
    public void record(Set<Glyph> glyphs) {
        record(glyphs.parallelStream());
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
        merge.combine(glyphs.parallel()
            .filter((glyph) -> glyph.alive && glyph != from)
            .collect(collector()));
    }


    /**
     * Container for collecting the first merge; when will it happen and which
     * glyphs are involved, aside from {@link FirstMergeRecorder#from}.
     */
    private class FirstMerge {

        /**
         * First time at which a merge event with {@link FirstMergeRecorder#from}
         * is recorded so far.
         */
        private double at;
        /**
         * Set of glyphs that touch {@link FirstMergeRecorder#from} at time
         * {@link #at}. In practice this will almost always contain just a
         * single glyph.
         */
        private Set<Glyph> glyphs;


        public FirstMerge() {
            this.at = Double.MAX_VALUE;
            this.glyphs = new HashSet<>(1);
        }

        public void accept(Glyph candidate) {
            double at = g.intersectAt(from, candidate);
            if (at < this.at) {
                this.at = at;
                this.glyphs.clear();
                this.glyphs.add(candidate);
            } else if (at == this.at) {
                this.glyphs.add(candidate);
            }
        }

        public FirstMerge combine(FirstMerge that) {
            if (that.at < this.at) {
                this.at = that.at;
                this.glyphs.clear();
                this.glyphs.addAll(that.glyphs);
            } else if (that.at == this.at) {
                this.glyphs.addAll(that.glyphs);
            }
            return this;
        }

        public void reset() {
            at = Double.MAX_VALUE;
            glyphs.clear();
        }

    }

}
