package gui;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class TileImageCache {

    public static final int CAPACITY = 256;


    private Map<String, BufferedImage> cache;
    private Deque<String> keys;


    public TileImageCache() {
        this.cache = new HashMap<>();
        this.keys = new ArrayDeque<>(CAPACITY);
    }

    public BufferedImage get(int x, int y, int z) throws MalformedURLException, IOException {
        String url = url(x, y, z);
        // return image, when in cache
        if (cache.containsKey(url)) {
            return cache.get(url);
        }
        // ensure that we don't overrun our capacity
        if (keys.size() == CAPACITY) {
            cache.remove(keys.pop());
        }
        // fetch image, add it into the cache and return it
        BufferedImage tile = ImageIO.read(new URL(url));
        cache.put(url, tile);
        keys.addLast(url);
        return tile;
    }


    private String url(int x, int y, int z) {
        return String.format("http://a.basemaps.cartocdn.com/light_all/%d/"
                + "%d/%d@2x.png", z, x, y);
    }

}
