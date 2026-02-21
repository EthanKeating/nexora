package dev.eths.nexora.cache;

import dev.eths.nexora.metadata.EntityMetadata;

public interface EntitySerializer {
    String serialize(EntityMetadata metadata, Object entity);

    <T> T deserialize(EntityMetadata metadata, String payload, Class<T> type);
}
