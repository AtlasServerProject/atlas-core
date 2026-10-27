package io.atlas.modules.shopkeeper.menu;

import io.atlas.modules.shopkeeper.ShopkeeperModule;
import io.atlas.modules.shopkeeper.model.ShopOffer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;
import static io.atlas.modules.market.model.MarketPolicy.formatPrice;

/** Every slot is a visual button. No inventory movement, shift-click, drop or drag. */
public final class ShopkeeperMenu extends ChestMenu {
    private final Entity npc;
    private final List<ShopOffer> offers;
    private final int page;
    private final ShopOffer selected;
    private final boolean purchase;
    private int count;
    private final SimpleContainer contents;
    private boolean consumed;
    public ShopkeeperMenu(int id, Inventory inventory, Entity npc, List<ShopOffer> offers, int page, ShopOffer selected, boolean purchase, int count) {
        this(id,inventory,npc,offers,page,selected,purchase,count,new SimpleContainer(54));
    }
    private ShopkeeperMenu(int id, Inventory inventory, Entity npc,List<ShopOffer> offers,int page,ShopOffer selected,boolean purchase,int count,SimpleContainer contents) {
        super(MenuType.GENERIC_9x6,id,inventory,contents,6);
        this.contents=contents;this.npc=npc;this.offers=List.copyOf(offers);this.page=page;this.selected=selected;this.purchase=purchase;this.count=count;
        for(int i=0;i<54;i++){
            contents.setItem(i,icon(Items.GRAY_STAINED_GLASS_PANE," "));
            var old=slots.get(i);var locked=new Slot(contents,i,old.x,old.y){public boolean mayPlace(ItemStack s){return false;}public boolean mayPickup(Player p){return false;}};locked.index=i;slots.set(i,locked);
        }
        if(selected==null){
            for(int j=0;j<45&&page*45+j<offers.size();j++){
                var o=offers.get(page*45+j);var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(o.item()));
                contents.setItem(j,icon(item,item.getDescription().getString(),"§aComprar: §f"+formatPrice(o.buy())+" CobbleDollars",o.sell()>0?"§6Vender: §f"+formatPrice(o.sell())+" CobbleDollars":"§8Este item não é comprado pela loja","§7Esquerdo: comprar • Direito: vender"));
            }
            contents.setItem(45,icon(Items.ARROW,"§ePágina anterior"));contents.setItem(53,icon(Items.ARROW,"§ePróxima página"));
        }else{
            contents.setItem(22,icon(BuiltInRegistries.ITEM.get(ResourceLocation.parse(selected.item())),"§b"+(purchase?"Comprar":"Vender")+" ×"+count,"§7Total: §e"+formatPrice(selected.total(purchase,count))+" CobbleDollars","§7Somente itens originais sem alterações"));
            contents.setItem(29,icon(Items.RED_DYE,"§cDiminuir"));contents.setItem(33,icon(Items.LIME_DYE,"§aAumentar"));contents.setItem(31,icon(Items.EMERALD,"§aConfirmar "+(purchase?"compra":"venda")));contents.setItem(45,icon(Items.ARROW,"§eVoltar"));
        }
        contents.setItem(49,icon(Items.GOLD_INGOT,"§bSaldo e atendimento","§7Saldo: §f"+new io.atlas.modules.economy.EconomyService().getFormattedBalance((ServerPlayer)inventory.player),"§7Preços em moeda do jogo; confira antes de confirmar."));
    }
    private static ItemStack icon(Item item,String name,String...lines){var s=new ItemStack(item);s.set(DataComponents.CUSTOM_NAME,Component.literal(name));s.set(DataComponents.LORE,new ItemLore(Arrays.stream(lines).map(Component::literal).map(c->(Component)c).toList()));return s;}
    private void quantity(int value){
        if(value<1||value>64||selected==null)return;
        count=value;
        contents.setItem(22,icon(BuiltInRegistries.ITEM.get(ResourceLocation.parse(selected.item())),"§b"+(purchase?"Comprar":"Vender")+" ×"+count,"§7Total: §e"+formatPrice(selected.total(purchase,count))+" CobbleDollars","§7Somente itens originais sem alterações"));
        broadcastChanges();
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(!(player instanceof ServerPlayer)||consumed||!stillValid(player)||selected==null||button<1001||button>1064)return false;
        quantity(button-1000);return true;
    }
    public Entity npc(){return npc;}
    public void consume(){consumed=true;}
    @Override public boolean stillValid(Player p){return npc.isAlive()&&npc.level()==p.level()&&npc.distanceToSqr(p)<=64&&io.atlas.modules.auth.AuthModule.getAuthService().isAuthenticated(p.getUUID());}
    @Override public ItemStack quickMoveStack(Player p,int s){return ItemStack.EMPTY;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!(p instanceof ServerPlayer player)||consumed||!stillValid(p)||type!=ClickType.PICKUP||button<0||button>1)return;
        var service=ShopkeeperModule.service();
        if(selected==null){
            if(slot>=0&&slot<45&&page*45+slot<offers.size()){var o=offers.get(page*45+slot);if(button==1&&o.sell()==0)return;service.menu(player,npc,offers,page,o,button==0,1);}
            else if(slot==45&&page>0)service.menu(player,npc,offers,page-1,null,true,1);
            else if(slot==53&&(page+1)*45<offers.size())service.menu(player,npc,offers,page+1,null,true,1);
        }else if(slot==45)service.menu(player,npc,offers,page,null,true,1);
        else if(slot==29||slot==33)quantity(Math.max(1,Math.min(64,count+(slot==33?1:-1))));
        else if(slot==31){consumed=true;service.trade(player,this,selected,purchase,count);}
    }
}
