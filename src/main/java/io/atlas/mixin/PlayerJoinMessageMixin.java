package io.atlas.mixin;
import net.minecraft.server.players.PlayerList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
/** PlayerService sends the welcome after loading identity and rank. */
@Mixin(PlayerList.class)
public abstract class PlayerJoinMessageMixin {
    @Redirect(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Z)V"))
    private void atlas$replaceJoinMessage(PlayerList players, Component message, boolean overlay) {
        // Welcome is delegated to PlayerService by the connection listener.
    }
}
