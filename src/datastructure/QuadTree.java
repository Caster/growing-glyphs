package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import datastructure.events.OutOfCell.Side;
import datastructure.growfunction.GrowFunction;

/**
 * A QuadTree implementation that can track growing glyphs.
 *
 * @see Glyph
 */
public class QuadTree {

    /**
     * The maximum number of glyphs that should intersect any leaf cell.
     */
    public static final int MAX_GLYPHS_PER_CELL = 5;


    /**
     * Enumeration of moments when a glyph may be inserted into the tree.
     */
    public enum InsertedWhen { INITIALLY, BY_ALGORITHM; }


    /**
     * Rectangle describing this cell.
     */
    private Rectangle2D cell;
    /**
     * Parent pointer. Will be {@code null} for the root cell.
     */
    private QuadTree parent;
    /**
     * Child cells, in the order: top left, top right, bottom left, bottom right.
     * Will be {@code null} for leaf cells.
     */
    private QuadTree[] children;
    /**
     * Function that is used to determine the size of glyphs.
     */
    private GrowFunction g;
    /**
     * Glyphs intersecting the cell. This is not just a set, but a map, because
     * it tracks whether glyphs were inserted initially or while running the
     * clustering algorithm.
     */
    private Map<Glyph, InsertedWhen> glyphs;


