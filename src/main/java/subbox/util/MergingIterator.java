package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class MergingIterator<T> implements Iterator<T> {

    @NotNull
    private final PeekingIterator<? extends T> left;
    @NotNull
    private final PeekingIterator<? extends T> right;
    @NotNull
    private final Comparator<? super T> comparator;

    public MergingIterator(@NotNull Iterator<? extends T> left,
                           @NotNull Iterator<? extends T> right,
                           @NotNull Comparator<? super T> comparator) {
        this.left = new PeekingIterator<>(left);
        this.right = new PeekingIterator<>(right);
        this.comparator = comparator;
    }

    @Override
    public boolean hasNext() {
        return left.hasNext() || right.hasNext();
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        if (!left.hasNext()) {
            return right.next();
        }

        if (!right.hasNext()) {
            return left.next();
        }

        T leftElement = left.peek();
        T rightElement = right.peek();
        if (comparator.compare(leftElement, rightElement) < 0) {
            return left.next();
        }
        return right.next();
    }

}
