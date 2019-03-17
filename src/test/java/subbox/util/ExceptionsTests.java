package subbox.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

class ExceptionsTests {

    @ParameterizedTest
    @MethodSource("argsWrapCheckedException")
    void testWrapCheckedException(@NotNull Callable<?> callable,
                                  @Nullable Class<? extends Throwable> expectedException,
                                  @Nullable Class<? extends Throwable> expectedCause) {
        if (expectedException == null) {
            Assertions.assertDoesNotThrow(() -> Exceptions.wrapCheckedException(callable));
            return;
        }

        Assertions.assertThrows(expectedException, () -> Exceptions.wrapCheckedException(callable));
        try {
            Exceptions.wrapCheckedException(callable);
        } catch (Throwable e) {
            if (expectedCause == null) {
                Assertions.assertNull(e.getCause());
            } else {
                Assertions.assertSame(e.getCause().getClass(), expectedCause);
            }
        }
    }

    @NotNull
    private static Stream<Arguments> argsWrapCheckedException() {
        return Stream.of(
                arguments(returning(null), null, null),
                arguments(returning("Hello, world!"), null, null),
                arguments(throwing(new Error()), Error.class, null),
                arguments(throwing(new Error(new Error())), Error.class, Error.class),
                arguments(throwing(new RuntimeException(new Error())), RuntimeException.class, Error.class),
                arguments(throwing(new Exception()), Exceptions.CheckedExceptionWrapper.class, Exception.class),
                arguments(throwing(new IOException(new TimeoutException())), Exceptions.CheckedExceptionWrapper.class, IOException.class),
                arguments(throwing(new TimeoutException()), Exceptions.CheckedExceptionWrapper.class, TimeoutException.class),
                arguments(throwing(new IllegalArgumentException(new GeneralSecurityException())), IllegalArgumentException.class, GeneralSecurityException.class)
        );
    }

    @NotNull
    private static <T> Callable<T> returning(@Nullable T t) {
        return () -> t;
    }

    @NotNull
    private static Callable<?> throwing(@NotNull Exception e) {
        return () -> {
            throw e;
        };
    }

    @NotNull
    private static Callable<?> throwing(@NotNull Error e) {
        return () -> {
            throw e;
        };
    }

}
