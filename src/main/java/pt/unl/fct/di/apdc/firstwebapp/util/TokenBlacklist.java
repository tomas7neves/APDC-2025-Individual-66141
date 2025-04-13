package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.HashSet;
import java.util.Set;

public class TokenBlacklist {
    private static Set<String> blacklistedTokens = new HashSet<>();

    public static boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    public static void blacklistToken(String token) {
        blacklistedTokens.add(token);
    }

    public static void removeToken(String token) {
        blacklistedTokens.remove(token);
    }
}
