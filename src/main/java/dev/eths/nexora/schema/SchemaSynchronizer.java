package dev.eths.nexora.schema;

import dev.eths.nexora.MigrationMode;
import dev.eths.nexora.metadata.EntityMetadata;
import dev.eths.nexora.metadata.FieldMetadata;
import dev.eths.nexora.metadata.IndexMetadata;
import dev.eths.nexora.sql.SqlDialect;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.logging.Logger;

public class SchemaSynchronizer {
    private static final String HISTORY_TABLE = "__nexora_schema_history";
    private final SqlDialect dialect;
    private final SchemaInspector inspector;

    public SchemaSynchronizer(SqlDialect dialect) {
        this.dialect = dialect;
        this.inspector = new SchemaInspector();
    }

    public MigrationPlan plan(Connection connection, List<EntityMetadata> entities, String schema) throws Exception {
        Map<String, TableInfo> tables = inspector.inspect(connection, schema);
        MigrationPlan plan = new MigrationPlan();

        for (EntityMetadata metadata : entities) {
            String tableName = metadata.getTableName().toLowerCase();
            TableInfo table = tables.get(tableName);
            if (table == null) {
                String renamedFrom = metadata.getTableRenamedFrom();
                if (renamedFrom != null && tables.containsKey(renamedFrom.toLowerCase())) {
                    String sql = dialect.renameTableSql(renamedFrom, metadata.getTableName());
                    plan.addStep(new MigrationStep(MigrationStepType.SAFE_AUTO, "Rename table " + renamedFrom + " -> " + metadata.getTableName(), sql));
                } else {
                    String sql = dialect.createTableSql(metadata);
                    plan.addStep(new MigrationStep(MigrationStepType.SAFE_AUTO, "Create table " + metadata.getTableName(), sql));
                }
                continue;
            }

            for (FieldMetadata field : metadata.getFields()) {
                ColumnInfo column = table.getColumns().get(field.getColumnName().toLowerCase());
                if (column == null) {
                    if (field.getRenamedFrom() != null && table.getColumns().containsKey(field.getRenamedFrom().toLowerCase())) {
                        String sql = dialect.renameColumnSql(metadata.getTableName(), field.getRenamedFrom(), field.getColumnName(), field);
                        plan.addStep(new MigrationStep(MigrationStepType.SAFE_AUTO, "Rename column " + field.getRenamedFrom() + " -> " + field.getColumnName(), sql));
                        continue;
                    }
                    if (field.isNullable() || !field.getDefaultValue().isEmpty()) {
                        String sql = dialect.addColumnSql(metadata.getTableName(), field);
                        plan.addStep(new MigrationStep(MigrationStepType.SAFE_AUTO, "Add column " + field.getColumnName(), sql));
                    } else {
                        plan.addStep(new MigrationStep(MigrationStepType.BLOCKED,
                                "Missing NOT NULL column " + field.getColumnName() + " with no default", null));
                    }
                    continue;
                }
                String expectedType = dialect.sqlType(field).toLowerCase();
                if (!column.getTypeName().toLowerCase().contains(expectedType.split("\\(")[0])) {
                    plan.addStep(new MigrationStep(MigrationStepType.BLOCKED,
                            "Column type mismatch for " + field.getColumnName() + " (" + column.getTypeName() + " != " + expectedType + ")", null));
                }
                if (!field.isNullable() && column.isNullable()) {
                    plan.addStep(new MigrationStep(MigrationStepType.BLOCKED,
                            "Column " + field.getColumnName() + " nullable in DB but non-nullable in model", null));
                }
            }

            for (IndexMetadata index : metadata.getIndexes()) {
                String indexName = index.getName().toLowerCase();
                if (!table.getIndexes().containsKey(indexName)) {
                    String sql = dialect.createIndexSql(metadata.getTableName(), index);
                    plan.addStep(new MigrationStep(MigrationStepType.SAFE_AUTO, "Create index " + index.getName(), sql));
                }
            }
        }
        return plan;
    }

    public void apply(Connection connection, MigrationPlan plan, MigrationMode mode, Logger logger) throws Exception {
        ensureHistoryTable(connection);
        for (MigrationStep step : plan.getSteps()) {
            if (step.getType() == MigrationStepType.SAFE_AUTO) {
                logger.info("[Schema] " + step.getDescription() + " -> " + step.getSql());
                if (mode == MigrationMode.APPLY_SAFE || mode == MigrationMode.WARN_ONLY) {
                    try (Statement statement = connection.createStatement()) {
                        statement.execute(step.getSql());
                    }
                }
            } else {
                // Unsafe changes are intentionally blocked unless the operator intervenes.
                logger.warning("[Schema] BLOCKED: " + step.getDescription());
            }
        }
        if (plan.hasBlockedSteps() && mode == MigrationMode.APPLY_SAFE) {
            throw new IllegalStateException("Blocked schema changes detected. See log for details.");
        }
        if (mode != MigrationMode.DRY_RUN) {
            writeHistory(connection, plan);
        }
    }

    private void ensureHistoryTable(Connection connection) throws Exception {
        boolean sqlite = connection.getMetaData().getDatabaseProductName().toLowerCase().contains("sqlite");
        String idColumn = sqlite ? "id INTEGER PRIMARY KEY AUTOINCREMENT" : "id BIGINT AUTO_INCREMENT PRIMARY KEY";
        String timestampType = sqlite ? "TEXT" : "TIMESTAMP";
        String sql = "CREATE TABLE IF NOT EXISTS " + dialect.quote(HISTORY_TABLE) + " (" +
                idColumn + "," +
                "applied_at " + timestampType + " DEFAULT CURRENT_TIMESTAMP," +
                "model_hash VARCHAR(64) NOT NULL," +
                "summary TEXT" +
                ")" + (sqlite ? "" : " ENGINE=InnoDB");
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void writeHistory(Connection connection, MigrationPlan plan) throws Exception {
        String hash = hashPlan(plan);
        String summary = summarize(plan);
        String sql = "INSERT INTO " + dialect.quote(HISTORY_TABLE) + " (model_hash, summary) VALUES (?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, hash);
            statement.setString(2, summary);
            statement.executeUpdate();
        }
    }

    private String summarize(MigrationPlan plan) {
        List<String> items = new ArrayList<>();
        for (MigrationStep step : plan.getSteps()) {
            items.add(step.getType() + ":" + step.getDescription());
        }
        return String.join(" | ", items);
    }

    private String hashPlan(MigrationPlan plan) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        StringJoiner joiner = new StringJoiner("|");
        for (MigrationStep step : plan.getSteps()) {
            joiner.add(step.getType() + ":" + step.getDescription());
        }
        byte[] hash = digest.digest(joiner.toString().getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
