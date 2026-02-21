package dev.eths.nexora.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EntityMetadata {
    private final Class<?> entityClass;
    private final String tableName;
    private final String tableRenamedFrom;
    private FieldMetadata primaryKey;
    private final List<FieldMetadata> fields = new ArrayList<>();
    private final List<RelationMetadata> relations = new ArrayList<>();
    private final List<IndexMetadata> indexes = new ArrayList<>();

    public EntityMetadata(Class<?> entityClass, String tableName, String tableRenamedFrom) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.tableRenamedFrom = tableRenamedFrom;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTableRenamedFrom() {
        return tableRenamedFrom;
    }

    public FieldMetadata getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(FieldMetadata primaryKey) {
        this.primaryKey = primaryKey;
    }

    public void addField(FieldMetadata field) {
        fields.add(field);
    }

    public void addRelation(RelationMetadata relation) {
        relations.add(relation);
    }

    public void addIndex(IndexMetadata index) {
        indexes.add(index);
    }

    public List<FieldMetadata> getFields() {
        return Collections.unmodifiableList(fields);
    }

    public List<RelationMetadata> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    public List<IndexMetadata> getIndexes() {
        return Collections.unmodifiableList(indexes);
    }

    public FieldMetadata getFieldByColumn(String columnName) {
        for (FieldMetadata field : fields) {
            if (field.getColumnName().equalsIgnoreCase(columnName)) {
                return field;
            }
        }
        return null;
    }
}
