package io.atlas.modules.pokedex.menu;
import io.atlas.modules.pokedex.PokedexModule;
import io.atlas.modules.pokedex.repository.DexRewardRepository.Reward;
import io.atlas.modules.pokedex.repository.PokedexRepository.Progress;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
public final class DexRewardsMenu extends ChestMenu {
 private final int mark,page;private final String mode,selected,route;private final List<String> options;private final List<Reward> rewards;private boolean used;
 public DexRewardsMenu(int id,Inventory inventory,Progress progress,List<Reward> rewards,int mark,String mode,List<String> options,int page,String selected,String route){super(MenuType.GENERIC_9x6,id,inventory,contents(progress,rewards,mark,mode,options,page,selected,route),6);this.route=route;this.mark=mark;this.mode=mode;this.options=List.copyOf(options);this.page=page;this.selected=selected;this.rewards=List.copyOf(rewards);}
 private static ItemStack icon(Item item,String name,String... lore){var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));stack.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(Component::literal).map(x->(Component)x).toList()));return stack;}
 private static SimpleContainer contents(Progress progress,List<Reward> rewards,int mark,String mode,List<String> options,int page,String selected,String route){var c=new SimpleContainer(54);
  if(mode.equals("HOME")){
   double percent=progress.total()==0?0:progress.caught()*100.0/progress.total();c.setItem(4,icon(Items.BOOK,route.equals("main")?"§b§lRota principal":"§b§lRota "+io.atlas.modules.pokedex.model.TypeRoutePlan.name(route),String.format(Locale.forLanguageTag("pt-BR"),"§f%d/%d espécies • %.2f%%",progress.caught(),progress.total(),percent),"§7Clique em um marco para ver e resgatar.","§7Cada prêmio pode ser recebido uma vez."));
   var marks=route.equals("main")?java.util.stream.IntStream.rangeClosed(1,10).map(x->x*10).boxed().toList():io.atlas.modules.pokedex.model.TypeRoutePlan.MARKS;for(int i=0;i<marks.size();i++){int m=marks.get(i);var rows=rewards.stream().filter(r->r.milestone()==m).toList();boolean available=!rows.isEmpty();long delivered=rows.stream().filter(r->r.state().equals("DELIVERED")).count();boolean done=available&&delivered==rows.size();c.setItem(19+i,icon(done?Items.PAPER:available?Items.CHEST:Items.GRAY_DYE,(done?"§7":available?"§a":"§8")+m+"%",summary(m,route),available?done?"§7Prêmio recebido":"§eClique para abrir":"§7Alcance este marco para liberar."));}
   c.setItem(45,icon(Items.GOLD_BLOCK,"§6Hall dos Professores"));c.setItem(46,icon(Items.MAP,"§bRotas por geração"));c.setItem(47,icon(Items.BLAZE_POWDER,"§bRotas por tipo"));c.setItem(49,icon(Items.CLOCK,"§eAtualizar"));return c;
  }
  if(mode.equals("MARK")){
   if(io.atlas.modules.pokedex.model.TypeRoutePlan.TYPES.contains(route)&&progress.completed()!=null)c.setItem(33,icon(Items.NAME_TAG,"§6Usar título: "+io.atlas.modules.pokedex.model.TypeRoutePlan.title(route),"§eClique para selecionar o título no chat."));
   var rows=rewards.stream().filter(r->r.milestone()==mark).toList();c.setItem(4,icon(Items.BOOK,"§bPrêmio de "+mark+"%",summary(mark,route)));
   boolean claim=rows.stream().anyMatch(r->r.state().equals("AVAILABLE"));c.setItem(13,icon(claim?Items.EMERALD:Items.PAPER,claim?"§aResgatar prêmio":"§7Estado da entrega",rows.stream().allMatch(r->r.state().equals("DELIVERED"))?"§7Todas as partes recebidas.":"§7Partes entregues: "+rows.stream().filter(r->r.state().equals("DELIVERED")).count()+"/"+rows.size(),claim?"§eClique para confirmar o resgate.":"§7Atualize para acompanhar a entrega."));
   long mints=rows.stream().filter(r->r.kind().equals("MINT")&&r.state().equals("CHOICE")).count();if(mints>0)c.setItem(21,icon(Items.GREEN_DYE,"§aEscolher mint","§7Escolhas restantes: "+mints,"§eClique para escolher uma Nature."));
   if(rows.stream().anyMatch(r->r.kind().equals("LEGENDARY_CHOICE")&&r.state().equals("CHOICE")))c.setItem(23,icon(Items.NETHER_STAR,"§6Escolher lendário shiny","§7Pelo menos três IVs perfeitos.","§eClique para escolher a espécie."));
   if(rows.stream().anyMatch(r->r.state().equals("REVIEW")))c.setItem(31,icon(Items.BARRIER,"§cAguardando conferência da staff"));
   c.setItem(49,icon(Items.CLOCK,"§eAtualizar"));
  }else if(mode.equals("CONFIRM")){c.setItem(13,icon(Items.EMERALD,"§aConfirmar escolha", "§f"+selected,"§7Esta escolha é definitiva.","§eClique para confirmar."));}
  else{for(int i=0;i<Math.min(45,options.size()-page*45);i++){String name=options.get(page*45+i);c.setItem(i,icon(mode.equals("MINT")?Items.GREEN_DYE:mode.equals("LEGENDARY")?Items.NETHER_STAR:Items.PAPER,"§f"+name.replace("cobblemon:",""),"§eClique para selecionar."));}if(page>0)c.setItem(45,icon(Items.ARROW,"§eAnterior"));if(options.size()>(page+1)*45)c.setItem(53,icon(Items.ARROW,"§ePróxima"));}
  c.setItem(48,icon(Items.ARROW,"§eVoltar"));return c;
 }
 private static String summary(int m,String route){if(!route.equals("main"))return switch(m){case 10->"§78 Great Balls";case 25->"§78 Ultra Balls";case 50->"§78 Rare Candies";case 75->"§71 mint à escolha";case 100->io.atlas.modules.pokedex.model.GenerationRoutePlan.ROUTES.contains(route)?"§71 inicial aleatório da geração, shiny F3":"§71 shiny F3 do tipo + título temático";default->"";};return switch(m){case 10->"§716 Poké Balls";case 20->"§716 Great Balls";case 30->"§78 Ultra Balls";case 40->"§78 Rare Candies";case 50->"§7Pesquisador Atlas + 32 Ultra Balls + 8 Quick Balls";case 60->"§732 Quick Balls + 32 Dusk Balls + 16 Rare Candies";case 70->"§732 Rare Candies + 2 mints + 1 Ability Capsule";case 80->"§73 shinies aleatórios com 3 IVs perfeitos";case 90->"§7Lendário aleatório + 64 Rare Candies + 3 mints + 2 Patches";case 100->"§7Lendário shiny à escolha + Professor Atlas";default->"";};}
 @Override public void clicked(int slot,int button,ClickType type,Player player){if(!(player instanceof ServerPlayer p)||type!=ClickType.PICKUP||used)return;var service=PokedexModule.rewards();
  if(mode.equals("HOME")){if(slot>=19&&slot<(route.equals("main")?29:24)){int m=route.equals("main")?(slot-18)*10:io.atlas.modules.pokedex.model.TypeRoutePlan.MARKS.get(slot-19);if(rewards.stream().noneMatch(r->r.milestone()==m))return;used=true;service.open(p,route,m,"MARK",0,null);}else if(slot==49){used=true;service.open(p,route,0,"HOME",0,null);}else if(slot==45){used=true;p.closeContainer();PokedexModule.service().hall(p,0);}else if(slot==46||slot==47){used=true;service.open(p,route,0,slot==46?"GENERATION":"TYPE",0,null);}return;}
  if(slot==48){used=true;service.open(p,route,mark,mark==0?"HOME":"MARK",0,null);return;}
  if(mode.equals("MARK")){if(slot==33&&io.atlas.modules.pokedex.model.TypeRoutePlan.TYPES.contains(route)){used=true;p.closeContainer();PokedexModule.types().select(p,route);return;}if(slot==13&&rewards.stream().anyMatch(r->r.milestone()==mark&&r.state().equals("AVAILABLE"))){used=true;p.closeContainer();service.claim(p,route,mark);}else if(slot==21&&rewards.stream().anyMatch(r->r.milestone()==mark&&r.kind().equals("MINT")&&r.state().equals("CHOICE"))){used=true;service.open(p,route,mark,"MINT",0,null);}else if(slot==23&&rewards.stream().anyMatch(r->r.milestone()==mark&&r.kind().equals("LEGENDARY_CHOICE")&&r.state().equals("CHOICE"))){used=true;service.open(p,route,mark,"LEGENDARY",0,null);}else if(slot==49){used=true;service.open(p,route,mark,"MARK",0,null);}return;}
  if(mode.equals("CONFIRM")){if(slot==13){used=true;p.closeContainer();if(mark==100)service.chooseLegend(p,selected.replace("cobblemon:",""));else service.mint(p,route,mark,selected);}return;}
  if(slot==45&&page>0||slot==53&&options.size()>(page+1)*45){used=true;service.open(p,route,mark,mode,slot==45?page-1:page+1,null);return;}
  if(slot>=0&&slot<45&&page*45+slot<options.size()){used=true;String value=options.get(page*45+slot);if(mode.equals("REGION")||mode.equals("TYPE")){p.closeContainer();PokedexModule.service().show(p,(mode.equals("REGION")?"regiao:":"tipo:")+value);}else service.open(p,route,mark,"CONFIRM",0,value);}
 }
 @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
