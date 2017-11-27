package datastructure;

import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import datastructure.events.OutOfCell.Side;

/**
 * A historic QuadTree tracks a QuadTree over time, and can be asked to
 * reconstruct the QuadTree at a given point in time afterwards.
 *
 * This is useful for debugging purposes.
 */
public class HistoricQuadTree {

    private double at;
    private final NavigableSet<Snapshot> changes;
    private final QuadTree track;


    public HistoricQuadTree(QuadTree track) {
        this.at = 0;
        this.changes = new TreeSet<>();
        final Snapshot initial = new Snapshot(this.at, null);
        this.changes.add(initial);
        this.track = track;

        track.addListener(new QuadTreeChangeListener() {
            @Override
            public void split(double at) {
                if (at == 0) {
                    changes.remove(initial);
                }
                QuadTree[] tChildren = track.getChildren();
                HistoricQuadTree[] children =
                        new HistoricQuadTree[tChildren.length];
                for (int i = 0; i < tChildren.length; ++i) {
                    children[i] = new HistoricQuadTree(tChildren[i]);
                }
                changes.add(new Snapshot(at, children));
            }

            @Override
            public void joined(double at) {
                changes.add(new Snapshot(at, null));
            }
        });
    }

    public void at(double at) {
        this.at = at;
    }

    // forward a bunch of methods to the tracked QuadTree cell, for convenient use in DrawPanel

    public double getHeight() {
        return track.getHeight();
    }

    public Rectangle2D getRectangle() {
        return track.getRectangle();
    }

    public double getWidth() {
        return track.getWidth();
    }

    public double getX() {
        return track.getX();
    }

    public double getY() {
        return track.getY();
    }

    public boolean isRoot() {
        return track.isRoot();
    }

    // reimplement a bunch of methods from the QuadTree, taking history into account

    public HistoricQuadTree findLeafAt(double x, double y) {
        if (!track.getRectangle().contains(x, y)) {
            return null;
        }
        // already a match?
        if (isLeaf()) {
            return this;
        }
        // find correct child
        HistoricQuadTree result;
        for (HistoricQuadTree child : current().children) {
            if ((result = child.findLeafAt(x, y)) != null) {
                return result;
            }
        }
        return null;
    }

    public HistoricQuadTree[] getChildren() {
        HistoricQuadTree[] result = current().children;
        for (HistoricQuadTree child : result) {
            child.at(at);
        }
        return result;
    }

    public Set<Glyph> getGlyphs() {
        return track.getGlyphsAlive();
    }

    public Set<HistoricQuadTree> getNeighbors(Side side) {
        return new HashSet<>(0);
    }

    public boolean isLeaf() {
        return (current().children == null);
    }


    private Snapshot current() {
        return changes.floor(new Snapshot(at, null));
    }


    private class Snapshot implements Comparable<Snapshot> {

        private final double at;
        private final HistoricQuadTree[] children;

        private Snapshot(double at, HistoricQuadTree[] children) {
            this.at = at;
            this.children = children;
        }

        @Override
        public int compareTo(Snapshot that) {
            return (int) Math.signum(this.at - that.at);
        }

    }

}
