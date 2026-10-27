package io.atlas.modules.shopkeeper.repository;

import io.atlas.modules.database.DatabaseConfig;
import io.atlas.modules.shopkeeper.model.ShopOffer;
import java.sql.*;
import java.util.*;

public final class ShopkeeperRepository {
    private final String url,user,password;
    public ShopkeeperRepository(){this(DatabaseConfig.jdbcUrl(),DatabaseConfig.USER,DatabaseConfig.PASSWORD);}
    public ShopkeeperRepository(String url,String user,String password){this.url=url;this.user=user;this.password=password;}
    private Connection connect() throws SQLException {return DriverManager.getConnection(url,user,password);}

    public void installCatalogue(String version,List<ShopOffer> offers) throws SQLException {
        if(offers.isEmpty())throw new IllegalArgumentException("Catálogo vazio");
        try(var c=connect()){
            c.setAutoCommit(false);
            try{
                try(var lock=c.prepareStatement("SELECT pg_advisory_xact_lock(710026)")){lock.execute();}
                try(var q=c.prepareStatement("SELECT 1 FROM npc_shop_catalogue_versions WHERE version=?")){q.setString(1,version);try(var r=q.executeQuery()){if(r.next()){c.rollback();return;}}}
                try(var s=c.prepareStatement("UPDATE npc_shop_offers SET enabled=false")){s.executeUpdate();}
                try(var s=c.prepareStatement("INSERT INTO npc_shop_offers(shop_id,item_id,buy_price,sell_price,stock_chance,price_variation,sort_order,enabled) VALUES(?,?,?,?,?,?,?,true) ON CONFLICT(shop_id,item_id) DO UPDATE SET buy_price=excluded.buy_price,sell_price=excluded.sell_price,stock_chance=excluded.stock_chance,price_variation=excluded.price_variation,sort_order=excluded.sort_order,enabled=true")){
                    int order=0;for(var o:offers){s.setString(1,o.shop());s.setString(2,o.item());s.setLong(3,o.buy());s.setLong(4,o.sell());s.setDouble(5,o.chance());s.setInt(6,o.variation());s.setInt(7,order++);s.addBatch();}s.executeBatch();
                }
                try(var s=c.prepareStatement("INSERT INTO npc_shop_catalogue_versions(version,offer_count) VALUES(?,?)")){s.setString(1,version);s.setInt(2,offers.size());s.executeUpdate();}
                c.commit();
            }catch(Exception e){c.rollback();throw e;}
        }
    }
    public List<ShopOffer> load() throws SQLException {
        try (var c = connect(); var s = c.prepareStatement("SELECT shop_id,item_id,buy_price,sell_price,stock_chance,price_variation FROM npc_shop_offers WHERE enabled ORDER BY shop_id,sort_order,item_id"); var r = s.executeQuery()) {
            var out = new ArrayList<ShopOffer>();
            while (r.next()) out.add(new ShopOffer(r.getString(1),r.getString(2),r.getLong(3),r.getLong(4),r.getDouble(5),r.getInt(6)));
            return List.copyOf(out);
        }
    }
    public void begin(UUID id, UUID player, ShopOffer offer, boolean purchase, int count) throws SQLException {
        try(var c=connect();var s=c.prepareStatement("INSERT INTO npc_shop_transactions(id,player_uuid,shop_id,item_id,direction,quantity,total,state) VALUES (?,?,?,?,?,?,?,'PREPARED')")) {
            s.setObject(1,id);s.setObject(2,player);s.setString(3,offer.shop());s.setString(4,offer.item());s.setString(5,purchase?"BUY":"SELL");s.setInt(6,count);s.setLong(7,offer.total(purchase,count));s.executeUpdate();
        }
    }
    public void state(UUID id, String state) throws SQLException {
        try(var c=connect();var s=c.prepareStatement("UPDATE npc_shop_transactions SET state=?,updated_at=now() WHERE id=?")) {s.setString(1,state);s.setObject(2,id);s.executeUpdate();}
    }
    public Set<UUID> unsettled() throws SQLException {
        try(var c=connect();var s=c.prepareStatement("SELECT DISTINCT player_uuid FROM npc_shop_transactions WHERE state IN ('PREPARED','APPLYING','REVIEW')");var r=s.executeQuery()) {var out=new HashSet<UUID>();while(r.next())out.add(r.getObject(1,UUID.class));return out;}
    }
}
