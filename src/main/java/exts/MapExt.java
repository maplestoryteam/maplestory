package exts;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public interface MapExt {
    // 副本币
    Set<Integer> partyCoins = Collections.synchronizedSet(new HashSet<>());

    static boolean query(int party) {
        return partyCoins.contains(party);
    }

    static boolean add(int party) {
        return partyCoins.add(party);
    }

    static boolean remove(int party) {
        return partyCoins.remove(party);
    }
}
