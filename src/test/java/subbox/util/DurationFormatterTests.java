package subbox.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.Duration;
import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;

class DurationFormatterTests {

    @ParameterizedTest
    @MethodSource("durationFormatterTestArgs")
    void testDurationFormatter(Duration duration, String expectedFormattedString) {
        String formattedString = DurationFormatter.format(duration);
        Assertions.assertEquals(expectedFormattedString, formattedString);
    }

    private static Stream<Arguments> durationFormatterTestArgs() {
        return Stream.of(
                arguments(Duration.ofNanos(0), "0ms"),
                arguments(Duration.ofNanos(5), "0.000005ms"),
                arguments(Duration.ofNanos(42_000), "0.042ms"),
                arguments(Duration.ofNanos(500_000), "0.5ms"),
                arguments(Duration.ofNanos(2_990_000), "2.99ms"),
                arguments(Duration.ofMillis(4), "4ms"),
                arguments(Duration.ofMillis(1_800), "1s800ms"),
                arguments(Duration.ofSeconds(3, 1500), "3s0.0015ms")
        );
    }

}
