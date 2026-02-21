package dev.eths.nexora.schema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchemaInspector {
    public Map<String, TableInfo> inspect(Connection connection, String schema) throws Exception {
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase();
        if (product.contains("sqlite")) {
            return inspectSqlite(connection);
        }
        Map<String, TableInfo> tables = new HashMap<>();
        DatabaseMetaData meta = connection.getMetaData();

        try (ResultSet rs = meta.getTables(connection.getCatalog(), schema, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                TableInfo table = new TableInfo(tableName.toLowerCase());
                tables.put(tableName.toLowerCase(), table);
            }
        }

        for (TableInfo table : tables.values()) {
            try (ResultSet rs = meta.getColumns(connection.getCatalog(), schema, table.getName(), "%")) {
                while (rs.next()) {
                    String name = rs.getString("COLUMN_NAME").toLowerCase();
                    String typeName = rs.getString("TYPE_NAME");
                    int size = rs.getInt("COLUMN_SIZE");
                    boolean nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                    String defaultValue = rs.getString("COLUMN_DEF");
                    table.getColumns().put(name, new ColumnInfo(name, typeName, size, nullable, defaultValue));
                }
            }

            Map<String, List<String>> indexColumns = new HashMap<>();
            Map<String, Boolean> indexUnique = new HashMap<>();
            try (ResultSet rs = meta.getIndexInfo(connection.getCatalog(), schema, table.getName(), false, false)) {
                while (rs.next()) {
                    String indexName = rs.getString("INDEX_NAME");
                    if (indexName == null) {
                        continue;
                    }
                    String columnName = rs.getString("COLUMN_NAME");
                    if (columnName == null) {
                        continue;
                    }
                    indexColumns.computeIfAbsent(indexName.toLowerCase(), k -> new ArrayList<>()).add(columnName.toLowerCase());
                    boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                    indexUnique.put(indexName.toLowerCase(), !nonUnique);
                }
            }
            for (Map.Entry<String, List<String>> entry : indexColumns.entrySet()) {
                String indexName = entry.getKey();
                table.getIndexes().put(indexName, new IndexInfo(indexName, entry.getValue(), indexUnique.getOrDefault(indexName, false)));
            }
        }
        return tables;
    }

    private Map<String, TableInfo> inspectSqlite(Connection connection) throws Exception {
        Map<String, TableInfo> tables = new HashMap<>();
        try (ResultSet rs = connection.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            while (rs.next()) {
                String tableName = rs.getString(1);
                TableInfo table = new TableInfo(tableName.toLowerCase());
                tables.put(tableName.toLowerCase(), table);
            }
        }

        for (TableInfo table : tables.values()) {
            try (ResultSet rs = connection.createStatement().executeQuery("PRAGMA table_info('" + table.getName() + "')")) {
                while (rs.next()) {
                    String name = rs.getString("name").toLowerCase();
                    String typeName = rs.getString("type");
                    boolean nullable = rs.getInt("notnull") == 0;
                    String defaultValue = rs.getString("dflt_value");
                    table.getColumns().put(name, new ColumnInfo(name, typeName, parseSize(typeName), nullable, defaultValue));
                }
            }

            try (ResultSet rs = connection.createStatement().executeQuery("PRAGMA index_list('" + table.getName() + "')")) {
                while (rs.next()) {
                    String indexName = rs.getString("name");
                    boolean unique = rs.getInt("unique") == 1;
                    List<String> columns = new ArrayList<>();
                    try (ResultSet colRs = connection.createStatement().executeQuery("PRAGMA index_info('" + indexName + "')")) {
                        while (colRs.next()) {
                            String columnName = colRs.getString("name");
                            if (columnName != null) {
                                columns.add(columnName.toLowerCase());
                            }
                        }
                    }
                    table.getIndexes().put(indexName.toLowerCase(), new IndexInfo(indexName.toLowerCase(), columns, unique));
                }
            }
        }
        return tables;
    }

    private int parseSize(String typeName) {
        if (typeName == null) {
            return 0;
        }
        int start = typeName.indexOf('(');
        int end = typeName.indexOf(')');
        if (start >= 0 && end > start) {
            try {
                return Integer.parseInt(typeName.substring(start + 1, end));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }
}
