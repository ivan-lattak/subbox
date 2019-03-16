package subbox.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MoreIterators {

    private MoreIterators() {
    }

    @NotNull
    public static <T> Iterator<T> mergeSorted(@NotNull List<? extends Iterator<T>> iterators,
                                              @NotNull Comparator<? super T> comparator) {
        if (iterators.isEmpty()) {
            return empty();
        }

        List<? extends Iterator<T>> iters = iterators;
        while (iters.size() > 1) {
            iters = mergePairs(iters, comparator);
        }

        return iters.get(0);
    }

    private static <T> List<Iterator<T>> mergePairs(@NotNull List<? extends Iterator<T>> iterators, @NotNull Comparator<? super T> comparator) {
        List<Iterator<T>> mergedIterators = new ArrayList<>();

        for (int i = 0; i < iterators.size(); i += 2) {
            if (i == iterators.size() - 1) {
                mergedIterators.add(iterators.get(i));
                continue;
            }

            mergedIterators.add(new MergingIter<>(iterators.get(i), iterators.get(i + 1), comparator));
        }

        return mergedIterators;
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
            this.left = Iterators.peekingIterator(left);
            this.right = Iterators.peekingIterator(right);
            this.comparator = comparator;
        }

        @Override
        public boolean hasNext() {
            return left.hasNext() || right.hasNext();
        }

        @Override
        public T next() {
            boolean leftIsEmpty = !left.hasNext();
            boolean rightIsEmpty = !right.hasNext();
            if (leftIsEmpty && rightIsEmpty) {
                throw new NoSuchElementException();
            }

            if (leftIsEmpty) {
                return right.next();
            }

            if (rightIsEmpty) {
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
