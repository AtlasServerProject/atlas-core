package io.atlas.modules.crates.menu;
import io.atlas.modules.crates.repository.CrateRepository.KeyBalance;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.inventory.*;import net.minecraft.world.entity.player.*;import net.minecraft.world.item.*;import net.minecraft.world.SimpleContainer;import net.minecraft.core.component.DataComponents;import net.minecraft.world.item.component.ItemLore;import net.minecraft.network.chat.Component;import net.minecraft.server.level.ServerPlayer;import java.util.*;
/** Read-only virtual wallet. Icons are never withdrawable items. */
public final class KeyMenu extends ChestMenu {
 public KeyMenu(int id,Inventory inventory,List<KeyBalance> keys){super(MenuType.GENERIC_9x3,id,inventory,contents(keys),3);}
 private static ItemStack icon(Item item,String name,String... lore){var s=new ItemStack(item);s.set(DataComponents.CUSTOM_NAME,Component.literal(name));s.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(x->(Component)Component.literal(x)).toList()));return s;}
 private static int keyModel(String identifier){if(identifier.equals("legendary_monthly"))return 880110;for(int generation=1;generation<=9;generation++)if(identifier.equals("generation_"+generation))return 880100+generation;return 0;}
 private static SimpleContainer contents(List<KeyBalance> keys){var c=new SimpleContainer(27);var owned=keys.stream().filter(k->k.quantity()>0).toList();for(int i=0;i<Math.min(18,owned.size());i++){var k=owned.get(i);var item=icon(Items.TRIPWIRE_HOOK,"§eChave • "+k.name(),"§aSaldo virtual: "+k.quantity(),"§7Use o botão direito na respectiva caixa.","§7Cada abertura consome uma chave.","§7Não é um item físico.");int model=keyModel(k.identifier());if(model!=0)item.set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(model));c.setItem(i,item);}if(owned.isEmpty())c.setItem(13,icon(Items.PAPER,"§7Você não possui chaves","§7Suas chaves aparecerão aqui quando houver saldo."));c.setItem(22,icon(Items.CHEST,"§bCompras do site","§7Abrir seu correio de compras."));c.setItem(26,icon(Items.CLOCK,"§eAtualizar saldos"));return c;}
 @Override public void clicked(int slot,int button,ClickType type,Player player){if(!(player instanceof ServerPlayer p)||type!=ClickType.PICKUP||button!=0)return;if(slot==22)io.atlas.modules.purchases.service.PurchasesService.open(p,0);else if(slot==26)io.atlas.modules.crates.CrateModule.service().openKeys(p);}
 @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
}
