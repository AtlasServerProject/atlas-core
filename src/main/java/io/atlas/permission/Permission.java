package io.atlas.permission;

import io.atlas.modules.rank.RankModule;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

public class Permission {

    public static boolean has(ServerPlayer player, String permission) {
        return RankModule.getRankService()
                .hasPermission(player.getUUID(), permission);
    }

    public static boolean check(ServerPlayer player, String permission) {
        return has(player, permission);
    }

    public static boolean deny(ServerPlayer player) {
        player.sendSystemMessage(
                Component.literal("§cVocê não tem permissão para usar este comando.")
        );
        return false;
    }
}