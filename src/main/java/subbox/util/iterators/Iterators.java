package subbox.util.iterators;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Iterators {

    private Iterators() {
    }

    @NotNull
    @SuppressWarnings("WeakerAccess")
    public static <T> PeekingIterator<T> peekingIterator(@NotNull Iterator<? extends T> iterator) {
        return new PeekingIter<>(iterator);
    }

    private static class PeekingIter<T> implements PeekingIterator<T> {
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
    public static <T> Iterator<T> mergeSorted(@NotNull List<? extends Iterator<T>> iterators,
                                              @NotNull Comparator<? super T> comparator) {
        if (iterators.isEmpty()) {
            return empty();
        }

        List<? extends Iterator<T>> iters = iterators;
        while (iters.size() > 1) {
            List<Iterator<T>> newIters = new ArrayList<>();

            for (int i = 0; i < iters.size(); i += 2) {
                if (i == iters.size() - 1) {
                    newIters.add(iters.get(i));
                    continue;
                }

                newIters.add(new MergingIter<>(iters.get(i), iters.get(i + 1), comparator));
            }

            iters = newIters;
        }

        return iters.get(0);
    }

    private static class MergingIter<T> implements Iterator<T> {
        @NotNull
        private final PeekingIterator<T> left;
        @NotNull
        private final PeekingIterator<T> right;
        @NotNull
        private final Comparator<? super T> comparator;

        MergingIter(@NotNull Iterator<T> left,
                    @NotNull Iterator<T> right,
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
    @SuppressWarnings("WeakerAccess")
    public static <T> Iterator<T> empty() {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public T next() {
                throw new NoSuchElementException();
            }
        };
    }

}
