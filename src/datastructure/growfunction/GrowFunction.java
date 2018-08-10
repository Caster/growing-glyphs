package datastructure.growfunction;

import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import datastructure.Glyph;
import datastructure.QuadTree;
import datastructure.events.OutOfCell.Side;
import datastructure.growfunction.shape.CirclesGrowShape;
import datastructure.growfunction.shape.GrowShape;
import datastructure.growfunction.shape.SquaresGrowShape;
import datastructure.growfunction.speed.BoundedLogarithmicGrowSpeed;
import datastructure.growfunction.speed.GrowSpeed;
import datastructure.growfunction.speed.LevelGrowSpeed;
import datastructure.growfunction.speed.LinearAreaGrowSpeed;
import datastructure.growfunction.speed.LinearGrowSpeed;
import datastructure.growfunction.speed.LogarithmicGrowSpeed;
import gui.GrowingGlyphs;
import gui.Settings.Setting;
import utils.Constants.S;

/**
 * Function determining how {@link Glyph Glyphs} should be scaled.
 */
public class GrowFunction implements GrowShape, GrowSpeed {

    /**
     * Map of names to instances of grow functions. These instances can be used
     * throughout the program, creating new instances should never be necessary.
     */
    private static final Map<String, GrowFunction> ALL = new HashMap<>();


    /**
     * Returns a map of names to instances of grow functions. Theses instances
     * can always be used, creating new instances should never be necessary.
     */
    public static Map<String, GrowFunction> getAll() {
        if (ALL.isEmpty()) {
            List<Class<? extends GrowShape>> shapes = Arrays.asList(
                CirclesGrowShape.class,
                SquaresGrowShape.class
            );
            List<Class<? extends GrowSpeed>> speeds = Arrays.asList(
                LevelGrowSpeed.class,
                LinearGrowSpeed.class,
                LinearAreaGrowSpeed.class,
                LogarithmicGrowSpeed.class,
                BoundedLogarithmicGrowSpeed.class
            );
            // construct growfunctions for all combinations
            for (Class<? extends GrowSpeed> speed : speeds) {
                for (Class<? extends GrowShape> shape : shapes) {
                    GrowFunction g = new GrowFunction(shape, speed);
                    ALL.put(g.getName(), g);
                }
            }
        }
        return ALL;
    }


    /**
     * Thresholds that apply to this grow function.
     */
    public final CompressionThreshold thresholds = new CompressionThreshold();


    /**
     * Name of the grow function - determined by its {@link GrowShape} and
     * {@link GrowShape}. See {@link #getName()}.
     */
    protected String name;


    /**
     * Shape of glyphs that is implied by this grow function.
     */
    private final GrowShape shape;
    /**
     * Speed that is used by this grow function.
     */
    private final GrowSpeed speed;


    /**
     * Constructs a grow function that grows glyphs with the given shape and speed.
     *
     * @param shape Function that determines shape of all glyphs.
     * @param speed Function that determines speed at which glyphs grow.
     */
    public GrowFunction(GrowShape shape, GrowSpeed speed) {
        this.shape = shape;
        this.speed = speed;
    }

