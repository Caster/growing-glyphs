package datastructure.growfunction.shape;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;
import utils.Utils;

public class SquaresGrowShape extends GrowShapeBase {

    public SquaresGrowShape(GrowFunction g) {
        super(g);
    }

    @Override
    public double dist(double px, double py, double qx, double qy) {
        return Utils.chebyshev(px, py, qx, qy);
    }

    @Override
    public double dist(Rectangle2D rect, double px, double py) {
        return Utils.chebyshev(rect, px, py);
    }
    @Override
    public Shape sizeAt(Glyph g, double at, int c) {
        double r = gf.radius(gf.radius(g, at), c);
        return new Rectangle2D.Double(g.getX() - r, g.getY() - r, 2 * r, 2 * r);
    }

}
