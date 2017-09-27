package gui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import algorithm.AgglomerativeClustering;
import algorithm.glyphgenerator.GlyphGenerator;
import algorithm.glyphgenerator.Perlin;
import algorithm.glyphgenerator.PopulationSim;
import algorithm.glyphgenerator.UniformRandom;
import datastructure.Glyph;
import datastructure.HierarchicalClustering;
import datastructure.QuadTree;
import datastructure.growfunction.GrowFunction;
import datastructure.growfunction.LinearlyGrowingSquares;
import gui.Settings.Setting;
import gui.Settings.SettingSection;
import io.PointIO;

/**
 * A debug view of {@link QuadTree QuadTrees} and growing {@link Glyph glyphs}.
 */
public class GrowingGlyphs extends JFrame {

    public static final GlyphGenerator[] GENERATORS = new GlyphGenerator[] {
            new UniformRandom(), new Perlin(), new PopulationSim()
        };
    public static final Settings SETTINGS = new Settings();
    public static final int NUM_POINTS_INITIALLY = 6;


    private AgglomerativeClustering clusterer;
    private GrowFunction g;
    private QuadTree tree;
    private HierarchicalClustering.View view;

    private DrawPanel drawPanel;
    private JFileChooser fc;
    private JLabel status;


    public GrowingGlyphs(int w, int h, GrowFunction g) {
        super("Growing Glyphs");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        this.g = g;
        this.view = null;
        this.tree = new QuadTree(
                DrawPanel.PADDING - w / 2,
                DrawPanel.PADDING - h / 2,
                w - DrawPanel.PADDING * 2,
                h - DrawPanel.PADDING * 2,
                g
            );
        this.clusterer = new AgglomerativeClustering(tree, g);
        this.drawPanel = new DrawPanel(tree, this);
        add(drawPanel, BorderLayout.CENTER);

        randomGlyphs(NUM_POINTS_INITIALLY, GENERATORS[0]);

        addKeyListener(new KeyListener());
        add(status = new JLabel("Ready. Press 'h' for help."), BorderLayout.SOUTH);
        status.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        this.fc = null;
        setJMenuBar(new Menu(this));

        pack();
        drawPanel.resetView();
    }

    public void randomGlyphs(int n, GlyphGenerator gen) {
        tree.clear();
        Glyph[] glyphs = new Glyph[n];
        gen.init(n, tree.getRectangle());
        for (int i = 0; i < n; ++i) {
            glyphs[i] = gen.next();
            tree.insertCenterOf(glyphs[i]);
        }
        drawPanel.setGlyphs(null);
        if (status != null) {
            status.setText("Loaded new random set of " + n + " glyphs.");
        }
    }

    public void setStatusText(String text) {
        status.setText(text);
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
        if (getFC().showOpenDialog(GrowingGlyphs.this) ==
                JFileChooser.APPROVE_OPTION) {
            openFile(getFC().getSelectedFile());
        }
    }

    /**
     * Read weighted point set from given file and show it in the GUI.
     *
     * @param file File to open.
     */
    private void openFile(File file) {
        tree.clear();
        PointIO.read(file, tree);
        drawPanel.setGlyphs(null);
        if (status != null) {
            status.setText("Loaded '" + file.getName() + "'.");
        }
    }

    /**
     * Ask user for number of points to generate, then do so using the given
     * glyph generator.
     *
     * @param generatorIndex Index in {@link #GENERATORS}.
     */
    private void random(int generatorIndex) {
        try {
            randomGlyphs(Integer.parseInt(
                    JOptionPane.showInputDialog(GrowingGlyphs.this,
                            "How many points?",
                            NUM_POINTS_INITIALLY)), GENERATORS[generatorIndex]);
        } catch (NumberFormatException nfe) {
            // Silly user, not typing integers when they should be.
            // We ignore them. That'll teach them. Maybe.
        }
    }

