package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

public final class Exceptions {

    private Exceptions() {
    }

    public static <T> T wrapCheckedException(@NotNull Callable<T> callable) {
        try {
            return callable.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new CheckedExceptionWrapper(e);
        }
    }

    static class CheckedExceptionWrapper extends RuntimeException {
        CheckedExceptionWrapper(Exception e) {
            super(e);
        }
    }

}
