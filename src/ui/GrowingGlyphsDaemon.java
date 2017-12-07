package ui;

import java.io.File;

import algorithm.AgglomerativeClustering;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.growfunction.GrowFunction;
import gui.GrowingGlyphs;
import io.CsvIO;
import io.PointIO;
import utils.Utils.Stats;
import utils.Utils.Timers;

/**
 * Thin wrapper around {@link AgglomerativeClustering} and the {@link QuadTree}
 * it works on. This class contains all functionality needed to execute the
 * Growing Glyphs program in background mode, all other functionality is
 * implemented in {@link GrowingGlyphs}.
 */
public class GrowingGlyphsDaemon {

    private AgglomerativeClustering clusterer;
    private boolean clustered;
    private GrowFunction g;
    private String dataSet;
    private File lastOpened;
    private int n;
    private QuadTree tree;

    public GrowingGlyphsDaemon(int w, int h, GrowFunction g) {
        this.g = g;
        this.tree = new QuadTree(-w / 2, -h / 2, w, h, g);
        this.clusterer = new AgglomerativeClustering(this.tree);
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
     */
    public void cluster() {
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
     */
    public void cluster(boolean includeOutOfCell, boolean step) {
        if (clustered) {
            return;
        }
        g.thresholds.defaultFor(dataSet); // TODO: allow to change this via GUI/CLI?
        g.initialize(n, 256); // TODO: allow to change this via GUI/CLI?
        clusterer.cluster(g, includeOutOfCell, step);
        clustered = true;
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

    public boolean isClustered() {
        return clustered;
    }

    /**
     * Read weighted point set from given file.
     *
     * @param file File to open.
     */
    public void openFile(File file) {
        tree.clear();
        if (file.getName().endsWith(".csv") || file.getName().endsWith(".tsv")) {
            dataSet = file.getName().substring(0, file.getName().length() - 4);
            n = CsvIO.read(file, tree);
        } else {
            dataSet = file.getName();
            n = PointIO.read(file, tree);
        }
        clustered = false;
        clusterer.reset();
        lastOpened = file;
        Stats.reset();
        Timers.reset();
    }

    public void reopen() {
        if (lastOpened != null) {
            openFile(lastOpened);
        }
    }

    public void setGrowFunction(GrowFunction g) {
        this.g = g;
    }

}
