# Relations

Nexora uses explicit relation includes. There are no lazy proxies. You must specify which relations to load for each query.

## Mapping
Use `@Relation` on a transient field to define the join metadata. The field should be annotated with `@TransientField` so it is not persisted as a column.

```java
@Relation(target = Clan.class, localColumn = "clan_id", targetColumn = "clan_id")
@TransientField
private Clan clan;
```

## Relation Paths
Create relation paths once, then reuse them during queries.

```java
public final class ProfileRelations {
    public static final RelationPath<Profile, Clan> CLAN = relation("clan");

    private static RelationPath<Profile, Clan> relation(String fieldName) {
        Field field = Profile.class.getDeclaredField(fieldName);
        Relation relation = field.getAnnotation(Relation.class);
        return new RelationPath<>(new RelationMetadata(field, relation.target(), relation.localColumn(), relation.targetColumn()));
    }
}
```

## Loading with Includes
```java
context.getRepository(Profile.class)
    .get(playerUuid)
    .with(ProfileRelations.CLAN)
    .async();
```
