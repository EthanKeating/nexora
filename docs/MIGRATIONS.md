# Safe Schema Sync

On startup, Nexora performs a safe schema synchronization:
1. Build model metadata from annotations.
2. Inspect the live database schema.
3. Create a migration plan.
4. Apply safe changes according to `MigrationMode`.

## Migration Modes
- `APPLY_SAFE`: Apply safe changes only.
- `DRY_RUN`: Compute the plan but do not execute changes.
- `WARN_ONLY`: Log warnings for unsafe changes, no modifications.

## Auto-applied (safe)
- Create missing tables
- Add missing columns when nullable or defaulted
- Create missing indexes
- Apply explicit rename hints

## Blocked (unsafe)
- Dropping tables or columns
- Type changes
- NOT NULL tightening without a default
- Renames without explicit hints

## Rename Hints
```java
@RenamedFrom("old_column")
@Column(name = "new_column")
private String value;

@TableRenamedFrom("old_table")
@Entity(table = "new_table")
public class Profile { }
```
