package dev.eths.nexora.metadata;

import java.util.List;

public class IndexMetadata {
    private final String name;
    private final List<String> columns;
    private final boolean unique;

    public IndexMetadata(String name, List<String> columns, boolean unique) {
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
