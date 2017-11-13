package algorithm;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.Glyph;
import datastructure.events.Event;
import datastructure.events.GlyphMerge;
import datastructure.growfunction.GrowFunction;

public class FirstMergeRecorder {

    /**
     * Glyph with which merges are recorded.
     */
    private Glyph from;
    /**
     * Function to determine when merges occur.
     */
    private GrowFunction g;
    /**
     * First time at which a merge event with {@link #from} is recorded so far.
     */
    private double minAt;
    /**
     * Set of glyphs that touch {@link #from} at time {@link #minAt}. In
     * practice this will almost always contain just a single glyph.
     */
    private Set<Glyph> minGlyphs;


    /**
     * Construct a recorder that will use the given {@link GrowFunction} to
     * determine when glyphs should merge.
     */
    public FirstMergeRecorder(GrowFunction g) {
        this.from = null;
        this.g = g;
        this.minAt = Double.MAX_VALUE;
        this.minGlyphs = new HashSet<>(1);
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
        for (Glyph with : minGlyphs) {
            if (with.mergingWith != null &&
                    with.mergingWith.getOther(with) == from) {
                if (l != null) {
                    l.log(Level.FINEST, "-> merge at {0} with {1}, but that "
                        + "was already recorded", new Object[] {minAt, with});
                }
                continue;
            }
            q.add(from.mergingWith = new GlyphMerge(from, with, minAt));
            if (l != null) {
                l.log(Level.FINEST, "-> merge at {0} with {1}",
                        new Object[] {minAt, with});
            }
        }
    }

    /**
     * Start recording possible merges with the given glyph, forgetting about
     * all previous state.
     *
     * @param from Glyph with which merges should be recorded starting now.
     */
    public void from(Glyph from) {
        this.from = from;
        this.minAt = Double.MAX_VALUE;
        this.minGlyphs.clear();
    }

    /**
     * Record a possible merge event.
     *
     * @param candidate Glyph that the {@code from} glyph could merge with.
     */
    public void record(Glyph candidate) {
        // silently ignore comparing a glyph to itself
        if (candidate == from) {
            return;
        }

        // see when these two would touch, record if interesting
        double at = g.intersectAt(from, candidate);
        if (at < minAt) {
            minAt = at;
            minGlyphs.clear();
            minGlyphs.add(candidate);
        } else if (at == minAt) {
            minGlyphs.add(candidate);
        }
    }

}
