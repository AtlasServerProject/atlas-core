package io.atlas.modules.legendary.model;
import java.util.*;
import java.util.random.RandomGenerator;
public final class LegendaryIvRules {
 private LegendaryIvRules(){}
 public static int[] ensure(int[] original,RandomGenerator random){
  if(original.length!=6)throw new IllegalArgumentException("Six IVs required");int[] values=original.clone();var choices=new ArrayList<Integer>();int perfect=0;
  for(int i=0;i<6;i++){if(values[i]<0||values[i]>31)throw new IllegalArgumentException("Invalid IV");if(values[i]==31)perfect++;else choices.add(i);}
  for(int remaining=3-perfect;remaining>0;remaining--){int index=random.nextInt(choices.size());values[choices.remove(index)]=31;}
  return values;
 }
}
