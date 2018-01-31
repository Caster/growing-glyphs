package algorithm.clustering;

import datastructure.Glyph;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.growfunction.GrowFunction;

/**
 * A clusterer is an algorithm that is able to find the agglomerative clustering
 * implied by a given set of {@link Glyph glyphs}. The glyphs can be given in
 * any form deemed appropriate by the algorithm, but currently it is assumed
 * that all glyphs are stored in a {@link QuadTree}.
 */
public abstract class Clusterer {

    /**
     * Tree with {@link Glyph glyphs} that need clustering.
     */
    protected QuadTree tree;
    /**
     * Resulting clustering.
     */
    protected HierarchicalClustering result;


    /**
     * Initialize algorithm for clustering growing glyphs on the given QuadTree.
     *
     * @param tree Tree with glyphs to be clustered.
     */
    public Clusterer(QuadTree tree) {
        this.tree = tree;
        this.result = null;
    }


    /**
     * Run clustering algorithm on the QuadTree provided at construction time.
     *
     * @param g GrowFunction to use for deciding when glyphs touch.
     * @param includeOutOfCell Whether events caused by a glyph growing out of
     *            a cell should be included in the resulting clustering.
     * @param step Whether processing should be paused after every event.
     * @return A reference to the clustering instance, for chaining.
     */
    public abstract Clusterer cluster(GrowFunction g,
            boolean includeOutOfCell, boolean step);

    /**
     * Returns the latest result of executing the clustering algorithm. Initially
     * {@code null}.
     *
     * @see #cluster()
     */
    public HierarchicalClustering getClustering() {
        return result;
    }

    /**
     * Forget about any clustering obtained so far.
     */
    public void reset() {
        result = null;
    }

}
