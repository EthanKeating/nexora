package dev.eths.nexora.repository;

import dev.eths.nexora.metadata.RelationMetadata;

public class RelationPath<T, R> {
    private final RelationMetadata metadata;

    public RelationPath(RelationMetadata metadata) {
        this.metadata = metadata;
    }

    public RelationMetadata getMetadata() {
        return metadata;
    }
}
