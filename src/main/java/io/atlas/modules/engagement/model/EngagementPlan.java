package io.atlas.modules.engagement.model;

import java.time.*;
import java.util.*;

public final class EngagementPlan {
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    public static final List<String> JOBS = List.of("miner","logger","farmer","fisher","trainer");
    public static String name(String job) { return switch(job) {
        case "miner" -> "Minerador"; case "logger" -> "Lenhador"; case "farmer" -> "Agricultor";
        case "fisher" -> "Pescador"; case "trainer" -> "Treinador"; default -> throw new IllegalArgumentException(); }; }
    public static int level(long xp) { int level=0; while(level<100 && xp>=threshold(level+1))level++; return level; }
    public static long threshold(int level) { return 100L*level*level; }
    public static long earnings(int from,int to) { return 50L*(to*(to+1L)-from*(from+1L)); }
    public static int nextDay(LocalDate last,int day,LocalDate today) {
        if(last==null)return 1;
        if(today.isBefore(last))throw new IllegalArgumentException("Data anterior ao último resgate");
        if(today.equals(last))return 0;
        return last.plusDays(1).equals(today)&&day<30?day+1:1;
    }
    public record Prize(String kind,String item,int amount,String label) {}
    public static Prize prize(int day) {
        if(day<1||day>30)throw new IllegalArgumentException();
        return switch(day) {
            case 7 -> new Prize("RANDOM","",1,"Pokémon aleatório F3");
            case 15 -> new Prize("SHINY","",1,"Shiny aleatório F3");
            case 21 -> new Prize("HA","",1,"Pokémon HA garantida F3 • Intransferível");
            case 30 -> new Prize("LEGENDARY_RANDOM","",1,"Lendário aleatório F3 • chance de shiny");
            case 1,4 -> new Prize("ITEM","cobblemon:poke_ball",day==1?16:32,"Poké Bolas");
            case 2,5 -> new Prize("ITEM","cobblemon:great_ball",day==2?12:24,"Great Balls");
            case 3 -> new Prize("ITEM","cobblemon:oran_berry",24,"Oran Berries");
            case 6,8,11 -> new Prize("ITEM","cobblemon:exp_candy_m",day==6?8:16,"Doces de experiência M");
            case 9,12,16,22,26 -> new Prize("ITEM","cobblemon:ultra_ball",day<16?24:48,"Ultra Balls");
            case 10,13,17,23,27 -> new Prize("ITEM","cobblemon:exp_candy_l",day<16?8:day<23?16:24,"Doces de experiência L");
            case 14,18,24,28 -> new Prize("ITEM","cobblemon:rare_candy",day<18?4:day<24?8:12,"Rare Candies");
            case 19 -> new Prize("ITEM","obc:bottle_cap",1,"Bottle Cap");
            case 20 -> new Prize("ITEM","cobblemon:protein",6,"Proteínas");
            case 25 -> new Prize("ITEM","cobblemon:exp_candy_xl",12,"Doces de experiência XL");
            case 29 -> new Prize("ITEM","obc:bottle_cap",3,"Bottle Caps");
            default -> throw new IllegalStateException();
        };
    }
}
