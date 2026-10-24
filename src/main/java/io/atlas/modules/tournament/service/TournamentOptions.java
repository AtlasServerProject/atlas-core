package io.atlas.modules.tournament.service;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
public final class TournamentOptions {
 private static final Set<String> TIERS=Set.of("OU","UU","MONOTYPE","UBERS","AG");
 public static String tier(String input){String tier=input.toUpperCase(Locale.ROOT);if(!TIERS.contains(tier))throw new IllegalArgumentException("Tier inválida. Use OU, UU, MONOTYPE, UBERS ou AG (Gen 9).");return tier;}
 public static int capacity(int capacity){if(capacity!=8&&capacity!=16)throw new IllegalArgumentException("Escolha 8 ou 16 jogadores.");return capacity;}
 public static long seconds(String input){
  var matcher=Pattern.compile("(\\d+)([smh])").matcher(input.toLowerCase(Locale.ROOT));long total=0;int end=0;
  try{while(matcher.find()){if(matcher.start()!=end)throw new IllegalArgumentException();long value=Long.parseLong(matcher.group(1));long unit=switch(matcher.group(2)){case "h"->3600;case "m"->60;default->1;};total=Math.addExact(total,Math.multiplyExact(value,unit));end=matcher.end();}}
  catch(RuntimeException e){throw invalidTime();}
  if(end!=input.length()||total<60||total>86400)throw invalidTime();return total;
 }
 private static IllegalArgumentException invalidTime(){return new IllegalArgumentException("Prazo inválido. Use de 1m a 24h; exemplos: 15m, 1h, 1h30m.");}
}
