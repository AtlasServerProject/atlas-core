package io.atlas.modules.player.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;

    private String nickname;

    private double balance;

    private boolean premium;

    private LocalDateTime firstJoin;

    private LocalDateTime lastJoin;

    public PlayerProfile(
            UUID uuid,
            String nickname,
            double balance,
            boolean premium,
            LocalDateTime firstJoin,
            LocalDateTime lastJoin
    ) {

        this.uuid = uuid;
        this.nickname = nickname;
        this.balance = balance;
        this.premium = premium;
        this.firstJoin = firstJoin;
        this.lastJoin = lastJoin;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public boolean isPremium() {
        return premium;
    }

    public void setPremium(boolean premium) {
        this.premium = premium;
    }

    public LocalDateTime getFirstJoin() {
        return firstJoin;
    }

    public void setFirstJoin(LocalDateTime firstJoin) {
        this.firstJoin = firstJoin;
    }

    public LocalDateTime getLastJoin() {
        return lastJoin;
    }

    public void setLastJoin(LocalDateTime lastJoin) {
        this.lastJoin = lastJoin;
    }
}