package io.atlas.modules.market.model;
import java.util.*;
public final class MarketPolicy {
 public static int limit(Collection<String> ranks){int level=ranks.stream().mapToInt(io.atlas.modules.vipcommands.service.VipCommandPolicy::level).max().orElse(0);return switch(level){case 1->6;case 2->8;case 3->10;default->5;};}
 public static String formatPrice(long value){return java.text.NumberFormat.getIntegerInstance(java.util.Locale.forLanguageTag("pt-BR")).format(value);}
 public static long adjustPrice(long value,long step,boolean increase){return Math.max(1,Math.min(1000000000L,increase?value+step:value-step));}
 public static int maxHours(Collection<String> ranks){int level=ranks.stream().mapToInt(io.atlas.modules.vipcommands.service.VipCommandPolicy::level).max().orElse(0);return switch(level){case 1->48;case 2->96;case 3->168;default->24;};}
 public static int maxSeconds(Collection<String> ranks){return maxHours(ranks)*3600;}
 public static int parseDuration(String input){
  if(input==null||input.length()>40)throw new IllegalArgumentException("Use um prazo como 30m, 24h, 1d ou 1d3h.");
  var matcher=java.util.regex.Pattern.compile("(?i)^(?:(\\d+)d)?(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?$").matcher(input);
  if(!matcher.matches()||input.isEmpty())throw new IllegalArgumentException("Use um prazo como 30m, 24h, 1d ou 1d3h.");
  long total=0;long[] units={86400,3600,60,1};
  try{for(int i=0;i<4;i++)if(matcher.group(i+1)!=null)total=Math.addExact(total,Math.multiplyExact(Long.parseLong(matcher.group(i+1)),units[i]));}catch(ArithmeticException|NumberFormatException error){throw new IllegalArgumentException("O prazo máximo é 7d.");}
  if(total<1||total>604800)throw new IllegalArgumentException("Use um prazo entre 1s e 7d, respeitando seu cargo.");return (int)total;
 }
 public static String duration(int seconds){var out=new ArrayList<String>();int[] units={86400,3600,60,1};String[] labels={"d","h","m","s"};for(int i=0;i<4;i++){int value=seconds/units[i];seconds%=units[i];if(value>0)out.add(value+labels[i]);}return out.isEmpty()?"0s":String.join(" ",out);}
 public static String remaining(java.time.Instant expires){if(expires==null)return "Aguardando publicação";long seconds=Math.max(0,java.time.Duration.between(java.time.Instant.now(),expires).getSeconds());return seconds==0?"Expirado":duration((int)Math.min(Integer.MAX_VALUE,seconds));}
 public static boolean price(long price){return price>=1&&price<=1000000000L;}
}
