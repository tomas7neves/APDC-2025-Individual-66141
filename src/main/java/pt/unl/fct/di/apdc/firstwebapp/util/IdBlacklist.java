package pt.unl.fct.di.apdc.firstwebapp.util;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class IdBlacklist {

    private static final Set<String> blacklistedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static void addToBlacklist(String id) {
        if (id != null) {
            blacklistedIds.add(id.toLowerCase());
        }
    }

    public static boolean isBlacklisted(String id) {
        return id != null && blacklistedIds.contains(id.toLowerCase());
    }

    public static void removeFromBlacklist(String id) {
        if (id != null) {
            blacklistedIds.remove(id.toLowerCase());
        }
    }
}