    /**
     * Construct a rectangular QuadTree cell at given coordinates.
     *
     * @param rect Rectangle describing the cell location.
     * @param g Function that is used to determine the size of glyphs.
     */
    public QuadTree(Rectangle2D rect, GrowFunction g) {
        this(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), g);
    }

    /**
     * Construct a square QuadTree cell at given coordinates.
     *
     * @param x X-coordinate of top left corner of cell.
     * @param y Y-coordinate of top left corner of cell.
     * @param s Size (width = height) of cell.
     * @param g Function that is used to determine the size of glyphs.
     */
    public QuadTree(double x, double y, double s, GrowFunction g) {
        this(x, y, s, s, g);
    }

    /**
     * Construct a rectangular QuadTree cell at given coordinates.
     *
     * @param x X-coordinate of top left corner of cell.
     * @param y Y-coordinate of top left corner of cell.
     * @param w Width of cell.
     * @param h Height of cell.
     * @param g Function that is used to determine the size of glyphs.
     */
    public QuadTree(double x, double y, double w, double h, GrowFunction g) {
        this.cell = new Rectangle2D.Double(x, y, w, h);

        this.parent = null;
        this.children = null;
        this.g = g;
        this.glyphs = new HashMap<>(MAX_GLYPHS_PER_CELL);
    }


    /**
     * Reset to being a cell without glyphs nor children.
     */
    public void clear() {
        this.children = null;
        for (Glyph s : this.glyphs.keySet()) {
            s.removeCell(this);
        }
        this.glyphs.clear();
    }

    /**
     * Return the leaf cell in this QuadTree that contains the given point. In
     * case the point lies outside of this QuadTree, {@code null} is returned.
     *
     * @param x X-coordinate of query point.
     * @param y Y-coordinate of query point.
     */
    public QuadTree findLeafAt(double x, double y) {
        if (!cell.contains(x, y)) {
            return null;
        }
        // already a match?
        if (isLeaf()) {
            return this;
        }
        // find correct child
        QuadTree result;
        for (QuadTree child : children) {
            if ((result = child.findLeafAt(x, y)) != null) {
                return result;
            }
        }
        return null;
    }

    public QuadTree[] getChildren() {
        return this.children;
    }

    public Set<Glyph> getGlyphs() {
        return glyphs.keySet();
    }

    public double getHeight() {
        return cell.getHeight();
    }

    public Set<QuadTree> getNeighbors(Side side) {
        Set<QuadTree> result = new HashSet<>();
        getNeighbors(side, result);
        return result;
    }

    public QuadTree getParent() {
        return parent;
    }

    public Rectangle2D getRectangle() {
        return cell;
    }

    public Rectangle2D getSide(Side side) {
        return new Rectangle2D.Double(
                cell.getX() + (side == Side.RIGHT ? cell.getWidth() : 0),
                cell.getY() + (side == Side.BOTTOM ? cell.getHeight() : 0),
                (side == Side.TOP || side == Side.BOTTOM ? cell.getWidth() : 0),
                (side == Side.RIGHT || side == Side.LEFT ? cell.getHeight() : 0)
            );
    }

    public Set<Glyph> getGlyphs(InsertedWhen filter) {
        return glyphs.keySet().stream()
            .filter(s -> glyphs.get(s) == filter)
            .collect(Collectors.toSet());
    }

    public double getWidth() {
        return cell.getWidth();
    }

    public double getX() {
        return cell.getX();
    }

    public double getY() {
        return cell.getY();
    }

    /**
     * Insert a given glyph into all cells of this QuadTree it intersects. This
     * method does not care about {@link QuadTree#MAX_GLYPHS_PER_CELL}.
     *
     * @param glyph The glyph to insert.
     * @param at Timestamp/zoom level at which insertion takes place.
     * @param g Function to determine size of glyph. Together with {@code at},
     *          this is used to decide which cells {@code glyph} intersects.
     */
    public void insert(Glyph glyph, double at, GrowFunction g) {
        if (g.intersectAt(glyph, cell) > at + Utils.EPS) {
            return;
        }
        if (isLeaf()) {
            glyphs.put(glyph, InsertedWhen.BY_ALGORITHM);
            glyph.addCell(this);
        } else {
            for (QuadTree child : children) {
                child.insert(glyph, at, g);
            }
        }
    }

    /**
     * Insert a given glyph into this QuadTree, but treat it as only its center
     * and handle the insertion as a regular QuadTree insertion. This means that
     * a split may be triggered by this insertion, in order to maintain the
     * maximum capacity of cells.
     *
     * @param glyph The glyph center to insert.
     * @return Whether center has been inserted.
     */
    public boolean insertCenterOf(Glyph glyph) {
        if (!cell.contains(glyph.getX(), glyph.getY())) {
            return false;
        }
        // can we insert here?
        if (isLeaf() && glyphs.size() < MAX_GLYPHS_PER_CELL) {
            glyphs.put(glyph, InsertedWhen.INITIALLY);
            glyph.addCell(this);
            return true;
        }
        // split if necessary
        if (isLeaf()) {
            split();
        }
        // insert into one child, only one insertion will succeed
        for (QuadTree child : children) {
            if (child.insertCenterOf(glyph)) {
                break;
            }
        }
        return true;
    }

    public boolean isLeaf() {
        return (this.children == null);
    }

    public boolean isRoot() {
        return (this.parent == null);
    }

    public List<QuadTree> leaves() {
        List<QuadTree> leaves = new ArrayList<>();
        Queue<QuadTree> considering = new ArrayDeque<>();
        considering.add(this);
        while (!considering.isEmpty()) {
            QuadTree cell = considering.poll();
            if (cell.isLeaf()) {
                leaves.add(cell);
            } else {
                considering.addAll(Arrays.asList(cell.children));
            }
        }
        return leaves;
    }

    /**
     * Remove the given glyph from this cell, if it is associated with it.
     * This method does <i>not</i> remove the cell from the glyph.
     *
     * @param glyph Glyph to be removed.
     */
    public void removeGlyph(Glyph glyph) {
        glyphs.remove(glyph);
    }

    /**
     * Like {@link #removeGlyph(Glyph)}, but only if the given glyph was inserted
     * at the given time. Otherwise, the glyph is not removed and nothing changes.
     *
     * @param glyph Glyph to be removed.
     * @param when Condition under which glyph is removed.
     */
    public void removeGlyphIf(Glyph glyph, InsertedWhen when) {
        glyphs.remove(glyph, when);
    }

    /**
     * Reset state to what it was after all initially inserted glyphs were
     * there. This clears the tree and rebuilds it from scratch.
     */
    public void reset() {
        Set<Glyph> toInsert = new HashSet<>();
        for (QuadTree leaf : leaves()) {
            toInsert.addAll(leaf.getGlyphs(InsertedWhen.INITIALLY));
        }
        clear();
        for (Glyph s : toInsert) {
            insertCenterOf(s);
        }
    }

    /**
     * Performs a regular QuadTree split, treating all glyphs associated with
     * this cell as points, namely their centers. Distributes associated glyphs
     * to the relevant child cells.
     */
    public void split() {
        // already split?
        if (!isLeaf()) {
            throw new RuntimeException("Cannot split cell that is already split.");
        }
        // do the split
        this.children = new QuadTree[4];
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        for (int i = 0; i < 4; ++i) {
            this.children[i] = new QuadTree(
                    x + (i % 2 == 0 ? 0 : w / 2),
                    y + (i < 2 ? 0 : h / 2),
                    w / 2, h / 2, g
                );
            this.children[i].parent = this;
        }
        // possibly distribute glyphs
        if (!glyphs.isEmpty()) {
            for (Glyph glyph : glyphs.keySet()) {
                for (QuadTree child : children) {
                    child.insertCenterOf(glyph);
                }
            }
            // only maintain glyphs in leaves
            glyphs.clear();
        }
    }

    @Override
    public String toString() {
        return String.format("%s[cell = %.2f x %.2f %s%.2f %s%.2f]",
                getClass().getName(),
                cell.getWidth(), cell.getHeight(),
                (cell.getX() >= 0 ? "+" : ""), cell.getX(),
                (cell.getY() >= 0 ? "+" : ""), cell.getY());
    }


    /**
     * Add all leaf cells on the given side of the current cell to the given
     * set. If the this cell is a leaf, it will add itself as a whole.
     *
     * @param side Side of cell to take leaves on.
     * @param result Set to add cells to.
     */
    private void getLeaves(Side side, Set<QuadTree> result) {
        getLeaves(side, null, result);
    }

    /**
     * Add all leaf cells on the given side of the current cell to the given
     * set that intersect the range defined by extending the given rectangle to
     * the given side and its opposite direction infinitely far.
     *
     * @see QuadTree#getLeaves(Side, Set)
     */
    private void getLeaves(Side side, Rectangle2D range, Set<QuadTree> result) {
        if (range != null) {
            // reduce checking overlap to a 1D problem, as the given range and
            // this cell are extended to infinity in one dimension
            double minR, maxR, minC, maxC;
            if (side == Side.TOP || side == Side.BOTTOM) {
                minR = range.getMinX();
                maxR = range.getMaxX();
                minC = cell.getMinX();
                maxC = cell.getMaxX();
            } else {
                minR = range.getMinY();
                maxR = range.getMaxY();
                minC = cell.getMinY();
                maxC = cell.getMaxY();
            }
            // in case there is no overlap, return
            if (!(maxC >= minR && maxR >= minC)) {
                return;
            }
        }
        if (isLeaf()) {
            result.add(this);
            return;
        }
        for (int i : side.quadrants()) {
            children[i].getLeaves(side, range, result);
        }
    }

    /**
     * Actual implementation of {@link #getNeighbors(Side)}.
     *
     * @param side Side of cell to look for neighbors.
     * @param result Set to add neighbors to.
     */
    private void getNeighbors(Side side, Set<QuadTree> result) {
        if (!isRoot()) {
            int quadrant = Utils.indexOf(parent.children, this);
            Side[] desc = Side.quadrant(quadrant);
            Side[] neighbors = Side.neighborQuadrants(desc);
            if (neighbors[0] == side || neighbors[1] == side) {
                parent.children[Side.quadrantNeighbor(quadrant, side)]
                        .getLeaves(side.opposite(), result);
                return;
            }
            // need to walk up the tree, go to side, go down the tree
            QuadTree neighbor = parent.upUntil(side);
            if (neighbor == null) {
                return;
            }
            neighbor.getLeaves(side, cell, result);
        }
    }

    /**
     * Move up the tree to the parent, until a cell is found that has a neighbor
     * on the given side, as a sibling. Returns that neighbor.
     *
     * @param side Side to check for siblings.
     * @return Cell that matches criteria, or {@code null} if no such cell
     *         exists on the path to the root of the tree.
     */
    private QuadTree upUntil(Side side) {
        if (isRoot()) {
            return null;
        }
        int quadrant = Utils.indexOf(parent.children, this);
        Side[] desc = Side.quadrant(quadrant);
        Side[] neighbors = Side.neighborQuadrants(desc);
        if (neighbors[0] == side || neighbors[1] == side) {
            return parent.children[Side.quadrantNeighbor(quadrant, side)];
        }
        return parent.upUntil(side);
    }

}
