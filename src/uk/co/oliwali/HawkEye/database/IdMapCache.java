package uk.co.oliwali.HawkEye.database;

import uk.co.oliwali.HawkEye.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author bob7l
 *
 * Stores Id and value references in two diffrent maps to insure
 * quick access to both the value and the key
 */
public class IdMapCache<K> {

    private final Map<K, String> idMap = new HashMap<>();

    private final Map<String, K> valueMap = new HashMap<>();

    public void put(K id, String value) {
        idMap.put(id, value);
        valueMap.put(value, id);
    }

    public void remove(K id, String value) {
        idMap.remove(id);
        valueMap.remove(value);
    }

    public String get(K id) {
        return idMap.get(id);
    }

    public K get(String value) {
        return valueMap.get(value);
    }

    public boolean containsKey(K id) {
        return idMap.containsKey(id);
    }

    public boolean containsKey(String value) {
        return valueMap.containsKey(value);
    }

    public K searchForId(String value) {
        K id = valueMap.get(value);

        if (id != null)
            return id;

        for (String str : valueMap.keySet()) {
            if (Util.startsWithIgnoreCase(str, value)) {
                return valueMap.get(str);
            }
        }

        return null;
    }
}
