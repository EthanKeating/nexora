package dev.eths.nexora.schema;

import java.util.HashMap;
import java.util.Map;

public class TableInfo {
    private final String name;
    private final Map<String, ColumnInfo> columns = new HashMap<>();
    private final Map<String, IndexInfo> indexes = new HashMap<>();

    public TableInfo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Map<String, ColumnInfo> getColumns() {
        return columns;
    }

    public Map<String, IndexInfo> getIndexes() {
        return indexes;
    }
}
