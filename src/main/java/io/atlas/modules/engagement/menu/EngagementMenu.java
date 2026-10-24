package io.atlas.modules.engagement.menu;

import io.atlas.modules.engagement.model.EngagementPlan;
import io.atlas.modules.engagement.repository.EngagementRepository;
import io.atlas.modules.engagement.EngagementModule;
import net.minecraft.world.inventory.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.time.LocalDate;
import java.util.*;

public final class EngagementMenu extends ChestMenu {
    private final boolean daily;private final int availableDay;private boolean used;
    public EngagementMenu(int id,Inventory inventory,boolean daily,EngagementRepository.Daily day,List<EngagementRepository.Progress> progress,LocalDate today){super(MenuType.GENERIC_9x6,id,inventory,contents(daily,day,progress,today),6);this.daily=daily;this.availableDay=daily?EngagementPlan.nextDay(day.last(),day.day(),today):0;}
    private static ItemStack icon(Item item,String name,String... lore){var s=new ItemStack(item);s.set(DataComponents.CUSTOM_NAME,Component.literal(name));s.set(DataComponents.LORE,new ItemLore(Arrays.stream(lore).map(Component::literal).map(c->(Component)c).toList()));return s;}
    private static SimpleContainer contents(boolean daily,EngagementRepository.Daily d,List<EngagementRepository.Progress> progress,LocalDate today){var c=new SimpleContainer(54);
        if(daily){int next=EngagementPlan.nextDay(d.last(),d.day(),today);int current=next==0?d.day():next;
            c.setItem(4,icon(Items.CLOCK,"§bLogin diário • 30 dias","§7Resgate uma vez por dia em /diario.","§7Virada: meia-noite de Brasília.","§cPerdeu um dia? A sequência volta ao dia 1.","§7Após o dia 30, começa um novo ciclo."));
            for(int day=1;day<=30;day++){var prize=EngagementPlan.prize(day);boolean claimed=day<current||next==0&&day==current;boolean ready=day==next;
                c.setItem(9+day-1,icon(ready?Items.EMERALD:claimed?Items.LIME_DYE:Items.GRAY_DYE,"§eDia "+day,"§f"+prize.label()+" ×"+prize.amount(),claimed?"§aResgatado":ready?"§aDisponível — clique para resgatar":"§7Continue a sequência",prize.kind().equals("HA")?"§cHA garantida: intransferível":"§7Pokémon de prêmio têm pelo menos 3 IVs 31."));
            }
            c.setItem(49,icon(Items.CHEST,next>0?"§aResgatar dia "+next:"§7Resgate de hoje concluído","§7Inventário cheio? A entrega fica pendente."));
        }else{
            c.setItem(4,icon(Items.BOOK,"§bProfissões simultâneas","§7As cinco profissões evoluem juntas.","§7Até 100 níveis por profissão.","§7Cada nível paga 100 × nível CobbleDollars.","§7Limite de 5.000 XP/dia por profissão."));
            Item[] icons={Items.IRON_PICKAXE,Items.IRON_AXE,Items.WHEAT,Items.FISHING_ROD,Items.EXPERIENCE_BOTTLE};
            String[] actions={"Extraia minérios naturais.","Corte troncos naturais.","Colha plantações maduras.","Pesque itens ou Pokémon com a vara.","Capture Pokémon e vença batalhas selvagens."};
            for(int i=0;i<5;i++){String job=EngagementPlan.JOBS.get(i);var p=progress.stream().filter(v->v.job().equals(job)).findFirst().orElse(new EngagementRepository.Progress(job,0,0));int level=EngagementPlan.level(p.xp());
                c.setItem(20+i,icon(icons[i],"§b"+EngagementPlan.name(job),"§fNível "+level+" / 100","§eXP: "+p.xp()+(level<100?" / "+EngagementPlan.threshold(level+1):" • máximo"),"§7"+actions[i],"§6Disponível: "+EngagementPlan.earnings(p.paidLevel(),level)+" CobbleDollars"));}
            c.setItem(49,icon(Items.EMERALD,"§aResgatar ganhos dos níveis","§7Cada nível paga uma única vez.","§7Atividades apenas no Survival, Nether e End."));
        }c.setItem(45,icon(Items.CLOCK,"§eAtualizar"));return c;
    }
    @Override public void clicked(int slot,int button,ClickType type,Player player){if(used||type!=ClickType.PICKUP||!(player instanceof ServerPlayer p))return;if(slot==45){used=true;EngagementModule.service().open(p,daily);}else if(slot==49||(daily&&availableDay>0&&slot==8+availableDay)){used=true;EngagementModule.service().claim(p,daily);}}
    @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
}
