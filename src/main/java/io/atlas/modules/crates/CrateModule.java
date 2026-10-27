package io.atlas.modules.crates;
import io.atlas.module.AtlasModule;import io.atlas.modules.crates.service.CrateService;import net.fabricmc.fabric.api.event.lifecycle.v1.*;import net.fabricmc.fabric.api.event.player.*;import net.minecraft.world.*;import net.minecraft.server.level.ServerPlayer;
public final class CrateModule implements AtlasModule {
 private static final CrateService SERVICE=new CrateService();public static CrateService service(){return SERVICE;}public String getName(){return "Caixas de geração — teste";}
 public void enable(){
  ServerLifecycleEvents.SERVER_STARTED.register(SERVICE::start);ServerLifecycleEvents.SERVER_STOPPING.register(s->SERVICE.stop());ServerTickEvents.END_SERVER_TICK.register(SERVICE::tick);
  AttackBlockCallback.EVENT.register((player,world,hand,pos,direction)->{var pool=SERVICE.at(world.dimension().location().toString(),pos);if(pool==null)return InteractionResult.PASS;if(!world.isClientSide&&hand==InteractionHand.MAIN_HAND&&player instanceof ServerPlayer p&&!p.isSpectator()&&p.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64)SERVICE.preview(p,pool,0);return InteractionResult.SUCCESS;});
  UseBlockCallback.EVENT.register((player,world,hand,hit)->{var pool=SERVICE.at(world.dimension().location().toString(),hit.getBlockPos());if(pool==null)return InteractionResult.PASS;if(!world.isClientSide&&hand==InteractionHand.MAIN_HAND&&player instanceof ServerPlayer p&&!p.isSpectator()&&p.distanceToSqr(hit.getBlockPos().getX()+.5,hit.getBlockPos().getY()+.5,hit.getBlockPos().getZ()+.5)<=64)SERVICE.roll(p,pool);return InteractionResult.SUCCESS;});
  PlayerBlockBreakEvents.BEFORE.register((world,player,pos,state,entity)->SERVICE.at(world.dimension().location().toString(),pos)==null);
 }
 public void disable(){}
}
