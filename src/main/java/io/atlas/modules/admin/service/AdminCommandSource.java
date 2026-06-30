package io.atlas.modules.admin.service;

import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

final class AdminCommandSource implements CommandSource {

    private final StringBuilder output = new StringBuilder();

    @Override
    public void sendSystemMessage(Component message) {
        if (!output.isEmpty()) {
            output.append(System.lineSeparator());
        }
        output.append(message.getString());
    }

    @Override
    public boolean acceptsSuccess() {
        return true;
    }

    @Override
    public boolean acceptsFailure() {
        return true;
    }

    @Override
    public boolean shouldInformAdmins() {
        return false;
    }

    String output() {
        return output.isEmpty()
                ? "Comando executado sem retorno."
                : output.toString();
    }
}
