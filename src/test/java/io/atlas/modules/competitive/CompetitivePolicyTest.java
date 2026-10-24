package io.atlas.modules.competitive;
import io.atlas.modules.competitive.service.CompetitivePolicy;
import io.atlas.modules.competitive.repository.CompetitiveValidator;
public final class CompetitivePolicyTest {
 private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
 public static void main(String[] args){
  String packed="Pikachu||00000000-0000-0000-0000-000000000001|200||0|leftovers|static|thunderbolt|15/15|Timid|4,0,0,252,0,252|M|,,,,,||100|255,pokeball,,,0,Electric";
  String changed=packed.replace("|200||0|","|120|brn|3|").replace("15/15","1/15");
  check(CompetitivePolicy.stable(packed).equals(CompetitivePolicy.stable(changed)),"normal HP/status/PP changes are allowed");
  check(!CompetitivePolicy.stable(packed).equals(CompetitivePolicy.stable(packed.replace("static","hugepower"))),"ability change detected");
  check(!CompetitivePolicy.stable(packed).equals(CompetitivePolicy.stable(packed.replace("leftovers","kingsrock"))),"external item change detected");
  check(!CompetitivePolicy.stable(packed).equals(CompetitivePolicy.stable(packed.replace("4,0,0,252,0,252","252,252,252,0,0,0"))),"EV change detected");
  var validator=new CompetitiveValidator();var legal=validator.validate("OU",packed,packed);check(legal.get("ok").getAsBoolean(),"native packed sets validated through subprocess");
  var invalid=validator.validate("OU",packed.replace("static","hugepower"),packed);check(!invalid.get("ok").getAsBoolean(),"native illegal ability rejected");
  check(legal.get("version").getAsString().equals(CompetitivePolicy.VERSION),"version binding");
  System.out.println("CompetitivePolicyTest PASS: mutable battle state accepted, external ability/item/EV changes rejected, Java-to-Node native packed validation, and snapshot version");
 }
}
