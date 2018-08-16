package algorithm.glyphgenerator;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import datastructure.Glyph;
import datastructure.QuadTree;

public class BlowUp extends GlyphGenerator implements GlyphGenerator.Stateful {

    /**
     * Mapping from glyphs to the distance to their nearest neighbor.
     */
    private Map<Point2D, Double> placed;
    private Point2D[] placedArr;
    private QuadTree tree;


    public BlowUp() {
        super("Blow up");
        placed = null;
        placedArr = null;
        tree = null;
    }

    @Override
    public void init(int n, Rectangle2D rect) {
        super.init(n, rect);

        // reset list of placed points
        placed = new HashMap<>(n);
        tree = null;
    }

    @Override
    public void init(QuadTree tree) {
        placed.putAll(tree.getLeaves().stream()
                .flatMap((c) -> c.getGlyphsAlive().stream())
                .map((g) -> new Point2D.Double(g.getX(), g.getY()))
                .collect(Collectors.toMap(
                    (p) -> p,
                    (p)-> {
                        // first, attempt using the QuadTree
                        double s = 1;
                        do {
                            Rectangle2D query = new Rectangle2D.Double(
                                    p.getX() - s, p.getY() - s, 2 * s, 2 * s);
                            List<Glyph> nearbyGlyphs = tree.getLeaves(query).stream()
                                .flatMap((c) -> c.getGlyphsAlive().stream())
                                .collect(Collectors.toList());
                            if (nearbyGlyphs.size() > 0) {
                                double distSq = Double.MAX_VALUE;
                                for (Glyph glyph : nearbyGlyphs) {
                                    distSq = Math.min(distSq, p.distanceSq(glyph.getX(), glyph.getY()));
                                }
                                return distSq;
                            }
                            s *= 2;
                        } while (s <= 64);

                        // failed? loop over all points
                        double distSq = Double.MAX_VALUE;
                        for (Point2D q : placed.keySet()) {
                            distSq = Math.min(distSq, q.distanceSq(p));
                        }
                        return distSq;
                    }
                )));
        this.placedArr = placed.keySet().toArray(new Point2D[0]);
        this.tree = tree;
    }

    @Override
    public Glyph next() {
        if (tree == null) {
            throw new IllegalStateException("the generator must be initialized "
                    + "with a QuadTree before it can generate glyphs");
        }

        count();

        Point2D closeTo = placedArr[rand.nextInt(placedArr.length)];
        Point2D p = new Point2D.Double();
        double toBeat = Math.sqrt(placed.get(closeTo).doubleValue());
        p.setLocation(
            (rand.nextDouble() * 2 - 1) * toBeat + closeTo.getX(),
            (rand.nextDouble() * 2 - 1) * toBeat + closeTo.getY());
        int w = rand.nextInt(WEIGHT_RANGE[1]) + WEIGHT_RANGE[0];

        return new Glyph(p.getX(), p.getY(), w, true);
    }

}
