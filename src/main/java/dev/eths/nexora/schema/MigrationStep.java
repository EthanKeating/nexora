package dev.eths.nexora.schema;

public class MigrationStep {
    private final MigrationStepType type;
    private final String description;
    private final String sql;

    public MigrationStep(MigrationStepType type, String description, String sql) {
        this.type = type;
        this.description = description;
        this.sql = sql;
    }

    public MigrationStepType getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getSql() {
        return sql;
    }
}
