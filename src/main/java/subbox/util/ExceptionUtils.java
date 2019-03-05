package subbox.util;

import java.io.IOException;
import java.io.UncheckedIOException;

public class ExceptionUtils {

    public static <T> T wrapIOException(IOExceptionCallable<T> callable) {
        try {
            return callable.call();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void wrapIOException(IOExceptionRunnable runnable) {
        try {
            runnable.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @FunctionalInterface
    public interface IOExceptionCallable<T> {

        T call() throws IOException;

    }

    @FunctionalInterface
    public interface IOExceptionRunnable {

        void run() throws IOException;

    }

}
