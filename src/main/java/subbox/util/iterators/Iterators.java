package subbox.util.iterators;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Iterators {

    private Iterators() {
    }

    static class PeekingIter<T> implements PeekingIterator<T> {

        @NotNull
        private final Iterator<? extends T> iterator;

        private boolean hasBuffered;
        private T buffered;

        PeekingIter(@NotNull Iterator<? extends T> iterator) {
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

        @Override
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

    @NotNull
    public static <T> PeekingIterator<T> peekingIterator(@NotNull Iterator<? extends T> iterator) {
        return new PeekingIter<>(iterator);
    }

    static class MergingIter<T> implements Iterator<T> {

        @NotNull
        private final PeekingIterator<? extends T> left;
        @NotNull
        private final PeekingIterator<? extends T> right;
        @NotNull
        private final Comparator<? super T> comparator;

        MergingIter(@NotNull Iterator<? extends T> left,
                    @NotNull Iterator<? extends T> right,
                    @NotNull Comparator<? super T> comparator) {
            this.left = peekingIterator(left);
            this.right = peekingIterator(right);
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

    @NotNull
    public static <T> Iterator<T> mergeSorted(@NotNull Iterator<? extends T> left,
                                              @NotNull Iterator<? extends T> right,
                                              @NotNull Comparator<? super T> comparator) {
        return new MergingIter<>(left, right, comparator);
    }
}
