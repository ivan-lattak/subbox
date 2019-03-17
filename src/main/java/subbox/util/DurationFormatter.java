package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Locale;

public final class DurationFormatter {

    private DurationFormatter() {
    }

    @NotNull
    public static String format(@NotNull Duration duration) {
        if (duration.isZero()) {
            return "0ms";
        }

        boolean negative = duration.isNegative();
        duration = duration.abs();

        long days = duration.toDaysPart();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        double millis = duration.toNanosPart() / 1_000_000.0;

        DecimalFormat format = null;
        if (millis != 0) {
            format = new DecimalFormat("#.######ms", new DecimalFormatSymbols(Locale.US));
        }

        return (negative ? "-" : "") +
                (days != 0 ? String.format("%dd", days) : "") +
                (hours != 0 ? String.format("%dh", hours) : "") +
                (minutes != 0 ? String.format("%dm", minutes) : "") +
                (seconds != 0 ? String.format("%ds", seconds) : "") +
                (millis != 0 ? format.format(millis) : "");
    }

}
