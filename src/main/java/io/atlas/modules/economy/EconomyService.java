package io.atlas.modules.economy;

import io.atlas.modules.player.model.PlayerProfile;
import net.minecraft.server.level.ServerPlayer;

import java.math.BigInteger;
import java.text.NumberFormat;
import java.util.Locale;

public class EconomyService {

    private final EconomyRepository repository = new EconomyRepository();
    private final CobbleDollarsEconomyBridge cobbleDollars = new CobbleDollarsEconomyBridge();

    public BigInteger getBalance(ServerPlayer player) {
        return cobbleDollars.getBalance(player);
    }

    public String getFormattedBalance(ServerPlayer player) {
        return format(getBalance(player));
    }

    public boolean has(ServerPlayer player, BigInteger amount) {
        return getBalance(player).compareTo(amount) >= 0;
    }

    public void deposit(ServerPlayer player, BigInteger amount) {
        cobbleDollars.add(player, amount);
    }

    public boolean withdraw(ServerPlayer player, BigInteger amount) {
        return cobbleDollars.subtract(player, amount);
    }

    public void setBalance(ServerPlayer player, BigInteger amount) {
        cobbleDollars.setBalance(player, amount);
    }

    public static String format(BigInteger amount) {
        return NumberFormat.getIntegerInstance(new Locale("pt", "BR")).format(amount);
    }

    public double getBalance(PlayerProfile profile) {
        return profile.getBalance();
    }

    public boolean has(PlayerProfile profile, double amount) {
        return profile.getBalance() >= amount;
    }

    public void deposit(PlayerProfile profile, double amount) {
        if (amount <= 0) return;

        double newBalance = profile.getBalance() + amount;
        profile.setBalance(newBalance);

        repository.updateBalance(profile.getUuid(), newBalance);
    }

    public boolean withdraw(PlayerProfile profile, double amount) {
        if (amount <= 0) return false;

        if (!has(profile, amount)) {
            return false;
        }

        double newBalance = profile.getBalance() - amount;
        profile.setBalance(newBalance);

        repository.updateBalance(profile.getUuid(), newBalance);
        return true;
    }

    public void setBalance(PlayerProfile profile, double amount) {
        if (amount < 0) amount = 0;

        profile.setBalance(amount);
        repository.updateBalance(profile.getUuid(), amount);
    }
}
