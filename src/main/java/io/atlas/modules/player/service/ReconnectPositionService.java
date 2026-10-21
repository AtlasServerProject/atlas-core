package io.atlas.modules.player.service;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.player.model.ReconnectPosition;
import io.atlas.modules.player.repository.ReconnectPositionRepository;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.stats.Stats;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
public final class ReconnectPositionService {
    private final ReconnectPositionRepository repository=new ReconnectPositionRepository();
    private final Map<UUID,ReconnectPosition> pending=new HashMap<>(),saved=new HashMap<>();
    private int ticks;
    public void joined(ServerPlayer player) {
        var stored=repository.find(player.getUUID());
        stored.ifPresent(pos->{pending.put(player.getUUID(),pos);saved.put(player.getUUID(),pos);});
        if(stored.isEmpty()&&player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME))>0)pending.put(player.getUUID(),capture(player));
    }
    public void tick(MinecraftServer server) {
        for(var id:java.util.List.copyOf(pending.keySet())) {
            var player=server.getPlayerList().getPlayer(id);
            if(player==null){pending.remove(id);continue;}
            if(!AuthModule.getAuthService().isAuthenticated(id))continue;
            var pos=pending.remove(id);
            var key=ResourceLocation.tryParse(pos.dimension());
            var world=key==null?null:server.getLevel(ResourceKey.create(Registries.DIMENSION,key));
            if(world==null||!Double.isFinite(pos.x())||!Double.isFinite(pos.y())||!Double.isFinite(pos.z())||!Float.isFinite(pos.yaw())||!Float.isFinite(pos.pitch())||pos.y()<world.getMinBuildHeight()||pos.y()>world.getMaxBuildHeight()||!world.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(pos.x(),pos.y(),pos.z())))continue;
            player.stopRiding();player.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);player.resetFallDistance();
            player.teleportTo(world,pos.x(),pos.y(),pos.z(),pos.yaw(),pos.pitch());
        }
        if(++ticks>=600){ticks=0;saveAll(server);}
    }
    public void disconnected(ServerPlayer player){save(player);pending.remove(player.getUUID());saved.remove(player.getUUID());}
    public void saveAll(MinecraftServer server){for(var player:server.getPlayerList().getPlayers())save(player);}
    private void save(ServerPlayer player) {
        if(!AuthModule.getAuthService().isAuthenticated(player.getUUID())||pending.containsKey(player.getUUID())||io.atlas.modules.moderation.ModerationModule.getStaffMode().skipPositionSave(player.getUUID()))return;
        var pos=capture(player);if(pos.equals(saved.get(player.getUUID())))return;
        repository.save(player.getUUID(),pos);saved.put(player.getUUID(),pos);
    }
    private ReconnectPosition capture(ServerPlayer p){return new ReconnectPosition(p.serverLevel().dimension().location().toString(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot());}
}
