package ui;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import algorithm.clustering.Clusterer;
import algorithm.clustering.QuadTreeClusterer;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.growfunction.GrowFunction;
import gui.GrowingGlyphs;
import gui.Settings.Setting;
import io.CsvIO;
import io.PointIO;
import utils.Constants.B;
import utils.Constants.D;
import utils.Constants.S;
import utils.Utils.Stats;
import utils.Utils.Timers;

/**
 * Thin wrapper around {@link QuadTreeClusterer} and the {@link QuadTree}
 * it works on. This class contains all functionality needed to execute the
 * Growing Glyphs program in background mode, all other functionality is
 * implemented in {@link GrowingGlyphs}.
 */
public class GrowingGlyphsDaemon {

    private static final Logger LOGGER = (B.LOGGING_ENABLED.get() ?
            Logger.getLogger(Clusterer.class.getName()) : null);


    private Clusterer clusterer;
    private boolean clustered;
    private GrowFunction g;
    private String dataSet;
    private File lastOpened;
    private int n;
    private QuadTree tree;

    public GrowingGlyphsDaemon(int w, int h, GrowFunction g) {
        this.g = g;
        this.tree = new QuadTree(-w / 2, -h / 2, w, h, g);
        this.clusterer = Clusterer.get(S.CLUSTERER.get(), this.tree);
        this.clustered = false;
        this.dataSet = null;
        this.lastOpened = null;
        this.n = 0;
    }

    public GrowFunction getGrowFunction() {
        return g;
    }

    public QuadTree getTree() {
        return tree;
    }

    /**
     * Execute clustering algorithm disabling all debugging options. This is a
     * convenience method providing defaults for
     * {@link #cluster(boolean, boolean, boolean)}.
     *
     * @throws InterruptedException When the thread was interrupted while clustering.
     */
    public void cluster() throws InterruptedException {
        cluster(false, false);
    }

    /**
     * Execute clustering algorithm. The parameters are all meant for debugging
     * purposes. Should you not be interested in that, use {@link #cluster()}.
     *
     * This method will do nothing if the current data set has already been
     * clustered before. The flag for that is cleared when a new file is opened.
     *
     * @param includeOutOfCell Whether points in time where glyphs grow out of
     *            their cell should be included in the output.
     * @param step Whether the algorithm should pause after every event, only
     *            to continue when the user inputs a line (or just pressed enter).
     * @throws InterruptedException When the thread was interrupted while clustering.
     */
    public void cluster(boolean includeOutOfCell, boolean step) throws InterruptedException {
        if (clustered) {
            return;
        }

        // initialize grow function as necessary
        g.thresholds.clear();
        if (GrowingGlyphs.SETTINGS.getBoolean(Setting.COMPRESSION)) {
            g.thresholds.defaultFor(dataSet);
        }
        g.initialize(n, D.MAX_RADIUS.get());

        // perform the actual clustering
        if (LOGGER != null) {
            LOGGER.log(Level.FINE, "clustering using the {0} algorithm",
                    this.clusterer.getName());
            LOGGER.log(Level.FINE, "{0}collecting stats",
                    (B.STATS_ENABLED.get() ? "" : "not "));
            LOGGER.log(Level.FINE, "{0}using QUAD+ optimization",
                    (B.ROBUST.get() ? "not " : ""));
            LOGGER.log(Level.FINE, "{0}using big glyph optimization",
                    (B.BIG_GLYPHS.get() ? "" : "not "));
        }
        clusterer.cluster(g, includeOutOfCell, step);
        clustered = true;
    }

    public Clusterer getClusterer() {
        return clusterer;
    }

    /**
     * Returns the latest result of executing the clustering algorithm. Initially
     * {@code null}.
     */
    public HierarchicalClustering getClustering() {
        return clusterer.getClustering();
    }

    public String getDataSet() {
        return dataSet;
    }

    public File getLastOpened() {
        return lastOpened;
    }

    public boolean isClustered() {
        return clustered;
    }

    /**
     * Read weighted point set from given file.
     *
     * @param file File to open.
     */
    public int openFile(File file) {
        tree.clear();
        if (file.getName().endsWith(".csv") || file.getName().endsWith(".tsv")) {
            dataSet = file.getName().substring(0, file.getName().length() - 4);
            n = CsvIO.read(file, tree);
        } else {
            dataSet = file.getName();
            n = PointIO.read(file, tree);
        }
        lastOpened = file;
        reset();
        return n;
    }

    public void reopen() {
        if (lastOpened != null) {
            openFile(lastOpened);
        }
    }

    public void reset() {
        clustered = false;
        clusterer.reset();
        Stats.reset();
        Timers.reset();
    }

    public void setClusterer(Clusterer clusterer) {
        if (clusterer.hasSameTreeAs(this.clusterer)) {
            this.clusterer = clusterer;
        } else if (LOGGER != null) {
            LOGGER.log(Level.WARNING, "trying to set clusterer with different "
                    + "input tree");
        }
    }

    public void setGrowFunction(GrowFunction g) {
        this.g = g;
    }

}
