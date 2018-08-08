package utils;

import java.util.Optional;

/**
 * A variant on the Java 8 {@link Optional} that is mutable and can contain
 * {@code null} even when it is non-empty.
 *
 * @param <T> Type of the value (possibly) contained in this wrapper.
 */
public class MutableOptional<T> {

    private boolean isSet;
    private T value;

    /**
     * Construct an empty wrapper.
     */
    public MutableOptional() {
        clear();
    }

    /**
     * Construct a wrapper around the given value.
     *
     * @param value The value to be wrapped, possibly {@code null}.
     */
    public MutableOptional(T value) {
        set(value);
    }


    /**
     * Empties this wrapper.
     */
    public void clear() {
        this.isSet = false;
        this.value = null;
    }

    /**
     * Returns the value wrapped by this container.
     *
     * @throws IllegalStateException When the wrapper is empty.
     */
    public T get() throws IllegalStateException {
        if (isSet) {
            return value;
        }
        throw new IllegalStateException("cannot get the value of an empty "
                + "MutableOptional");
    }

    /**
     * Returns whether this wrapper contains a value.
     */
    public boolean isSet() {
        return isSet;
    }

    /**
     * Wraps the given value in this container.
     *
     * @param value The value to be wrapped, possibly {@code null}.
     */
    public void set(T value) {
        this.isSet = true;
        this.value = value;
    }

}
