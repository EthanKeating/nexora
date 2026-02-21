package dev.eths.nexora.schema;

public class ColumnInfo {
    private final String name;
    private final String typeName;
    private final int size;
    private final boolean nullable;
    private final String defaultValue;

    public ColumnInfo(String name, String typeName, int size, boolean nullable, String defaultValue) {
        this.name = name;
        this.typeName = typeName;
        this.size = size;
        this.nullable = nullable;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getTypeName() {
        return typeName;
    }

    public int getSize() {
        return size;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
