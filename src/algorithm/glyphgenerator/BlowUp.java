package algorithm.glyphgenerator;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import datastructure.Glyph;
import datastructure.QuadTree;
import utils.Constants.D;
import utils.Utils;

public class BlowUp extends GlyphGenerator implements GlyphGenerator.Stateful {

    /**
     * Mapping from glyphs to the distance to their nearest neighbor.
     */
    private Map<Point2D, Info> placed;
    private Point2D[] placedArr;
    private QuadTree tree;
    private Glyph[] glyphs;


    public BlowUp() {
        super("Blow up");
        reset();
    }

    @Override
    public int getNumPlaced() {
        return placedArr.length + i;
    }

    @Override
    public void init(int n, Rectangle2D rect) {
        super.init(n, rect);
        reset();
    }

    @Override
    public boolean init(QuadTree tree) {
        int numPlaced = Utils.size(tree.iteratorGlyphsAlive()) + n;
        this.glyphs = new Glyph[n + numPlaced];
        this.placed = new HashMap<>(numPlaced);
        this.placed.putAll(tree.getLeaves().stream()
                .flatMap((c) -> c.getGlyphsAlive().stream())
                .collect(Collectors.toMap(
                    (g) -> new Point2D.Double(g.getX(), g.getY()),
                    (g)-> {
                        // first, attempt using the QuadTree
                        double s = 0.25;
                        do {
                            Rectangle2D query = new Rectangle2D.Double(
                                    g.getX() - s, g.getY() - s, 2 * s, 2 * s);
                            List<Glyph> nearbyGlyphs = tree.getLeaves(query).stream()
                                .flatMap((c) -> c.getGlyphsAlive().stream())
                                .collect(Collectors.toList());
                            if (nearbyGlyphs.size() > 0 && !(nearbyGlyphs.size() == 1 &&
                                    nearbyGlyphs.get(0) == g)) {
                                double distSq = Double.MAX_VALUE;
                                for (Glyph glyph : nearbyGlyphs) {
                                    double d = new Point2D.Double(g.getX(), g.getY())
                                        .distanceSq(glyph.getX(), glyph.getY());
                                    if (d > 0) {
                                        distSq = Math.min(distSq, d);
                                    }
                                }
                                return new Info(g, Math.sqrt(distSq));
                            }
                            s *= 2;
                        } while (s <= rect.getWidth() * 2);
                        throw new RuntimeException("no nearest neighbor found");
                    }
                )));
        this.placedArr = placed.keySet().toArray(new Point2D[0]);
        this.tree = tree;

        calculateGlyphs();
        return true;
    }

    @Override
    public Glyph next() {
        if (tree == null) {
            throw new IllegalStateException("the generator must be initialized "
                    + "with a QuadTree before it can generate glyphs");
        }

        count();

        Glyph result = glyphs[i - 1];
        if (i == placedArr.length + n) {
            reset();
        }
        return result;
    }


    private void calculateGlyphs() {
        int numPlaced = placedArr.length;
        // number of glyphs to replace each glyph by
        int k = (n + numPlaced) / numPlaced;
        // number of glyphs that need a single extra glyph
        int k2 = (n + numPlaced) - k * numPlaced;

        int gI = 0;
        for (int i = 0; i < numPlaced; ++i) {
            int kC = k + (i < k2 ? 1 : 0);
            Info info = placed.get(placedArr[i]);
            int nRemaining = info.glyph.getN();
            if (nRemaining < kC) {
                nRemaining = kC; // minimum of 1 per glyph...
            }
            info.glyph.setN(rand.nextInt(nRemaining - kC + 1) + 1);
            nRemaining -= info.glyph.getN();
            glyphs[gI++] = info.glyph;
            for (int j = 1; j < kC; ++j) {
                Point2D p = new Point2D.Double();
                do {
                    double r = rand.nextDouble();
                    double θ = rand.nextDouble() * Math.PI * 2;
                    p.setLocation(
                        info.glyph.getX() + info.nnd * Math.sqrt(r) * Math.cos(θ),
                        info.glyph.getY() + info.nnd * Math.sqrt(r) * Math.sin(θ));
                } while (!rect.contains(p) ||
                        nndSq(p) < D.MIN_CELL_SIZE.get() * D.MIN_CELL_SIZE.get());
                glyphs[gI++] = new Glyph(
                    p.getX(), p.getY(),
                    (j == kC - 1 ? nRemaining :
                        rand.nextInt(nRemaining - kC + j + 1) + 1),
                    true
                );
                nRemaining -= glyphs[gI - 1].getN();
            }
        }
    }

    private double nndSq(Point2D p) {
        double s = 0.25;
        do {
            Rectangle2D query = new Rectangle2D.Double(
                    p.getX() - s, p.getY() - s, 2 * s, 2 * s);
            List<Glyph> nearbyGlyphs = tree.getLeaves(query).stream()
                .flatMap((c) -> c.getGlyphsAlive().stream())
                .collect(Collectors.toList());
            if (nearbyGlyphs.size() > 0 && !(nearbyGlyphs.size() == 1 &&
                    Utils.Double.eq(nearbyGlyphs.get(0).getX(), p.getX()) &&
                    Utils.Double.eq(nearbyGlyphs.get(0).getY(), p.getY()))) {
                double distSq = Double.MAX_VALUE;
                for (Glyph glyph : nearbyGlyphs) {
                    double d = p.distanceSq(glyph.getX(), glyph.getY());
                    if (d > 0) {
                        distSq = Math.min(distSq, d);
                    }
                }
                return distSq;
            }
            s *= 2;
        } while (s <= rect.getWidth() * 2);
        throw new RuntimeException("no nearest neighbor found");
    }

    private void reset() {
        placed = null;
        placedArr = null;
        tree = null;
        glyphs = null;
    }


    private static class Info {

        private Glyph glyph;
        private double nnd;

        private Info(Glyph glyph, double nnd) {
            this.glyph = glyph;
            this.nnd = nnd;
        }

    }

}
