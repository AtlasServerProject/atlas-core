package io.atlas.modules.ranked.service;
public final class EloRules {
 public static final int INITIAL=1000;
 public static int delta(int winner,int loser){
  if(winner<0||loser<0)throw new IllegalArgumentException("Invalid rating");
  double expected=1.0/(1.0+Math.pow(10.0,((double)loser-winner)/400.0));
  return Math.min(loser,(int)Math.round(32.0*(1.0-expected)));
 }
 private EloRules(){}
}
