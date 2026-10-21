package io.atlas.modules.legendary.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import java.nio.file.*;
import java.util.*;

public final class LegendaryConfig {
    public static class Main {
        public boolean enabled, announceSpawn=true, announceCapture=true, enableShinyAnnouncements=true, enableSoundEffects=true,
                enableTitles, enableActionBar, allowCreative, excludeBattlingPlayers=true;
        public double spawnIntervalHours=5, globalSpawnChance=.15;
        public int warningMinutes=15,maxLegendaryPerCycle=1,despawnAfterMinutes=15,maxActiveLegendaryEvents=1,
                spawnMinDistance=24,spawnMaxDistance=64,positionAttempts=32;
        public List<String> allowedWorlds=List.of();
        public String offlinePolicy="SKIP";
        public long intervalMillis(){return (long)(spawnIntervalHours*3600000);}
    }
    public static class Spawn {
        public String species,displayName,rarity,surface,announcement,acquisition="AUTOMATIC";
        public int generation,minY,maxY,level=70;
        public double weight;
        public boolean enabled;
        public List<String> biomes,times,weather,dimensions,requirements=List.of();
    }
    public static class Message {
        public String title,story,captureTitle,primary,secondary,symbol,spawnText,captureText;
    }
    public static class Sound { public boolean enabled; public String id,category="MASTER"; public float volume=.6f,pitch=1; }
    public final Main main;
    public final List<Spawn> spawns;
    public final Map<String,Message> messages;
    public final Map<String,Double> rarities;
    public final Map<String,Sound> sounds;
    public final Map<String,String> unavailable=new LinkedHashMap<>();
    private LegendaryConfig(Main m,List<Spawn> s,Map<String,Message> msg,Map<String,Double> r,Map<String,Sound> snd){main=m;spawns=s;messages=msg;rarities=r;sounds=snd;}
    public static LegendaryConfig load(MinecraftServer server) throws Exception {
        Path dir=FabricLoader.getInstance().getConfigDir().resolve("atlas-legendary");Files.createDirectories(dir);
        for(String name:List.of("config.json","legendary-spawns.json","legendary-messages.json","legendary-rarities.json","legendary-sounds.json")) {
            Path target=dir.resolve(name);
            if(!Files.exists(target)) try(var in=LegendaryConfig.class.getResourceAsStream("/atlas-legendary/"+name)) {
                if(in==null)throw new IllegalStateException("Padrão ausente");Files.copy(in,target);
            }
        }
        Gson g=new Gson();
        Main m=read(g,dir,"config.json",Main.class);
        List<Spawn> s=read(g,dir,"legendary-spawns.json",new TypeToken<List<Spawn>>(){}.getType());
        Map<String,Message> msg=read(g,dir,"legendary-messages.json",new TypeToken<Map<String,Message>>(){}.getType());
        Map<String,Double> r=read(g,dir,"legendary-rarities.json",new TypeToken<Map<String,Double>>(){}.getType());
        Map<String,Sound> snd=read(g,dir,"legendary-sounds.json",new TypeToken<Map<String,Sound>>(){}.getType());
        if(m==null||s==null||msg==null||r==null||snd==null)throw new IllegalArgumentException("JSON vazio");
        if(!Double.isFinite(m.spawnIntervalHours)||m.spawnIntervalHours<.01||m.spawnIntervalHours>8760||m.warningMinutes<0||m.warningMinutes*60000L>=m.intervalMillis()
                ||!Double.isFinite(m.globalSpawnChance)||m.globalSpawnChance<0||m.globalSpawnChance>1||m.maxLegendaryPerCycle!=1||m.maxActiveLegendaryEvents<1||m.maxActiveLegendaryEvents>16
                ||m.despawnAfterMinutes<1||m.despawnAfterMinutes>1440||m.spawnMinDistance<8||m.spawnMaxDistance<m.spawnMinDistance||m.spawnMaxDistance>128||m.positionAttempts<1||m.positionAttempts>128
                ||!Set.of("SKIP","ATTEMPT_ONCE").contains(m.offlinePolicy))throw new IllegalArgumentException("Configuração global inválida");
        if(m.allowedWorlds==null||m.enabled&&m.allowedWorlds.isEmpty())throw new IllegalArgumentException("Mundos permitidos ausentes");
        for(String world:m.allowedWorlds)if(server.getLevel(net.minecraft.resources.ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(world)))==null)throw new IllegalArgumentException("Dimensão ausente: "+world);
        for(double w:r.values())if(!Double.isFinite(w)||w<0)throw new IllegalArgumentException("Peso de raridade inválido");
        LegendaryConfig result=new LegendaryConfig(m,List.copyOf(s),Map.copyOf(msg),Map.copyOf(r),Map.copyOf(snd));
        Set<String> modelSpecies=LegendaryModelRegistry.available();
        Set<String> ids=new HashSet<>();var biomes=server.registryAccess().registryOrThrow(Registries.BIOME);
        for(Spawn e:s){
            if(e.species==null||!e.species.matches("[a-z0-9_]+")||!ids.add(e.species)||e.displayName==null||e.displayName.length()>60||!Double.isFinite(e.weight)||e.weight<=0||e.minY>e.maxY||e.level<1||e.level>100
                    ||e.biomes==null||e.times==null||e.weather==null||e.dimensions==null||e.requirements==null||!r.containsKey(e.rarity)||!msg.containsKey(e.announcement)
                    ||!Set.of("GROUND","WATER","AIR").contains(e.surface)||!Set.of("AUTOMATIC","EVENT","EVOLUTION").contains(e.acquisition))throw new IllegalArgumentException("Espécie configurada incorretamente: "+e.species);
            for(String b:e.biomes)if(!biomes.containsKey(ResourceLocation.parse(b)))throw new IllegalArgumentException("Bioma inválido: "+b);
            for(String t:e.times)if(!Set.of("ANY","DAY","NIGHT","DAWN","DUSK","MORNING","AFTERNOON").contains(t))throw new IllegalArgumentException("Horário inválido");
            for(String w:e.weather)if(!Set.of("ANY","CLEAR","RAIN","THUNDER").contains(w))throw new IllegalArgumentException("Clima inválido");
            for(String req:e.requirements)if(!Set.of("SKY_VISIBLE","UNDERGROUND").contains(req))throw new IllegalArgumentException("Requisito não suportado: "+req);
            for(String d:e.dimensions)ResourceLocation.parse(d);
            if(e.enabled&&(e.biomes.isEmpty()||e.times.isEmpty()||e.weather.isEmpty()||e.dimensions.isEmpty()))throw new IllegalArgumentException("Condições vazias: "+e.species);
            var species=PokemonSpecies.getByName(e.species);
            if(species==null||!species.getImplemented())result.unavailable.put(e.species,"Espécie ausente ou não implementada");
            if(!modelSpecies.contains(e.species))result.unavailable.put(e.species,"Modelo, poser ou textura não confirmados nos mods instalados");
            if(e.enabled&&!e.acquisition.equals("AUTOMATIC"))throw new IllegalArgumentException("Evolução/evento não pode ser habilitado no ciclo automático: "+e.species);
        }
        for(Message value:msg.values())if(value==null||value.title==null||value.story==null||value.captureTitle==null||value.symbol==null||value.spawnText==null||value.captureText==null
                ||net.minecraft.network.chat.TextColor.parseColor(value.primary).result().isEmpty()||net.minecraft.network.chat.TextColor.parseColor(value.secondary).result().isEmpty())throw new IllegalArgumentException("Mensagem inválida");
        for(Sound sound:snd.values())if(sound==null||!Float.isFinite(sound.volume)||sound.volume<0||sound.volume>2||!Float.isFinite(sound.pitch)||sound.pitch<.1||sound.pitch>2||!Set.of("MASTER","MUSIC","RECORDS","WEATHER","BLOCKS","HOSTILE","NEUTRAL","PLAYERS","AMBIENT","VOICE").contains(sound.category)
                ||!net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.containsKey(ResourceLocation.parse(sound.id)))throw new IllegalArgumentException("Som inválido");
        return result;
    }
    private static <T>T read(Gson g,Path dir,String file,java.lang.reflect.Type type)throws Exception{
        Path path=dir.resolve(file);if(Files.size(path)>2_000_000)throw new IllegalArgumentException("JSON muito grande");try(var reader=Files.newBufferedReader(path)){return g.fromJson(reader,type);}
    }
    public boolean usable(Spawn e){return e.enabled&&!unavailable.containsKey(e.species)&&main.allowedWorlds.stream().anyMatch(e.dimensions::contains)&&rarities.get(e.rarity)>0;}
}
