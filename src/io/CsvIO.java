package io;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import datastructure.Glyph;
import datastructure.LatLng;
import datastructure.QuadTree;
import datastructure.Utils;

public class CsvIO {

    public static void read(File file, QuadTree tree) {
        try (Scanner reader = new Scanner(file)) {
            // read title line
            if (!reader.hasNextLine()) {
                return;
            }
            String line = reader.nextLine();
            String splitOn = (line.indexOf('\t') > 0 ? "\t" : "\\s*,\\s*");
            String[] cols = line.split(splitOn);
            int latInd = Utils.indexOf(cols, "latitude");
            int lngInd = Utils.indexOf(cols, "longitude");
            if (latInd < 0 || lngInd < 0) {
                throw new RuntimeException("need columns 'latitude' and "
                        + "'longitude' in data");
            }
            // read data
            Map<LatLng, Integer> read = new HashMap<>();
            while (reader.hasNextLine()) {
                line = reader.nextLine();
                cols = line.split(splitOn);
                // skip missing and null coordinates
                if (cols.length <= Math.max(latInd, lngInd) || cols[latInd].equals("NULL") ||
                        cols[lngInd].equals("NULL")) {
                    continue;
                }
                // parse coordinates
                LatLng p = new LatLng(
                        Double.parseDouble(cols[latInd]),
                        Double.parseDouble(cols[lngInd])
                    );
                // increment count for that coordinate
                if (read.containsKey(p)) {
                    read.put(p, read.get(p) + 1);
                } else {
                    read.put(p, 1);
                }
            }
            // insert data into tree
            for (LatLng ll : read.keySet()) {
                // QuadTree is built on zoom level 1 for now, but centered around [0, 0]
                Point2D p = ll.toPoint(1);
                tree.insertCenterOf(new Glyph(p.getX() - 256, p.getY() - 256, read.get(ll)));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
