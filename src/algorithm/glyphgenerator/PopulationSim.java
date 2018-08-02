package algorithm.glyphgenerator;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.Glyph;

public class PopulationSim extends GlyphGenerator {

    private static final Logger LOGGER =
            Logger.getLogger(PopulationSim.class.getName());
    /**
     * Minimum squared nearest neighbor distance that is accepted.
     */
    private static final double MIN_DIST_SQ = 10;
    /**
     * Minimum value for the multiplier.
     */
    private static final double MIN_MULTIPLIER = 0.005;
    /**
     * Modifier on maximum distance to compare nearest neighbor distance to.
     * Lowering this value will make it so that points cluster more.
     */
    private static final double MOD = 0.005;
    /**
     * Parameter for weighting the impact of the multiplier.
     * Lowering this will give the point cloud more time to spread out initially.
     */
    private static final double MULTIPLIER_WEIGHT = 2.0;
    /**
     * Number of points that is placed completely randomly at the start. Only
     * after that, restrictions from nearest neighbor distance take effect.
     */
    private static final int POINTS_PLACED_FREELY = 5;

    private int i;
    private double maxDistSq;
    private ArrayList<Point2D> placed;


    public PopulationSim() {
        super("Population simulation");
        placed = new ArrayList<>();
    }

    @Override
    public void init(int n, Rectangle2D rect) {
        super.init(n, rect);

        // reset counter
        i = 0;
        // diagonal of the rectangle is maximum distance between two points
        maxDistSq = rect.getWidth() * rect.getWidth() +
                rect.getHeight() * rect.getHeight();
        maxDistSq *= MOD;
        LOGGER.log(Level.FINE, "INIT {0}, maxDistSq = {1}",
                new Object[] {PopulationSim.class.getName(), maxDistSq});
        // reset list of placed points
        placed.clear();
        placed.ensureCapacity(n);
    }

    @Override
    public Glyph next() {
        if (i == n) {
            throw new RuntimeException();
        }

        double multiplier = Math.max(MIN_MULTIPLIER, MULTIPLIER_WEIGHT - MULTIPLIER_WEIGHT *
                Math.log(i + POINTS_PLACED_FREELY - 1) / Math.log(n - POINTS_PLACED_FREELY + 1));
        LOGGER.log(Level.FINE, "placing glyph {0}, multiplier {1}",
                new Object[] {i + 1, multiplier});
        Point2D p = new Point2D.Double();
        double near = random(p);
        int w = rand.nextInt(WEIGHT_RANGE[1]) + WEIGHT_RANGE[0];
        while (i > POINTS_PLACED_FREELY - 1) { // first X points are always placed
            double r = rand.nextDouble();
            double toBeat = near / (maxDistSq * multiplier);
            LOGGER.log(Level.FINER, "nearest neighbor at {0}, r = {1} > {2}? {3}",
                    new Object[] {near, r, toBeat, (r > toBeat ? "yes" : "no")});
            if (r > toBeat) {
                break;
            }
            near = random(p);
        }

        placed.add(p);
        i++;
        LOGGER.log(Level.FINE, "at position {0}", p);
        return new Glyph(p.getX(), p.getY(), w, true);
    }


    private double nearestNeighborDistSq(Point2D p) {
        double distSq = Double.MAX_VALUE;
        for (Point2D q : placed) {
            distSq = Math.min(distSq, q.distanceSq(p));
        }
        return distSq;
    }

    /**
     * Set location of given point to something random, return squared nearest
     * neighbor distance. Ensure that point is at least {@link #MIN_DIST_SQ}
     * distance away from its nearest neighbor.
     */
    private double random(Point2D p) {
        double near;
        do {
            p.setLocation(rand.nextDouble() * rect.getWidth() + rect.getX(),
                rand.nextDouble() * rect.getHeight() + rect.getY());
            near = nearestNeighborDistSq(p);
        } while (near < MIN_DIST_SQ);
        return near;
    }

}
