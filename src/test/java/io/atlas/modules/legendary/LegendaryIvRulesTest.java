package io.atlas.modules.legendary;
import io.atlas.modules.legendary.model.LegendaryIvRules;
import java.util.*;
public final class LegendaryIvRulesTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 public static void main(String[] args){var random=new Random(42);
  for(int n=0;n<10000;n++){int[] original=new int[6];for(int i=0;i<6;i++)original[i]=random.nextInt(32);int[] result=LegendaryIvRules.ensure(original,random);check(Arrays.stream(result).filter(v->v==31).count()>=3,"minimum 3 distinct perfect IVs");for(int i=0;i<6;i++)check(result[i]==original[i]||result[i]==31,"only promotion to perfect");check(Arrays.equals(result,LegendaryIvRules.ensure(result,random)),"idempotent, preserves 4/5/6 perfect IVs");}
  check(Arrays.stream(LegendaryIvRules.ensure(new int[6],random)).filter(v->v==31).count()==3,"zero-perfect receives exactly three");
  int[] six={31,31,31,31,31,31};check(Arrays.equals(six,LegendaryIvRules.ensure(six,random)),"six-perfect preserved");
  System.out.println("LegendaryIvRulesTest PASS: minimum, distinct stats, preservation, bounds and idempotence");
 }
}
