package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;

public final class DurationFormatter {

    private DurationFormatter() {
    }

    @NotNull
    public static String format(@NotNull Duration duration) {
        boolean negative = duration.isNegative();
        duration = duration.abs();

        long days = duration.toDaysPart();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        double millis = duration.toNanosPart() / 1_000_000.0;

        return (negative ? "-" : "") +
                (days != 0 ? days + "d" : "") +
                (hours != 0 ? hours + "h" : "") +
                (minutes != 0 ? minutes + "m" : "") +
                (seconds != 0 ? seconds + "s" : "") +
                (millis != 0 ? millis + "ms" : "");
    }

}
