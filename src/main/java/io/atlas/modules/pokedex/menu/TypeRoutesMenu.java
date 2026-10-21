package io.atlas.modules.pokedex.menu;
import io.atlas.modules.pokedex.PokedexModule;
import io.atlas.modules.pokedex.model.TypeRoutePlan;
import io.atlas.modules.pokedex.repository.TypeRouteRepository.Progress;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class TypeRoutesMenu extends ChestMenu {
 private boolean used;
 private final boolean generations;
 public TypeRoutesMenu(int id,Inventory inventory,List<Progress> progress,boolean generations){super(MenuType.GENERIC_9x3,id,inventory,contents(progress,generations),3);this.generations=generations;}
 private static ItemStack icon(String name,String... lore){var s=new ItemStack(Items.BOOK);s.set(DataComponents.CUSTOM_NAME,Component.literal(name));s.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(Component::literal).map(c->(Component)c).toList()));return s;}
 private static SimpleContainer contents(List<Progress> progress,boolean generations){var routes=generations?io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES:TypeRoutePlan.TYPES;var c=new SimpleContainer(27);for(int i=0;i<routes.size();i++){String type=routes.get(i);var p=progress.stream().filter(x->x.type().equals(type)).findFirst().orElse(null);double percentage=p==null||p.total()==0?0:p.caught()*100.0/p.total();c.setItem(i,icon("§bRota "+TypeRoutePlan.name(type),String.format(Locale.forLanguageTag("pt-BR"),"§f%d/%d • %.2f%%",p==null?0:p.caught(),p==null?0:p.total(),percentage),"§7Marcos: 10%, 25%, 50%, 75%, 100%",generations?"§7Prêmio final: inicial shiny F3 aleatório.":p!=null&&p.completed()!=null?"§6Título: "+TypeRoutePlan.title(type):"§7Complete para ganhar shiny F3 e título.","§eClique para abrir esta rota."));}c.setItem(22,icon("§eVoltar à rota principal"));return c;}
 @Override public void clicked(int slot,int button,ClickType type,Player player){if(!(player instanceof ServerPlayer p)||type!=ClickType.PICKUP||used)return;if(slot>=0&&slot<(generations?9:18)){used=true;PokedexModule.rewards().open(p,(generations?io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES:TypeRoutePlan.TYPES).get(slot),0,"HOME",0,null);}else if(slot==22){used=true;PokedexModule.rewards().open(p);}}
 @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
