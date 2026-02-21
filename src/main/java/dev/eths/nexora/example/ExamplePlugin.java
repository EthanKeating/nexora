package dev.eths.nexora.example;

import dev.eths.nexora.CacheMode;
import dev.eths.nexora.DatabaseType;
import dev.eths.nexora.MigrationMode;
import dev.eths.nexora.NexoraConfig;
import dev.eths.nexora.NexoraContext;
import dev.eths.nexora.example.entities.Clan;
import dev.eths.nexora.example.entities.Profile;
import dev.eths.nexora.example.relations.ProfileRelations;
import dev.eths.nexora.repository.EntityRepository;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

public class ExamplePlugin extends JavaPlugin {
    private NexoraContext context;
    public EntityRepository<Profile, UUID> profiles;
    public EntityRepository<Clan, UUID> clans;

    @Override
    public void onEnable() {
        NexoraConfig config = NexoraConfig.builder()
                .jdbcUrl("jdbc:mysql://localhost:3306/nexora")
                .username("root")
                .password("password")
                .defaultCacheTtl(Duration.ofMinutes(5))
                .cacheMaxSize(10_000)
                .cacheMode(CacheMode.MEMORY_ONLY)
                .databaseType(DatabaseType.AUTO)
                .autoFlushDebounceMs(50)
                .migrationMode(MigrationMode.APPLY_SAFE)
                .build();

        context = NexoraContext.builder(this, config)
                .register(Profile.class)
                .register(Clan.class)
                .build();

        profiles = context.getRepository(Profile.class);
        clans = context.getRepository(Clan.class);

        UUID playerId = UUID.randomUUID();
        profiles.get(playerId)
                .with(ProfileRelations.CLAN)
                .cache(Duration.ofMinutes(10))
                .async()
                .thenAcceptSync(this, profileOptional -> profileOptional.ifPresent(profile -> {
                    Bukkit.broadcastMessage("Loaded " + profile.getProfileId());
                    profile.setCoins(profile.getCoins() + 5);
                }));

        Profile newProfile = new Profile(playerId);
        newProfile.setCoins(0);
        context.getRepository(Profile.class).attachNew(newProfile);
        newProfile.setCoins(100);

        Optional<Profile> loaded = profiles.getNow(playerId);
        loaded.ifPresent(profile -> profile.setXp(profile.getXp() + 20));
    }

    @Override
    public void onDisable() {
        if (context != null) {
            context.close();
        }
    }
}
