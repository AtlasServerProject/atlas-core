package io.atlas.modules.moderation;

import io.atlas.AtlasMod;
import io.atlas.module.AtlasModule;
import io.atlas.modules.moderation.listener.ModerationConnectionListener;
import io.atlas.modules.moderation.service.ModerationService;
import io.atlas.modules.rank.RankModule;

public class ModerationModule implements AtlasModule {

    private static final ModerationService SERVICE =
            new ModerationService(RankModule.getRankService());

    private static final io.atlas.modules.moderation.service.StaffToolsService STAFF_TOOLS =
            new io.atlas.modules.moderation.service.StaffToolsService(RankModule.getRankService());

    public static io.atlas.modules.moderation.service.StaffToolsService getStaffTools() { return STAFF_TOOLS; }

    private static final io.atlas.modules.moderation.service.StaffModeService STAFF_MODE =
            new io.atlas.modules.moderation.service.StaffModeService();

    public static io.atlas.modules.moderation.service.StaffModeService getStaffMode() { return STAFF_MODE; }

    public static ModerationService getService() {
        return SERVICE;
    }

    @Override
    public String getName() {
        return "Moderation";
    }

    @Override
    public void enable() {
        ModerationConnectionListener.register(SERVICE);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(STAFF_MODE::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPING.register(STAFF_MODE::stopping);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> STAFF_MODE.recover(handler.player));
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STAFF_MODE.disconnected(handler.player));
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(STAFF_TOOLS::tick);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STAFF_TOOLS.disconnected(handler.player));
        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) ->
                !(entity instanceof net.minecraft.server.level.ServerPlayer player) || !STAFF_TOOLS.blocksInteraction(player.getUUID()));
        net.fabricmc.fabric.api.event.player.AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                STAFF_TOOLS.blocksInteraction(player.getUUID()) ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) ->
                STAFF_TOOLS.blocksInteraction(player.getUUID()) ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                STAFF_TOOLS.blocksInteraction(player.getUUID()) ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
                STAFF_TOOLS.blocksInteraction(player.getUUID()) ? net.minecraft.world.InteractionResult.FAIL : net.minecraft.world.InteractionResult.PASS);
        net.fabricmc.fabric.api.event.player.UseItemCallback.EVENT.register((player, world, hand) ->
                STAFF_TOOLS.blocksInteraction(player.getUUID()) ? net.minecraft.world.InteractionResultHolder.fail(player.getItemInHand(hand))
                        : net.minecraft.world.InteractionResultHolder.pass(player.getItemInHand(hand)));
        AtlasMod.LOGGER.info("Moderation Manager iniciado.");
    }

    @Override
    public void disable() {
        STAFF_TOOLS.clear();
    }
}
