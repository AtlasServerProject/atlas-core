package io.atlas.modules.economy;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.math.BigInteger;

public class CobbleDollarsEconomyBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("AtlasEconomy");
    private static final String EXTENSION_CLASS =
            "fr.harmex.cobbledollars.common.utils.extensions.PlayerExtensionKt";

    private static Method getCobbleDollarsMethod;
    private static Method setCobbleDollarsMethod;
    private static Method updateCobbleDollarsAccountMethod;
    private static boolean resolved;
    private static boolean warningLogged;

    public boolean isAvailable(ServerPlayer player) {
        resolve(player);
        return getCobbleDollarsMethod != null && setCobbleDollarsMethod != null;
    }

    public BigInteger getBalance(ServerPlayer player) {
        if (!isAvailable(player)) {
            return BigInteger.ZERO;
        }

        try {
            Object value = getCobbleDollarsMethod.invoke(null, player);
            if (value instanceof BigInteger balance) {
                return balance.max(BigInteger.ZERO);
            }
        } catch (ReflectiveOperationException exception) {
            logWarning(exception);
        }

        return BigInteger.ZERO;
    }

    public void setBalance(ServerPlayer player, BigInteger amount) {
        if (!isAvailable(player)) {
            return;
        }

        BigInteger safeAmount = amount == null ? BigInteger.ZERO : amount.max(BigInteger.ZERO);

        try {
            setCobbleDollarsMethod.invoke(null, player, safeAmount);
            updateAccount(player);
        } catch (ReflectiveOperationException exception) {
            logWarning(exception);
        }
    }

    public void add(ServerPlayer player, BigInteger amount) {
        if (amount == null || amount.signum() <= 0) {
            return;
        }

        setBalance(player, getBalance(player).add(amount));
    }

    public boolean subtract(ServerPlayer player, BigInteger amount) {
        if (amount == null || amount.signum() <= 0) {
            return false;
        }

        BigInteger current = getBalance(player);
        if (current.compareTo(amount) < 0) {
            return false;
        }

        setBalance(player, current.subtract(amount));
        return true;
    }

    private static void resolve(ServerPlayer player) {
        if (resolved) {
            return;
        }

        resolved = true;

        try {
            Class<?> extensionClass = Class.forName(EXTENSION_CLASS);
            Class<?> playerClass = player.getClass();

            getCobbleDollarsMethod = findMethod(extensionClass, "getCobbleDollars", playerClass);
            setCobbleDollarsMethod = findMethod(extensionClass, "setCobbleDollars", playerClass, BigInteger.class);
            updateCobbleDollarsAccountMethod = findMethod(extensionClass, "updateCobbleDollarsAccount", playerClass);

            if (getCobbleDollarsMethod == null || setCobbleDollarsMethod == null) {
                LOGGER.warn("CobbleDollars instalado, mas a API esperada não foi encontrada. Economia Atlas ficará em modo legado.");
            } else {
                LOGGER.info("Economia Atlas integrada ao CobbleDollars.");
            }
        } catch (ClassNotFoundException exception) {
            LOGGER.warn("CobbleDollars não encontrado. Economia Atlas ficará em modo legado.");
        }
    }

    private static Method findMethod(Class<?> source, String name, Class<?>... argumentTypes) {
        for (Method method : source.getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != argumentTypes.length) {
                continue;
            }

            Class<?>[] parameters = method.getParameterTypes();
            boolean matches = true;
            for (int index = 0; index < parameters.length; index++) {
                if (!parameters[index].isAssignableFrom(argumentTypes[index])) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return method;
            }
        }

        return null;
    }

    private static void updateAccount(ServerPlayer player) {
        if (updateCobbleDollarsAccountMethod == null) {
            return;
        }

        try {
            updateCobbleDollarsAccountMethod.invoke(null, player);
        } catch (ReflectiveOperationException exception) {
            logWarning(exception);
        }
    }

    private static void logWarning(Exception exception) {
        if (warningLogged) {
            return;
        }

        warningLogged = true;
        LOGGER.warn("Não foi possível sincronizar a economia Atlas com CobbleDollars.", exception);
    }
}
