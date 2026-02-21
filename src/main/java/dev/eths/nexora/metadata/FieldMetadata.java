package dev.eths.nexora.metadata;

import java.lang.reflect.Field;

public class FieldMetadata {
    private final Field field;
    private final String columnName;
    private final boolean nullable;
    private final int length;
    private final String defaultValue;
    private final boolean primaryKey;
    private final String renamedFrom;

    public FieldMetadata(Field field, String columnName, boolean nullable, int length, String defaultValue, boolean primaryKey, String renamedFrom) {
        this.field = field;
        this.columnName = columnName;
        this.nullable = nullable;
        this.length = length;
        this.defaultValue = defaultValue;
        this.primaryKey = primaryKey;
        this.renamedFrom = renamedFrom;
    }

    public Field getField() {
        return field;
    }

    public String getColumnName() {
        return columnName;
    }

    public boolean isNullable() {
        return nullable;
    }

    public int getLength() {
        return length;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public String getRenamedFrom() {
        return renamedFrom;
    }
}
