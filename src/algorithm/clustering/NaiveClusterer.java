package algorithm.clustering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import datastructure.Glyph;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.events.GlyphMerge;
import datastructure.growfunction.GrowFunction;
import utils.Constants.B;
import utils.Utils;
import utils.Utils.Timers;

/**
 * The naive clusterer implements clustering in O(n^2 log n) time by calculating
 * merge events between all pairs of glyphs.
 */
public class NaiveClusterer extends Clusterer {

    private static final Logger LOGGER = (B.LOGGING_ENABLED.get() ?
            Logger.getLogger(Clusterer.class.getName()) : null);


    /**
     * {@inheritDoc}
     */
    public NaiveClusterer(QuadTree tree) {
        super(tree);
    }


    @Override
    public Clusterer cluster(GrowFunction g, boolean includeOutOfCell,
            boolean step) {
        if (LOGGER != null) {
            LOGGER.log(Level.FINE, "using the {0} grow function", g.getName());
        }
        if (B.TIMERS_ENABLED.get()) {
            Timers.start("clustering");
        }

        // find number of glyphs
        int n = tree.getLeaves().parallelStream().collect(
                Collectors.summingInt((cell) -> cell.getGlyphs().size()));
        // create a result for each glyph, and a map to find them
        Map<Glyph, HierarchicalClustering> map = new HashMap<>(2 * n);
        // we create a queue that records for each pair of glyphs when they merge
        Queue<GlyphMerge> q = new PriorityQueue<>();
        // keep track of how many glyphs we have left
        List<Glyph> glyphsAliveList = new ArrayList<>(n);
        for (QuadTree leaf : tree.getLeaves()) {
            for (Glyph glyph : leaf.getGlyphs()) {
                map.put(glyph, new HierarchicalClustering(glyph, 0));
                if (!glyph.isAlive()) {
                    if (LOGGER != null) {
                        LOGGER.log(Level.SEVERE, "unexpected dead glyph in input");
                    }
                    return null;
                }
                glyphsAliveList.add(glyph);
            }
        }
        if (LOGGER != null) {
            LOGGER.log(Level.FINE, "initialized {0} glyphs", n);
        }

        // create merge events for all pairs of glyphs; glyphs earlier in the
        // overall list of glyphs track glyphs later in the list, not vice versa
        for (int i = 0; i < n; ++i) {
            Glyph glyphI = glyphsAliveList.get(i);
            for (int j = i + 1; j < n; ++j) {
                q.add(new GlyphMerge(glyphI, glyphsAliveList.get(j), g));
            }
        }

        // some (temporary) data structures that are reused in the algorithm
        HierarchicalClustering[] from = new HierarchicalClustering[2];
        List<HierarchicalClustering> fromList = new ArrayList<>();
        Set<Glyph> glyphsAlive = new HashSet<>(glyphsAliveList);
        // process merge events until only a single glyph remains
        while (glyphsAlive.size() > 1) {
            // find first merge event involving two alive glyphs
            GlyphMerge next;
            do {
                next = q.poll();
            } while (!next.getGlyphs()[0].isAlive() ||
                     !next.getGlyphs()[1].isAlive());

            // process it: perform the merge (may have happened before previous ones)
            Glyph merged = new Glyph(next.getGlyphs());
            double at = next.getAt();
            Utils.map(next.getGlyphs(), map, from);
            fromList.clear();
            fromList.add(from[0]);
            fromList.add(from[1]);
            for (int i = 0; i < 2; ++i) {
                if (from[i].getAt() > next.getAt()) {
                    fromList.addAll(from[i].getCreatedFrom());
                    fromList.remove(from[i]);
                    at = Math.max(at, from[i].getAt());
                }
            }
            HierarchicalClustering mergedHC = new HierarchicalClustering(merged, at);
            for (HierarchicalClustering fromHC : fromList) {
                mergedHC.alsoCreatedFrom(fromHC);
            }
            map.put(merged, mergedHC);
            merged.participate();

            // the last merge is the root of the resulting clustering
            result = mergedHC;

            // update glyphs alive
            for (Glyph glyph : next.getGlyphs()) {
                glyph.perish();
                glyphsAlive.remove(glyph);
            }
            glyphsAlive.add(merged);

            // insert new events into the queue
            for (Glyph glyph : glyphsAlive) {
                if (glyph == merged)  continue;

                GlyphMerge event = new GlyphMerge(merged, glyph, g);
                q.add(event);
            }
        }


        if (B.TIMERS_ENABLED.get()) {
            Timers.stop("clustering");
            Timers.logAll(LOGGER);
        }
        return this;
    }

    @Override
    public String getName() {
        return "Naive Clusterer";
    }

}
