package algorithm;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import datastructure.Glyph;
import datastructure.QuadTree;
import datastructure.Ring;
import datastructure.events.Event;
import datastructure.events.GlyphMerge;
import datastructure.growfunction.GrowFunction;
import utils.Utils.Timers;

/**
 * This class can be used to record the first merge(s) that will occur with a
 * glyph. More than one merge may be recorded, and glyphs can be
 * {@link Glyph#record(GlyphMerge) made aware} of the merges that are happening
 * to them. This class respects {@link Glyph#MAX_MERGES_TO_RECORD}.
 */
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
     * Container that records when the first merge(s) occur(s), and which glyph(s)
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
        for (GlyphMerge merge : merge.shift()) {
            q.add(merge);
            Glyph with = merge.getOther(from);
            if (AgglomerativeClustering.TRACK) {
                with.trackedBy.add(from);
            }
            if (l != null) {
                l.log(Level.FINEST, "-> merge at {0} with {1}",
                        new Object[] {merge.getAt(), with});
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
        if (AgglomerativeClustering.ROBUST) {
            merge.getGlyphs().addAll(glyphs.parallel()
                .filter((glyph) -> glyph.alive && glyph != from)
                .collect(Collectors.toSet()));

        } else {
            merge.combine(glyphs.parallel()
                .filter((glyph) -> glyph.alive && glyph != from)
                .collect(collector()));
        }
    }

    public void recordAllPairs(QuadTree cell, Queue<Event> q, Logger logger) {
        Glyph[] glyphs = cell.getGlyphs().toArray(new Glyph[0]);
        for (int i = 0; i < glyphs.length; ++i) {
            // add events for when two glyphs in the same cell touch
            from(glyphs[i]);
            Timers.start("first merge recording");
            record(glyphs, i + 1, glyphs.length);
            Timers.stop("first merge recording");
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
        private Ring<Double> at;
        /**
         * Set of glyphs that touch {@link FirstMergeRecorder#from} at time
         * {@link #at}. In practice this will almost always contain just a
         * single glyph. Similarly to {@link #at}, this is a list that tracks
         * the set for the first, second, ... merge events.
         */
        private Ring<Set<Glyph>> glyphs;


        public FirstMerge() {
            this.at = new Ring<>(Collections.nCopies(
                    Glyph.MAX_MERGES_TO_RECORD, Double.MAX_VALUE));
            this.glyphs = new Ring<>(Collections.nCopies(
                    Glyph.MAX_MERGES_TO_RECORD, new HashSet<>(1)));
        }

        public void accept(Glyph candidate) {
            double at = g.intersectAt(from, candidate);
            for (int i = 0; i < Glyph.MAX_MERGES_TO_RECORD; ++i) {
                if (at < this.at.get(i)) {
                    this.at.set(i, at);
                    this.glyphs.get(i).clear();
                    this.glyphs.get(i).add(candidate);
                } else if (at == this.at.get(i)) {
                    this.glyphs.get(i).add(candidate);
                }
            }
        }

        public FirstMerge combine(FirstMerge that) {
            for (int i = 0; i < Glyph.MAX_MERGES_TO_RECORD; ++i) {
                if (that.at.get(i) < this.at.get(i)) {
                    this.at.set(i, that.at.get(i));
                    this.glyphs.get(i).clear();
                    this.glyphs.get(i).addAll(that.glyphs.get(i));
                } else if (that.at.get(i) == this.at.get(i)) {
                    this.glyphs.get(i).addAll(that.glyphs.get(i));
                }
            }
            return this;
        }

        public Set<Glyph> getGlyphs() {
            return glyphs.get(0);
        }

        public void reset() {
            for (int i = 0; i < Glyph.MAX_MERGES_TO_RECORD; ++i) {
                at.set(i, Double.MAX_VALUE);
                glyphs.get(i).clear();
            }
        }

        public GlyphMerge[] shift() {
            double at = this.at.shift();
            Set<Glyph> glyphs = this.glyphs.shift();
            GlyphMerge[] result = new GlyphMerge[glyphs.size()];
            int i = 0;
            for (Glyph with : glyphs) {
                result[i++] = new GlyphMerge(from, with,
                    (AgglomerativeClustering.ROBUST ? g.intersectAt(from, with) : at));
            }
            return result;
        }

    }

}
