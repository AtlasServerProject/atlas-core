package io.atlas.modules.moderation.service;

/** Permission and text rules kept independent from Minecraft for verification. */
public final class StaffNotesPolicy {
    private StaffNotesPolicy() {}
    public static boolean mayAccess(boolean console, boolean authenticated, boolean staff,
                                    int actorPriority, int targetPriority) {
        return console || (authenticated && staff && actorPriority > targetPriority);
    }
    public static String text(String value) {
        if (value == null) throw new IllegalArgumentException("Informe o texto da nota ou motivo.");
        String clean = value.strip();
        if (clean.isEmpty() || clean.length() > 500 || clean.codePoints().anyMatch(c ->
                Character.isISOControl(c) || Character.getType(c) == Character.FORMAT || c == '§')) {
            throw new IllegalArgumentException("Use de 1 a 500 caracteres, sem controles ou códigos de formatação.");
        }
        return clean;
    }
}