    /**
     * Execute clustering algorithm.
     *
     * @param events Ignored.
     */
    private void run(ActionEvent...events) {
        status.setText("Clustering...");
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tree.reset();
                boolean debug = SETTINGS.getBoolean(Setting.DEBUG);
                clusterer.cluster(!debug, debug, SETTINGS.getBoolean(Setting.STEP));
                if (clusterer.getClustering() != null) {
                    view = new HierarchicalClustering.View(clusterer.getClustering());
                    view.next(); // show first step that has actual glyphs
                    drawPanel.setGlyphs(view.getGlyphs(g));
                }
                status.setText("Clustering... done!");
            }
        });
    }

    /**
     * Show a save dialog, save current tree to file.
     *
     * @param events Ignored.
     */
    private void save(ActionEvent...events) {
        if (getFC().showSaveDialog(GrowingGlyphs.this) ==
                JFileChooser.APPROVE_OPTION) {
            PointIO.write(tree, getFC().getSelectedFile());
        }
    }


    public static void main(String[] args) {
        int w = 512 + DrawPanel.PADDING * 2;
        int h = w;
        File toOpen = null;
        if (args.length > 0) {
            try {
                int i = 0;
                // argument for path to open
                if (args.length > i) {
                    toOpen = new File(args[i]);
                    if (toOpen.isFile() && toOpen.canRead()) {
                        i++;
                    } else {
                        toOpen = null;
                    }
                }
                // arguments for window size
                if (args.length > i) {
                    h = w = Integer.parseInt(args[i++]);
                }
                if (args.length > i) {
                    h = Integer.parseInt(args[i++]);
                }
            } catch (Exception e) {
                // ignore, we have a default anyway
                System.err.println("Usage: java gui.GrowingGlyphs [path to open] "
                        + "[width = " + h + "] [height = width]");
            }
        }
        GrowFunction g = new LinearlyGrowingSquares();
        GrowingGlyphs gg = new GrowingGlyphs(w, h, g);
        if (toOpen != null) {
            gg.openFile(toOpen);
        }
        gg.setVisible(true);
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
                JOptionPane.showMessageDialog(GrowingGlyphs.this,
                        "<html>Press any of the following keys.<br>"
                        + "<b><code>O</code></b> - "
                            + "Open a set of glyphs from a file.<br>"
                        + "<b><code>S</code></b> - "
                            + "Save the current set to a file.<br>"
                        + "<b><code>R</code></b> - "
                            + "Load a new random set of glyphs.<br>"
                        + "<b><code>A</code></b> - "
                            + "Execute clustering algorithm.<br>"
                        + "<b><code>P</code></b> - "
                            + "Print current clustering to STDOUT.<br>"
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
            case KeyEvent.VK_P:
                System.out.println(clusterer.getClustering());
                break;
            case KeyEvent.VK_R:
                random(0);
                break;
            case KeyEvent.VK_S:
                save();
                break;
            case KeyEvent.VK_LEFT:
                if (view != null) {
                    view.previous();
                    drawPanel.setGlyphs(view.getGlyphs(g));
                }
                break;
            case KeyEvent.VK_RIGHT:
                if (view != null) {
                    view.next();
                    drawPanel.setGlyphs(view.getGlyphs(g));
                }
                break;
            case KeyEvent.VK_HOME:
                if (view != null) {
                    view.start();
                    drawPanel.setGlyphs(view.getGlyphs(g));
                }
                break;
            case KeyEvent.VK_END:
                if (view != null) {
                    view.end();
                    drawPanel.setGlyphs(view.getGlyphs(g));
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
        public Menu(GrowingGlyphs frame) {
            JMenu fileMenu = new JMenu("File");
            fileMenu.add(new MenuItem("Open", frame::open));
            fileMenu.add(new MenuItem("Save", frame::save));
            fileMenu.add(new MenuItem("Quit", (ActionEvent e) -> {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }));
            add(fileMenu);

            JMenu optionsMenu = new JMenu("Options");
            for (SettingSection section : SettingSection.values()) {
                JMenu subOptionsMenu;
                if (section == SettingSection.MISC) {
                    subOptionsMenu = optionsMenu;
                } else {
                    subOptionsMenu = new JMenu(section.getName());
                }
                for (Setting setting : Setting.booleanSettings(section)) {
                    subOptionsMenu.add(new MenuItemCheck(setting, (ActionEvent e) -> {
                        SETTINGS.toggle(setting);
                        if (setting.triggersRepaint()) {
                            frame.repaint();
                        }
                    }));
                }
                if (section != SettingSection.MISC) {
                    optionsMenu.add(subOptionsMenu);
                }
            }
            optionsMenu.addSeparator();
            optionsMenu.add(new MenuItem("Cluster", frame::run));
            add(optionsMenu);

            JMenu genMenu = new JMenu("Generate");
            for (int i = 0; i < GENERATORS.length; ++i) {
                final int ind = i;
                genMenu.add(new MenuItem(GENERATORS[i].getName(), (ActionEvent e) -> {
                    frame.random(ind);
                }));
            }
            add(genMenu);
        }
    }


    private static class MenuItem extends JMenuItem {
        public MenuItem(String name, ActionListener onClick) {
            super(name);
            addActionListener(onClick);
        }
    }


    private static class MenuItemCheck extends JCheckBoxMenuItem {
        public MenuItemCheck(Setting setting, ActionListener onClick) {
            this(setting.toString(), SETTINGS.getBoolean(setting), onClick);
        }

        public MenuItemCheck(String name, boolean initial, ActionListener onClick) {
            super(name, initial);
            addActionListener(onClick);
        }
    }

}
