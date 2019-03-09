package subbox.util;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Maps {

    private Maps() {
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> orderedMapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return (Map<K, V>) newLinkedHashMap(k1, v1, k2, v2, k3, v3, k4, v4);
    }

    @NotNull
    private static Map<Object, Object> newLinkedHashMap(@NotNull Object... objects) {
        if (objects.length % 2 != 0) {
            throw new Error("odd number of objects");
        }

        Map<Object, Object> map = new LinkedHashMap<>(objects.length / 2);
        for (int i = 0; i < objects.length; i += 2) {
            map.put(objects[i], objects[i + 1]);
        }

        return Collections.unmodifiableMap(map);
    }

}
