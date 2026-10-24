package io.atlas.modules.legendary;
import com.google.gson.JsonObject;
import io.atlas.modules.legendary.service.LegendaryWebhookService;
public final class LegendaryWebhookTest {
 public static void main(String[] args) {
  JsonObject data=new JsonObject();data.addProperty("trainer","TreinadorTeste");data.addProperty("shiny",true);
  for(String s:new String[]{"HP","Attack","Defense","Sp. Attack","Sp. Defense","Speed"})data.addProperty(s,31);
  data.addProperty("total",186);data.addProperty("percentage","100");data.addProperty("quality","Perfeito");data.addProperty("nature","timid");data.addProperty("ability","synchronize");
  var embed=LegendaryWebhookService.captureEmbed("Mew",data);
  if(!embed.get("title").getAsString().contains("Mew"))throw new AssertionError("Espécie ausente");
  if(embed.getAsJsonArray("fields").size()!=7)throw new AssertionError("Campos incompletos");
  String text=embed.toString();for(String value:new String[]{"TreinadorTeste","186/186","100%","Perfeito","timid","synchronize","Sp. Defense"})if(!text.contains(value))throw new AssertionError("Campo ausente: "+value);
  if(!LegendaryWebhookService.biomeLabel("minecraft:plains").equals("Planícies"))throw new AssertionError("Bioma incorreto");
  LegendaryWebhookService.spawn(null,null,null,null,true);
  LegendaryWebhookService.capture(null,data,true);
  LegendaryWebhookService.outcome(null,"EXPIRED",true);
  System.out.println("LegendaryWebhookTest PASS: capture IVs, nature, ability, shiny, biome and test suppression");
 }
}
