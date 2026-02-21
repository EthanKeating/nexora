package dev.eths.nexora.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    public static List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }
}
