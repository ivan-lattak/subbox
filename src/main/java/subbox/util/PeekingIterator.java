package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class PeekingIterator<T> implements Iterator<T> {

    @NotNull
    private final Iterator<T> iterator;

    private boolean hasBuffered;
    private T buffered;

    public PeekingIterator(@NotNull Iterator<T> iterator) {
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return hasBuffered || iterator.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (hasBuffered) {
            hasBuffered = false;
            return buffered;
        }

        return iterator.next();
    }

    public T peek() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (!hasBuffered) {
            hasBuffered = true;
            buffered = iterator.next();
        }

        return buffered;
    }

}
