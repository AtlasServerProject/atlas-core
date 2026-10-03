package io.atlas.modules.vip;
import io.atlas.module.AtlasModule;
import io.atlas.modules.vip.service.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
public final class VipModule implements AtlasModule {
 private static final VipService service=new VipService();private final VipDeliveryService delivery=new VipDeliveryService(service);private final VipRuntimeService runtime=new VipRuntimeService();
 public static VipService service(){return service;}
 public String getName(){return "Commercial VIP";}
 public void enable(){
  ServerLifecycleEvents.SERVER_STARTED.register(server->delivery.start());ServerLifecycleEvents.SERVER_STOPPING.register(server->delivery.stop());
  net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server)->service.invalidate(handler.player.getUUID()));
  ServerTickEvents.END_SERVER_TICK.register(runtime::tick);
 }
 public void disable(){delivery.stop();}
}
