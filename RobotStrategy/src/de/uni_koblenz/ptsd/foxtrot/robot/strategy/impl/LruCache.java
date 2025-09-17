package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/** Simple access-ordered LRU cache. */
class LruCache<K,V> extends LinkedHashMap<K,V> {
    private final int maxEntries;
    LruCache(int maxEntries) {
        super(64, 0.75f, true);
        this.maxEntries = maxEntries;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > maxEntries;
    }
}
