package io.atlas.modules.auth.service;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

public final class PremiumPlayerDataMigrator {

    private PremiumPlayerDataMigrator() {
    }

    public static void migrate(MinecraftServer server, GameProfile profile)
            throws IOException {
        UUID officialUuid = profile.getId();
        UUID offlineUuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + profile.getName()).getBytes(StandardCharsets.UTF_8)
        );
        if (officialUuid.equals(offlineUuid)) {
            return;
        }

        Path worldRoot = server.getWorldPath(LevelResource.ROOT);
        Path marker = worldRoot.resolve("atlas/premium-migrations/")
                .resolve(officialUuid + ".done");
        if (Files.exists(marker)) {
            return;
        }

        String offlinePrefix = offlineUuid.toString().substring(0, 2);
        String officialPrefix = officialUuid.toString().substring(0, 2);
        List<FileMigration> migrations = List.of(
                direct("playerdata", offlineUuid, officialUuid, ".dat"),
                direct("playerdata", offlineUuid, officialUuid, ".dat_old"),
                direct("advancements", offlineUuid, officialUuid, ".json"),
                direct("stats", offlineUuid, officialUuid, ".json"),
                nested("cobblemonplayerdata", offlinePrefix, officialPrefix,
                        offlineUuid, officialUuid, ".json"),
                nested("cobblemonplayerdata", offlinePrefix, officialPrefix,
                        offlineUuid, officialUuid, ".json.old"),
                nested("pokedex", offlinePrefix, officialPrefix,
                        offlineUuid, officialUuid, ".nbt"),
                nested("pokedex", offlinePrefix, officialPrefix,
                        offlineUuid, officialUuid, ".nbt.old")
        );

        Path backupRoot = worldRoot.resolve("atlas/premium-backups/")
                .resolve(officialUuid.toString());
        for (FileMigration migration : migrations) {
            Path source = worldRoot.resolve(migration.source());
            if (!Files.exists(source)) {
                continue;
            }

            Path target = worldRoot.resolve(migration.target());
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                Path backup = backupRoot.resolve(migration.target());
                Files.createDirectories(backup.getParent());
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.COPY_ATTRIBUTES);
            }

            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.COPY_ATTRIBUTES);
            replaceEmbeddedUuid(target, offlineUuid, officialUuid);
        }

        Files.createDirectories(marker.getParent());
        Files.writeString(
                marker,
                "offline=" + offlineUuid + System.lineSeparator()
                        + "official=" + officialUuid + System.lineSeparator(),
                StandardCharsets.UTF_8
        );
    }

    private static void replaceEmbeddedUuid(
            Path target,
            UUID offlineUuid,
            UUID officialUuid
    ) throws IOException {
        if (!target.getFileName().toString().endsWith(".json")) {
            return;
        }

        String content = Files.readString(target, StandardCharsets.UTF_8);
        String migrated = content.replace(offlineUuid.toString(), officialUuid.toString());
        if (!content.equals(migrated)) {
            Files.writeString(target, migrated, StandardCharsets.UTF_8);
        }
    }

    private static FileMigration direct(
            String directory,
            UUID offlineUuid,
            UUID officialUuid,
            String suffix
    ) {
        return new FileMigration(
                directory + "/" + offlineUuid + suffix,
                directory + "/" + officialUuid + suffix
        );
    }

    private static FileMigration nested(
            String directory,
            String offlinePrefix,
            String officialPrefix,
            UUID offlineUuid,
            UUID officialUuid,
            String suffix
    ) {
        return new FileMigration(
                directory + "/" + offlinePrefix + "/" + offlineUuid + suffix,
                directory + "/" + officialPrefix + "/" + officialUuid + suffix
        );
    }

    private record FileMigration(String source, String target) {
    }
}
