package datastructure.growfunction.shape;

import datastructure.growfunction.GrowFunction;

/**
 * This class serves as a base for all classes implementing the {@link GrowShape}
 * interface; it includes a {@link GrowFunction} member that can be asked for the
 * distance between glyphs, which is necessary to be able to determine when
 * glyphs will intersect. It can also be asked the number of entities a glyph
 * represents taking compression level into account, which is needed to be able
 * to determine the radius of a glyph.
 */
public abstract class GrowShapeBase implements GrowShape {

    /**
     * Grow function that this grow speed is used by.
     */
    protected GrowFunction gf;


    public GrowShapeBase(GrowFunction g) {
        this.gf = g;
    }

}
