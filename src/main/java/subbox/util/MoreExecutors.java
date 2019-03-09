package subbox.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;

import static java.util.concurrent.TimeUnit.SECONDS;

public final class MoreExecutors {

    private MoreExecutors() {
    }

    public static ExecutorService newBoundedCachedThreadPool(int maxSize) {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(maxSize, maxSize,
                60, SECONDS, new LinkedBlockingQueue<>());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }

}
