package io.atlas.modules.legendary.service;

import io.atlas.modules.legendary.config.LegendaryConfig;
import io.atlas.modules.legendary.model.LegendaryRules;
import io.atlas.modules.auth.AuthModule;
import com.cobblemon.mod.common.battles.BattleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.random.RandomGenerator;

public final class LegendaryEligibilityService {
    public boolean player(ServerPlayer p,LegendaryConfig c){
        return !p.isSpectator()&&(!p.isCreative()||c.main.allowCreative)
                &&AuthModule.getAuthService().isAuthenticated(p.getUUID())
                &&c.main.allowedWorlds.contains(p.level().dimension().location().toString())
                &&(!c.main.excludeBattlingPlayers||BattleRegistry.getBattleByParticipatingPlayer(p)==null);
    }
    public boolean environment(ServerLevel world,BlockPos pos,LegendaryConfig.Spawn e,LegendaryConfig c){
        String dimension=world.dimension().location().toString();
        String biome=world.getBiome(pos).unwrapKey().map(k->k.location().toString()).orElse("");
        return c.main.allowedWorlds.contains(dimension)&&e.dimensions.contains(dimension)&&e.biomes.contains(biome)
                &&pos.getY()>=e.minY&&pos.getY()<=e.maxY
                &&e.times.stream().anyMatch(t->LegendaryRules.timeMatches(t,world.getDayTime()))
                &&e.weather.stream().anyMatch(w->LegendaryRules.weatherMatches(w,world.isRaining(),world.isThundering()))
                &&e.requirements.stream().allMatch(r->r.equals("SKY_VISIBLE")?world.canSeeSky(pos):!world.canSeeSky(pos));
    }
    public BlockPos position(ServerPlayer player,LegendaryConfig.Spawn e,LegendaryConfig c,RandomGenerator random){
        var world=player.serverLevel();
        for(int i=0;i<c.main.positionAttempts;i++){
            double angle=random.nextDouble()*Math.PI*2;
            double radius=c.main.spawnMinDistance+random.nextDouble()*(c.main.spawnMaxDistance-c.main.spawnMinDistance);
            int x=player.getBlockX()+(int)Math.round(Math.cos(angle)*radius),z=player.getBlockZ()+(int)Math.round(Math.sin(angle)*radius);
            BlockPos pos=new BlockPos(x,player.getBlockY(),z);
            if(!world.hasChunkAt(pos))continue;
            if(world.dimension().equals(net.minecraft.world.level.Level.NETHER)||e.requirements.contains("UNDERGROUND")||e.biomes.stream().anyMatch(b->b.endsWith("deep_dark")||b.endsWith("lush_caves")||b.endsWith("dripstone_caves"))) {
                pos=new BlockPos(x,player.getBlockY()+random.nextInt(-4,5),z);
            } else {
                int y=world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x,z);
                pos=new BlockPos(x,e.surface.equals("AIR")?y+8:e.surface.equals("WATER")?y-2:y,z);
            }
            if(!world.getWorldBorder().isWithinBounds(pos)||!environment(world,pos,e,c))continue;
            if(e.surface.equals("WATER")) {if(!world.getFluidState(pos).is(FluidTags.WATER)||!world.getFluidState(pos.above()).is(FluidTags.WATER))continue;}
            else {
                if(!world.getBlockState(pos).getCollisionShape(world,pos).isEmpty()||!world.getBlockState(pos.above()).getCollisionShape(world,pos.above()).isEmpty()
                        ||!world.getFluidState(pos).isEmpty())continue;
                if(e.surface.equals("GROUND")&&!world.getBlockState(pos.below()).isFaceSturdy(world,pos.below(),net.minecraft.core.Direction.UP))continue;
            }
            if(world.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)||world.getBlockState(pos.below()).is(net.minecraft.world.level.block.Blocks.CACTUS)
                    ||world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.FIRE)||world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.SOUL_FIRE))continue;
            return pos;
        }
        return null;
    }
}
