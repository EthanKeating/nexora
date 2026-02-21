package dev.eths.nexora.sql;

import dev.eths.nexora.metadata.EntityMetadata;
import dev.eths.nexora.metadata.FieldMetadata;
import dev.eths.nexora.metadata.IndexMetadata;

import java.util.List;

public interface SqlDialect {
    String quote(String identifier);

    String sqlType(FieldMetadata field);

    String createTableSql(EntityMetadata metadata);

    String addColumnSql(String tableName, FieldMetadata field);

    String createIndexSql(String tableName, IndexMetadata index);

    String renameTableSql(String fromTable, String toTable);

    String renameColumnSql(String tableName, String fromColumn, String toColumn, FieldMetadata newField);

    String insertSql(EntityMetadata metadata);

    String upsertSql(EntityMetadata metadata);

    String updateSql(EntityMetadata metadata, List<FieldMetadata> updateFields);
}
