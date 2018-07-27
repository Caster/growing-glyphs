package datastructure.growfunction.shape;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;
import utils.Utils;

public class CirclesGrowShape extends GrowShapeBase {

    public CirclesGrowShape(GrowFunction g) {
        super(g);
    }

    @Override
    public double dist(Glyph a, Glyph b) {
        return Utils.euclidean(a.getX(), a.getY(), b.getX(), b.getY()) -
                gf.border(a) - gf.border(b);
    }

    @Override
    public double dist(Rectangle2D rect, Glyph g) {
        double d = Utils.euclidean(rect, g.getX(), g.getY());
        if (d < 0) {
            return Double.NEGATIVE_INFINITY;
        }
        return d - gf.border(g);
    }

    @Override
    public Shape sizeAt(Glyph g, double at, int c) {
        double r = gf.radius(gf.radius(g, at), c);
        return new Ellipse2D.Double(g.getX() - r, g.getY() - r, 2 * r, 2 * r);
    }

}
