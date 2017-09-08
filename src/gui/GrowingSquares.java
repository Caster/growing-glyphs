package gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import algorithm.AgglomerativeClustering;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.Square;
import datastructure.growfunction.GrowFunction;
import datastructure.growfunction.LinearlyGrowingSquares;
import gui.Settings.Setting;
import io.PointIO;

/**
 * A debug view of {@link QuadTree QuadTrees} and growing {@link Square squares}.
 */
public class GrowingSquares extends JFrame {

    public static final Settings SETTINGS = new Settings();
    public static final int NUM_POINTS_INITIALLY = 6;


    private AgglomerativeClustering clusterer;
    private GrowFunction g;
    private QuadTree tree;
    private Random r;
    private HierarchicalClustering.View view;

    private DrawPanel drawPanel;
    private JFileChooser fc;
    private JLabel status;


    public GrowingSquares(int w, int h, GrowFunction g) {
        super("Growing Squares");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setLayout(new BorderLayout());

        this.g = g;
        this.view = null;
        this.tree = new QuadTree(DrawPanel.PADDING, DrawPanel.PADDING,
                w - DrawPanel.PADDING * 2, h - DrawPanel.PADDING * 2, g);
        this.clusterer = new AgglomerativeClustering(tree, g);
        this.drawPanel = new DrawPanel(tree);
        add(drawPanel, BorderLayout.CENTER);

        this.r = new Random();
        randomSquares(NUM_POINTS_INITIALLY);

        addKeyListener(new KeyListener());
        add(status = new JLabel("Ready. Press 'h' for help."), BorderLayout.SOUTH);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        this.fc = null;
        setJMenuBar(new Menu(this));

        pack();
    }

    public void randomSquares(int n) {
        tree.clear();
        Square[] squares = new Square[n];
        for (int i = 0; i < n; ++i) {
            squares[i] = new Square(
                    r.nextDouble() * tree.getWidth() + tree.getX(),
                    r.nextDouble() * tree.getHeight() + tree.getY(),
                    r.nextInt(10) + 1
                );
            tree.insertCenterOf(squares[i]);
        }
        drawPanel.setSquares(null);
        if (status != null) {
            status.setText("Loaded new random set of " + n + " squares.");
        }
    }


    private JFileChooser getFC() {
        if (fc == null) {
            fc = new JFileChooser();
        }
        return fc;
    }

    /**
     * Show an open dialog, open that file.
     *
     * @param events Ignored.
     */
    private void open(ActionEvent...events) {
        if (getFC().showOpenDialog(GrowingSquares.this) ==
                JFileChooser.APPROVE_OPTION) {
            tree.clear();
            PointIO.read(getFC().getSelectedFile(), tree);
            drawPanel.setSquares(null);
            if (status != null) {
                status.setText("Loaded '" + getFC().getSelectedFile().getName() + "'.");
            }
        }
    }

    /**
     * Execute clustering algorithm.
     *
     * @param events Ignored.
     */
    private void run(ActionEvent...events) {
        tree.reset();
        boolean debug = SETTINGS.getBoolean(Setting.DEBUG);
        clusterer.cluster(!debug, debug);
        if (clusterer.getClustering() != null) {
            view = new HierarchicalClustering.View(clusterer.getClustering());
            view.next(); // show first step that has actual squares
            drawPanel.setSquares(view.getSquares(g));
        }
    }

    /**
     * Show a save dialog, save current tree to file.
     *
     * @param events Ignored.
     */
    private void save(ActionEvent...events) {
        if (getFC().showSaveDialog(GrowingSquares.this) ==
                JFileChooser.APPROVE_OPTION) {
            PointIO.write(tree, getFC().getSelectedFile());
        }
    }