    /**
     * Constructs a grow function that grows glyphs with the given shape and speed.
     *
     * This constructor variant constructs fresh instances of the given functions
     * and immediately links them to the created {@link GrowFunction} instance.
     *
     * @param shape Function that determines shape of all glyphs.
     * @param speed Function that determines speed at which glyphs grow.
     */
    public GrowFunction(Class<? extends GrowShape> shape,
            Class<? extends GrowSpeed> speed) {
        try {
            this.shape = shape.getConstructor(this.getClass()).newInstance(this);
            this.speed = speed.getConstructor(this.getClass()).newInstance(this);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }


    /**
     * Given a glyph, return the width of its border.
     *
     * @param g Glyph to determine the border width of.
     * @param at Timestamp at which border width is determined.
     * @see #radius(double, int)
     */
    public double border(Glyph g, double at) {
        if (!GrowingGlyphs.SETTINGS.getBoolean(Setting.BORDERS)) {
            return 0;
        }
        return border(thresholds.getCompressionLevel(g), at);
    }

    @Override
    public double dist(Glyph a, Glyph b) {
        return shape.dist(a, b);
    }


    @Override
    public double dist(Rectangle2D rect, Glyph g) {
        return shape.dist(rect, g);
    }

    /**
     * Returns at which zoom level a glyph touches the given side of the given
     * cell. The glyph is scaled using this {@link GrowFunction}.
     *
     * @param glyph Growing glyph.
     * @param cell Cell that glyph is assumed to be inside of, altough if not
     *            the time of touching is still correctly calculated.
     * @param side Side of cell for which calculation should be done.
     * @return Zoom level at which {@code glyph} touches {@code side} side of
     *         {@code cell}.
     */
    public double exitAt(Glyph glyph, QuadTree cell, Side side) {
        return intersectAt(cell.getSide(side), glyph);
    }

    /**
     * Given a glyph, return the compression factor to be used on that glyph.
     *
     * @param glyph Glyph to find compression factor for.
     */
    public double getCompression(Glyph glyph) {
        return thresholds.getCompression(glyph);
    }

    /**
     * Given a glyph, return the compression level to be used on that glyph.
     *
     * @param glyph Glyph to find compression level for.
     */
    public int getCompressionLevel(Glyph glyph) {
        return thresholds.getCompressionLevel(glyph);
    }

    /**
     * Returns the human readable name of this function. This method guarantees
     * to always return the same exact instance of {@link String}.
     */
    public String getName() {
        if (this.name == null) {
            String shapeName = shape.getClass().getSimpleName();
            String speedName = speed.getClass().getSimpleName();
            Pattern wordPattern = Pattern.compile("[A-Z][a-z]+");
            Matcher shMatcher = wordPattern.matcher(shapeName);
            Matcher spMatcher = wordPattern.matcher(speedName);
            if (spMatcher.find() && shMatcher.find()) {
                StringBuilder result = new StringBuilder(spMatcher.group());
                while (spMatcher.find() && !spMatcher.group().equals("Grow")) {
                    result.append(" ");
                    result.append(spMatcher.group());
                }
                result.append(" Growing");
                do {
                    result.append(" ");
                    result.append(shMatcher.group());
                } while (shMatcher.find() && !shMatcher.group().equals("Grow"));
                this.name = result.toString();
                if (this.name.equals(S.GROW_FUNCTION.get())) {
                    this.name = S.GROW_FUNCTION.get();
                }
            } else {
                this.name = "unknown grow function";
            }
        }
        return this.name;
    }

    /**
     * Initialize a grow function to fit the specific data set that is to be
     * clustered. The default implementation of this function does nothing, but
     * specific grow function implementations may use this method to set
     * parameters and fit better on the data set.
     *
     * @param numGlyphs The number of glyphs that is present initially.
     * @param maxRadius The maximum radius of glyphs. The minimum radius of glyphs
     *            is always 0 because of restrictions in the clustering algorithm.
     */
    @Override
    public void initialize(int numGlyphs, double maxRadius) {
        speed.initialize(numGlyphs, maxRadius);
    }

    @Override
    public double intersectAt(Glyph a, Glyph b) {
        return speed.intersectAt(a, b);
    }

    @Override
    public double intersectAt(Rectangle2D r, Glyph glyph) {
        return speed.intersectAt(r, glyph);
    }

    /**
     * Same as {@link #intersectAt(Rectangle2D, Glyph)}, just with different order
     * of parameters. This is a convenience function.
     */
    public double intersectAt(Glyph glyph, Rectangle2D r) {
        return intersectAt(r, glyph);
    }

    @Override
    public double radius(Glyph g, double at) {
        return speed.radius(g, at);
    }

    /**
     * Given the radius of a glyph and its compression level, return the radius
     * of that glyph including its border. The border width is determined by the
     * compression level.
     *
     * <p>In particular, a glyph with compression level <code>k</code> will have a
     * border of width <code>2k</code>. However, for negative timestamps the border
     * may be less wide; it grows into existence at linear pace.
     *
     * @param radius Radius without border.
     * @param compressionLevel Compression level of the glyph.
     * @param at Timestamp at which radius is determined.
     * @see #border(Glyph)
     */
    public double radius(double radius, int compressionLevel, double at) {
        return radius + border(compressionLevel, at);
    }

    /**
     * Returns a shape representing the glyph at the given time stamp/zoom
     * level, according to this grow function. This will return the shape
     * without a border; the result is exactly the same as calling
     * {@link #sizeAt(Glyph, double, int)} with {@code compressionLevel 0}.
     *
     * @param glyph Glyph to compute the size of.
     * @param at Time stamp or zoom level at which size must be computed.
     * @return A rectangle representing the glyph at time/zoom {@code at}.
     */
    public Shape sizeAt(Glyph glyph, double at) {
        return sizeAt(glyph, at, 0);
    }

    @Override
    public Shape sizeAt(Glyph glyph, double at, int compressionLevel) {
        return shape.sizeAt(glyph, at, compressionLevel);
    }

    /**
     * Convenience function that returns the shape of all given glyphs.
     *
     * @param at Time stamp or zoom level at which size must be computed.
     * @param glyphs Zero or more glyphs to compute the size of.
     * @return Array with the shapes of the give glyphs, in the given order.
     */
    public Shape[] sizesAt(double at, Glyph... glyphs) {
        Shape[] result = new Shape[glyphs.length];
        for (int i = 0; i < glyphs.length; ++i) {
            result[i] = sizeAt(glyphs[i], at);
        }
        return result;
    }

    /**
     * Convenience function that returns the shape of all given glyphs.
     *
     * @param at Time stamp or zoom level at which size must be computed.
     * @param glyphs List of zero or more glyphs to compute the size of.
     * @return Array with the shapes of the give glyphs, in the given order.
     */
    public Shape[] sizesAt(double at, List<Glyph> glyphs) {
        return this.sizesAt(at, glyphs.toArray(new Glyph[0]));
    }


    /**
     * Given a compression level, returns how wide the border for that level is.
     *
     * @param compressionLevel Compression level to calculate border width of.
     * @param at Timestamp at which border width is determined.
     * @see #border(Glyph)
     * @see #radius(double, int)
     */
    private double border(int compressionLevel, double at) {
        if (!GrowingGlyphs.SETTINGS.getBoolean(Setting.BORDERS)) {
            return 0;
        }
        double w = 2 * compressionLevel;
        if (at < 0) {
            w = Math.max(0, w + at);
        }
        return w;
    }

}
