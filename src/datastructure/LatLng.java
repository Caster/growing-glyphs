package datastructure;

import java.awt.geom.Point2D;

import algorithm.WebMercator;

public class LatLng {

    public double lat;
    public double lng;


    public LatLng(double lat, double lng) {
        this.lat = lat;
        this.lng = lng;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(lat);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lng);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LatLng other = (LatLng) obj;
        if (Double.doubleToLongBits(lat) != Double.doubleToLongBits(other.lat))
            return false;
        if (Double.doubleToLongBits(lng) != Double.doubleToLongBits(other.lng))
            return false;
        return true;
    }

    public Point2D toPoint(int zoom) {
        return WebMercator.latLngToPoint(this, zoom);
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + lat + ", " + lng + "]";
    }

}
