package io.atlas.modules.competitive.service;
import java.util.*;
public final class CompetitivePolicy {
 public static final String VERSION="atlas-gen9-20261023-1";
 public static String format(String tier){return switch(tier){case "OU"->"gen9ou";case "UU"->"gen9uu";case "MONOTYPE"->"gen9monotype";case "UBERS"->"gen9ubers";case "AG"->"gen9anythinggoes";default->throw new IllegalArgumentException("Tier inválida");};}
 /** Ignore HP, status, sleep duration and PP; these change legitimately during battle. */
 public static String stable(String packed){
  var rows=new ArrayList<String>();for(String row:packed.split("\\]")){String[] f=row.split("\\|",-1);if(f.length<17)throw new IllegalArgumentException("Equipe nativa incompleta");for(int i:new int[]{3,4,5,9})f[i]="";rows.add(String.join("|",f));}Collections.sort(rows);return String.join("]",rows);
 }
 private CompetitivePolicy(){}
}
