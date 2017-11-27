package datastructure;

/**
 * Listener interface for listening to changes to {@link QuadTree QuadTrees}.
 */
public interface QuadTreeChangeListener {

    /**
     * Triggered when a QuadTree cell joined its children into itself.
     *
     * @param at When the join happened.
     */
    public void joined(double at);

    /**
     * Triggered when a QuadTree cell split itself into four cells.
     *
     * @param at When the split happened.
     */
    public void split(double at);

}
