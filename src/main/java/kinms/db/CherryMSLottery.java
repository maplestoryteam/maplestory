package kinms.db;

import client.MapleCharacter;
import handling.channel.ChannelServer;
import server.maps.MapleMapFactory;

import java.util.Collection;

public interface CherryMSLottery {

    void addChar(MapleCharacter paramMapleCharacter);

    void doLottery();

    void drawalottery();

    long getAllpeichu();

    void setAllpeichu(long paramLong);

    long getAlltouzhu();

    void setAlltouzhu(long paramLong);

    ChannelServer getChannelServer();

    Collection<MapleCharacter> getCharacters();

    void setCharacters(Collection<MapleCharacter> paramCollection);

    MapleMapFactory getMapleMapFactory();

    int getTouNumbyType(int paramInt);

    int getZjNum();

    void setZjNum(int paramInt);

    void warp(int paramInt, MapleCharacter paramMapleCharacter);
}
