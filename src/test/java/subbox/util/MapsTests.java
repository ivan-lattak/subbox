package subbox.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

class MapsTests {

    @RepeatedTest(100)
    void testOrderedMapOf() {
        List<Map.Entry<String, String>> entries = List.of(
                Map.entry("Answer", "42"),
                Map.entry("Hello", "world"),
                Map.entry("Marco", "Polo"),
                Map.entry("", "Apples!")
        );
        List<String> keys = entries.stream().map(Map.Entry::getKey).collect(toList());
        List<String> values = entries.stream().map(Map.Entry::getValue).collect(toList());

        Map<String, String> map = Maps.orderedMapOf(
                "Answer", "42",
                "Hello", "world",
                "Marco", "Polo",
                "", "Apples!"
        );

        Assertions.assertIterableEquals(entries, map.entrySet());
        Assertions.assertIterableEquals(keys, map.keySet());
        Assertions.assertIterableEquals(values, map.values());
    }

}
