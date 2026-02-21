package dev.eths.nexora.metadata;

import java.lang.reflect.Field;

public class RelationMetadata {
    private final Field field;
    private final Class<?> target;
    private final String localColumn;
    private final String targetColumn;

    public RelationMetadata(Field field, Class<?> target, String localColumn, String targetColumn) {
        this.field = field;
        this.target = target;
        this.localColumn = localColumn;
        this.targetColumn = targetColumn;
    }

    public Field getField() {
        return field;
    }

    public Class<?> getTarget() {
        return target;
    }

    public String getLocalColumn() {
        return localColumn;
    }

    public String getTargetColumn() {
        return targetColumn;
    }
}
