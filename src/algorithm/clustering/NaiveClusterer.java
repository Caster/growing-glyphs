package algorithm.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import datastructure.Glyph;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.growfunction.GrowFunction;
import utils.Constants.B;
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
        // we create a matrix that records for each pair of glyphs when they merge
        double[][] mergeMatrix = new double[n][n];
        // keep track of how many glyphs we have left
        List<Glyph> glyphsAlive = new ArrayList<>(n);
        for (QuadTree leaf : tree.getLeaves()) {
            for (Glyph glyph : leaf.getGlyphs()) {
                map.put(glyph, new HierarchicalClustering(glyph, 0));
                glyph.alive = true;
                glyphsAlive.add(glyph);
            }
        }
        LOGGER.log(Level.FINE, "initialized {0} glyphs", n);

        // create merge events for all pairs of glyphs; glyphs earlier in the
        // overall list of glyphs track glyphs later in the list, not vice versa
        int numEvts = 0;
        for (int i = 0; i < n; ++i) {
            Glyph glyphI = glyphsAlive.get(i);
            mergeMatrix[i][i] = Double.POSITIVE_INFINITY;
            for (int j = i + 1; j < n; ++j) {
                mergeMatrix[i][j] = g.intersectAt(glyphI, glyphsAlive.get(j));
                mergeMatrix[j][i] = Double.POSITIVE_INFINITY;
                numEvts++;
            }
        }
        LOGGER.log(Level.FINE, "created {0} merge events", numEvts);

        for (int i = 0; i < n; ++i) {
            Arrays.sort(mergeMatrix[i]);
        }


        if (B.TIMERS_ENABLED.get()) {
            Timers.logAll(LOGGER);
        }
        return this;
    }

}
