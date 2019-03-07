package subbox.util;

import java.util.concurrent.Future;

public abstract class UncancellableFuture<V> implements Future<V> {

    @Override
    public final boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public final boolean isCancelled() {
        return false;
    }

}
