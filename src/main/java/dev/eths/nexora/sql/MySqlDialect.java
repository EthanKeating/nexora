package dev.eths.nexora.sql;

import dev.eths.nexora.metadata.EntityMetadata;
import dev.eths.nexora.metadata.FieldMetadata;
import dev.eths.nexora.metadata.IndexMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MySqlDialect implements SqlDialect {
    @Override
    public String quote(String identifier) {
        return "`" + identifier + "`";
    }

    @Override
    public String sqlType(FieldMetadata field) {
        Class<?> type = field.getField().getType();
        if (type == java.util.UUID.class) {
            return "CHAR(36)";
        }
        if (type == String.class) {
            return "VARCHAR(" + field.getLength() + ")";
        }
        if (type == int.class || type == Integer.class) {
            return "INT";
        }
        if (type == long.class || type == Long.class) {
            return "BIGINT";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "TINYINT(1)";
        }
        if (type == double.class || type == Double.class) {
            return "DOUBLE";
        }
        if (type == float.class || type == Float.class) {
            return "FLOAT";
        }
        if (type == java.time.Instant.class) {
            return "TIMESTAMP";
        }
        if (type.isEnum()) {
            return "VARCHAR(64)";
        }
        return "VARCHAR(255)";
    }

    @Override
    public String createTableSql(EntityMetadata metadata) {
        List<String> columns = new ArrayList<>();
        for (FieldMetadata field : metadata.getFields()) {
            columns.add(columnDefinition(field));
        }
        String primaryKey = quote(metadata.getPrimaryKey().getColumnName());
        columns.add("PRIMARY KEY (" + primaryKey + ")");
        return "CREATE TABLE " + quote(metadata.getTableName()) + " (\n  " +
                String.join(",\n  ", columns) + "\n) ENGINE=InnoDB";
    }

    @Override
    public String addColumnSql(String tableName, FieldMetadata field) {
        return "ALTER TABLE " + quote(tableName) + " ADD COLUMN " + columnDefinition(field);
    }

    @Override
    public String createIndexSql(String tableName, IndexMetadata index) {
        String unique = index.isUnique() ? "UNIQUE " : "";
        String columns = index.getColumns().stream().map(this::quote).collect(Collectors.joining(", "));
        return "CREATE " + unique + "INDEX " + quote(index.getName()) + " ON " + quote(tableName) + " (" + columns + ")";
    }

    @Override
    public String renameTableSql(String fromTable, String toTable) {
        return "RENAME TABLE " + quote(fromTable) + " TO " + quote(toTable);
    }

    @Override
    public String renameColumnSql(String tableName, String fromColumn, String toColumn, FieldMetadata newField) {
        return "ALTER TABLE " + quote(tableName) + " CHANGE COLUMN " + quote(fromColumn) + " " + columnDefinition(newField);
    }

    @Override
    public String insertSql(EntityMetadata metadata) {
        List<String> columns = metadata.getFields().stream().map(FieldMetadata::getColumnName).toList();
        String columnList = columns.stream().map(this::quote).collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(col -> "?").collect(Collectors.joining(", "));
        return "INSERT INTO " + quote(metadata.getTableName()) + " (" + columnList + ") VALUES (" + placeholders + ")";
    }

    @Override
    public String upsertSql(EntityMetadata metadata) {
        List<String> columns = metadata.getFields().stream().map(FieldMetadata::getColumnName).toList();
        String columnList = columns.stream().map(this::quote).collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(col -> "?").collect(Collectors.joining(", "));
        String updates = metadata.getFields().stream()
                .filter(field -> !field.isPrimaryKey())
                .map(field -> quote(field.getColumnName()) + "=VALUES(" + quote(field.getColumnName()) + ")")
                .collect(Collectors.joining(", "));
        return "INSERT INTO " + quote(metadata.getTableName()) + " (" + columnList + ") VALUES (" + placeholders + ") ON DUPLICATE KEY UPDATE " + updates;
    }

    @Override
    public String updateSql(EntityMetadata metadata, List<FieldMetadata> updateFields) {
        String setClause = updateFields.stream()
                .map(field -> quote(field.getColumnName()) + "=?")
                .collect(Collectors.joining(", "));
        return "UPDATE " + quote(metadata.getTableName()) + " SET " + setClause + " WHERE " +
                quote(metadata.getPrimaryKey().getColumnName()) + "=?";
    }

    private String columnDefinition(FieldMetadata field) {
        StringBuilder sb = new StringBuilder();
        sb.append(quote(field.getColumnName())).append(" ").append(sqlType(field));
        if (!field.isNullable()) {
            sb.append(" NOT NULL");
        } else {
            sb.append(" NULL");
        }
        if (!field.getDefaultValue().isEmpty()) {
            sb.append(" DEFAULT ").append(field.getDefaultValue());
        }
        return sb.toString();
    }
}
