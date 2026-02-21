package dev.eths.nexora.cache;

public final class CacheKey {
    private CacheKey() {
    }

    public static String entityKey(String table, String id) {
        return "entity:" + table + ":" + id;
    }
}
