package io.atlas.modules.shopkeeper;
import io.atlas.modules.shopkeeper.repository.ShopkeeperRepository;
import java.util.*;
public final class ShopkeeperRepositoryTest {
    static void check(boolean b){if(!b)throw new AssertionError();}
    public static void main(String[]args)throws Exception{
        var r=new ShopkeeperRepository(args[0],"synthetic_operator","");var offers=r.load();check(offers.size()==78);check(offers.stream().map(o->o.shop()).distinct().count()==7);
        var player=UUID.randomUUID();var id=UUID.randomUUID();var o=offers.getFirst();r.begin(id,player,o,true,64);check(r.unsettled().contains(player));
        try{r.begin(UUID.randomUUID(),player,o,false,1);throw new AssertionError("parallel pending trade accepted");}catch(java.sql.SQLException expected){check("23505".equals(expected.getSQLState()));}
        r.state(id,"REVIEW");check(r.unsettled().contains(player));r.state(id,"COMPLETED");check(!r.unsettled().contains(player));
        var next=UUID.randomUUID();r.begin(next,player,o,false,1);r.state(next,"CANCELLED");check(!r.unsettled().contains(player));
        try{r.state(next,"BOGUS");throw new AssertionError("invalid state accepted");}catch(java.sql.SQLException expected){check("23514".equals(expected.getSQLState()));}
        var replacement=java.util.List.of(new io.atlas.modules.shopkeeper.model.ShopOffer("blocks","minecraft:stone",20,0,1,0));
        r.installCatalogue("synthetic-nine-v1",replacement);check(r.load().equals(replacement));
        r.installCatalogue("synthetic-nine-v1",java.util.List.of(o));check(r.load().equals(replacement));
        check(!r.unsettled().contains(player));
        System.out.println("ShopkeeperRepositoryTest PASS: 78 offers, 7 shops, audit, unique pending trade, review lock, completed/cancelled and invalid-state rejection");
    }
}
