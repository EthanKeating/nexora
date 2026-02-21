package dev.eths.nexora.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.eths.nexora.metadata.EntityMetadata;
import dev.eths.nexora.metadata.FieldMetadata;

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JacksonEntitySerializer implements EntitySerializer {
    private final ObjectMapper mapper;

    public JacksonEntitySerializer() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public String serialize(EntityMetadata metadata, Object entity) {
        Map<String, Object> values = new HashMap<>();
        for (FieldMetadata field : metadata.getFields()) {
            try {
                Field reflection = field.getField();
                reflection.setAccessible(true);
                Object value = reflection.get(entity);
                if (value instanceof UUID) {
                    values.put(field.getColumnName(), value.toString());
                } else if (value instanceof Instant) {
                    values.put(field.getColumnName(), ((Instant) value).toString());
                } else if (value instanceof Enum<?>) {
                    values.put(field.getColumnName(), ((Enum<?>) value).name());
                } else {
                    values.put(field.getColumnName(), value);
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Failed to serialize " + metadata.getEntityClass().getName(), e);
            }
        }
        try {
            return mapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize " + metadata.getEntityClass().getName(), e);
        }
    }

    @Override
    public <T> T deserialize(EntityMetadata metadata, String payload, Class<T> type) {
        try {
            Map<String, Object> values = mapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
            T instance = type.getDeclaredConstructor().newInstance();
            for (FieldMetadata field : metadata.getFields()) {
                Object value = values.get(field.getColumnName());
                if (value == null) {
                    continue;
                }
                Field reflection = field.getField();
                reflection.setAccessible(true);
                Class<?> targetType = reflection.getType();
                if (targetType == UUID.class) {
                    reflection.set(instance, UUID.fromString(value.toString()));
                } else if (targetType == Instant.class) {
                    reflection.set(instance, Instant.parse(value.toString()));
                } else if (targetType.isEnum()) {
                    @SuppressWarnings("unchecked")
                    Class<? extends Enum> enumType = (Class<? extends Enum>) targetType;
                    reflection.set(instance, Enum.valueOf(enumType, value.toString()));
                } else {
                    reflection.set(instance, mapper.convertValue(value, targetType));
                }
            }
            return instance;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize " + metadata.getEntityClass().getName(), e);
        }
    }
}
