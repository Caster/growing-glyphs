package algorithm;

import java.awt.geom.Point2D;

import datastructure.LatLng;

/**
 * Functions for projecting and unprojecting coordinates and points using the
 * Web Mercator projection. The code in this class is based directly off of the
 * code in the Leaflet library (@url http://leafletjs.com/).
 *
 * @url https://github.com/Leaflet/Leaflet/blob/master/src/geo/projection/Projection.SphericalMercator.js
 *
 * This class provides a spherical mercator projection, which is used by the
 * `EPSG:3857` CRS and most online maps.
 *
 * Points are in a space where the origin is in the upper left corner. Thus, the
 * tile on the lowest zoom level (z = 0) looks as follows.
 *
 *
 *                               longitude > >
 *
 *   (0, 0)                (128, 0)               (256, 0)
 *          +-----------------+-----------------+
 *          |                 |                 |
 *          |                 |                 |
 *          |                 |                 |
 *          |                 |                 |     ^
 *          |                 |                 |     ^
 *          |                 |                 |  latitude
 *          |                 |                 |
 *          |                 | (128, 128)      |
 * (0, 128) +-----------------+-----------------+ (256, 128)
 *          |                 | (0LNG, 0LAT)    |
 *          |                 |                 |
 *          |                 |                 |
 *          |                 |                 |
 *          |                 |                 |
 *          |                 |                 |
 *          |                 |                 |
 *          |                 |                 |
 *          +-----------------+-----------------+
 * (0, 256)               (128, 256)              (256, 256)
 */
public class WebMercator {

    private static final double R = 6378137.0;
    private static final double TRANSFORM_SCALE = (0.5 / (Math.PI * R));


    /**
     * Projects coordinates to a 2D point using spherical Mercator projection.
     */
    public static Point2D latLngToPoint(LatLng ll, int zoom) {
        Point2D projectedPoint = project(ll.lat, ll.lng);
        return transform(projectedPoint, scale(zoom));
    }

    /**
     * Unprojects a 2D point to coordinates using spherical Mercator projection.
     */
    public static LatLng pointToLatLng(Point2D p, int zoom) {
        Point2D untransformedPoint = untransform(p, scale(zoom));
        return unproject(untransformedPoint);
    }


    private static int scale(int zoom) {
        // 256 * 2^zoom = 2^8 * 2^zoom = 2^(8 + zoom)
        return 1 << (8 + zoom);
    }

    private static Point2D project(double lat, double lng) {
        double d = Math.PI / 180.0;
        double max = 1 - 1E-15;
        double sin = Math.max(Math.min(Math.sin(lat * d), max), -max);
        return new Point2D.Double(
            WebMercator.R * lng * d,
            WebMercator.R * Math.log((1 + sin) / (1 - sin)) / 2
        );
    }

    private static LatLng unproject(Point2D p) {
        double d = 180.0 / Math.PI;
        return new LatLng(
            (2 * Math.atan(Math.exp(p.getY() / WebMercator.R)) - (Math.PI / 2)) * d,
            p.getX() * d / WebMercator.R
        );
    }

    /*
     * The below transformation functions are based off of the Leaflet
     * transformation with parameters [ WebMercator.TRANSFORM_SCALE, 0.5,
     *                                 -WebMercator.TRANSFORM_SCALE, 0.5].
     */

    private static Point2D transform(Point2D p, int scale) {
        return new Point2D.Double(
            scale * (WebMercator.TRANSFORM_SCALE * p.getX() + 0.5),
            scale * (-WebMercator.TRANSFORM_SCALE * p.getY() + 0.5)
        );
    }

    private static Point2D untransform(Point2D p, int scale) {
        double dScale = scale;
        return new Point2D.Double(
            (p.getX() / dScale - 0.5) / WebMercator.TRANSFORM_SCALE,
            (p.getY() / dScale - 0.5) / -WebMercator.TRANSFORM_SCALE
        );
    }

}
