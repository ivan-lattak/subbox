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
                arguments(Duration.ZERO, "0ms"),
                arguments(Duration.ofNanos(0), "0ms"),
                arguments(Duration.ofNanos(5), "0.000005ms"),
                arguments(Duration.ofNanos(42_000), "0.042ms"),
                arguments(Duration.ofNanos(500_000), "0.5ms"),
                arguments(Duration.ofNanos(2_990_000), "2.99ms"),
                arguments(Duration.ofMillis(4), "4ms"),
                arguments(Duration.ofMillis(1_800), "1s800ms"),
                arguments(Duration.ofSeconds(3).plusNanos(1500), "3s0.0015ms"),
                arguments(Duration.ofSeconds(15).plusMillis(31), "15s31ms"),
                arguments(Duration.ofSeconds(180), "3m"),
                arguments(Duration.ofMinutes(7).plusNanos(16_000_256), "7m16.000256ms"),
                arguments(Duration.ofMinutes(11).plusSeconds(12), "11m12s"),
                arguments(Duration.ofMinutes(30).plusSeconds(20).plusMillis(10), "30m20s10ms"),
                arguments(Duration.ofHours(9).plusMinutes(42), "9h42m"),
                arguments(Duration.ofHours(12), "12h"),
                arguments(Duration.ofHours(20).plusSeconds(15).plusNanos(2800), "20h15s0.0028ms"),
                arguments(Duration.ofHours(25).plusMinutes(75), "1d2h15m"),
                arguments(Duration.ofNanos(123456789012345678L), "1428d21h33m9s12.345678ms"),
                arguments(Duration.ZERO.negated(), "0ms"),
                arguments(Duration.ofNanos(0).negated(), "0ms"),
                arguments(Duration.ofMillis(65).plusNanos(789).negated(), "-65.000789ms"),
                arguments(Duration.ofNanos(10644967890123456L).negated(), "-123d4h56m7s890.123456ms")
        );
    }

}
