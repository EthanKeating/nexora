package dev.eths.nexora.example.relations;

import dev.eths.nexora.annotations.Relation;
import dev.eths.nexora.example.entities.Clan;
import dev.eths.nexora.example.entities.Profile;
import dev.eths.nexora.metadata.RelationMetadata;
import dev.eths.nexora.repository.RelationPath;

import java.lang.reflect.Field;

public final class ProfileRelations {
    public static final RelationPath<Profile, Clan> CLAN = relation("clan");

    private ProfileRelations() {
    }

    private static RelationPath<Profile, Clan> relation(String fieldName) {
        try {
            Field field = Profile.class.getDeclaredField(fieldName);
            Relation relation = field.getAnnotation(Relation.class);
            if (relation == null) {
                throw new IllegalStateException("Missing @Relation on " + fieldName);
            }
            return new RelationPath<>(new RelationMetadata(field, relation.target(), relation.localColumn(), relation.targetColumn()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create relation path for " + fieldName, e);
        }
    }
}
