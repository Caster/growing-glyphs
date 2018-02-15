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
        return Math.max(0, Utils.euclidean(rect, g.getX(), g.getY()) -
                gf.border(g));
    }

    @Override
    public Shape sizeAt(Glyph g, double at, int c) {
        double r = gf.radius(gf.radius(g, at), c);
        return new Ellipse2D.Double(g.getX() - r, g.getY() - r, 2 * r, 2 * r);
    }

}
