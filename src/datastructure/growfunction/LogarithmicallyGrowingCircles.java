package datastructure.growfunction;

import java.awt.geom.Rectangle2D;

import datastructure.Glyph;
import utils.Utils;

/**
 * This {@link GrowFunction} scales {@link Glyph circles} logarithmically. For this
 * function, the zoom level is interpreted as time. The radius of a circle at time
 * {@code t} is then defined as {@code log(t * n)}, where {@code n} is the number
 * of entities represented by the circle.
 *
 * @see LogarithmicGrowFunction
 */
public class LogarithmicallyGrowingCircles extends LogarithmicGrowFunction {

    @Override
    protected double dist(double px, double py, double qx, double qy) {
        return Utils.euclidean(px, py, qx, qy);
    }

    @Override
    protected double dist(Rectangle2D rect, double px, double py) {
        return Utils.euclidean(rect, px, py);
    }

}
