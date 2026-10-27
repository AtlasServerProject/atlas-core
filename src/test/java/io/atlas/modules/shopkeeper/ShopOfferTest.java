package io.atlas.modules.shopkeeper;
import io.atlas.modules.shopkeeper.model.ShopOffer;
public final class ShopOfferTest {
    private static void check(boolean b){if(!b)throw new AssertionError();}
    private static void invalid(Runnable r){try{r.run();throw new AssertionError("accepted invalid input");}catch(IllegalArgumentException expected){}}
    public static void main(String[]args){
        var o=new ShopOffer("balls","cobblemon:poke_ball",200,50,1,10);
        check(o.total(true,64)==12800&&o.total(false,64)==3200);
        invalid(()->o.total(true,0));invalid(()->o.total(false,-1));invalid(()->o.total(true,65));
        invalid(()->new ShopOffer("b","c:x",100,100,1,0));invalid(()->new ShopOffer("b","c:x",0,0,1,0));
        invalid(()->new ShopOffer("b","c:x",100,0,Double.NaN,0));invalid(()->new ShopOffer("b","c:x",100,0,1,11));
        invalid(()->new ShopOffer("b","c:x",100,0,1,0).total(false,1));
        for(long day=0;day<10000;day++){var d=o.daily(day);check(d!=null&&d.buy()>=180&&d.buy()<=220&&d.sell()==50&&d.equals(o.daily(day)));}
        var close=new ShopOffer("b","c:x",100,99,1,10);for(int d=0;d<1000;d++)check(close.daily(d).buy()>close.sell());
        var rare=new ShopOffer("b","c:x",100,25,.5,0);int stocked=0;for(int d=0;d<1000;d++)if(rare.daily(d)!=null)stocked++;check(stocked>0&&stocked<1000);
        check(new ShopOffer("b","c:x",1_000_000_000L,1,1,0).total(true,64)==64_000_000_000L);
        System.out.println("ShopOfferTest PASS: invalid quantities/prices, sell-disabled, stable daily stock, price boundaries, anti-arbitrage and large totals");
    }
}
