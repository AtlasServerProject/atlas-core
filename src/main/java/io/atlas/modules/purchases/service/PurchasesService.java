package io.atlas.modules.purchases.service;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.site.repository.SiteRepository;
import io.atlas.modules.vip.VipModule;
import io.atlas.modules.vip.service.VipDeliveryService.Purchase;
import io.atlas.modules.purchases.menu.PurchasesMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleMenuProvider;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
public final class PurchasesService {
 private static final Set<UUID> busy=ConcurrentHashMap.newKeySet();
 private static final SiteRepository identities=new SiteRepository();
 public static void open(ServerPlayer player,int page){
  if(page<0||!AuthModule.getAuthService().isAuthenticated(player.getUUID()))return;
  if(!busy.add(player.getUUID()))return;
  try{var owner=identities.identity(player.getUUID());player.displayClientMessage(Component.literal("§7Consultando seu correio de compras..."),false);
   VipModule.deliveries().purchases(owner.subject(),owner.playerId(),page).whenComplete((items,error)->player.getServer().execute(()->{busy.remove(player.getUUID());if(player.hasDisconnected()||!AuthModule.getAuthService().isAuthenticated(player.getUUID()))return;if(error!=null){player.displayClientMessage(Component.literal("§cCorreio temporariamente indisponível. Tente /compras novamente."),false);return;}player.openMenu(new SimpleMenuProvider((id,inventory,p)->new PurchasesMenu(id,inventory,items,page,null),Component.literal("Correio de compras")));}));
  }catch(Exception e){busy.remove(player.getUUID());player.displayClientMessage(Component.literal("§cNão foi possível consultar seu correio."),false);}
 }
 public static void confirm(ServerPlayer player,Purchase item,int page){if(!AuthModule.getAuthService().isAuthenticated(player.getUUID()))return;player.openMenu(new SimpleMenuProvider((id,inventory,p)->new PurchasesMenu(id,inventory,new Purchase[]{item},page,item),Component.literal("Confirmar ativação")));}
 public static void activate(ServerPlayer player,Purchase item){
  if(!AuthModule.getAuthService().isAuthenticated(player.getUUID())||!busy.add(player.getUUID()))return;
  player.closeContainer();
  try{var owner=identities.identity(player.getUUID());VipModule.deliveries().activate(item.id(),owner.subject(),owner.playerId()).whenComplete((ignored,error)->player.getServer().execute(()->{busy.remove(player.getUUID());if(player.hasDisconnected())return;player.displayClientMessage(Component.literal(error==null?"§aAtivação solicitada. Consulte /vip status ou /compras para confirmar a conclusão.":"§cNão foi possível confirmar a ativação. Consulte /compras antes de tentar novamente."),false);}));}catch(Exception e){busy.remove(player.getUUID());player.displayClientMessage(Component.literal("§cAtivação temporariamente indisponível."),false);}
 }
}
