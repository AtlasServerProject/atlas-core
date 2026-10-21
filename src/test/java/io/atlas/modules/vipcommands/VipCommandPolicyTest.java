package io.atlas.modules.vipcommands;
import io.atlas.modules.vipcommands.service.VipCommandPolicy;
import java.util.List;
public class VipCommandPolicyTest {
 public static void main(String[] args){
  for(String id:List.of("VIP","VIPPLUS","VIPPLUSPLUS","VIP+","VIP++")){if(!VipCommandPolicy.permits(List.of(id),true,false,1))throw new AssertionError(id);if(VipCommandPolicy.permits(List.of(id),false,false,1))throw new AssertionError("unauthenticated");}
  for(String id:List.of("VIP","VIPPLUS","SUP","MOD","MEMBER"))if(VipCommandPolicy.permits(List.of(id),true,false,3))throw new AssertionError("VIP3 exclusive: "+id);
  if(!VipCommandPolicy.permits(List.of("VIPPLUSPLUS"),true,false,3))throw new AssertionError("VIP3 inheritance");
  if(VipCommandPolicy.permits(List.of(),true,false,1))throw new AssertionError("expired or unranked");
  if(!VipCommandPolicy.permits(List.of("ADMIN"),true,true,3)||VipCommandPolicy.permits(List.of("DONO"),false,true,3))throw new AssertionError("staff authentication");
  System.out.println("PASS: VIP tier inheritance, VIP3-only spawns, expiry, staff and authentication.");
 }
}
