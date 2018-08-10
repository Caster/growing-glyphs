package gui;

import java.awt.Shape;

import datastructure.Glyph;
import datastructure.growfunction.GrowFunction;
import gui.Settings.Setting;

/**
 * A GlyphShape represents the shape of a {@link Glyph} at a certain point in time.
 */
public class GlyphShape {

    public final int compressionLevel;
    public final int n;
    public final Shape shape;
    public final Shape shapeWithBorder;


    public GlyphShape(Glyph glyph, double at, GrowFunction g) {
        this.compressionLevel = g.thresholds.getCompressionLevel(glyph);
        this.n = glyph.getN();
        this.shape = g.sizeAt(glyph, at);
        if (GrowingGlyphs.SETTINGS.getBoolean(Setting.BORDERS)) {
            this.shapeWithBorder = g.sizeAt(glyph, at, this.compressionLevel);
        } else {
            this.shapeWithBorder = this.shape;
        }
    }

}
