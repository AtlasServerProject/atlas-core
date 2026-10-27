package io.atlas.modules.audit.service;

import com.google.gson.JsonObject;
import io.atlas.modules.audit.repository.ActivityLogRepository;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;

public final class ActivityLogService {
    public static String redact(String text) {
        return text.replaceAll("(?i)/(?:[\\w-]+:)?(?:recuperarsenha|email|login|register|changepassword|changepass|senha|site\\s+vincular)\\b[^\\r\\n]*","/[COMANDO SENSÍVEL OCULTO]")
            .replaceAll("(?i)(senha|password|token|secret|api[_ -]?key)\\s*[:=]\\s*[^\\s]+","$1=[OCULTO]")
            .replaceAll("(?i)https?://[^\\s]+/api(?:/v\\d+)?/webhooks/[^\\s]+","[WEBHOOK OCULTO]")
            .replaceAll("[\\w-]{23,}\\.[\\w-]{6}\\.[\\w-]{25,}","[TOKEN OCULTO]");
    }
    public static void record(String kind,String actor,String uuid,String channel,String text) {
        JsonObject event=new JsonObject();
        event.addProperty("id",java.util.UUID.randomUUID().toString());
        event.addProperty("date",java.time.Instant.now().toString());
        event.addProperty("kind",kind);event.addProperty("actor",actor);event.addProperty("uuid",uuid);
        event.addProperty("channel",channel);event.addProperty("text",redact(text));
        ActivityLogRepository.append(event);
    }
    public static void chat(ServerPlayer sender,String channel,String text) {
        record("CHAT",sender.getName().getString(),sender.getUUID().toString(),channel,text);
    }
    public static void command(CommandSourceStack source,String command) {
        String root=command.strip().replaceFirst("^/","").split("\\s+",2)[0];
        // Never persist command arguments: passwords and arbitrary mod credentials must not leave memory.
        record("COMMAND_ATTEMPT",source.getTextName(),source.getEntity() instanceof ServerPlayer p?p.getUUID().toString():"console","command","/"+root+" [argumentos omitidos]");
    }
    private ActivityLogService() {}
}
