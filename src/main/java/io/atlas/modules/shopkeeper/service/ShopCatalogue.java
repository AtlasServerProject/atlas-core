package io.atlas.modules.shopkeeper.service;
import io.atlas.modules.shopkeeper.model.*;
import net.minecraft.core.registries.*;
import net.minecraft.tags.TagKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import java.util.*;
public final class ShopCatalogue {
 public static final String VERSION="atlas-mart-nine-v1";
 private static boolean tag(ItemStack s,String key){return s.is(TagKey.create(Registries.ITEM,ResourceLocation.parse("cobblemon:"+key)));}
 private static final Set<String> MACHINES=Set.of("healing_machine","pc","pasture","fossil_analyzer","restoration_tank","fossil_restoration_tank","tm_machine");
 private static final Set<String> EV=Set.of("pomeg_berry","kelpsy_berry","qualot_berry","hondew_berry","grepa_berry","tamato_berry","protein","iron","calcium","zinc","carbos","hp_up","pp_up","pp_max");
 private static final Set<String> MEDICINE=Set.of("potion","super_potion","hyper_potion","max_potion","revive","max_revive","antidote","burn_heal","ice_heal","awakening","paralyze_heal","full_heal","full_restore","ether","max_ether","elixir","max_elixir");
 private static final Set<String> MISC=Set.of("torch","soul_torch","lantern","soul_lantern","chain","ladder","scaffolding","painting","item_frame","glow_item_frame","armor_stand","flower_pot","chest","barrel","crafting_table","furnace","blast_furnace","smoker","anvil","chipped_anvil","grindstone","stonecutter","loom","cartography_table","smithing_table","enchanting_table","bookshelf","book","paper","leather","glass_bottle","bucket","water_bucket","lava_bucket","shears","name_tag","lead","saddle");
 public static List<ShopOffer> build(){
  var out=new ArrayList<ShopOffer>();
  for(var item:BuiltInRegistries.ITEM){
   var key=BuiltInRegistries.ITEM.getKey(item);String id=key.toString(),p=key.getPath(),ns=key.getNamespace();var stack=new ItemStack(item);
   if(item==Items.AIR||ShopRestrictions.redstone(id)||ShopRestrictions.vip(id))continue;
   String shop=null;long price=0;
   if(ns.equals("minecraft")){
    if(MISC.contains(p)){shop="misc";price=p.contains("anvil")?10000:500;}
    else if(item instanceof BlockItem && !p.endsWith("_ore")&&!p.startsWith("deepslate_")&&!Set.of("structure_block","jigsaw","structure_void","barrier","light","debug_stick","bedrock","end_portal_frame","spawner","trial_spawner","vault","reinforced_deepslate","budding_amethyst","ancient_debris","dragon_egg","suspicious_sand","suspicious_gravel","crying_obsidian").contains(p)){shop="blocks";price=blockPrice(p);}
    // Deepslate building variants are allowed; exclude only ores.
    if(shop==null&&item instanceof BlockItem&&p.startsWith("deepslate_")&&!p.endsWith("_ore")){shop="blocks";price=30;}
   }else if(ns.equals("cobblefurnies")&&item instanceof BlockItem){shop="furniture";price=2000;}
   else if(ns.equals("tmcraft")&&p.startsWith("tm_")&&!p.equals("tm_move_recorder")){shop="tms";price=150000;}
   else if(ns.equals("mega_showdown")){shop="held";price=switch(p){case "keystone"->1000000;case "mega_stone"->2000000;case "blank_z"->1500000;default->1250000;};}
   else if(ns.equals("cobblemon")){
    if(MACHINES.contains(p)){shop="machines";price=p.equals("pc")?100000:p.equals("healing_machine")?150000:250000;}
    else if(item instanceof com.cobblemon.mod.common.item.PokeBallItem){shop="balls";price=p.contains("origin")||p.contains("beast")?50000:p.contains("ultra")?1200:p.contains("great")?600:2000;if(p.equals("poke_ball"))price=200;}
    else if(tag(stack,"evolution_items")||tag(stack,"evolution_stones")){shop="evolution";price=evolutionPrice(p);}
    else if(EV.contains(p)){shop="ev";price=p.endsWith("_berry")?100000:p.startsWith("pp_")?500000:250000;}
    else if(tag(stack,"held/is_held_item")){shop="held";price=Set.of("choice_band","choice_specs","choice_scarf","life_orb","leftovers","heavy_duty_boots","assault_vest","focus_sash","lucky_egg","exp_share").contains(p)?1000000:350000;}
    else if(MEDICINE.contains(p)||tag(stack,"berries")&&!EV.contains(p)){shop="misc";price=MEDICINE.contains(p)?5000:1000;}
   }
   if(shop!=null)out.add(new ShopOffer(shop,id,price,0,1,0));
  }
  out.sort(Comparator.comparing(ShopOffer::shop).thenComparing(ShopOffer::item));return List.copyOf(out);
 }
 private static long blockPrice(String p){if(Set.of("diamond_block","netherite_block","emerald_block","gold_block","raw_gold_block","raw_iron_block","iron_block","raw_copper_block","coal_block").contains(p))return 100000; if(p.contains("quartz")||p.contains("purpur")||p.contains("prismarine"))return 100;if(p.contains("copper"))return 150;if(p.contains("obsidian"))return 200;if(p.contains("glass")||p.contains("concrete")||p.contains("terracotta"))return 40;return 20;}
 public static long evolutionPrice(String p){
  if(Set.of("fire_stone","water_stone","thunder_stone","leaf_stone","ice_stone").contains(p))return 100000;
  if(p.endsWith("_stone")||p.equals("oval_stone"))return 200000;
  if(Set.of("chipped_pot","masterpiece_teacup","scroll_of_darkness","scroll_of_waters","auspicious_armor","malicious_armor","prism_scale","reaper_cloth","black_augurite","peat_block").contains(p))return 1500000;
  if(Set.of("metal_alloy","dubious_disc","electirizer","magmarizer","protector","galarica_wreath","galarica_cuff","ribbon_sweet","star_sweet").contains(p))return 750000;
  return 400000;
 }
}
