package io.atlas.mixin;

import com.mojang.authlib.GameProfile;
import io.atlas.AtlasMod;
import io.atlas.modules.auth.model.PremiumProfileResolution;
import io.atlas.modules.auth.service.PremiumLoginService;
import io.atlas.modules.auth.service.PremiumPlayerDataMigrator;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private Connection connection;

    @Shadow
    public abstract void disconnect(Component reason);

    @Unique
    private volatile PremiumProfileResolution atlas$profileResolution;

    @Unique
    private boolean atlas$lookupStarted;

    @Unique
    private UUID atlas$verifiedPremiumUuid;

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void atlas$resolvePremiumProfile(
            ServerboundHelloPacket packet,
            CallbackInfo callback
    ) {
        if (atlas$profileResolution != null) {
            return;
        }

        callback.cancel();
        if (atlas$lookupStarted) {
            return;
        }

        atlas$lookupStarted = true;
        PremiumLoginService.resolve(server.getProfileRepository(), packet.name())
                .orTimeout(10, TimeUnit.SECONDS)
                .whenComplete((resolution, error) -> server.execute(() -> {
                    if (!connection.isConnected()) {
                        return;
                    }

                    if (error != null
                            || resolution == null
                            || resolution.status()
                            == PremiumProfileResolution.Status.UNAVAILABLE) {
                        AtlasMod.LOGGER.warn(
                                "Login de {} interrompido: verificação Premium indisponível.",
                                packet.name(),
                                error
                        );
                        disconnect(Component.literal(
                                "Não foi possível verificar sua conta agora. Tente novamente."
                        ));
                        return;
                    }

                    atlas$profileResolution = resolution;
                    ((ServerLoginPacketListenerImpl) (Object) this).handleHello(packet);
                }));
    }

    @Redirect(
            method = "handleHello",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"
            )
    )
    private boolean atlas$requirePremiumAuthentication(MinecraftServer server) {
        return atlas$profileResolution != null
                && atlas$profileResolution.status()
                == PremiumProfileResolution.Status.PREMIUM;
    }

    @Inject(method = "startClientVerification", at = @At("HEAD"))
    private void atlas$rememberVerifiedPremium(
            GameProfile profile,
            CallbackInfo callback
    ) {
        if (atlas$profileResolution != null
                && atlas$profileResolution.status()
                == PremiumProfileResolution.Status.PREMIUM) {
            PremiumLoginService.markVerified(profile);
            atlas$verifiedPremiumUuid = profile.getId();
            AtlasMod.LOGGER.info(
                    "Conta Premium verificada: {} ({}).",
                    profile.getName(),
                    profile.getId()
            );
        }
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void atlas$clearInterruptedPremiumLogin(
            DisconnectionDetails details,
            CallbackInfo callback
    ) {
        if (atlas$verifiedPremiumUuid != null) {
            PremiumLoginService.clear(atlas$verifiedPremiumUuid);
        }
    }

    @Inject(
            method = "verifyLoginAndFinishConnectionSetup",
            at = @At("HEAD"),
            cancellable = true
    )
    private void atlas$migratePremiumPlayerData(
            GameProfile profile,
            CallbackInfo callback
    ) {
        if (atlas$profileResolution == null
                || atlas$profileResolution.status()
                != PremiumProfileResolution.Status.PREMIUM) {
            return;
        }

        try {
            PremiumPlayerDataMigrator.migrate(server, profile);
        } catch (Exception exception) {
            AtlasMod.LOGGER.error(
                    "Não foi possível migrar os dados Premium de {}.",
                    profile.getName(),
                    exception
            );
            PremiumLoginService.clear(profile.getId());
            disconnect(Component.literal(
                    "Não foi possível migrar seus dados com segurança. Contate a equipe."
            ));
            callback.cancel();
        }
    }
}
