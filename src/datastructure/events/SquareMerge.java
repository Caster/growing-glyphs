package datastructure.events;

import datastructure.Square;
import datastructure.growfunction.GrowFunction;

/**
 * @see Type#MERGE
 */
public class SquareMerge extends Event {

    public SquareMerge(Square a, Square b, GrowFunction g) {
        super(g.intersectAt(a, b), 2);
        this.squares[0] = a;
        this.squares[1] = b;
    }

    @Override
    public Type getType() {
        return Type.MERGE;
    }

}
