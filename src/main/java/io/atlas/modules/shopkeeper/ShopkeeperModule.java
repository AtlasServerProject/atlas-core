package io.atlas.modules.shopkeeper;

import io.atlas.module.AtlasModule;
import io.atlas.modules.shopkeeper.service.ShopkeeperService;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

public final class ShopkeeperModule implements AtlasModule {
    private static final ShopkeeperService SERVICE = new ShopkeeperService();
    public static ShopkeeperService service() { return SERVICE; }
    public String getName() { return "Lojistas Atlas"; }
    public void enable() {
        ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> SERVICE.stop());
        UseEntityCallback.EVENT.register(SERVICE::interact);
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,world,hand,hit)->{
            if(!world.isClientSide && io.atlas.modules.shopkeeper.model.ShopRestrictions.redstone(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(player.getItemInHand(hand).getItem()).toString())){
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§cComponentes de redstone estão bloqueados no servidor."),true);
                return net.minecraft.world.InteractionResult.FAIL;
            }
            return net.minecraft.world.InteractionResult.PASS;
        });
    }
    public void disable() { }
}
