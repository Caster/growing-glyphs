package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Locale;
import java.util.Scanner;

import datastructure.Glyph;
import datastructure.QuadTree;
import utils.Utils.Locales;

public class PointIO {

    public static void read(File file, QuadTree tree) {
        Locales.push(Locale.US);
        try (Scanner reader = new Scanner(new FileInputStream(file))) {
            while (reader.hasNextDouble()) {
                tree.insertCenterOf(new Glyph(reader.nextDouble(),
                        reader.nextDouble(), reader.nextInt(10), true));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Locales.pop();
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
