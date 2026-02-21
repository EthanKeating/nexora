package dev.eths.nexora.schema;

import java.util.List;

public class IndexInfo {
    private final String name;
    private final List<String> columns;
    private final boolean unique;

    public IndexInfo(String name, List<String> columns, boolean unique) {
        this.name = name;
        this.columns = columns;
        this.unique = unique;
    }

    public String getName() {
        return name;
    }

    public List<String> getColumns() {
        return columns;
    }

    public boolean isUnique() {
        return unique;
    }
}
