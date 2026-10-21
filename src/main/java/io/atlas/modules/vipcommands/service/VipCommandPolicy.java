package io.atlas.modules.vipcommands.service;
import java.util.*;
public final class VipCommandPolicy {
 public static boolean permits(Collection<String> identifiers,boolean authenticated,boolean administrator,int required){if(!authenticated)return false;if(administrator)return true;return identifiers.stream().anyMatch(identifier->level(identifier)>=required);}
 public static int level(String identifier){return switch(identifier.toUpperCase(Locale.ROOT)){case "VIP"->1;case "VIPPLUS","VIP+"->2;case "VIPPLUSPLUS","VIP++"->3;default->0;};}
}
