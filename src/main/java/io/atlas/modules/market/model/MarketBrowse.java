package io.atlas.modules.market.model;
public record MarketBrowse(String kind,boolean own,int page,String search,Sort sort,ItemFilter itemFilter,PokemonFilter pokemonFilter) {
 public enum Sort { RECENT("Recentes primeiro","created_at DESC,id"), CHEAP("Menor preço","price ASC,created_at DESC,id"), EXPENSIVE("Maior preço","price DESC,created_at DESC,id"), EXPIRING("Expiram primeiro","expires_at ASC NULLS LAST,created_at DESC,id");public final String label,sql;Sort(String label,String sql){this.label=label;this.sql=sql;} }
 public enum ItemFilter { ALL("Todos"),BLOCKS("Blocos"),MINERALS("Minérios"),POKEBALLS("Poké Bolas"),MODIFIERS("Modificadores"),OTHER("Outros");public final String label;ItemFilter(String label){this.label=label;} }
 public enum PokemonFilter { NONE("Sem filtro"),ALL("Todos os Pokémon"),MALE("Somente machos ♂"),FEMALE("Somente fêmeas ♀"),HIDDEN("Hidden Ability"),SHINY("Apenas shiny"),LEGENDARY("Lendários e míticos");public final String label;PokemonFilter(String label){this.label=label;}public int mask(){return switch(this){case MALE->1;case FEMALE->2;case HIDDEN->4;case SHINY->8;case LEGENDARY->16;default->0;};} }
 public MarketBrowse {page=Math.max(0,page);if(!java.util.Set.of("ALL","ITEM","POKEMON").contains(kind))throw new IllegalArgumentException();}
 public static MarketBrowse defaults(String kind,boolean own,int page,String search){return new MarketBrowse(kind,own,page,search,Sort.RECENT,ItemFilter.ALL,PokemonFilter.NONE);}
 public MarketBrowse page(int page){return new MarketBrowse(kind,own,page,search,sort,itemFilter,pokemonFilter);}
 public MarketBrowse own(boolean own){return new MarketBrowse(kind,own,0,search,sort,itemFilter,pokemonFilter);}
 public MarketBrowse kind(String kind){return new MarketBrowse(kind,own,0,search,sort,ItemFilter.ALL,PokemonFilter.NONE);}
 public MarketBrowse sort(boolean next){return new MarketBrowse(kind,own,0,search,cycle(sort,next),itemFilter,pokemonFilter);}
 public MarketBrowse items(boolean next){var f=cycle(itemFilter,next);return new MarketBrowse("ITEM",own,0,search,sort,f,PokemonFilter.NONE);}
 public MarketBrowse pokemon(boolean next){var f=cycle(pokemonFilter,next);return new MarketBrowse(f==PokemonFilter.NONE?"ALL":"POKEMON",own,0,search,sort,ItemFilter.ALL,f);}
 public static <E extends Enum<E>> E cycle(E value,boolean next){var all=value.getDeclaringClass().getEnumConstants();return all[Math.floorMod(value.ordinal()+(next?1:-1),all.length)];}
}
