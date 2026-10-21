package io.atlas.modules.legendary.config;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;
import java.nio.file.*;
import java.util.*;

/** Verify server-installed client assets; never assumes an unimplemented species has a model. */
public final class LegendaryModelRegistry {
    private LegendaryModelRegistry(){}
    private static Set<String> cache;
    public static synchronized Set<String> available() throws Exception {
        if(cache!=null)return cache;
        Set<String> models=new HashSet<>(),posers=new HashSet<>(),textures=new HashSet<>();List<Path> resolvers=new ArrayList<>();
        for(var mod:FabricLoader.getInstance().getAllMods())for(Path root:mod.getRootPaths()){
            Path assets=root.resolve("assets");if(!Files.isDirectory(assets))continue;
            try(var namespaces=Files.list(assets)){
                for(Path namespace:namespaces.toList()){
                    String ns=namespace.getFileName().toString();Path base=namespace.resolve("bedrock/pokemon");
                    if(Files.isDirectory(base))try(var paths=Files.walk(base)){
                        for(Path path:paths.filter(Files::isRegularFile).toList()){
                            String relative=base.relativize(path).toString();String name=path.getFileName().toString();
                            if(relative.startsWith("models/")&&name.endsWith(".json"))models.add(ns+":"+name.substring(0,name.length()-5));
                            if(relative.startsWith("posers/")&&name.endsWith(".json"))posers.add(ns+":"+name.substring(0,name.length()-5));
                            if(relative.startsWith("resolvers/")&&name.endsWith(".json"))resolvers.add(path);
                        }
                    }
                    Path tex=namespace.resolve("textures/pokemon");if(Files.isDirectory(tex))try(var paths=Files.walk(tex)){
                        paths.filter(Files::isRegularFile).forEach(p->textures.add(ns+":"+namespace.relativize(p)));
                    }
                }
            }
        }
        Set<String> supported=new HashSet<>();
        for(Path path:resolvers)try(var reader=Files.newBufferedReader(path)){
            JsonObject json=JsonParser.parseReader(reader).getAsJsonObject();
            if(!json.has("species")||!json.has("variations"))continue;
            for(JsonElement el:json.getAsJsonArray("variations")){
                var v=el.getAsJsonObject();
                if(v.has("model")&&v.has("poser")&&v.has("texture")&&models.contains(v.get("model").getAsString())&&posers.contains(v.get("poser").getAsString())&&textures.contains(v.get("texture").getAsString())){
                    supported.add(json.get("species").getAsString().replaceFirst("^[^:]+:",""));break;
                }
            }
        }catch(RuntimeException ignored){/* Unrelated malformed resolver does not enable a species. */}
        cache=Set.copyOf(supported);return cache;
    }
}
