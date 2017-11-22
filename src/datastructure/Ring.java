package datastructure;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Ring<E> implements Collection<E> {

    /**
     * Data elements. All of type {@code E}, even though it is not possible to
     * create arrays with generics, so the compiler doesn't know that.
     */
    private Object[] data;
    /**
     * Number of elements in the collection.
     */
    private int size;
    /**
     * Index of first element.
     */
    private int start;


    /**
     * Construct a new Ring with the given capacity and no elements.
     *
     * @param capacity Maximum capacity.
     */
    public Ring(int capacity) {
        this.data = new Object[capacity];
        this.size = 0;
        this.start = 0;
    }

    public Ring(List<E> initialValues) {
        this(initialValues.size());
        addAll(initialValues);
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return (size == 0);
    }

    @Override
    public boolean contains(Object o) {
        for (Iterator<E> it = iterator(); it.hasNext();) {
            E e = it.next();
            if (e.equals(o)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public E get(int index) {
        if (0 > index || index >= size) {
            throw new ArrayIndexOutOfBoundsException(index);
        }
        return (E) data[(start + index) % data.length];
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int current = start - 1;

            @Override
            public boolean hasNext() {
                return (current + 1 < start + size);
            }

            @Override
            @SuppressWarnings("unchecked")
            public E next() {
                current = (current + 1) % data.length;
                return (E) data[current];
            }
        };
    }

    public E pop() {
        E e = shift();
        if (size > 0) {
            size--;
        }
        return e;
    }

    public void set(int index, E value) {
        if (0 <= index && index < size) {
            data[(start + index) % data.length] = value;
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    @SuppressWarnings("unchecked")
    public E shift() {
        if (size > 0) {
            start = (start + 1) % data.length;
        }
        return (E) data[(start - 1 + data.length) % data.length];
    }

    @Override
    public Object[] toArray() {
        Object[] result = new Object[size];
        for (int i = 0; i < size; ++i) {
            result[i] = data[(start + i) % data.length];
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        if (a.length < size) {
            return (T[]) toArray();
        }
        for (int i = 0; i < size; ++i) {
            a[i] = (T) data[(start + i) % data.length];
        }
        if (a.length > size)
            a[size] = null;
        return a;
    }

    @Override
    public boolean add(E e) {
        data[(start + size) % data.length] = e;
        if (size < data.length) {
            size++;
        } else {
            start++;
        }
        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        for (E e : c) {
            add(e);
        }
        return true;
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        start = 0;
        size = 0;
    }

}
