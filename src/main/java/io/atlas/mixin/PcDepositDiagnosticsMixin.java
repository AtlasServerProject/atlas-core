package io.atlas.mixin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.pc.link.PCLinkManager;
import com.cobblemon.mod.common.net.messages.server.storage.pc.MovePartyPokemonToPCPacket;
import io.atlas.AtlasMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.cobblemon.mod.common.net.serverhandling.storage.pc.MovePartyPokemonToPCHandler", remap = false)
public class PcDepositDiagnosticsMixin {
    // Cobblemon is distributed in intermediary mappings; target only its typed handler, not its bridge.
    @Inject(method = "handle(Lcom/cobblemon/mod/common/net/messages/server/storage/pc/MovePartyPokemonToPCPacket;Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/class_3222;)V", at = @At("HEAD"), remap = false)
    private void atlas$inspectDeposit(MovePartyPokemonToPCPacket packet, MinecraftServer server,
                                      ServerPlayer player, CallbackInfo callback) {
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
        var pokemon = party.get(packet.getPartyPosition());
        var pc = PCLinkManager.INSTANCE.getPC(player);
        AtlasMod.LOGGER.info("PC deposit: player={}, link={}, slot={}, matchingPokemon={}, destination={}",
                player.getGameProfile().getName(), pc != null, packet.getPartyPosition(),
                pokemon != null && pokemon.getUuid().equals(packet.getPokemonID()), packet.getPcPosition());
    }
}
