package io.atlas.mixin;

import com.mojang.authlib.GameProfile;
import io.atlas.AtlasMod;
import io.atlas.modules.auth.model.PremiumProfileResolution;
import io.atlas.modules.auth.service.PremiumLoginService;
import io.atlas.modules.auth.service.PremiumPlayerDataMigrator;
import io.atlas.modules.lobby.service.DevEnvironment;
import io.atlas.modules.lobby.service.DevTransferService;
import net.minecraft.network.Connection;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.network.protocol.cookie.ClientboundCookieRequestPacket;
import net.minecraft.network.protocol.cookie.ServerboundCookieResponsePacket;
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

    @Shadow private String requestedUsername;
    @Shadow protected abstract void startClientVerification(GameProfile profile);

    @Unique private boolean atlas$devCookieRequested;
    @Unique private boolean atlas$devCookieReceived;

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
        if (DevEnvironment.DEVELOPMENT) {
            callback.cancel();
            if (atlas$devCookieRequested) return;
            if (!packet.name().matches("[A-Za-z0-9_]{3,16}")) {
                disconnect(Component.literal("Nome de jogador inválido."));
                return;
            }
            requestedUsername = packet.name();
            atlas$devCookieRequested = true;
            connection.send(new ClientboundCookieRequestPacket(DevTransferService.COOKIE));
            return;
        }
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

    @Inject(method = "handleCookieResponse", at = @At("HEAD"), cancellable = true)
    private void atlas$validateDevAccess(ServerboundCookieResponsePacket packet, CallbackInfo callback) {
        if (!DevEnvironment.DEVELOPMENT) return;
        callback.cancel();
        if (!atlas$devCookieRequested || atlas$devCookieReceived || !packet.key().equals(DevTransferService.COOKIE)) {
            disconnect(Component.literal("Acesso ao lobby de testes exige /mundodev no Atlas."));
            return;
        }
        atlas$devCookieReceived = true;
        DevTransferService.claim(packet.payload(), requestedUsername).whenComplete((identity, error) -> server.execute(() -> {
            if (!connection.isConnected()) return;
            if (error != null || identity == null) {
                Throwable cause = error;
                while (cause != null && cause.getCause() != null) cause = cause.getCause();
                AtlasMod.LOGGER.warn("Autorização dev falhou para {}: {}.", requestedUsername,
                        cause == null ? "identidade ausente" : cause.getClass().getSimpleName());
                disconnect(Component.literal("Acesso negado. Entre no Atlas, faça login e use /mundodev com cargo Dono/ADM."));
                return;
            }
            DevTransferService.admit(connection, identity);
            startClientVerification(new GameProfile(identity.uuid(), identity.username()));
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
