package io;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import datastructure.Glyph;
import datastructure.LatLng;
import datastructure.QuadTree;
import utils.Utils;
import utils.Constants.B;
import utils.Utils.Locales;

public class CsvIO {

    private static final Logger LOGGER = (B.LOGGING_ENABLED.get() ?
            Logger.getLogger(CsvIO.class.getName()) : null);


    public static int read(File file, QuadTree tree) {
        if (B.LOGGING_ENABLED.get()) {
            LOGGER.log(Level.FINE, "ENTRY into CsvIO#read()");
        }
        Locales.push(Locale.US);
        if (B.TIMERS_ENABLED.get()) {
            Utils.Timers.start("reading file");
        }
        final int[] ignoredRead = new int[] {0, 0};
        try (Scanner reader = new Scanner(file)) {
            // read title line
            if (!reader.hasNextLine()) {
                return -1;
            }
            String titleLine = reader.nextLine();
            String splitOn = (titleLine.indexOf('\t') > 0 ? "\t" : "\\s*,\\s*");
            String[] titleCols = titleLine.split(splitOn);
            int latInd = Utils.indexOf(titleCols, "latitude");
            int lngInd = Utils.indexOf(titleCols, "longitude");
            int nInd = Utils.indexOf(titleCols, "n");
            if (latInd < 0 || lngInd < 0) {
                throw new RuntimeException("need columns 'latitude' and "
                        + "'longitude' in data");
            }
            // read data
            Map<LatLng, Integer> read = new HashMap<>();
            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                String[] cols = line.split(splitOn);
                // skip missing and null coordinates
                if (cols.length <= Math.max(latInd, lngInd) || cols[latInd].equals("NULL") ||
                        cols[lngInd].equals("NULL")) {
                    ignoredRead[0]++;
                    continue;
                }
                // parse coordinates
                try {
                    LatLng p = new LatLng(
                            Double.parseDouble(cols[latInd]),
                            Double.parseDouble(cols[lngInd])
                        );
                    // increment count for that coordinate
                    int weight = 1;
                    if (nInd >= 0) {
                        weight = Integer.parseInt(cols[nInd]);
                    }
                    if (read.containsKey(p)) {
                        read.put(p, read.get(p) + weight);
                    } else {
                        read.put(p, weight);
                    }
                    ignoredRead[1]++;
                } catch (NumberFormatException nfe) {
                    ignoredRead[0]++;
                    continue;
                }
            }
            // insert data into tree
            for (LatLng ll : read.keySet()) {
                // QuadTree is built on zoom level 1, but centered around [0, 0]
                Point2D p = ll.toPoint(1);
                tree.insertCenterOf(new Glyph(p.getX() - 256, p.getY() - 256,
                        read.get(ll), true));
            }
            if (B.LOGGING_ENABLED.get()) {
                LOGGER.log(Level.INFO, "loaded {0} locations", read.size());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if (B.TIMERS_ENABLED.get()) {
            Utils.Timers.log("reading file", LOGGER);
        }
        if (B.LOGGING_ENABLED.get()) {
            LOGGER.log(Level.INFO, "read {0} entries and ignored {1}",
                    new Object[] {ignoredRead[1], ignoredRead[0]});
        }
        Locales.pop();
        if (B.LOGGING_ENABLED.get())
            LOGGER.log(Level.FINE, "RETURN from CsvIO#read()");
        return ignoredRead[1];
    }

}
