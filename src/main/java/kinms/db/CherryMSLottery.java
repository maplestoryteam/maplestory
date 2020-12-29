package kinms.db;

import client.MapleCharacter;
import handling.channel.ChannelServer;

import java.util.Collection;

import server.maps.MapleMapFactory;

public interface CherryMSLottery {

    void addChar(MapleCharacter paramMapleCharacter);

    void doLottery();

    void drawalottery();

    long getAllpeichu();

    long getAlltouzhu();

    ChannelServer getChannelServer();

    Collection<MapleCharacter> getCharacters();

    MapleMapFactory getMapleMapFactory();

    int getTouNumbyType(int paramInt);

    int getZjNum();

    void setAllpeichu(long paramLong);

    void setAlltouzhu(long paramLong);

    void setCharacters(Collection<MapleCharacter> paramCollection);

    void setZjNum(int paramInt);

    void warp(int paramInt, MapleCharacter paramMapleCharacter);
}
