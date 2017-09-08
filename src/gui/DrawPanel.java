package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

import javax.swing.JPanel;

import datastructure.QuadTree;
import datastructure.QuadTree.InsertedWhen;
import datastructure.Square;

/**
 * Panel that draws a {@link QuadTree} and the {@link Square squares} inside of it.
 */
public class DrawPanel extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {

    public static final int MARK_RADIUS = 3;
    public static final double MIN_ZOOM = 0.1;
    public static final int PADDING = 10;


    /**
     * QuadTree that is shown.
     */
    private QuadTree tree;
    /**
     * Squares that are shown on top of the QuadTree.
     */
    private Rectangle2D[] squares;
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


    public DrawPanel(QuadTree tree) {
        this.tree = tree;
        this.squares = null;
        this.dragStart = null;
        this.translation = new Point2D.Double();
        resetView();

        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension((int) tree.getWidth() + PADDING * 2,
                (int) tree.getHeight() + PADDING * 2);
    }

    public Rectangle2D[] getSquares() {
        return squares;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke((float) (2 / zoom)));

        // background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        // transform for panning and zooming
        double w = getWidth();
        double h = getHeight();
        g2.translate(translation.x, translation.y);
        g2.scale(zoom, zoom);
        g2.translate((w / zoom - w) / 2.0, (h / zoom - h) / 2.0);

        // QuadTree
        Queue<QuadTree> toDraw = new ArrayDeque<>();
        toDraw.add(tree);
        double r = MARK_RADIUS / zoom;
        while (!toDraw.isEmpty()) {
            g.setColor(Color.GRAY);
            QuadTree cell = toDraw.poll();
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
            g.setColor(Color.BLACK);
            for (Square s : cell.getSquares(InsertedWhen.INITIALLY)) {
                g2.fill(new Rectangle2D.Double(
                        s.getX() - r,
                        s.getY() - r,
                        r * 2, r * 2
                    ));
            }
            if (!cell.isLeaf()) {
                toDraw.addAll(Arrays.asList(cell.getChildren()));
            }
        }

        // squares
        if (squares != null) {
            for (Rectangle2D rect : squares) {
                g2.draw(rect);
            }
        }
    }

    public void resetView() {
        this.translation.setLocation(0, 0);
        this.zoom = 1.0;
        repaint();
    }

    public void setSquares(Rectangle2D[] squares) {
        this.squares = squares;
        repaint();
    }


    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        zoom -= e.getPreciseWheelRotation() / 10;
        if (zoom < MIN_ZOOM) {
            zoom = MIN_ZOOM;
        }
        repaint();
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
    public void mouseMoved(MouseEvent e) {}


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
