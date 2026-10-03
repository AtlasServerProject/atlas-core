package io.atlas.modules.site;
import io.atlas.module.AtlasModule;
import io.atlas.modules.site.service.SiteService;
public final class SiteModule implements AtlasModule {
 private static final SiteService service=new SiteService();
 public static SiteService service(){return service;}
 public String getName(){return "Site";}
 public void enable(){service.start();}
 public void disable(){service.stop();}
}
