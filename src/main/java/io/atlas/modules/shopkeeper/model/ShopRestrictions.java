package io.atlas.modules.shopkeeper.model;
import java.util.Set;
/** Shared deny rules, independent of Minecraft for regression tests. */
public final class ShopRestrictions {
 private static final Set<String> MECHANISMS=Set.of("lectern","bell","redstone","redstone_block","redstone_torch","redstone_wall_torch","redstone_wire","repeater","comparator","observer","piston","sticky_piston","piston_head","moving_piston","dispenser","dropper","hopper","crafter","daylight_detector","target","tripwire","tripwire_hook","lever","tnt","redstone_lamp","note_block","jukebox","trapped_chest","lightning_rod","sculk_sensor","calibrated_sculk_sensor","sculk_shrieker","iron_door","iron_trapdoor","powered_rail","detector_rail","activator_rail","rail","command_block","chain_command_block","repeating_command_block");
 public static boolean bottleCap(String id){String p=id.substring(id.indexOf(':')+1).replace("_","");return p.contains("bottlecap")||p.equals("silvercap")||p.equals("goldcap")||p.equals("goldencap");}
 public static boolean redstone(String id){String p=id.substring(id.indexOf(':')+1);return p.contains("redstone")||MECHANISMS.contains(p)||p.endsWith("_button")||p.endsWith("_pressure_plate")||p.contains("piston")||p.contains("sculk_sensor");}
 public static boolean vip(String id){String p=id.substring(id.indexOf(':')+1);if(id.startsWith("zamega:"))return true;
  if(id.startsWith("mega_showdown:"))return !Set.of("keystone","mega_stone","blank_z","sparkling_stone_light","sparkling_stone_dark").contains(p);
  return p.endsWith("_mint")||p.contains("mint_leaf")||p.contains("mint_seeds")||bottleCap(id)||p.contains("master_ball")||p.contains("park_ball")||p.equals("griseous_orb")||p.equals("griseous_core")||p.equals("adamant_orb")||p.equals("lustrous_orb")||p.endsWith("_plate")||p.endsWith("_memory")||p.endsWith("_drive")||p.startsWith("rusted_")||p.contains("rotom_catalog")||p.equals("reveal_glass")||p.equals("prison_bottle")||p.equals("dna_splicers")||p.contains("solarizer")||p.contains("lunarizer")||p.equals("reins_of_unity");
 }
}
