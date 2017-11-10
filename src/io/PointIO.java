package io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import datastructure.QuadTree;
import datastructure.QuadTree.InsertedWhen;
import datastructure.Glyph;

public class PointIO {

    public static void read(File file, QuadTree tree) {
        try (Scanner reader = new Scanner(new FileInputStream(file))) {
            while (reader.hasNextDouble()) {
                tree.insertCenterOf(new Glyph(reader.nextDouble(),
                        reader.nextDouble(), reader.nextInt(10)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void write(QuadTree tree, File file) {
        try (PrintStream writer = new PrintStream(new FileOutputStream(file))) {
            for (QuadTree leaf : tree.getLeaves()) {
                for (Glyph s : leaf.getGlyphs(InsertedWhen.INITIALLY)) {
                    writer.println(s.getX() + " " + s.getY() + " " + s.getN());
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
