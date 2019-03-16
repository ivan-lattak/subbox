package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.concurrent.Callable;

public final class Exceptions {

    private Exceptions() {
    }

    public static <T> T wrapCheckedException(@NotNull Callable<T> callable) {
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (Exception e) {
            throw new CheckedExceptionWrapper(e);
        }
    }

    private static class CheckedExceptionWrapper extends RuntimeException {
        CheckedExceptionWrapper(Exception e) {
            super(e);
        }
    }

}
