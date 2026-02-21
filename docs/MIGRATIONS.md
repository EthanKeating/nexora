# Safe Schema Sync

Nexora performs a safe schema synchronization at startup:
1. Build model metadata from annotations.
2. Inspect the live database schema.
3. Create a plan.
4. Apply safe changes only.

## Auto-applied (safe)
- Create missing tables
- Add missing columns when nullable or defaulted
- Create missing indexes
- Apply explicit rename hints

## Blocked (unsafe)
- Dropping tables or columns
- Type changes
- NOT NULL tightening without default
- Renames without explicit hints

## Rename hints
```java
@RenamedFrom("old_column")
@Column(name = "new_column")
private String value;

@TableRenamedFrom("old_table")
@Entity(table = "new_table")
public class Profile { ... }
```
