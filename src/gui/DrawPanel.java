package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;

import javax.swing.JPanel;

import datastructure.Glyph;
import datastructure.HistoricQuadTree;
import datastructure.QuadTree;
import datastructure.events.OutOfCell.Side;
import gui.Settings.Setting;
import utils.Constants.D;
import utils.Constants.I;

/**
 * Panel that draws a {@link QuadTree} and the {@link Glyph glyphs} inside of it.
 */
public class DrawPanel extends JPanel implements
        MouseListener, MouseMotionListener, MouseWheelListener {

    /**
     * Parent frame.
     */
    private GrowingGlyphs parent;
    /**
     * QuadTree that is shown.
     */
    private HistoricQuadTree tree;
    /**
     * Cache for map tiles.
     */
    private TileImageCache cache;
    /**
     * Glyphs that are shown on top of the QuadTree.
     */
    private GlyphShape[] glyphs;
    /**
     * Glyph that is to be highlighted. Can be {@code null} when no glyph is
     * highlighted at the moment.
     */
    private Glyph highlightedGlyph;
    /**
     * Point where mouse started dragging. {@code null} when no drag is occurring.
     */
    private Point2D.Double dragStart;
    /**
     * Translation of scene.
     */
    private Point2D.Double translation;
    /**
     * Zoom factor of scene.
     */
    private double zoom;


    public DrawPanel(HistoricQuadTree tree, GrowingGlyphs parent) {
        this.parent = parent;
        this.tree = tree;
        this.cache = new TileImageCache();
        this.glyphs = null;
        this.highlightedGlyph = null;
        this.dragStart = null;
        this.translation = new Point2D.Double();
        resetView();

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        setFocusable(true);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) tree.getWidth() + I.PADDING.get() * 2,
                (int) tree.getHeight() + I.PADDING.get() * 2);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setStroke(new BasicStroke((float) (2 / zoom)));

        // background
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // transform for panning and zooming
        g2.translate(translation.x + getWidth() / 2,
                translation.y + getHeight() / 2);
        g2.scale(zoom, zoom);

        // map in background
        if (GrowingGlyphs.SETTINGS.getBoolean(Setting.DRAW_MAP)) {
            int z = (int) Math.max(0, Math.floor(Math.log(zoom + 1) / Math.log(2)));
            int l = 1 << z;
            // determine viewport in viewspace coordinates
            Rectangle2D.Double viewport = new Rectangle2D.Double();
            Point2D p = new Point2D.Double();
            toViewSpace(p);
            viewport.x = p.getX();
            viewport.y = p.getY();
            p.setLocation(getWidth(), getHeight());
            toViewSpace(p);
            viewport.width = p.getX() - viewport.x;
            viewport.height = p.getY() - viewport.y;
            // work with copy of tree bbox, scale to correct size, translate in loop
            Rectangle2D rect = tree.getRectangle();
            double rx = rect.getX(); // save coords of root top left coordinate
            double ry = rect.getY();
            double rw = rect.getWidth();
            double s = rw / l; // assumes square bbox
            // find approximate range of tiles to iterate
            int xMin = Math.max(0, (int) Math.floor(l * (viewport.x - rx) / rw));
            int xMax = Math.min(l, (int) Math.ceil(l * (viewport.getMaxX() - rx) / rw));
            int yMin = Math.max(0, (int) Math.floor(l * (viewport.y - ry) / rw));
            int yMax = Math.min(l, (int) Math.ceil(l * (viewport.getMaxY() - ry) / rw));
            // now draw all tiles in the correct locations
            AffineTransform t = new AffineTransform();
            for (int x = xMin; x < xMax; ++x) {
                for (int y = yMin; y < yMax; ++y) {
                    t.setToIdentity();
                    t.translate(rx + x * s, ry + y * s);
                    t.scale(s / 512, s / 512);
                    try {
                        BufferedImage tile = cache.get(x, y, z);
                        g2.drawImage(tile, t, null);
                    } catch (IOException e) {
                        // well, then we don't draw the image
                        continue;
                    }
                }
            }
        }

        // QuadTree
        if (GrowingGlyphs.SETTINGS.getBoolean(Setting.DRAW_CELLS) ||
                GrowingGlyphs.SETTINGS.getBoolean(Setting.DRAW_CENTERS)) {
            Queue<HistoricQuadTree> toDraw = new ArrayDeque<>();
            toDraw.add(tree);
            double r = I.MARK_RADIUS.get() / zoom;
            while (!toDraw.isEmpty()) {
                HistoricQuadTree cell = toDraw.poll();
                // cell outlines
                if (GrowingGlyphs.SETTINGS.getBoolean(Setting.DRAW_CELLS)) {
                    g2.setColor(Color.GRAY);
                    if (cell.isRoot()) {
                        g2.draw(cell.getRectangle());
                    }
                    if (!cell.isLeaf()) {
                        g2.draw(new Line2D.Double(
                                cell.getX() + cell.getWidth() / 2,
                                cell.getY(),
                                cell.getX() + cell.getWidth() / 2,
                                cell.getY() + cell.getHeight()
                            ));
                        g2.draw(new Line2D.Double(
                                cell.getX(),
                                cell.getY() + cell.getHeight() / 2,
                                cell.getX() + cell.getWidth(),
                                cell.getY() + cell.getHeight() / 2
                            ));
                    }
                }
                // glyphs in cells (only the centers!)
                if (GrowingGlyphs.SETTINGS.getBoolean(Setting.DRAW_CENTERS) &&
                        cell.getGlyphs() != null) {
                    for (Glyph s : cell.getGlyphs()) {
                        g2.setColor(s == highlightedGlyph ? Color.RED : Color.BLACK);
                        g2.fill(new Rectangle2D.Double(
                                s.getX() - r,
                                s.getY() - r,
                                r * 2, r * 2
                            ));
                    }
                }
                // recursively draw children
                if (!cell.isLeaf()) {
                    toDraw.addAll(Arrays.asList(cell.getChildren()));
                }
            }
        }

        // glyphs (actual shapes)
        if (GrowingGlyphs.SETTINGS.getBoolean(Setting.DRAW_GLYPHS) &&
                glyphs != null) {
            g2.setColor(Color.BLACK);
            for (GlyphShape glyph : glyphs) {
                Area border = new Area(glyph.shapeWithBorder);
                border.subtract(new Area(glyph.shape));
                g2.fill(border);
            }
        }
    }

    public void resetView() {
        this.translation.setLocation(0, 0);
        this.zoom = Math.max(Math.min(
                (getWidth() - 2 * I.PADDING.get()) / tree.getWidth(),
                (getHeight() - 2 * I.PADDING.get()) / tree.getHeight()
            ), D.MIN_ZOOM.get());
        repaint();
    }

    public void setGlyphs(GlyphShape[] glyphs) {
        this.glyphs = glyphs;
        repaint();
    }

    /**
     * Update location of a point in screen space to its projected location in
     * view space. Useful for mouse events and determining viewport.
     */
    public void toViewSpace(Point2D p) {
        p.setLocation(
                (p.getX() - translation.x - getWidth() / 2) / zoom,
                (p.getY() - translation.y - getHeight() / 2) / zoom
            );
    }


    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double oldZoom = zoom;
        Point p = e.getPoint();
        toViewSpace(p); // use current zoom level to determine view space coords
        // update zoom level
        int r = e.getWheelRotation();
        boolean zoomIn = (r < 0);
        double factor = (zoomIn ? 1.1 : 1 / 1.1);
        zoom *= Math.pow(factor, Math.abs(r));
        // enforce minimum zoom level
        if (zoom < D.MIN_ZOOM.get()) {
            zoom = D.MIN_ZOOM.get();
        }
        // update translation to keep point under cursor the same, repaint
        if (oldZoom != zoom) {
            double change = zoom - oldZoom;
            translation.x -= change * p.getX();
            translation.y -= change * p.getY();
            repaint();
        }
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        translation.setLocation(
                translation.getX() + e.getX() - dragStart.getX(),
                translation.getY() + e.getY() - dragStart.getY()
            );
        dragStart.setLocation(e.getPoint());
        repaint();
    }


    @Override
    public void mouseMoved(MouseEvent e) {
        if (GrowingGlyphs.SETTINGS.getBoolean(Setting.SHOW_COORDS)) {
            Point p = e.getPoint();
            toViewSpace(p);
            // find closest glyph
            HistoricQuadTree leaf = tree.findLeafAt(p.getX(), p.getY());
            String extra = "";
            if (leaf != null) {
                Glyph closest = null;
                double minDist = Double.MAX_VALUE;
                // also search neighboring cells of leaf, nearest point may be there
                List<HistoricQuadTree> nodes = new ArrayList<>();
                nodes.add(leaf);
                for (Side side : Side.values()) {
                    nodes.addAll(leaf.getNeighbors(side));
                }
                for (HistoricQuadTree node : nodes) {
                    for (Glyph glyph : node.getGlyphs()) {
                        double d;
                        if ((d = p.distanceSq(glyph.getX(), glyph.getY())) < minDist) {
                            minDist = d;
                            closest = glyph;
                        }
                    }
                }
                if (closest != highlightedGlyph) {
                    highlightedGlyph = closest;
                    repaint();
                }
                // add to status when a point is found
                if (closest != null) {
                    extra = String.format(", highlighted glyph at [%.2f, %.2f]",
                        closest.getX(), closest.getY());
                }
            } else if (highlightedGlyph != null) {
                highlightedGlyph = null;
                repaint();
            }
            // update status
            parent.setStatusText(String.format(
                "Cursor at [%.0f, %.0f]%s", p.getX(), p.getY(), extra));
        }
    }


    @Override
    public void mouseClicked(MouseEvent e) {}


    @Override
    public void mouseEntered(MouseEvent e) {}


    @Override
    public void mouseExited(MouseEvent e) {}


    @Override
    public void mousePressed(MouseEvent e) {
        dragStart = new Point2D.Double(e.getX(), e.getY());
    }


    @Override
    public void mouseReleased(MouseEvent e) {
        dragStart = null;
    }

}
