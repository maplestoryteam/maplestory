package exts;

import client.MapleCharacter;
import exts.model.PlayerMapTime;
import handling.channel.ChannelServer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public interface PlayerMapTimeExt {
    Map<String, PlayerMapTime> PLAYER_MAP_TIME_MAP = new ConcurrentHashMap<>();

    static List<PlayerMapTime> overMinute(int overMinute) {
        return PLAYER_MAP_TIME_MAP.values().parallelStream().map(pmt -> {
            pmt.setMinute((int) ((System.currentTimeMillis() - pmt.getEnter()) / 1000 / 60));
            return pmt;
        }).filter(pmt -> pmt.getMinute() > overMinute)
                .collect(Collectors.toList());
    }

    static void remove(String character) {
        new Thread(() -> PLAYER_MAP_TIME_MAP.remove(character)).start();
    }

    static void put(String character, int mapId) {
        new Thread(() -> {
            if (!PLAYER_MAP_TIME_MAP.containsKey(character)
                    || PLAYER_MAP_TIME_MAP.get(character).getMapId() != mapId) {
                PLAYER_MAP_TIME_MAP.put(character, new PlayerMapTime(character, mapId, System.currentTimeMillis()));
            }
        }).start();
    }

    static void swap(String from, String to) {
        MapleCharacter toc = null, formc = null;
        int toch = 0, formch = 0;
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            toc = cs.getPlayerStorage().getCharacterByName(to);
            if (toc != null) {
                toch = cs.getChannel();
                break;
            }
        }
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            formc = cs.getPlayerStorage().getCharacterByName(from);
            if (formc != null) {
                formch = cs.getChannel();
                break;
            }
        }
        if (toc == null || formc == null) {
            return;
        }

        if (toch == 0 || formch == 0) {
            return;
        }

        // 首先切换地图
        formc.changeMap(toc.getMapId());
        if (toch != formch) {
            // 切换频道
            formc.changeChannel(toch);
        }


//        MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
//        if (victim != null) {
//            victim.changeMap(c.getPlayer().getMap(), c.getPlayer().getMap().findClosestSpawnpoint(c.getPlayer().getPosition()));
//        } else {
//            victim = ChannelServer.getInstance(ch).getPlayerStorage().getCharacterByName(splitted[1]);
//            c.getPlayer().dropMessage(5, "正在传送玩家到身边");
//            victim.dropMessage(5, "GM正在传送你");
//            if (victim.getMapId() != c.getPlayer().getMapId()) {
//                final MapleMap mapp = victim.getClient().getChannelServer().getMapFactory().getMap(c.getPlayer().getMapId());
//                victim.changeMap(mapp, mapp.getPortal(0));
//            }
//            victim.changeChannel(c.getChannel());
//        }
    }
}

