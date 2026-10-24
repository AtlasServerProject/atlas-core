package io.atlas.modules.rules.service;

import com.google.gson.JsonParser;
import io.atlas.AtlasMod;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.rules.repository.RulesRepository;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class RulesService {
    private final RulesRepository repository = new RulesRepository();
    private final Map<UUID, Boolean> accepted = new HashMap<>();
    private final Set<UUID> displayed = new HashSet<>();
    private final Map<UUID, Integer> reminders = new HashMap<>();
    private int version;
    private List<String> rules;
    private int ticks;
    public void start(MinecraftServer server) {
        try {
            var path = Path.of("config", "atlas-rules.json");
            if (!Files.exists(path)) {
                Files.createDirectories(path.getParent());
                try (var in = RulesService.class.getResourceAsStream("/atlas-rules/rules.json")) {
                    Files.copy(Objects.requireNonNull(in), path);
                }
            }
            var config = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
            version = config.get("version").getAsInt();
            var lines = new ArrayList<String>();
            config.getAsJsonArray("rules").forEach(line -> lines.add(line.getAsString()));
            if (version < 1 || lines.isEmpty() || lines.stream().anyMatch(String::isBlank))
                throw new IllegalArgumentException("Regras vazias ou versão inválida");
            rules = List.copyOf(lines);
        } catch (Exception e) { throw new IllegalStateException("Não foi possível carregar as regras Atlas", e); }
    }
    public boolean hasAccepted(UUID player) { return Boolean.TRUE.equals(accepted.get(player)); }
    public boolean pending(UUID player) { return !hasAccepted(player); }
    public Component requiredMessage() { return Component.literal("§eLeia §f/regras §ee confirme com §f/regras aceitar §epara jogar."); }
    public void tick(MinecraftServer server) {
        if (++ticks % 20 != 0 || rules == null) return;
        for (var player : server.getPlayerList().getPlayers()) {
            var id = player.getUUID();
            if (!AuthModule.getAuthService().isAuthenticated(id)) continue;
            try {
                if (!accepted.containsKey(id)) accepted.put(id, repository.accepted(id, version));
                if (hasAccepted(id)) continue;
                if (!displayed.contains(id)) show(player);
                else if (ticks - reminders.getOrDefault(id, 0) >= 600) {
                    player.sendSystemMessage(requiredMessage()); reminders.put(id, ticks);
                }
            } catch (RuntimeException e) {
                if (ticks - reminders.getOrDefault(id, -600) >= 600) {
                    AtlasMod.LOGGER.error("Falha no aceite de regras de {}", id, e);
                    player.sendSystemMessage(Component.literal("§cNão foi possível consultar as regras. Aguarde e tente /regras novamente."));
                    reminders.put(id, ticks);
                }
            }
        }
    }
    public int show(ServerPlayer player) {
        if (rules == null) { player.sendSystemMessage(Component.literal("§cRegras ainda carregando.")); return 0; }
        player.sendSystemMessage(Component.literal("\n◆ ATLAS • REGRAS DO SERVIDOR ◆").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
        for (int i=0; i<rules.size(); i++) player.sendSystemMessage(Component.literal("§e"+(i+1)+". §f"+rules.get(i)));
        displayed.add(player.getUUID()); reminders.put(player.getUUID(), ticks);
        if (!hasAccepted(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Ao confirmar, você declara que leu e aceita estas regras.").withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.literal("[ LI E ACEITO AS REGRAS ]").withStyle(style -> style
                    .withColor(ChatFormatting.GREEN).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/regras aceitar"))));
        } else player.sendSystemMessage(Component.literal("§aVocê já aceitou esta versão das regras."));
        return 1;
    }
    public int accept(ServerPlayer player) {
        var id = player.getUUID();
        if (rules == null) return 0;
        if (!displayed.contains(id)) { player.sendSystemMessage(requiredMessage()); return show(player); }
        try {
            repository.accept(id, version);
            accepted.put(id, true);
            player.sendSystemMessage(Component.literal("§aRegras aceitas! Bem-vindo ao Atlas. Sua jornada está liberada."));
            player.server.getCommands().sendCommands(player);
            return 1;
        } catch (RuntimeException e) {
            AtlasMod.LOGGER.error("Falha ao salvar aceite de regras de {}", id, e);
            player.sendSystemMessage(Component.literal("§cO aceite não foi salvo. Tente /regras aceitar novamente.")); return 0;
        }
    }
    public void remove(UUID id) { accepted.remove(id); displayed.remove(id); reminders.remove(id); }
    public void clear() { accepted.clear(); displayed.clear(); reminders.clear(); }
}
