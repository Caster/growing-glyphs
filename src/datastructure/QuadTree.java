package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import datastructure.events.OutOfCell.Side;
import datastructure.growfunction.GrowFunction;
import utils.Constants.B;
import utils.Constants.D;
import utils.Constants.I;
import utils.Utils;
import utils.Utils.Timers;

/**
 * A QuadTree implementation that can track growing glyphs.
 *
 * @see Glyph
 */
public class QuadTree implements Iterable<QuadTree> {

    /**
     * Rectangle describing this cell.
     */
    private Rectangle2D cell;
    /**
     * Parent pointer. Will be {@code null} for the root cell.
     */
    private QuadTree parent;
    /**
     * Whether {@link #parent} thinks {@code this} is a child of it. The root
     * cell can never be an orphan, because it does not have a parent.
     */
    private boolean isOrphan;
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
     * Glyphs intersecting the cell.
     */
    private Set<Glyph> glyphs;
    /**
     * Listeners listening to events.
     */
    private Set<QuadTreeChangeListener> listeners;
    /**
     * Cache {@link #getNeighbors(Side)} for every side, but only for leaves.
     */
    private List<Set<QuadTree>> neighbors;


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
        this.isOrphan = false;
        this.children = null;
        this.g = g;
        this.glyphs = new HashSet<>(I.MAX_GLYPHS_PER_CELL.get());
        if (B.ENABLE_LISTENERS.get()) {
            this.listeners = new HashSet<>(1);
        } else {
            this.listeners = null;
        }
        this.neighbors = new ArrayList<>(Side.values().length);
        for (@SuppressWarnings("unused") Side side : Side.values()) {
            this.neighbors.add(new HashSet<>());
        }
    }


    /**
     * Attach a listener to this QuadTree cell and notify it of events.
     *
     * @param listener Listener to be added.
     */
    public void addListener(QuadTreeChangeListener listener) {
        if (B.ENABLE_LISTENERS.get()) {
            this.listeners.add(listener);
        }
    }

    /**
     * Reset to being a cell without glyphs nor children.
     */
    public void clear() {
        if (children != null) {
            for (QuadTree child : children) {
                child.clear();
            }
            children = null;
        }
        for (Glyph glyph : glyphs) {
            glyph.removeCell(this);
        }
        glyphs.clear();
        for (Set<QuadTree> neighborsOnSide : neighbors) {
            neighborsOnSide.clear();
        }
        if (B.ENABLE_LISTENERS.get()) {
            for (QuadTreeChangeListener listener : listeners) {
                listener.clear();
            }
        }
    }

    /**
     * Return the leaf cell in this QuadTree that contains the given point. In
     * case the point lies outside of this QuadTree, {@code null} is returned.
     *
     * @param x X-coordinate of query point.
     * @param y Y-coordinate of query point.
     */
    public QuadTree findLeafAt(double x, double y) {
        if (x < cell.getMinX() || x > cell.getMaxX() ||
                y < cell.getMinY() || y > cell.getMaxY()) {
            return null;
        }
        // already a match?
        if (isLeaf()) {
            return this;
        }
        // find correct child
        return children[Side.quadrant(cell, x, y)].findLeafAt(x, y);
    }

    public QuadTree[] getChildren() {
        return this.children;
    }

    public Set<Glyph> getGlyphs() {
        return glyphs;
    }

    public Set<Glyph> getGlyphsAlive() {
        return glyphs.stream()
                .filter(glyph -> glyph.alive)
                .collect(Collectors.toSet());
    }

    public double getHeight() {
        return cell.getHeight();
    }

    public List<QuadTree> getLeaves() {
        Timers.start("[QuadTree] getLeaves");
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
        Timers.stop("[QuadTree] getLeaves");
        return leaves;
    }

    /**
     * Returns a set of all QuadTree cells that are descendants of this cell,
     * {@link #isLeaf() leaves} and have one side touching the {@code side}
     * border of this cell.
     *
     * @param side Side of cell to find leaves on.
     */
    public Set<QuadTree> getLeaves(Side side) {
        Timers.start("[QuadTree] getLeaves");
        Set<QuadTree> result = new HashSet<>();
        getLeaves(side, result);
        Timers.stop("[QuadTree] getLeaves");
        return result;
    }

    /**
     * Return a set of all leaves of this QuadTree that intersect the given
     * glyph at the given point in time.
     *
     * @param glyph The glyph to consider.
     * @param at Timestamp/zoom level at which glyph size is determined.
     * @param g Function to determine size of glyph. Together with {@code at},
     *          this is used to decide which cells {@code glyph} intersects.
     * @see #insert(Glyph, double, GrowFunction)
     */
    public Set<QuadTree> getLeaves(Glyph glyph, double at, GrowFunction g) {
        Timers.start("[QuadTree] getLeaves");
        Set<QuadTree> result = new HashSet<>();
        getLeaves(glyph, at, g, result);
        Timers.stop("[QuadTree] getLeaves");
        return result;
    }

    /**
     * Return a set of the neighboring cells on the given side of this cell.
     *
     * @param side Side of cell to find neighbors on.
     */
    public Set<QuadTree> getNeighbors(Side side) {
        return neighbors.get(side.ordinal());
    }

    /**
     * Returns the first ancestor (parent, grandparent, ...) that is <i>not</i>
     * an {@link #isOrphan() orphan}. In case this node is not an orphan, this
     * will return a self reference.
     */
    public QuadTree getNonOrphanAncestor() {
        QuadTree node = this;
        while (node.isOrphan()) {
            node = node.parent;
        }
        return node;
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

    /**
     * Returns the number of cells that make up this QuadTree.
     */
    public int getSize() {
        if (isLeaf()) {
            return 1;
        }
        int size = 1;
        for (QuadTree child : children) {
            size += child.getSize();
        }
        return size;
    }

    /**
     * Returns the maximum number of links that need to be followed before a
     * leaf cell is reached.
     */
    public int getTreeHeight() {
        if (isLeaf()) {
            return 0;
        }
        int height = 1;
        for (QuadTree child : children) {
            int childHeight = child.getTreeHeight();
            height = Math.max(height, childHeight + 1);
        }
        return height;
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
     * @see #getLeaves(Glyph, double, GrowFunction)
     */
    public void insert(Glyph glyph, double at, GrowFunction g) {
        if (g.intersectAt(glyph, cell) > at + Utils.EPS) {
            return;
        }
        Timers.start("[QuadTree] insert");
        if (isLeaf()) {
            glyphs.add(glyph);
            glyph.addCell(this);
        } else {
            for (QuadTree child : children) {
                child.insert(glyph, at, g);
            }
        }
        Timers.stop("[QuadTree] insert");
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
        if (glyph.getX() < cell.getMinX() || glyph.getX() > cell.getMaxX() ||
                glyph.getY() < cell.getMinY() || glyph.getY() > cell.getMaxY()) {
            return false;
        }
        // can we insert here?
        Timers.start("[QuadTree] insert");
        if (isLeaf() && glyphs.size() < I.MAX_GLYPHS_PER_CELL.get()) {
            glyphs.add(glyph);
            glyph.addCell(this);
            return true;
        }
        // split if necessary
        if (isLeaf()) {
            split();
        }
        // insert into correct child
        children[Side.quadrant(cell, glyph.getX(), glyph.getY())]
                .insertCenterOf(glyph);
        Timers.stop("[QuadTree] insert");
        return true;
    }

    /**
     * Returns whether this cell is an orphan, which it is when its parent has
     * joined and forgot about its children.
     */
    public boolean isOrphan() {
        return isOrphan;
    }

    /**
     * Returns whether this cell has child cells.
     */
    public boolean isLeaf() {
        return (this.children == null);
    }

    /**
     * Returns whether this cell has a parent.
     */
    public boolean isRoot() {
        return (this.parent == null);
    }

    /**
     * {@inheritDoc}
     *
     * This iterator will iterate over all cells of the QuadTree in a top-down
     * manner: first a node, then its children.
     */
    @Override
    public Iterator<QuadTree> iterator() {
        return new Quaderator(this);
    }

    /**
     * Remove the given glyph from this cell, if it is associated with it.
     * This method does <i>not</i> remove the cell from the glyph.
     *
     * @param glyph Glyph to be removed.
     * @param at Time/zoom level at which glyph is removed. Used only to
     *           record when a join took place, if a join is triggered.
     * @return Whether removing the glyph caused this cell to merge with its
     *         siblings into its parent, making this cell an
     *         {@link #isOrphan() orphan}. If this happens, merge events may
     *         need to be updated and out of cell events may be outdated.
     */
    public boolean removeGlyph(Glyph glyph, double at) {
        glyphs.remove(glyph);
        if (parent != null) {
            return parent.joinMaybe(at);
        }
        return false;
    }

    /**
     * Performs a regular QuadTree split, treating all glyphs associated with
     * this cell as points, namely their centers. Distributes associated glyphs
     * to the relevant child cells.
     *
     * @see #split(double, GrowFunction)
     */
    public void split() {
        Timers.start("[QuadTree] split");
        splitCell();
        // possibly distribute glyphs
        if (!glyphs.isEmpty()) {
            for (Glyph glyph : glyphs) {
                children[Side.quadrant(cell, glyph.getX(), glyph.getY())]
                        .insertCenterOf(glyph);
            }
            // only maintain glyphs in leaves
            glyphs.clear();
        }
        // notify listeners
        if (B.ENABLE_LISTENERS.get()) {
            for (QuadTreeChangeListener listener : listeners) {
                listener.split(0);
            }
        }
        Timers.stop("[QuadTree] split");
    }

    /**
     * Split this cell in four and associate glyphs in this cell with the child
     * cells they overlap.
     *
     * @param at Timestamp/zoom level at which split takes place.
     * @param g Function to determine size of glyph. Together with {@code at},
     *          this is used to decide which cells a glyph intersects.
     * @see #split()
     */
    public void split(double at, GrowFunction g) {
        Timers.start("[QuadTree] split");
        splitCell();
        // possibly distribute glyphs
        if (!glyphs.isEmpty()) {
            // insert glyph in every child cell it overlaps
            // (a glyph can be inserted into more than one cell!)
            for (Glyph glyph : glyphs) {
                // don't bother with dead glyphs
                if (glyph.alive) {
                    insert(glyph, at, g);
                }
            }
            // only maintain glyphs in leaves
            glyphs.clear();
            // ensure that split did in fact have an effect
            for (QuadTree child : children) {
                if (child.glyphs.size() > I.MAX_GLYPHS_PER_CELL.get()) {
                    child.split(at, g);
                }
            }
        }
        // notify listeners
        if (B.ENABLE_LISTENERS.get()) {
            for (QuadTreeChangeListener listener : listeners) {
                listener.split(at);
            }
        }
        Timers.stop("[QuadTree] split");
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
     * Actual implementation of {@link #getLeaves(Glyph, double, GrowFunction)}.
     */
    private void getLeaves(Glyph glyph, double at, GrowFunction g, Set<QuadTree> result) {
        if (g.intersectAt(glyph, cell) > at + Utils.EPS) {
            return;
        }
        if (isLeaf()) {
            result.add(this);
        } else {
            for (QuadTree child : children) {
               child.getLeaves(glyph, at, g, result);
           }
        }
    }

    /**
     * Add all leaf cells on the given side of the current cell to the given
     * set. If this cell is a leaf, it will add itself as a whole.
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
            double[] r = new double[2];
            double[] c = new double[2];
            if (side == Side.TOP || side == Side.BOTTOM) {
                r[0] = range.getMinX();
                r[1] = range.getMaxX();
                c[0] = cell.getMinX();
                c[1] = cell.getMaxX();
            } else {
                r[0] = range.getMinY();
                r[1] = range.getMaxY();
                c[0] = cell.getMinY();
                c[1] = cell.getMaxY();
            }
            // in case there is no overlap, return
            if (!Utils.openIntervalsOverlap(r, c)) {
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
     * If the total number of glyphs of all children is at most
     * {@link #MAX_GLYPHS_PER_CELL} and those children are leaves, delete the
     * children (thus making this cell a leaf), and adopt the glyphs of the
     * deleted children in this cell.
     *
     * @param at Time/zoom level at which join takes place. Used only to record
     *           when a join happened, if one is triggered.
     * @return Whether a join was performed.
     */
    private boolean joinMaybe(double at) {
        if (isLeaf()) {
            return false;
        }
        int s = 0;
        for (QuadTree child : children) {
            if (!child.isLeaf()) {
                return false;
            }
            s += child.getGlyphsAlive().size();
        }
        if (s > I.MAX_GLYPHS_PER_CELL.get()) {
            return false;
        }

        // do a join, become a leaf, adopt glyphs and neighbors of children
        for (int quadrant = 0; quadrant < children.length; ++quadrant) {
            QuadTree child = children[quadrant];
            for (Glyph glyph : child.getGlyphsAlive()) {
                glyphs.add(glyph);
                glyph.addCell(this);
                glyph.removeCell(child);
            }
            child.glyphs.clear();
            child.isOrphan = true;
            // adopt neighbors, keep neighbors of orphan intact
            // only adopt neighbors outside of the joined cell
            for (Side side : Side.quadrant(quadrant)) {
                neighbors.get(side.ordinal()).addAll(
                    child.neighbors.get(side.ordinal()));
            }
        }
        // update neighbor pointers of neighbors; point to this instead of
        // any of what previously were our children, but are now orphans
        for (Side side : Side.values()) {
            for (QuadTree neighbor : neighbors.get(side.ordinal())) {
                Set<QuadTree> neighborNeighbors = neighbor.neighbors.get(
                    side.opposite().ordinal());
                for (QuadTree child : children) {
                    neighborNeighbors.remove(child);
                }
                // the below does not need an "interval overlaps" check; if the
                // child interval overlapped, then our interval will also
                neighborNeighbors.add(this);
            }
        }
        // we become a leaf now, sorry kids
        children = null;

        // recursively check if parent could join now
        if (parent != null) {
            parent.joinMaybe(at);
        }

        // notify listeners
        if (B.ENABLE_LISTENERS.get()) {
            for (QuadTreeChangeListener listener : listeners) {
                listener.joined(at);
            }
        }

        // since we joined, return `true` independent of whether parent joined
        return true;
    }

    /**
     * If not a leaf yet, create child cells and associate them with this cell
     * as being their parent. This method does <em>not</em> reassign any glyphs
     * associated with this cell to children.
     *
     * The {@link #neighbors} of the newly created child cells will be
     * initialized correctly. The {@link #neighbors} of this cell will be
     * cleared - only leaf cells maintain who their neighbors are.
     *
     * @see #split()
     * @see #split(double, GrowFunction)
     */
    private void splitCell() {
        // already split?
        if (!isLeaf()) {
            throw new RuntimeException("cannot split cell that is already split");
        }

        // do the split
        this.children = new QuadTree[4];
        double x = getX();
        double y = getY();
        double w = getWidth();
        double h = getHeight();
        if (w / 2 < D.MIN_CELL_SIZE.get() || h / 2 < D.MIN_CELL_SIZE.get()) {
            throw new RuntimeException("cannot split a tiny cell");
        }
        for (int i = 0; i < 4; ++i) {
            this.children[i] = new QuadTree(
                    x + (i % 2 == 0 ? 0 : w / 2),
                    y + (i < 2 ? 0 : h / 2),
                    w / 2, h / 2, g
                );
            this.children[i].parent = this;
        }

        // update neighbors of neighbors; we split now
        for (Side side : Side.values()) {
            for (QuadTree neighbor : neighbors.get(side.ordinal())) {
                Set<QuadTree> neighborsOnOurSide = neighbor.neighbors.get(
                    side.opposite().ordinal());
                neighborsOnOurSide.remove(this);
                for (int quadrant : side.quadrants()) {
                    // only add as neighbors when they actualy neighbor our
                    // newly created children
                    if (Utils.openIntervalsOverlap(
                            Side.interval(children[quadrant].cell, side),
                            Side.interval(neighbor.cell, side))) {
                        neighborsOnOurSide.add(children[quadrant]);
                    }
                }
            }
        }

        // update neighbors
        for (Side side : Side.values()) {
            Set<QuadTree> neighborsOnSide = neighbors.get(side.ordinal());
            for (int quadrant : side.quadrants()) {
                double[] qi = Side.interval(children[quadrant].cell, side);
                // distribute own neighbors over children
                children[quadrant].neighbors.get(side.ordinal()).addAll(
                    neighborsOnSide.stream()
                        // ensure that the neighbor is "in range" of the child
                        // cell; technically, we would need to consider the
                        // side.opposite() of the neighbor, but since it reduces
                        // to a 1D comparison with the exact same interval, we
                        // save ourselves some calculation and do it like this
                        .filter((neighbor) -> Utils.openIntervalsOverlap(qi,
                            Side.interval(neighbor.cell, side)))
                        .collect(Collectors.toSet())
                );
                // the children are neighbors of each other; record this
                children[quadrant].neighbors.get(side.opposite().ordinal()).add(
                    children[Side.quadrantNeighbor(quadrant, side.opposite())]);
            }
            neighborsOnSide.clear();
        }
    }


    /**
     * Iterator for QuadTrees.
     */
    private static class Quaderator implements Iterator<QuadTree> {

        private Queue<QuadTree> toVisit;


        public Quaderator(QuadTree quadTree) {
            this.toVisit = new LinkedList<>();
            this.toVisit.add(quadTree);
        }

        @Override
        public boolean hasNext() {
            return !toVisit.isEmpty();
        }

        @Override
        public QuadTree next() {
            QuadTree next = toVisit.poll();
            if (!next.isLeaf()) {
                for (QuadTree child : next.children) {
                    toVisit.add(child);
                }
            }
            return next;
        }

    }

}
