package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Small Least-Recently-Used (LRU) cache built on top of {@link LinkedHashMap}.
 *
 * <p>This cache uses access-order iteration (see the {@link LinkedHashMap}
 * constructor below), so the eldest entry according to recent access will be
 * evicted once the configured capacity is exceeded. Lookups via {@link #get(Object)}
 * and updates via {@link #put(Object, Object)} both count as access.
 *
 * <h2>Characteristics</h2>
 * <ul>
 *   <li><b>Eviction policy:</b> strict LRU by access order; when {@code size() > maxEntries}
 *       the eldest entry is removed on the next insertion/update.</li>
 *   <li><b>Time complexity:</b> all operations are {@code O(1)} on average.</li>
 *   <li><b>Thread-safety:</b> not thread-safe. Wrap with {@link java.util.Collections#synchronizedMap(Map)}
 *       or guard externally if used from multiple threads.</li>
 *   <li><b>Null handling:</b> follows {@link LinkedHashMap} semantics (permits {@code null} values/keys
 *       depending on your JVM; avoid {@code null} keys for clarity).</li>
 * </ul>
 *
 * @param <K> key type
 * @param <V> value type
 */
class LruCache<K,V> extends LinkedHashMap<K,V> {
    private final int maxEntries;

    /**
     * Creates a new LRU cache with a maximum number of entries.
     *
     * <p>The underlying {@link LinkedHashMap} is initialized with an initial capacity of
     * {@code 64}, a load factor of {@code 0.75}, and <em>access-order</em> iteration enabled
     * (the third argument {@code true}).
     *
     * @param maxEntries the maximum number of entries to retain; when the size grows beyond
     *                   this value, the least-recently-used entry is evicted
     */
    LruCache(int maxEntries) {
        super(64, 0.75f, true);
        this.maxEntries = maxEntries;
    }

    /**
     * Decides whether to remove the eldest entry after an insertion or update.
     *
     * <p>Returns {@code true} when the cache size exceeds {@link #maxEntries}, which triggers
     * eviction of the least-recently-used entry (as defined by access order).
     *
     * @param eldest the eldest entry according to the map's iteration order
     * @return {@code true} to remove {@code eldest} and keep the size within the capacity
     */
    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        return size() > maxEntries;
    }
}
