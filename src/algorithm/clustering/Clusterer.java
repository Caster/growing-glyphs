package algorithm.clustering;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

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
     * Map of names to instances of algorithms. These instances can be used
     * throughout the program, creating new instances should never be necessary.
     */
    private static final Map<String, Class<? extends Clusterer>> ALL =
            new HashMap<>();


    /**
     * Returns a fresh instance of the clusterer by the given name, initialized
     * on the given {@link QuadTree}.
     *
     * @param name Name of clusterer to instantiate.
     * @param tree Input for clusterer, passed to its constructor.
     */
    public static Clusterer get(String name, QuadTree tree) {
        Class<? extends Clusterer> cClass = getAll().get(name);
        try {
            return cClass.getConstructor(QuadTree.class).newInstance(tree);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns a map of names to instances of algorithms. Theses instances
     * can always be used, creating new instances should never be necessary.
     */
    public static Map<String, Class<? extends Clusterer>> getAll() {
        if (ALL.isEmpty()) {
            for (Class<? extends Clusterer> c : Arrays.asList(
                    NaiveClusterer.class,
                    QuadTreeClusterer.class)) {
                try {
                    String name = c.getConstructor(QuadTree.class).newInstance(
                            new Object[] {null}).getName();
                    ALL.put(name, c);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ALL;
    }


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
     * Returns a human-readable name of this algorithm. Should be unique for all
     * algorithms in the {@link algorithm.clustering} package.
     */
    public abstract String getName();

    /**
     * Returns whether the input tree of this clusterer is the exact same
     * instance as that of the given clusterer.
     *
     * @param that Clusterer to compare to.
     */
    public boolean hasSameTreeAs(Clusterer that) {
        return (this.tree == that.tree);
    }

    /**
     * Forget about any clustering obtained so far.
     */
    public void reset() {
        result = null;
    }

}
