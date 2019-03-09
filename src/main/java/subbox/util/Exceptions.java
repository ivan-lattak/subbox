package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;

public final class Exceptions {

    private Exceptions() {
    }

    public static <T> T wrapIOException(@NotNull IOExceptionCallable<T> callable) {
        try {
            return callable.call();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void wrapIOException(@NotNull IOExceptionRunnable runnable) {
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