    public static void main(String[] args) {
        int w = 600;
        int h = w;
        if (args.length > 0) {
            try {
                h = w = Integer.parseInt(args[0]);
                if (args.length > 1) {
                    h = Integer.parseInt(args[1]);
                }
            } catch (NumberFormatException nfe) {
                // ignore, we have a default anyway
                System.err.println("Usage: java gui.GrowingSquares [width = 600] "
                        + "[height = width]");
            }
        }
        GrowFunction g = new LinearlyGrowingSquares();
        (new GrowingSquares(w, h, g)).setVisible(true);
    }


    private class KeyListener implements java.awt.event.KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {}

        @Override
        public void keyReleased(KeyEvent e) {
            switch (e.getKeyCode()) {
            case KeyEvent.VK_A:
                run();
                break;
            case KeyEvent.VK_H:
                JOptionPane.showMessageDialog(GrowingSquares.this,
                        "<html>Press any of the following keys.<br>"
                        + "<b><code>O</code></b> - "
                            + "Open a set of squares from a file.<br>"
                        + "<b><code>S</code></b> - "
                            + "Save the current set to a file.<br>"
                        + "<b><code>R</code></b> - "
                            + "Load a new random set of squares.<br>"
                        + "<b><code>A</code></b> - "
                            + "Execute clustering algorithm.<br>"
                        + "<b><code>←</code></b> - "
                            + "View previous step of clustering.<br>"
                        + "<b><code>→</code></b> - "
                            + "View next step of clustering.<br>"
                        + "<b><code>home</code></b> - "
                            + "View first step of clustering.<br>"
                        + "<b><code>end</code></b> - "
                            + "View last step of clustering.<br><br>"
                        + "Drag the mouse and scroll to pan and zoom.");
                break;
            case KeyEvent.VK_O:
                open();
                break;
            case KeyEvent.VK_R:
                try {
                    randomSquares(Integer.parseInt(
                            JOptionPane.showInputDialog(GrowingSquares.this,
                                    "How many points?",
                                    NUM_POINTS_INITIALLY)));
                } catch (NumberFormatException nfe) {
                    // Silly user, not typing integers when they should be.
                    // We ignore them. That'll teach them. Maybe.
                }
                break;
            case KeyEvent.VK_S:
                save();
                break;
            case KeyEvent.VK_LEFT:
                if (view != null) {
                    view.previous();
                    drawPanel.setSquares(view.getSquares(g));
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (view != null) {
                    view.next();
                    drawPanel.setSquares(view.getSquares(g));
                }
                break;
            case KeyEvent.VK_HOME:
                if (view != null) {
                    view.start();
                    drawPanel.setSquares(view.getSquares(g));
                }
                break;
            case KeyEvent.VK_END:
                if (view != null) {
                    view.end();
                    drawPanel.setSquares(view.getSquares(g));
                }
                break;
            case KeyEvent.VK_SPACE:
                drawPanel.resetView();
                break;
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {}
    }


    private static class Menu extends JMenuBar {
        public Menu(GrowingSquares frame) {
            JMenu fileMenu = new JMenu("File");
            fileMenu.add(new MenuItem("Open", frame::open));
            fileMenu.add(new MenuItem("Save", frame::save));
            fileMenu.add(new MenuItem("Quit", (ActionEvent e) -> {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }));
            add(fileMenu);

            JMenu optionsMenu = new JMenu("Options");
            optionsMenu.add(new MenuItemCheck("Debug", (ActionEvent e) -> {
                SETTINGS.toggle(Setting.DEBUG);
            }));
            optionsMenu.addSeparator();
            optionsMenu.add(new MenuItem("Cluster", frame::run));
            add(optionsMenu);
        }
    }


    private static class MenuItem extends JMenuItem {
        public MenuItem(String name, ActionListener onClick) {
            super(name);
            addActionListener(onClick);
        }
    }


    private static class MenuItemCheck extends JCheckBoxMenuItem {
        public MenuItemCheck(String name, ActionListener onClick) {
            super(name, SETTINGS.getBoolean(Setting.DEBUG));
            addActionListener(onClick);
        }
    }

}
