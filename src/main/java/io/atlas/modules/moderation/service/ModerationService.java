package io.atlas.modules.moderation.service;

import io.atlas.modules.moderation.model.ModerationDuration;
import io.atlas.modules.moderation.model.Punishment;
import io.atlas.modules.moderation.model.PunishmentType;
import io.atlas.modules.moderation.repository.ModerationRepository;
import io.atlas.modules.rank.model.Rank;
import io.atlas.modules.rank.service.RankService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public class ModerationService {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault());

    private final ModerationRepository repository = new ModerationRepository();
    private final RankService rankService;

    public ModerationService(RankService rankService) {
        this.rankService = rankService;
    }

    public boolean canModerate(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null || rankService.isStaff(player.getUUID());
    }

    public Result warn(CommandSourceStack source, String username, String reason) {
        Optional<ModerationRepository.PlayerRef> target = repository.findPlayer(username);
        if (target.isEmpty()) return Result.playerNotFound(username);
        Result permission = checkHierarchy(source, target.get());
        if (!permission.success()) return permission;

        long id = repository.create(PunishmentType.WARN, target.get(), null, actor(source), actorName(source), reason, null);
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.get().uuid());
        if (online != null) {
            online.sendSystemMessage(Component.literal("§cVocê recebeu um aviso da equipe: §f" + reason));
        }
        return Result.success("§aAviso #" + id + " aplicado em §f" + target.get().username() + "§a.");
    }

    public Result kick(CommandSourceStack source, ServerPlayer target, String reason) {
        Result permission = checkHierarchy(source, new ModerationRepository.PlayerRef(0, target.getUUID(), target.getName().getString()));
        if (!permission.success()) return permission;

        repository.findPlayer(target.getUUID()).ifPresent(playerRef ->
                repository.create(PunishmentType.KICK, playerRef, cleanIp(target.getIpAddress()), actor(source), actorName(source), reason, null)
        );
        target.connection.disconnect(Component.literal("§cVocê foi expulso do Atlas.\n§7Motivo: §f" + reason));
        return Result.success("§aJogador §f" + target.getName().getString() + " §aexpulso.");
    }

    public Result mute(CommandSourceStack source, String username, ModerationDuration duration, String reason) {
        Optional<ModerationRepository.PlayerRef> target = repository.findPlayer(username);
        if (target.isEmpty()) return Result.playerNotFound(username);
        Result permission = checkHierarchy(source, target.get());
        if (!permission.success()) return permission;

        Instant expiresAt = duration.permanent() ? null : Instant.now().plus(duration.duration());
        long id = repository.create(PunishmentType.MUTE, target.get(), null, actor(source), actorName(source), reason, expiresAt);
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.get().uuid());
        if (online != null) {
            online.sendSystemMessage(Component.literal("§cVocê foi mutado. §7Duração: §f" + describe(expiresAt)
                    + "§7. Motivo: §f" + reason));
        }
        return Result.success("§aMute #" + id + " aplicado em §f" + target.get().username()
                + "§a por §f" + describe(expiresAt) + "§a.");
    }

    public Result unmute(CommandSourceStack source, String username, String reason) {
        Optional<ModerationRepository.PlayerRef> target = repository.findPlayer(username);
        if (target.isEmpty()) return Result.playerNotFound(username);
        Result permission = checkHierarchy(source, target.get());
        if (!permission.success()) return permission;

        int count = repository.revokePlayerPunishments(target.get().uuid(), PunishmentType.MUTE, actor(source), actorName(source), reason);
        if (count == 0) return Result.failure("§cEsse jogador não possui mute ativo.");
        return Result.success("§aMute removido de §f" + target.get().username() + "§a.");
    }

    public Result ban(CommandSourceStack source, String username, String reason) {
        Optional<ModerationRepository.PlayerRef> target = repository.findPlayer(username);
        if (target.isEmpty()) return Result.playerNotFound(username);
        Result permission = checkHierarchy(source, target.get());
        if (!permission.success()) return permission;

        long id = repository.create(PunishmentType.BAN, target.get(), null, actor(source), actorName(source), reason, null);
        ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.get().uuid());
        if (online != null) {
            online.connection.disconnect(Component.literal("§cVocê foi banido do Atlas.\n§7Motivo: §f" + reason));
        }
        return Result.success("§aBan #" + id + " aplicado em §f" + target.get().username() + "§a.");
    }

    public Result unban(CommandSourceStack source, String username, String reason) {
        Optional<ModerationRepository.PlayerRef> target = repository.findPlayer(username);
        int atlasCount = target
                .map(player -> repository.revokePlayerPunishments(
                        player.uuid(),
                        PunishmentType.BAN,
                        actor(source),
                        actorName(source),
                        reason
                ))
                .orElse(0);
        boolean vanillaRevoked = revokeVanillaBan(source, username);

        if (atlasCount == 0 && !vanillaRevoked) {
            return target.isEmpty()
                    ? Result.playerNotFound(username)
                    : Result.failure("§cEsse jogador não possui ban ativo.");
        }

        String name = target.map(ModerationRepository.PlayerRef::username).orElse(username);
        return Result.success("§aBan removido de §f" + name + "§a.");
    }

    public Result banIp(CommandSourceStack source, String target, String reason) {
        String ip = resolveIp(source, target);
        if (ip == null || ip.isBlank()) {
            return Result.failure("§cInforme um IP ou um jogador online.");
        }
        long id = repository.create(PunishmentType.BAN_IP, null, cleanIp(ip), actor(source), actorName(source), reason, null);
        source.getServer().getPlayerList().getPlayers().stream()
                .filter(player -> cleanIp(player.getIpAddress()).equals(cleanIp(ip)))
                .forEach(player -> player.connection.disconnect(Component.literal(
                        "§cSeu IP foi banido do Atlas.\n§7Motivo: §f" + reason
                )));
        return Result.success("§aBanIP #" + id + " aplicado em §f" + cleanIp(ip) + "§a.");
    }

    public Result unbanIp(CommandSourceStack source, String ip, String reason) {
        int count = repository.revokeIpBans(cleanIp(ip), actor(source), actorName(source), reason);
        if (count == 0) return Result.failure("§cEsse IP não possui ban ativo.");
        return Result.success("§aBanIP removido de §f" + cleanIp(ip) + "§a.");
    }

    public Optional<Punishment> activeMute(UUID uuid) {
        return repository.findActivePlayerPunishment(uuid, PunishmentType.MUTE);
    }

    public Optional<Punishment> activeBan(UUID uuid) {
        return repository.findActivePlayerPunishment(uuid, PunishmentType.BAN);
    }

    public Optional<Punishment> activeIpBan(String ip) {
        return repository.findActiveIpBan(cleanIp(ip));
    }

    public List<Punishment> history(String username) {
        return repository.history(username, 10);
    }

    public String formatPunishment(Punishment punishment) {
        String status = punishment.revokedAt() != null
                ? "§7revogada"
                : punishment.expiresAt() != null && punishment.expiresAt().isBefore(Instant.now())
                ? "§8expirada"
                : "§aativa";
        return "§7#" + punishment.id()
                + " §f" + punishment.type()
                + " §8| " + status
                + " §8| §7por §f" + punishment.actorName()
                + " §8| §7motivo: §f" + punishment.reason();
    }

    public Component disconnectMessage(Punishment punishment) {
        return Component.literal("§cVocê está banido do Atlas.\n§7Motivo: §f" + punishment.reason()
                + "\n§7Expira: §f" + describe(punishment.expiresAt()));
    }

    public Component mutedMessage(Punishment punishment) {
        return Component.literal("§cVocê está mutado. §7Expira: §f" + describe(punishment.expiresAt())
                + "§7. Motivo: §f" + punishment.reason());
    }

    private Result checkHierarchy(CommandSourceStack source, ModerationRepository.PlayerRef target) {
        ServerPlayer actor = source.getPlayer();
        if (actor == null) return Result.ok();
        if (!rankService.isStaff(actor.getUUID())) {
            return Result.failure("§cApenas staff pode usar comandos de moderação.");
        }
        int actorPriority = highestPriority(actor.getUUID());
        int targetPriority = highestPriority(target.uuid());
        if (targetPriority >= actorPriority && targetPriority > 0) {
            return Result.failure("§cVocê não pode punir alguém com cargo igual ou superior ao seu.");
        }
        return Result.ok();
    }

    private int highestPriority(UUID uuid) {
        return rankService.getHighestRank(uuid).map(Rank::getPriority).orElse(0);
    }

    private ModerationRepository.PlayerRef actor(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null ? null : repository.findPlayer(player.getUUID()).orElse(null);
    }

    private String actorName(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player == null ? "CONSOLE" : player.getName().getString();
    }

    private String resolveIp(CommandSourceStack source, String target) {
        ServerPlayer online = source.getServer().getPlayerList().getPlayerByName(target);
        if (online != null) {
            return online.getIpAddress();
        }
        return target;
    }

    private String describe(Instant expiresAt) {
        return expiresAt == null ? "permanente" : DATE_FORMAT.format(expiresAt);
    }

    private boolean revokeVanillaBan(CommandSourceStack source, String username) {
        try {
            source.getServer().getCommands().performPrefixedCommand(
                    source.getServer()
                            .createCommandSourceStack()
                            .withPermission(4)
                            .withSuppressedOutput(),
                    "pardon " + username
            );
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static String cleanIp(String rawIp) {
        if (rawIp == null) return "";
        String value = rawIp.trim();
        if (value.startsWith("/")) value = value.substring(1);
        int slash = value.indexOf('/');
        if (slash >= 0) value = value.substring(0, slash);
        int colon = value.lastIndexOf(':');
        if (colon > 0 && value.indexOf(':') == colon) value = value.substring(0, colon);
        return value.toLowerCase(Locale.ROOT);
    }

    public record Result(boolean success, String message) {
        public static Result ok() { return new Result(true, ""); }
        public static Result success(String message) { return new Result(true, message); }
        public static Result failure(String message) { return new Result(false, message); }
        public static Result playerNotFound(String username) {
            return failure("§cJogador não encontrado no banco: §f" + username + "§c.");
        }
    }
}
