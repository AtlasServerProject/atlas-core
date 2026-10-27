package io.atlas.modules.shopkeeper.service;

import io.atlas.AtlasMod;
import io.atlas.modules.auth.AuthModule;
import io.atlas.modules.lobby.service.LobbyWorlds;
import io.atlas.modules.shopkeeper.menu.ShopkeeperMenu;
import io.atlas.modules.shopkeeper.model.ShopOffer;
import io.atlas.modules.shopkeeper.repository.ShopkeeperRepository;
import io.atlas.modules.research.service.ResearchMoneyService;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.EntityHitResult;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

public final class ShopkeeperService {
    private static final Map<String,String> SHOPS=Map.ofEntries(
        Map.entry("atlas_npc_pokemart_ground_vendor_1","blocks"),Map.entry("atlas_npc_pokemart_ground_vendor_2","misc"),Map.entry("atlas_npc_pokemart_ground_vendor_3","machines"),
        Map.entry("atlas_npc_pokemart_middle_vendor_1","balls"),Map.entry("atlas_npc_pokemart_middle_vendor_2","tms"),Map.entry("atlas_npc_pokemart_middle_vendor_3","held"),
        Map.entry("atlas_npc_pokemart_vendor_1","evolution"),Map.entry("atlas_npc_pokemart_vendor_2","ev"),Map.entry("atlas_npc_pokemart_vendor_3","furniture"));
    private final ShopkeeperRepository repository=new ShopkeeperRepository();
    private final ResearchMoneyService money=new ResearchMoneyService();
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"atlas-shopkeepers");t.setDaemon(true);return t;});
    private final Set<UUID> busy=ConcurrentHashMap.newKeySet(),review=ConcurrentHashMap.newKeySet();
    private volatile List<ShopOffer> catalogue=List.of();
    private volatile boolean ready;
    private MinecraftServer server;
    public void start(MinecraftServer server){this.server=server;var generated=ShopCatalogue.build();worker.execute(()->{try{repository.installCatalogue(ShopCatalogue.VERSION,generated);review.addAll(repository.unsettled());catalogue=repository.load();ready=true;AtlasMod.LOGGER.info("Lojistas Atlas: {} ofertas carregadas, {} contas aguardando conferência",catalogue.size(),review.size());}catch(Exception e){AtlasMod.LOGGER.error("Lojistas indisponíveis: catálogo não carregado",e);}});}
    public void reload(net.minecraft.commands.CommandSourceStack source){
        worker.execute(()->{try{
            var loaded=repository.load();var pending=repository.unsettled();
            server.execute(()->{catalogue=loaded;review.clear();review.addAll(pending);ready=true;
                source.sendSuccess(()->Component.literal("Lojistas atualizados: "+loaded.size()+" ofertas; "+pending.size()+" contas em conferência."),false);});
        }catch(Exception e){AtlasMod.LOGGER.error("Falha ao atualizar lojistas",e);server.execute(()->source.sendFailure(Component.literal("Não foi possível atualizar o catálogo.")));}});
    }
    public int diagnose(net.minecraft.commands.CommandSourceStack source){
        var missing=catalogue.stream().map(ShopOffer::item).filter(i->!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(i))).toList();
        source.sendSuccess(()->Component.literal("Lojistas Atlas: pronto="+ready+", lojas="+SHOPS.size()+", ofertas="+catalogue.size()+", itens ausentes="+missing+", operações em andamento="+busy.size()+", contas em conferência="+review.size()),false);return 1;
    }
    public void stop(){worker.shutdown();}
    private String shop(Entity npc){return SHOPS.entrySet().stream().filter(e->npc.getTags().contains(e.getKey())).map(Map.Entry::getValue).findFirst().orElse(null);}
    private boolean eligible(ServerPlayer p,Entity npc){return !p.hasDisconnected()&&AuthModule.getAuthService().isAuthenticated(p.getUUID())&&!p.isCreative()&&!p.isSpectator()&&LobbyWorlds.isEmerald(p.level())&&npc.isAlive()&&npc.level()==p.level()&&npc.distanceToSqr(p)<=64&&shop(npc)!=null&&com.cobblemon.mod.common.battles.BattleRegistry.getBattleByParticipatingPlayer(p)==null;}
    private long day(){return server.overworld().getDayTime()/24000L;}
    private List<ShopOffer> offers(Entity npc){var key=shop(npc);long today=day();return catalogue.stream().filter(o->o.shop().equals(key)).filter(o->!io.atlas.modules.shopkeeper.model.ShopRestrictions.vip(o.item())&&!io.atlas.modules.shopkeeper.model.ShopRestrictions.redstone(o.item())).map(o->o.daily(today)).filter(Objects::nonNull).filter(o->BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(o.item()))).toList();}
    public InteractionResult interact(Player p,net.minecraft.world.level.Level world,InteractionHand hand,Entity npc,EntityHitResult hit){
        if(!(p instanceof ServerPlayer player)||!LobbyWorlds.isEmerald(world)||shop(npc)==null||!BuiltInRegistries.ENTITY_TYPE.getKey(npc.getType()).toString().startsWith("easy_npc:humanoid"))return InteractionResult.PASS;
        if(hand==InteractionHand.MAIN_HAND){
            if(!ready)message(player,"§cLoja temporariamente indisponível.");
            else if(busy.contains(player.getUUID()))message(player,"§eAguarde a conclusão da operação anterior.");
            else if(review.contains(player.getUUID()))message(player,"§cUma operação anterior precisa de conferência da staff.");
            else if(!eligible(player,npc))message(player,"§cFaça login, use Survival e termine a batalha para negociar.");
            else{menu(player,npc,offers(npc),0,null,true,1);}
        }
        return InteractionResult.SUCCESS;
    }
    public void menu(ServerPlayer p,Entity npc,List<ShopOffer> offers,int page,ShopOffer selected,boolean purchase,int count){
        if(!eligible(p,npc)||busy.contains(p.getUUID())||review.contains(p.getUUID()))return;
        p.openMenu(new SimpleMenuProvider((id,inventory,viewer)->new ShopkeeperMenu(id,inventory,npc,offers,page,selected,purchase,count),Component.literal("Atlas Mart • "+switch(shop(npc)){case "balls"->"Poké Bolas";case "blocks"->"Blocos de construção";case "misc"->"Miscelâneos";case "machines"->"Máquinas";case "tms"->"TMs";case "held"->"Held Items";case "ev"->"EVs e proteínas";case "furniture"->"CobbleFurnies";case "evolution"->"Evolução";case "battle"->"Itens de batalha";case "vitamins"->"Vitaminas";default->"Loja";})));
    }
    private <T>T main(Supplier<T> action)throws Exception{var f=new CompletableFuture<T>();server.execute(()->{try{f.complete(action.get());}catch(Throwable e){f.completeExceptionally(e);}});return f.get(30,TimeUnit.SECONDS);}
    private static void message(ServerPlayer p,String text){p.sendSystemMessage(Component.literal(text));}
    private ItemStack item(ShopOffer offer){return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(offer.item())));}
    private boolean room(ServerPlayer p,ItemStack item,int amount){int space=0;for(var s:p.getInventory().items){if(s.isEmpty())space+=item.getMaxStackSize();else if(ItemStack.isSameItemSameComponents(s,item))space+=Math.max(0,s.getMaxStackSize()-s.getCount());}return space>=amount;}
    private boolean hasItems(ServerPlayer p,ItemStack item,int amount){int found=0;for(var s:p.getInventory().items)if(ItemStack.isSameItemSameComponents(s,item))found+=s.getCount();return found>=amount;}
    private String validate(ServerPlayer p,ShopkeeperMenu source,ShopOffer offer,boolean purchase,int count){
        if(p.getTags().size()>990)return "Espere a conclusão de outras operações ou procure a staff.";
        if(!ready||!eligible(p,source.npc())||p.containerMenu!=source||!p.containerMenu.getCarried().isEmpty())return "Atendimento encerrado. Abra a loja novamente com o cursor vazio.";
        if(offers(source.npc()).stream().noneMatch(o->o.equals(offer)))return "O estoque/preço mudou. Abra a loja novamente.";
        try{long total=offer.total(purchase,count);var stack=item(offer);if(purchase){if(!room(p,stack,count))return "Libere espaço no inventário.";if(money.balance(p).compareTo(java.math.BigInteger.valueOf(total))<0)return "Saldo insuficiente.";}else if(!hasItems(p,stack,count))return "Você não tem essa quantidade de itens originais, sem nome ou alterações.";}
        catch(Exception e){return "Não foi possível validar itens e saldo. Nada foi negociado.";}
        return null;
    }
    public void trade(ServerPlayer p,ShopkeeperMenu source,ShopOffer offer,boolean purchase,int count){
        if(review.contains(p.getUUID())||!busy.add(p.getUUID()))return;
        String error=validate(p,source,offer,purchase,count);
        if(error!=null){busy.remove(p.getUUID());message(p,"§c"+error);p.closeContainer();return;}
        var transaction=UUID.randomUUID();
        worker.execute(()->{
            boolean begun=false;
            try{
                repository.begin(transaction,p.getUUID(),offer,purchase,count);begun=true;
                repository.state(transaction,"APPLYING");
                boolean applied=main(()->{
                    String changed=validate(p,source,offer,purchase,count);
                    if(changed!=null){message(p,"§c"+changed);return false;}
                    try{
                        long total=offer.total(purchase,count);var stack=item(offer);
                        if(purchase){money.move(p,transaction,total,"atlasShopMoney:",true,false);int left=count;while(left>0){var part=stack.copyWithCount(Math.min(left,stack.getMaxStackSize()));int n=part.getCount();if(!p.getInventory().add(part)||!part.isEmpty())throw new IllegalStateException("Delivery needs review");left-=n;}}
                        else{int left=count;for(var s:p.getInventory().items){if(ItemStack.isSameItemSameComponents(s,stack)){int n=Math.min(left,s.getCount());s.shrink(n);left-=n;if(left==0)break;}}if(left!=0)throw new IllegalStateException("Sale needs review");money.move(p,transaction,total,"atlasShopMoney:",false,false);}
                        if(!p.addTag("atlasShopItems:"+transaction))throw new IllegalStateException("Receipt capacity reached");p.getInventory().setChanged();
                        ((io.atlas.mixin.PlayerListSaveInvoker)server.getPlayerList()).atlas$savePlayer(p);
                        var file=server.getWorldPath(net.minecraft.world.level.storage.LevelResource.PLAYER_DATA_DIR).resolve(p.getUUID()+".dat");
                        var disk=net.minecraft.nbt.NbtIo.readCompressed(file,net.minecraft.nbt.NbtAccounter.create(16777216));
                        boolean receipt=false;for(var tag:disk.getList("Tags",8))if(tag.getAsString().equals("atlasShopItems:"+transaction))receipt=true;
                        if(!receipt)throw new IllegalStateException("Receipt not durable");
                        return true;
                    }catch(Exception e){throw new IllegalStateException(e);}
                });
                repository.state(transaction,applied?"COMPLETED":"CANCELLED");
                if(applied)server.execute(()->{p.removeTag("atlasShopMoney:"+transaction);p.removeTag("atlasShopItems:"+transaction);((io.atlas.mixin.PlayerListSaveInvoker)server.getPlayerList()).atlas$savePlayer(p);message(p,"§a"+(purchase?"Compra":"Venda")+" concluída: ×"+count+" por "+io.atlas.modules.market.model.MarketPolicy.formatPrice(offer.total(purchase,count))+" CobbleDollars.");});
            }catch(Exception e){
                review.add(p.getUUID());
                if(begun){try{repository.state(transaction,"REVIEW");}catch(Exception auditError){AtlasMod.LOGGER.error("Lojista: estado pendente preservado {}",transaction,auditError);}}
                AtlasMod.LOGGER.error("Lojista: operação {} requer conferência; não repetir automaticamente",transaction,e);
                final boolean needsReview=begun;
                server.execute(()->message(p,needsReview?"§cOperação em conferência. Informe à staff: "+transaction:"§cLoja indisponível. Nenhuma operação foi iniciada."));
            }finally{busy.remove(p.getUUID());server.execute(()->{if(p.containerMenu==source)p.closeContainer();});}
        });
    }
}
