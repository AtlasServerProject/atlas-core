package io.atlas.modules.legendary.service;

import com.google.gson.*;
import io.atlas.modules.audit.repository.ActivityLogRepository;
import io.atlas.modules.legendary.config.LegendaryConfig;
import com.cobblemon.mod.common.pokemon.Pokemon;
import java.time.Instant;
import java.util.UUID;

/** Public announcement payloads only. Webhook credentials remain outside the Minecraft process. */
public final class LegendaryWebhookService {
    public static JsonObject embed(String title,String description,int color) {
        JsonObject e=new JsonObject();e.addProperty("title",title);e.addProperty("description",description.length()>3500?description.substring(0,3500):description);
        e.addProperty("color",color);e.addProperty("timestamp",Instant.now().toString());
        JsonObject footer=new JsonObject();footer.addProperty("text","Atlas • Eventos lendários");e.add("footer",footer);return e;
    }
    private static void field(JsonObject e,String name,String value,boolean inline) {
        if(!e.has("fields"))e.add("fields",new JsonArray());
        JsonObject f=new JsonObject();f.addProperty("name",name);f.addProperty("value",value.length()>1000?value.substring(0,1000):value);f.addProperty("inline",inline);e.getAsJsonArray("fields").add(f);
    }
    private static void publish(String kind,JsonObject embed) {
        JsonObject payload=new JsonObject();JsonArray embeds=new JsonArray();embeds.add(embed);payload.add("embeds",embeds);
        JsonObject mentions=new JsonObject();mentions.add("parse",new JsonArray());payload.add("allowed_mentions",mentions);
        JsonObject record=new JsonObject();record.addProperty("id",UUID.randomUUID().toString());record.addProperty("date",Instant.now().toString());record.addProperty("kind",kind);record.add("payload",payload);
        ActivityLogRepository.append("atlas-legendary-events",record);
    }
    public static String biomeLabel(String biome) {
        String key=biome.contains(":")?biome.substring(biome.indexOf(':')+1):biome;
        return switch(key) {
            case "plains"->"Planícies";case "forest"->"Floresta";case "birch_forest"->"Floresta de bétulas";
            case "dark_forest"->"Floresta escura";case "swamp"->"Pântano";case "desert"->"Deserto";
            case "jungle"->"Selva";case "ocean"->"Oceano";case "deep_ocean"->"Oceano profundo";
            case "taiga"->"Taiga";case "snowy_plains"->"Planícies nevadas";case "savanna"->"Savana";
            case "the_end"->"The End";case "nether_wastes"->"Nether";
            default->key.replace('_',' ');
        };
    }
    public static void warning(int minutes) {
        publish("WARNING",embed("✦ Uma energia misteriosa se aproxima…","Uma tentativa de aparição lendária acontecerá em **"+minutes+" minutos**!\nPrepare-se para explorar o Atlas.\n\nA tentativa depende das condições do mundo e não garante uma aparição.",0xF6C85F));
    }
    public static void spawn(LegendaryConfig c,LegendaryConfig.Spawn rule,Pokemon pokemon,String biome,boolean test) {
        if(test)return;
        var message=c.messages.get(rule.announcement);
        JsonObject e=embed("✨ "+rule.displayName+" apareceu no Atlas!",message.story,0x67E8F9);
        field(e,"Situação","🟢 Presente no mundo",true);field(e,"Bioma",biomeLabel(biome),true);
        field(e,"Shiny",pokemon.getShiny()?"✨ Sim!":"Não",true);
        field(e,"Tempo de aparição",c.main.despawnAfterMinutes+" minutos — pode se estender durante batalha.",false);
        publish("SPAWN",e);
    }
    public static JsonObject captureEmbed(String species,JsonObject data) {
        JsonObject e=embed("🏆 "+species+" foi capturado!","Uma nova conquista lendária na comunidade Atlas!",data.get("shiny").getAsBoolean()?0xF6C85F:0x57F287);
        field(e,"Treinador",data.get("trainer").getAsString(),true);
        field(e,"Shiny",data.get("shiny").getAsBoolean()?"✨ Sim!":"Não",true);
        StringBuilder stats=new StringBuilder();
        for(String name:new String[]{"HP","Attack","Defense","Sp. Attack","Sp. Defense","Speed"})stats.append(name).append(": **").append(data.get(name).getAsInt()).append("**\n");
        field(e,"IVs",stats.toString(),false);
        field(e,"IVs totais",data.get("total").getAsInt()+"/186 • "+data.get("percentage").getAsString()+"%",true);
        field(e,"Qualidade",data.get("quality").getAsString(),true);
        field(e,"Natureza",data.get("nature").getAsString(),true);
        field(e,"Habilidade",data.get("ability").getAsString(),true);return e;
    }
    public static void capture(LegendaryConfig.Spawn rule,JsonObject data,boolean test) {
        if(!test)publish("CAPTURE",captureEmbed(rule.displayName,data));
    }
    public static void outcome(LegendaryConfig.Spawn rule,String status,boolean test) {
        if(test)return;
        String text=switch(status){case "DEFEATED"->"foi derrotado em batalha.";case "EXPIRED"->"partiu após o tempo de aparição se esgotar.";case "DESPAWNED","LOST"->"não está mais presente no mundo. Sua energia se dissipou.";default->null;};
        if(text!=null)publish(status,embed("✦ Fim do encontro lendário", "**"+rule.displayName+"** "+text,status.equals("DEFEATED")?0xFB7185:0xA78BFA));
    }
    private LegendaryWebhookService() {}
}
