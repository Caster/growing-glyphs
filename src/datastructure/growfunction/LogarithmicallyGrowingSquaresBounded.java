package datastructure.growfunction;

/**
 * This {@link GrowFunction} scales exactly like
 * {@link LogarithmicallyGrowingCirclesBounded}, but instead uses square shaped
 * glyphs.
 */
public class LogarithmicallyGrowingSquaresBounded extends LogarithmicallyGrowingSquares {

    public LogarithmicallyGrowingSquaresBounded() {
        super();
        this.name = "Logarithmically Growing Squares (Bounded)";
    }

    @Override
    public void initialize(int numGlyphs, double maxRadius) {
        fA = maxRadius / (Math.log(numGlyphs) / LOG_DIV);
    }

}
