package io.atlas.modules.legendary.model;

import java.util.List;
import java.util.random.RandomGenerator;

/** Pure rules, independent of Minecraft: real calendar cycles and one global roll. */
public final class LegendaryRules {
    private LegendaryRules() {}
    public record Recovery(long next, long missed) {}
    public static Recovery recover(long next, long now, long interval) {
        if (interval <= 0) throw new IllegalArgumentException("Intervalo inválido");
        if (next > now) return new Recovery(next, 0);
        long missed = (now - next) / interval + 1;
        return new Recovery(Math.addExact(next, Math.multiplyExact(missed, interval)), missed);
    }
    public static boolean timeMatches(String period, long time) {
        long t = Math.floorMod(time, 24000);
        return switch (period) {
            case "ANY" -> true;
            case "DAWN" -> t >= 23000 || t < 1000;
            case "MORNING" -> t >= 0 && t < 6000;
            case "AFTERNOON" -> t >= 6000 && t < 12000;
            case "DAY" -> t >= 0 && t < 12000;
            case "DUSK" -> t >= 12000 && t < 14000;
            case "NIGHT" -> t >= 13000 && t < 23000;
            default -> false;
        };
    }
    public static boolean weatherMatches(String condition, boolean rain, boolean thunder) {
        return switch (condition) {
            case "ANY" -> true; case "CLEAR" -> !rain && !thunder;
            case "RAIN" -> rain; case "THUNDER" -> thunder; default -> false;
        };
    }
    public static int select(List<Double> weights, RandomGenerator random) {
        double total = weights.stream().mapToDouble(Double::doubleValue).sum();
        if (!Double.isFinite(total) || total <= 0) return -1;
        double roll = random.nextDouble() * total;
        for (int i=0;i<weights.size();i++) { roll -= weights.get(i); if (roll < 0) return i; }
        return weights.size()-1;
    }
    public static String quality(int total) {
        if (total < 0 || total > 186) throw new IllegalArgumentException("IV inválido");
        if (total == 186) return "IVs máximos";
        double percent = total * 100.0 / 186;
        return percent >= 95 ? "Quase perfeito" : percent >= 85 ? "Extraordinário"
                : percent >= 70 ? "Excelente" : percent >= 50 ? "Bom" : "Comum";
    }
    public static String percentage(int total) { return String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.2f", total * 100.0 / 186); }
    public static float eventProgress(long created,long expires,long now){
        if(expires<=created)return 0;
        return (float)Math.max(0,Math.min(1,(expires-(double)now)/(expires-(double)created)));
    }
    public static String liveOutcome(int health,boolean battling,boolean busy,long now,long expires){
        if(battling||busy)return null;
        if(health<=0)return "DEFEATED";
        return now>=expires?"EXPIRED":null;
    }
}
