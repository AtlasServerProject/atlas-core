package io.atlas.modules.audit.repository;

import com.google.gson.JsonObject;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.time.*;

public final class ActivityLogRepository {
    public static void append(JsonObject event) { append("atlas-activity",event); }
    public static synchronized void append(String stream,JsonObject event) {
        if(!stream.matches("[a-z-]+"))throw new IllegalArgumentException("Nome de stream inválido");
        try {
            Path directory=Path.of("logs",stream);
            Files.createDirectories(directory);
            try { Files.setPosixFilePermissions(directory,java.nio.file.attribute.PosixFilePermissions.fromString("rwx------")); }
            catch(UnsupportedOperationException ignored) {}
            Path file=directory.resolve(LocalDate.now(ZoneOffset.UTC)+".jsonl");
            if(!Files.exists(file)) {
                try { Files.createFile(file,java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------"))); }
                catch(UnsupportedOperationException ignored) { Files.createFile(file); }
            }
            Files.writeString(file,event+"\n",StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            try { Files.setPosixFilePermissions(file,java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")); }
            catch(UnsupportedOperationException ignored) {}
        } catch(java.io.IOException error) {
            io.atlas.AtlasMod.LOGGER.error("Falha ao persistir registro privado de atividade.");
        }
    }
    private ActivityLogRepository() {}
}
