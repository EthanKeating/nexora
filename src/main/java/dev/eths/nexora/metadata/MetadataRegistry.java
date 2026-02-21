package dev.eths.nexora.metadata;

import dev.eths.nexora.annotations.Column;
import dev.eths.nexora.annotations.Entity;
import dev.eths.nexora.annotations.Index;
import dev.eths.nexora.annotations.Indexes;
import dev.eths.nexora.annotations.PrimaryKey;
import dev.eths.nexora.annotations.Relation;
import dev.eths.nexora.annotations.RenamedFrom;
import dev.eths.nexora.annotations.TableRenamedFrom;
import dev.eths.nexora.annotations.TransientField;
import dev.eths.nexora.util.NameUtil;
import dev.eths.nexora.util.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class MetadataRegistry {
    private static final Logger logger = Logger.getLogger(MetadataRegistry.class.getName());
    private final Map<Class<?>, EntityMetadata> entities = new HashMap<>();

    public EntityMetadata register(Class<?> entityClass) {
        EntityMetadata existing = entities.get(entityClass);
        if (existing != null) {
            return existing;
        }

        Entity entity = entityClass.getAnnotation(Entity.class);
        if (entity == null) {
            throw new IllegalArgumentException("Missing @Entity on " + entityClass.getName());
        }
        TableRenamedFrom tableRenamedFrom = entityClass.getAnnotation(TableRenamedFrom.class);
        EntityMetadata metadata = new EntityMetadata(entityClass, entity.table(), tableRenamedFrom == null ? null : tableRenamedFrom.value());

        List<Index> classIndexes = new ArrayList<>();
        Index singleIndex = entityClass.getAnnotation(Index.class);
        if (singleIndex != null) {
            classIndexes.add(singleIndex);
        }
        Indexes indexes = entityClass.getAnnotation(Indexes.class);
        if (indexes != null) {
            classIndexes.addAll(Arrays.asList(indexes.value()));
        }

        for (Field field : ReflectionUtil.getAllFields(entityClass)) {
            if (field.isAnnotationPresent(TransientField.class)) {
                continue;
            }
            if (field.isAnnotationPresent(Relation.class)) {
                Relation relation = field.getAnnotation(Relation.class);
                metadata.addRelation(new RelationMetadata(field, relation.target(), relation.localColumn(), relation.targetColumn()));
                continue;
            }

            Column column = field.getAnnotation(Column.class);
            if (column == null) {
                continue;
            }

            String columnName = column.name().isEmpty() ? NameUtil.toSnakeCase(field.getName()) : column.name();
            boolean primaryKey = field.isAnnotationPresent(PrimaryKey.class);
            RenamedFrom renamedFrom = field.getAnnotation(RenamedFrom.class);
            FieldMetadata fieldMetadata = new FieldMetadata(field, columnName, column.nullable(), column.length(), column.defaultValue(), primaryKey,
                    renamedFrom == null ? null : renamedFrom.value());
            metadata.addField(fieldMetadata);
            if (primaryKey) {
                if (metadata.getPrimaryKey() != null) {
                    throw new IllegalArgumentException("Multiple @PrimaryKey fields in " + entityClass.getName());
                }
                metadata.setPrimaryKey(fieldMetadata);
            }

            Index fieldIndex = field.getAnnotation(Index.class);
            if (fieldIndex != null) {
                List<String> columns = fieldIndex.columns().length == 0 ? List.of(columnName) : Arrays.asList(fieldIndex.columns());
                String indexName = fieldIndex.name().isEmpty()
                        ? defaultIndexName(metadata.getTableName(), columns, fieldIndex.unique())
                        : fieldIndex.name();
                metadata.addIndex(new IndexMetadata(indexName, columns, fieldIndex.unique()));
            }
        }

        for (Index index : classIndexes) {
            if (index.columns().length == 0) {
                logger.warning("Skipping class-level @Index on " + entityClass.getName() + " because no columns were provided.");
                continue;
            }
            List<String> columns = Arrays.asList(index.columns());
            String indexName = index.name().isEmpty()
                    ? defaultIndexName(metadata.getTableName(), columns, index.unique())
                    : index.name();
            metadata.addIndex(new IndexMetadata(indexName, columns, index.unique()));
        }

        if (metadata.getPrimaryKey() == null) {
            throw new IllegalArgumentException("Missing @PrimaryKey in " + entityClass.getName());
        }

        entities.put(entityClass, metadata);
        return metadata;
    }

    public EntityMetadata getMetadata(Class<?> entityClass) {
        return entities.get(entityClass);
    }

    public List<EntityMetadata> getAll() {
        return new ArrayList<>(entities.values());
    }

    public String defaultIndexName(String tableName, List<String> columns, boolean unique) {
        String prefix = unique ? "uidx" : "idx";
        return prefix + "_" + tableName + "_" + String.join("_", columns);
    }
}
