package subbox.util.iterators;

import java.util.Iterator;

public interface PeekingIterator<T> extends Iterator<T> {

    T peek();

}
