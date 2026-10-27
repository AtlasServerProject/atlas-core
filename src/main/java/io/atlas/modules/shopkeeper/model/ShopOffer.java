package io.atlas.modules.shopkeeper.model;

/** Authoritative prices in whole CobbleDollars; never supplied by a client. */
public record ShopOffer(String shop, String item, long buy, long sell, double chance, int variation) {
    public ShopOffer {
        if (shop == null || item == null || buy < 1 || buy > 1_000_000_000L || sell < 0
                || sell >= buy || !Double.isFinite(chance) || chance <= 0 || chance > 1
                || variation < 0 || variation > 10) throw new IllegalArgumentException("Oferta inválida");
    }
    public long total(boolean purchase, int count) {
        if (count < 1 || count > 64 || !purchase && sell == 0) throw new IllegalArgumentException("Quantidade ou venda inválida");
        return Math.multiplyExact(purchase ? buy : sell, count);
    }
    public ShopOffer daily(long day) {
        var random = new java.util.Random(java.util.Objects.hash(day, shop, item));
        if (random.nextDouble() >= chance) return null;
        int percent = variation == 0 ? 0 : random.nextInt(variation * 2 + 1) - variation;
        long price = Math.max(sell + 1, Math.multiplyExact(buy, 100 + percent) / 100);
        return new ShopOffer(shop, item, price, sell, 1, 0);
    }
}
