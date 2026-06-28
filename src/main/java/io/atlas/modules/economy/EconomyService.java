package io.atlas.modules.economy;

import io.atlas.modules.player.model.PlayerProfile;

public class EconomyService {

    private final EconomyRepository repository = new EconomyRepository();

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