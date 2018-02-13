package gui;

import java.awt.Shape;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;

/**
 * A GlyphShape represents the shape of a {@link Glyph} at a certain point in time.
 */
public class GlyphShape {

    public final int compressionLevel;
    public final Shape shape;
    public final Shape shapeWithBorder;


    public GlyphShape(Glyph glyph, double at, GrowFunction g) {
        this.compressionLevel = g.thresholds.getCompressionLevel(glyph);
        this.shape = g.sizeAt(glyph, at);
        this.shapeWithBorder = g.sizeAt(glyph, at, this.compressionLevel);
    }

}
