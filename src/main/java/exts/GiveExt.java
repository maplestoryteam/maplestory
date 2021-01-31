package exts;

import handling.channel.ChannelServer;
import server.ServerProperties;
import server.maps.MapleMap;
import tools.MaplePacketCreator;

// 自动挂机奖励
public class GiveExt {
    public static final boolean giveEnable = Boolean.valueOf(ServerProperties.getProperty("KingMS.giveEnable", "false"));
    public static final long givePeriod = Long.valueOf(ServerProperties.getProperty("KingMS.givePeriod", "10")) * 60 * 1000;
    public static final boolean giveExp = Boolean.valueOf(ServerProperties.getProperty("KingMS.giveExp", "false"));
    public static final boolean givePoint = Boolean.valueOf(ServerProperties.getProperty("KingMS.givePoint", "false"));
    public static final int giveExpOfLevelRate = Integer.valueOf(ServerProperties.getProperty("KingMS.giveExpOfLevelRate", "15"));
    public static final int givePointPerNum = Integer.valueOf(ServerProperties.getProperty("KingMS.givePointPerNum", "5"));

    public static final void start() {
        try {
            ChannelServer.getAllInstances().parallelStream().forEach(ch -> {
                MapleMap mapleMap = ch.getMapFactory().getMap(910000000);
                mapleMap.getCharacters().parallelStream().forEach(chr -> {
                    try {
                        if (giveExp) {
                            chr.gainExp(chr.getLevel() * giveExpOfLevelRate, true, false, true);
                        }
                        if (givePoint) {
                            chr.modifyCSPoints(2, givePointPerNum);
                        }
                        chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[系统奖励] 挂机获得[" + givePointPerNum + "] 抵用卷!"));
                        chr.getClient().getSession().write(MaplePacketCreator.serverNotice(5, "[系统奖励] 挂机获得[" + chr.getLevel() * giveExpOfLevelRate + "] 经验!"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
