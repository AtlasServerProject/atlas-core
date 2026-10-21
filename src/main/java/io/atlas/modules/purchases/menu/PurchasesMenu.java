package io.atlas.modules.purchases.menu;
import io.atlas.modules.purchases.service.PurchasesService;
import io.atlas.modules.vip.service.VipDeliveryService.Purchase;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class PurchasesMenu extends ChestMenu {
 private final Purchase[] purchases;private final int page;private final Purchase confirmation;private boolean used;
 public PurchasesMenu(int id,Inventory inventory,Purchase[] items,int page,Purchase confirmation){super(MenuType.GENERIC_9x4,id,inventory,contents(items,page,confirmation),4);this.purchases=items;this.page=page;this.confirmation=confirmation;}
 private static ItemStack icon(Item item,String name,String... lore){var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));stack.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(Component::literal).map(x->(Component)x).toList()));return stack;}
 private static SimpleContainer contents(Purchase[] items,int page,Purchase confirmation){var container=new SimpleContainer(36);
  if(confirmation!=null){container.setItem(13,icon(Items.EMERALD,"§aConfirmar: "+confirmation.productName(),"§7O prazo de "+confirmation.days()+" dias começa na ativação.","§7Mesmo nível soma dias; maior nível pausa o inferior.","§eClique para confirmar a ativação."));container.setItem(31,icon(Items.BARRIER,"§cCancelar"));return container;}
  for(int i=0;i<Math.min(27,items.length);i++){var item=items[i];String status=switch(item.status()){case "AVAILABLE"->"§aDisponível — clique para ativar";case "ACTIVATED"->"§7Ativado";case "ACTIVATING"->"§eAtivação em andamento";default->"§cEm revisão";};container.setItem(i,icon(item.status().equals("AVAILABLE")?Items.CHEST:Items.PAPER,item.productName(),status,"§7Duração: "+item.days()+" dias","§7Compra: "+item.id().toString().substring(0,8)));}
  if(items.length==0)container.setItem(13,icon(Items.PAPER,"§7Nenhuma compra neste correio"));if(page>0)container.setItem(27,icon(Items.ARROW,"§ePágina anterior"));container.setItem(31,icon(Items.CLOCK,"§eAtualizar","§7Página "+(page+1)));if(items.length>27)container.setItem(35,icon(Items.ARROW,"§ePróxima página"));return container;
 }
 @Override public void clicked(int slot,int button,ClickType type,Player player){if(!(player instanceof ServerPlayer p)||type!=ClickType.PICKUP||used)return;if(confirmation!=null){if(slot==13){used=true;PurchasesService.activate(p,confirmation);}else if(slot==31){used=true;PurchasesService.open(p,page);}return;}if(slot>=0&&slot<Math.min(27,purchases.length)&&purchases[slot].status().equals("AVAILABLE")){used=true;PurchasesService.confirm(p,purchases[slot],page);}else if(slot==31||slot==27&&page>0||slot==35&&purchases.length>27){used=true;PurchasesService.open(p,slot==27?page-1:slot==35?page+1:page);}}
 @Override public ItemStack quickMoveStack(Player player,int index){return ItemStack.EMPTY;}
}
