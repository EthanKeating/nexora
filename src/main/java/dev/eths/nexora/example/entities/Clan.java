package dev.eths.nexora.example.entities;

import dev.eths.nexora.annotations.Column;
import dev.eths.nexora.annotations.Entity;
import dev.eths.nexora.annotations.Index;
import dev.eths.nexora.annotations.PrimaryKey;
import dev.eths.nexora.entity.ManagedEntity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity(table = "clans")
public class Clan extends ManagedEntity<UUID> {
    @PrimaryKey
    @Column(name = "clan_id")
    private UUID clanId;

    @Index(unique = true)
    @Column(name = "name", length = 32, nullable = false)
    private String name;

    @Index
    @Column(name = "tag", length = 8, nullable = true)
    private String tag;

    @Column(name = "created_at", nullable = true)
    private Instant createdAt;

    public Clan() {
    }

    public Clan(UUID clanId) {
        this.clanId = clanId;
    }

    public UUID getClanId() {
        return clanId;
    }

    public void setClanId(UUID clanId) {
        if (!Objects.equals(this.clanId, clanId)) {
            this.clanId = clanId;
            markDirty("clan_id");
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (!Objects.equals(this.name, name)) {
            this.name = name;
            markDirty("name");
        }
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        if (!Objects.equals(this.tag, tag)) {
            this.tag = tag;
            markDirty("tag");
        }
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        if (!Objects.equals(this.createdAt, createdAt)) {
            this.createdAt = createdAt;
            markDirty("created_at");
        }
    }
}
