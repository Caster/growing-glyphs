package datastructure.events;

import datastructure.GrowFunction;
import datastructure.QuadTree;
import datastructure.Square;

/**
 * @see Type#OUT_OF_CELL
 */
public class OutOfCell extends Event {

    public enum Side {
        TOP(0, 1), RIGHT(1, 3), BOTTOM(2, 3), LEFT(0, 2);


        private int[] quadrants;


        private Side(int quadrant1, int quadrant2) {
            this.quadrants = new int[] {quadrant1, quadrant2};
        }


        /**
         * Given a quadrant, for example TOP + LEFT, return the two neighboring
         * quadrants, for example RIGHT and BOTTOM.
         *
         * Note that this will always return the other two sides. Calling with
         * (a, b) is equivalent to calling with (b, a).
         *
         * @param s Descriptor of quadrant.
         * @return Where to find neighboring quadrants.
         * @throws IllegalArgumentException When {@code s[0] == s[1]} or either
         *             is {@code null}, or {@code s.length != 2}.
         */
        public static Side[] neighborQuadrants(Side[] s) {
            if (s.length != 2 || s[0] == s[1] || s[0] == null || s[1] == null) {
                throw new IllegalArgumentException();
            }
            Side[] result = new Side[2];
            int i = 0;
            for (Side side : values()) {
                if (side != s[0] && side != s[1]) {
                    result[i++] = side;
                }
            }
            return result;
        }

        /**
         * Given an index of a quadrant as used in {@link QuadTree}, return a
         * descriptor of that quadrant.
         *
         * @param index Index of a quadrant. Quadrants are indexed as in the
         *            children of a {@link QuadTree}. That is, {@code [top left,
         *            top right, bottom left, bottom right]}.
         * @return A descriptor of the quadrant. This is simply an array with
         *         two sides that together describe the quadrant.
         */
        public static Side[] quadrant(int index) {
            if (index < 0 || index > 3) {
                throw new IllegalArgumentException();
            }
            Side[] descriptor = new Side[2];
            int i = 0;
            for (Side side : values()) {
                if (side.quadrants[0] == index || side.quadrants[1] == index) {
                    descriptor[i++] = side;
                }
            }
            return descriptor;
        }

        /**
         * Given a quadrant as per {@link #quadrant(int)} and a side, return the
         * quadrant that lies to that side. Some combinations will be illegal,
         * in those cases garbage output is returned (as in, incorrect).
         */
        public static int quadrantNeighbor(int index, Side side) {
            switch (index) {
            case 0: // top left quadrant
                return side.ordinal();
            case 1: // top right quadrant
                return (index + side.ordinal()) % 4;
            case 2: // bottom left quadrant
                return (side == Side.TOP ? 0 : 3);
            case 3: // bottom right quadrant
                return (side == Side.TOP ? 1 : 2);
            }
            return -1; // to make compiler happy
        }


        public Side opposite() {
            return values()[(ordinal() + 2) % 4];
        }

        public Side[] others() {
            Side[] result = new Side[3];
            int i = 0;
            for (Side that : values()) {
                if (that != this) {
                    result[i++] = that;
                }
            }
            return result;
        }

        /**
         * Returns the two quadrants on this side. For example, passing LEFT
         * will yield the indices of the top left and bottom left quadrants.
         * Indices as per {@link #quadrant(int)}.
         *
         * @return Indices of quadrants on given side.
         */
        public int[] quadrants() {
            return quadrants;
        }
    }


    private Side side;


    public OutOfCell(Square square, GrowFunction g, QuadTree cell, Side side) {
        super(g.exitAt(square, cell, side), 1);
        this.squares[0] = square;
        this.side = side;
    }

    public Side getSide() {
        return side;
    }

    @Override
    public Type getType() {
        return Type.OUT_OF_CELL;
    }

}
