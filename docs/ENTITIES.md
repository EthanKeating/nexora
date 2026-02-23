# Entities

Nexora entities are plain Java classes annotated with mapping metadata. For write-through behavior, extend `ManagedEntity` and call `markDirty("column")` in setters.

## Required Rules
- Provide a no-args constructor.
- Mark a single primary key field with `@PrimaryKey`.
- Use `@Entity(table = "...")` to specify the table name.

## Common Annotations
- `@Entity(table = "...")`: Table name.
- `@PrimaryKey`: Primary key field.
- `@Column`: Column mapping and constraints.
- `@Index` / `@Indexes`: Index metadata.
- `@Relation`: Defines a relation join for explicit includes.
- `@TransientField`: Not mapped to the database.
- `@RenamedFrom` / `@TableRenamedFrom`: Rename hints for safe schema sync.

## Column Options
```java
@Column(
    name = "coins",
    nullable = false,
    length = 255,
    defaultValue = "0"
)
```

## Example
```java
@Entity(table = "profiles")
public class Profile extends ManagedEntity<UUID> {
    @PrimaryKey
    @Column(name = "profile_id")
    private UUID profileId;

    @Column(name = "coins", nullable = false, defaultValue = "0")
    private int coins;

    public Profile() { }

    public void setCoins(int coins) {
        if (this.coins != coins) {
            this.coins = coins;
            markDirty("coins");
        }
    }
}
```
