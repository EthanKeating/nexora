package dev.eths.nexora.example.entities;

import dev.eths.nexora.annotations.Column;
import dev.eths.nexora.annotations.Entity;
import dev.eths.nexora.annotations.Index;
import dev.eths.nexora.annotations.PrimaryKey;
import dev.eths.nexora.annotations.Relation;
import dev.eths.nexora.annotations.TransientField;
import dev.eths.nexora.entity.ManagedEntity;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity(table = "profiles")
public class Profile extends ManagedEntity<UUID> {
    @PrimaryKey
    @Column(name = "profile_id")
    private UUID profileId;

    @Index
    @Column(name = "clan_id", nullable = true)
    private UUID clanId;

    @Column(name = "coins", nullable = false, defaultValue = "0")
    private int coins;

    @Column(name = "xp", nullable = false, defaultValue = "0")
    private long xp;

    @Column(name = "last_seen", nullable = true)
    private Instant lastSeen;

    @Relation(target = Clan.class, localColumn = "clan_id", targetColumn = "clan_id")
    @TransientField
    private Clan clan;

    public Profile() {
    }

    public Profile(UUID profileId) {
        this.profileId = profileId;
    }

    public UUID getProfileId() {
        return profileId;
    }

    public void setProfileId(UUID profileId) {
        if (!Objects.equals(this.profileId, profileId)) {
            this.profileId = profileId;
            markDirty("profile_id");
        }
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

    public int getCoins() {
        return coins;
    }

    public void setCoins(int coins) {
        if (this.coins != coins) {
            this.coins = coins;
            markDirty("coins");
        }
    }

    public long getXp() {
        return xp;
    }

    public void setXp(long xp) {
        if (this.xp != xp) {
            this.xp = xp;
            markDirty("xp");
        }
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        if (!Objects.equals(this.lastSeen, lastSeen)) {
            this.lastSeen = lastSeen;
            markDirty("last_seen");
        }
    }

    public Clan getClan() {
        return clan;
    }

    public void setClan(Clan clan) {
        this.clan = clan;
    }
}
