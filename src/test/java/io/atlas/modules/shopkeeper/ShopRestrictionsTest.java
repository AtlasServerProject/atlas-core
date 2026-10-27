package io.atlas.modules.shopkeeper;
import io.atlas.modules.shopkeeper.model.ShopRestrictions;
public final class ShopRestrictionsTest {
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[]args){
  for(var id:new String[]{"minecraft:redstone","minecraft:redstone_torch","minecraft:oak_button","minecraft:light_weighted_pressure_plate","minecraft:piston","minecraft:comparator","minecraft:hopper","minecraft:crafter","minecraft:observer"})check(ShopRestrictions.redstone(id),id);
  for(var id:new String[]{"minecraft:stone","minecraft:oak_planks","cobblemon:pc","cobblemon:metal_coat"})check(!ShopRestrictions.redstone(id),id);
  for(var id:new String[]{"obc:bottle_cap","obc:bottle_cap_gold","obc:bottle_cap_attack_withered","obc:bottle_cap_special_defence"})check(ShopRestrictions.bottleCap(id)&&ShopRestrictions.vip(id),id);
  for(var id:new String[]{"cobblemon:master_ball","cobblemon:park_ball","cobblemon:adamant_mint","mega_showdown:charizardite_x","mega_showdown:normalium_z","mega_showdown:rotom_catalog","zamega:heatranite"})check(ShopRestrictions.vip(id),id);
  for(var id:new String[]{"mega_showdown:keystone","mega_showdown:mega_stone","mega_showdown:blank_z","mega_showdown:sparkling_stone_light","mega_showdown:sparkling_stone_dark","cobblemon:poke_ball","cobblemon:fire_stone"})check(!ShopRestrictions.vip(id),id);
  check(!ShopRestrictions.bottleCap("minecraft:honey_bottle"),"honey bottle remains craftable");
  System.out.println("ShopRestrictionsTest PASS: redstone mechanisms, all cap variants, VIP mega/form/Z/mints, five exceptions and normal items");
 }
}
