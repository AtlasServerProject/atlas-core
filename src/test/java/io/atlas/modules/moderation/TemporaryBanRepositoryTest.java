package io.atlas.modules.moderation;

import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.database.DatabaseManager;
import io.atlas.modules.moderation.model.PunishmentType;
import io.atlas.modules.moderation.repository.ModerationRepository;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;

public final class TemporaryBanRepositoryTest {
    private static void check(boolean value, String label) { if (!value) throw new AssertionError(label); }
    public static void main(String[] args) throws Exception {
        if (!DatabaseConfig.DATABASE.equals("atlas_compat_182") || DatabaseConfig.PORT != 25432)
            throw new IllegalStateException("Isolated compatibility database required");
        try (var c=DriverManager.getConnection(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD)) {
            c.setAutoCommit(false);
            var field=DatabaseManager.class.getDeclaredField("connection");field.setAccessible(true);var old=field.get(null);field.set(null,c);
            try {
                UUID uuid=UUID.randomUUID();String name="TempBan"+uuid.toString().substring(0,8);
                try(var p=c.prepareStatement("INSERT INTO players(uuid,username) VALUES(?,?)")){p.setObject(1,uuid);p.setString(2,name);p.executeUpdate();}
                var repo=new ModerationRepository();var player=repo.findPlayer(uuid).orElseThrow();
                long expired=repo.create(PunishmentType.BAN,player,null,null,"TEST","expired",Instant.now().minusSeconds(60));
                check(repo.findActivePlayerPunishment(uuid,PunishmentType.BAN).isEmpty(),"expired ban permits access");
                long active=repo.create(PunishmentType.BAN,player,null,null,"TEST","finite",Instant.now().plusSeconds(3600));
                var ban=repo.findActivePlayerPunishment(uuid,PunishmentType.BAN).orElseThrow();
                check(ban.id()==active && ban.expiresAt()!=null,"active finite ban denies access");
                check(repo.findActivePlayerPunishment(UUID.randomUUID(),PunishmentType.BAN).isEmpty(),"other player unaffected");
                check(repo.revokePlayerPunishments(uuid,PunishmentType.BAN,null,"TEST","review")==1,"unban revokes active temporary ban");
                check(repo.findActivePlayerPunishment(uuid,PunishmentType.BAN).isEmpty(),"revoked ban permits access");
                check(repo.history(name,10).stream().anyMatch(p->p.id()==active && p.revokedAt()!=null),"revocation retained in history");
                long perma=repo.create(PunishmentType.BAN,player,null,null,"TEST","permanent",null);
                check(repo.findActivePlayerPunishment(uuid,PunishmentType.BAN).orElseThrow().id()==perma,"permanent bans preserved");
                check(repo.history(name,10).stream().anyMatch(p->p.id()==expired),"expiration preserves occurrence history");
                System.out.println("TemporaryBanRepositoryTest PASS: active, expired, revoked, permanent, isolation and history");
            } finally { c.rollback();field.set(null,old); }
        }
    }
}
