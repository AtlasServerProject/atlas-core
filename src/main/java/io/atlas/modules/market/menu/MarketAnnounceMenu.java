package io.atlas.modules.market.menu;

import io.atlas.modules.market.MarketModule;
import io.atlas.modules.market.model.MarketPolicy;
import io.atlas.modules.market.repository.MarketRepository.Draft;
import com.cobblemon.mod.common.Cobblemon;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Read-only wizard: category, party member, price, then the existing confirmation. */
public final class MarketAnnounceMenu extends ChestMenu {
    public enum Stage { TYPE, PARTY, PRICE }
    private static final long[] STEPS = {1,10,100,1000,10000,100000,1000000,10000000,100000000};
    private final Stage stage;
    private final SimpleContainer contents;
    private final UUID[] party = new UUID[6];
    private final Draft selected;
    private long price;
    private int durationSeconds;
    private int maximumSeconds;
    private static final int[] DURATIONS={1,30,60,300,900,1800,3600,21600,43200,86400,172800,259200,345600,432000,518400,604800};
    private boolean used;

    public MarketAnnounceMenu(int id, Inventory inventory, Stage stage, Draft selected) {
        this(id, inventory, stage, selected, new SimpleContainer(stage==Stage.PRICE?36:27));
    }
    private MarketAnnounceMenu(int id, Inventory inventory, Stage stage, Draft selected, SimpleContainer contents) {
        super(stage==Stage.PRICE?MenuType.GENERIC_9x4:MenuType.GENERIC_9x3,id,inventory,contents,stage==Stage.PRICE?4:3);
        this.stage=stage; this.selected=selected; this.contents=contents;
        this.price=selected==null?100:selected.price();
        var player=(ServerPlayer)inventory.player;this.maximumSeconds=MarketModule.service().maximumSeconds(player);this.durationSeconds=selected==null?86400:Math.min(maximumSeconds,selected.durationSeconds());
        if(stage==Stage.TYPE) {
            contents.setItem(15,MarketVisual.icon(io.atlas.modules.market.service.MarketSpriteIcons.from(com.cobblemon.mod.common.api.pokemon.PokemonProperties.Companion.parse("pikachu gender=male", " ", "=").create()),"§bAnunciar Pokémon",List.of("§7Escolha um Pokémon da sua equipe.")));
            contents.setItem(11,icon(Items.EMERALD,"§6Anunciar item","§7Arraste a pilha para o centro do baú."));
            contents.setItem(18,icon(Items.ARROW,"§eVoltar ao GTS"));
        } else if(stage==Stage.PARTY) {
            var store=Cobblemon.INSTANCE.getStorage().getParty(player);
            for(int i=0;i<6;i++) {
                var pokemon=store.get(i);
                if(pokemon==null) contents.setItem(partySlot(i),icon(Items.GRAY_STAINED_GLASS_PANE,"§7Espaço "+(i+1)+" vazio"));
                else {
                    party[i]=pokemon.getUuid();
                    contents.setItem(partySlot(i),MarketVisual.pokemonChoice(pokemon,i+1));
                }
            }
            contents.setItem(18,icon(Items.ARROW,"§eVoltar"));
        } else renderPrice(player);
    }
    private static int partySlot(int i){return i<3?11+i:20+i-3;}
    private static ItemStack icon(Item item,String name,String... lore) {
        var stack=new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));
        stack.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(Component::literal).map(c->(Component)c).toList()));
        return stack;
    }
    private void renderPrice(ServerPlayer player) {
        contents.setItem(13,MarketMenu.preview(player,selected.kind(),selected.payload(),selected.name(),price,selected.sellerName()));
        contents.setItem(4,icon(Items.GOLD_INGOT,"§6Preço: "+MarketPolicy.formatPrice(price)+" CobbleDollars","§7Clique para redefinir para 1."));
        for(int i=0;i<STEPS.length;i++) contents.setItem(18+i,icon(Items.GOLD_NUGGET,"§eAjustar "+MarketPolicy.formatPrice(STEPS[i]),"§aClique esquerdo: adicionar","§cClique direito: diminuir"));
        contents.setItem(30,icon(Items.EMERALD,"§aRevisar anúncio","§7Preço total: "+MarketPolicy.formatPrice(price)+" CobbleDollars","§7Ainda será necessário confirmar a publicação."));
        contents.setItem(31,icon(Items.CLOCK,"§bDuração: "+MarketPolicy.duration(durationSeconds),"§7Máximo do seu cargo: "+MarketPolicy.duration(maximumSeconds),"§bEsquerdo: próximo • §eDireito: anterior"));
        contents.setItem(32,icon(Items.ARROW,"§eVoltar à escolha"));
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(!(player instanceof ServerPlayer p)||type!=ClickType.PICKUP||used||(button!=0&&button!=1))return;
        var service=MarketModule.service();
        if(stage==Stage.TYPE) {
            if(slot==15){used=true;service.choosePokemon(p);}
            else if(slot==11){used=true;service.chooseItem(p);}
            else if(slot==18){used=true;service.open(p,"ALL",false,0,"");}
        } else if(stage==Stage.PARTY) {
            for(int i=0;i<6;i++)if(slot==partySlot(i)&&party[i]!=null){used=true;service.pricePokemon(p,party[i]);return;}
            if(slot==18){used=true;service.announce(p);}
        } else {
            if(slot>=18&&slot<27){price=MarketPolicy.adjustPrice(price,STEPS[slot-18],button==0);renderPrice(p);broadcastChanges();}
            else if(slot==4){price=1;renderPrice(p);broadcastChanges();}
            else if(slot==30){used=true;service.review(p,selected.price(price).duration(durationSeconds));}
            else if(slot==31){var allowed=Arrays.stream(DURATIONS).filter(h->h<=maximumSeconds).toArray();int index=0;while(index<allowed.length-1&&allowed[index]!=durationSeconds)index++;durationSeconds=allowed[Math.floorMod(index+(button==0?1:-1),allowed.length)];renderPrice(p);broadcastChanges();}
            else if(slot==32){used=true;service.announce(p);}
        }
    }
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
