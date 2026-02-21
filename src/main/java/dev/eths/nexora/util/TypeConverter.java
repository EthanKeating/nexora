package dev.eths.nexora.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

public final class TypeConverter {
    private TypeConverter() {
    }

    public static Object toDatabaseValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof UUID) {
            return value.toString();
        }
        if (value instanceof Instant) {
            return Timestamp.from((Instant) value);
        }
        if (value instanceof Enum<?>) {
            return ((Enum<?>) value).name();
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? 1 : 0;
        }
        return value;
    }

    public static Object fromResultSet(ResultSet rs, String column, Class<?> targetType) throws SQLException {
        if (targetType == UUID.class) {
            String value = rs.getString(column);
            return value == null ? null : UUID.fromString(value);
        }
        if (targetType == Instant.class) {
            Timestamp ts = rs.getTimestamp(column);
            return ts == null ? null : ts.toInstant();
        }
        if (targetType.isEnum()) {
            String value = rs.getString(column);
            if (value == null) {
                return null;
            }
            @SuppressWarnings("unchecked")
            Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
            return Enum.valueOf(enumType, value);
        }
        if (targetType == boolean.class || targetType == Boolean.class) {
            return rs.getInt(column) != 0;
        }
        if (targetType == int.class || targetType == Integer.class) {
            return rs.getInt(column);
        }
        if (targetType == long.class || targetType == Long.class) {
            return rs.getLong(column);
        }
        if (targetType == double.class || targetType == Double.class) {
            return rs.getDouble(column);
        }
        if (targetType == float.class || targetType == Float.class) {
            return rs.getFloat(column);
        }
        return rs.getObject(column);
    }
}
