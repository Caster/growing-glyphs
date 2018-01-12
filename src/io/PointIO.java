package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.Glyph;
import datastructure.QuadTree;
import utils.Constants.B;
import utils.Constants.I;
import utils.Utils;
import utils.Utils.Locales;

public class PointIO {

    private static final Logger LOGGER = (B.LOGGING_ENABLED.get() ?
            Logger.getLogger(PointIO.class.getName()) : null);


    public static int read(File file, QuadTree tree) {
        LOGGER.log(Level.FINE, "ENTRY into PointIO#read()");
        int sum = 0;
        Locales.push(Locale.US);
        if (B.TIMERS_ENABLED.get()) {
            Utils.Timers.start("reading file");
        }
        Set<Glyph> largest = new HashSet<>(I.LARGE_SQUARES_TRACK.get());
        int smallestLarge = Integer.MAX_VALUE;
        try (Scanner reader = new Scanner(new FileInputStream(file))) {
            while (reader.hasNextDouble()) {
                double x = reader.nextDouble();
                double y = reader.nextDouble();
                int n = reader.nextInt(10);
                Glyph glyph = new Glyph(x, y, n, true);
                if (I.LARGE_SQUARES_TRACK.get() > 0 && (
                        largest.size() < I.LARGE_SQUARES_TRACK.get() ||
                        n > smallestLarge)) {
                    if (largest.size() == I.LARGE_SQUARES_TRACK.get()) {
                        for (Glyph large : largest) {
                            if (large.getN() == smallestLarge) {
                                largest.remove(large);
                                break;
                            }
                        }
                    }
                    largest.add(glyph);
                    smallestLarge = Integer.MAX_VALUE;
                    for (Glyph large : largest) {
                        smallestLarge = Math.min(smallestLarge, large.getN());
                    }
                }
                tree.insertCenterOf(glyph);
                sum += n;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (B.TIMERS_ENABLED.get()) {
            Utils.Timers.log("reading file", LOGGER, Level.INFO);
        }
        for (Glyph large : largest) {
            large.track = true;
        }
        Locales.pop();
        LOGGER.log(Level.FINE, "RETURN from PointIO#read()");
        return sum;
    }

    public static void write(QuadTree tree, File file) {
        Locales.push(Locale.US);
        try (PrintStream writer = new PrintStream(new FileOutputStream(file))) {
            for (QuadTree leaf : tree.getLeaves()) {
                for (Glyph s : leaf.getGlyphs()) {
                    writer.println(s.getX() + " " + s.getY() + " " + s.getN());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Locales.pop();
    }

}
