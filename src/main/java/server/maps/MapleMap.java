/*
 This file is part of the OdinMS Maple Story Server
 Copyright (C) 2008 ~ 2010 Patrick Huy <patrick.huy@frz.cc>
 Matthias Butz <matze@odinms.de>
 Jan Christian Meyer <vimes@odinms.de>

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License version 3
 as published by the Free Software Foundation. You may not use, modify
 or distribute this program under any other version of the
 GNU Affero General Public License.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.maps;

import client.MapleBuffStat;
import client.MapleCharacter;
import client.MapleClient;
import client.inventory.*;
import client.status.MonsterStatus;
import client.status.MonsterStatusEffect;
import constants.GameConstants;
import constants.ServerConstants;
import database.DatabaseConnection;
import exts.PlayerMapTimeExt;
import handling.MaplePacket;
import handling.channel.ChannelServer;
import handling.world.MaplePartyCharacter;
import handling.world.World;
import scripting.EventManager;
import server.*;
import server.MapleCarnivalFactory.MCSkill;
import server.MapleSquad.MapleSquadType;
import server.Timer.MapTimer;
import server.events.MapleEvent;
import server.life.*;
import server.maps.MapleNodes.MapleNodeInfo;
import server.maps.MapleNodes.MaplePlatform;
import server.maps.MapleNodes.MonsterPoint;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.StringUtil;
import tools.packet.MTSCSPacket;
import tools.packet.MobPacket;
import tools.packet.PetPacket;

import java.awt.*;
import java.lang.ref.WeakReference;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MapleMap {

    /*
     * Holds mappings of OID -> MapleMapObject separated by MapleMapObjectType.
     * Please acquire the appropriate lock when reading and writing to the
     * LinkedHashMaps. The MapObjectType Maps themselves do not need to
     * synchronized in any way since they should never be modified.
     */
    private Map<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> mapobjects;
    private Map<MapleMapObjectType, ReentrantReadWriteLock> mapobjectlocks;
    private List<MapleCharacter> characters = new ArrayList<>();
    private ReentrantReadWriteLock charactersLock = new ReentrantReadWriteLock();
    private int runningOid = 100000;
    private Lock runningOidLock = new ReentrantLock();
    private List<Spawns> monsterSpawn = new ArrayList<>();
    private AtomicInteger spawnedMonstersOnMap = new AtomicInteger(0);
    private Map<Integer, MaplePortal> portals = new HashMap<>();
    private MapleFootholdTree footholds = null;
    private float monsterRate;
    private float recoveryRate;
    private MapleMapEffect mapEffect;
    private byte channel;
    private short decHP = 0;
    private short createMobInterval = 9000;
    private short firstCreateMobInterval = 1000;
    private boolean respawned = false;
    private int consumeItemCoolTime = 0;
    private int protectItem = 0;
    private int decHPInterval = 10000;
    private int mapid;
    private int returnMapId;
    private int timeLimit;
    private int fieldLimit;
    private int maxRegularSpawn = 0;
    private int fixedMob;
    private int forcedReturnMap = 999999999;
    private int lvForceMove = 0;
    private int lvLimit = 0;
    private int permanentWeather = 0;
    private boolean town, clock, personalShop, everlast = false, dropsDisabled = false, gDropsDisabled = false,
            soaring = false, squadTimer = false, isSpawns = true;
    private String mapName, streetName, onUserEnter, onFirstUserEnter, speedRunLeader = "";
    private List<Integer> dced = new ArrayList<>();
    private ScheduledFuture<?> squadSchedule;
    private long speedRunStart = 0, lastSpawnTime = 0, lastHurtTime = 0;
    private MapleNodes nodes;
    private MapleSquadType squad;
    private int fieldType;
    private Map<String, Integer> environment = new LinkedHashMap<>();

    public MapleMap(final int mapid, int channel, int returnMapId, float monsterRate) {
        this.mapid = mapid;
        this.channel = (byte) channel;
        this.returnMapId = returnMapId;
        if (this.returnMapId == 999999999) {
            this.returnMapId = mapid;
        }
        this.monsterRate = monsterRate;
        EnumMap<MapleMapObjectType, LinkedHashMap<Integer, MapleMapObject>> objsMap = new EnumMap<>(MapleMapObjectType.class);
        EnumMap<MapleMapObjectType, ReentrantReadWriteLock> objlockmap = new EnumMap<>(MapleMapObjectType.class);
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            objsMap.put(type, new LinkedHashMap<>());
            objlockmap.put(type, new ReentrantReadWriteLock());
        }
        mapobjects = Collections.unmodifiableMap(objsMap);
        mapobjectlocks = Collections.unmodifiableMap(objlockmap);
    }

    public void setSpawns(final boolean fm) {
        this.isSpawns = fm;
    }

    public boolean getSpawns() {
        return isSpawns;
    }

    public void setFixedMob(int fm) {
        this.fixedMob = fm;
    }

    public void setForceMove(int fm) {
        this.lvForceMove = fm;
    }

    public int getForceMove() {
        return lvForceMove;
    }

    public void setLevelLimit(int fm) {
        this.lvLimit = fm;
    }

    public int getLevelLimit() {
        return lvLimit;
    }

    public void setReturnMapId(int rmi) {
        this.returnMapId = rmi;
    }

    public void setSoaring(boolean b) {
        this.soaring = b;
    }

    public boolean canSoar() {
        return soaring;
    }

    public void toggleDrops() {
        this.dropsDisabled = !dropsDisabled;
    }

    public void setDrops(final boolean b) {
        this.dropsDisabled = b;
    }

    public void toggleGDrops() {
        this.gDropsDisabled = !gDropsDisabled;
    }

    public int getId() {
        return mapid;
    }

    public MapleMap getReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(returnMapId);
    }

    public int getReturnMapId() {
        return returnMapId;
    }

    public int getForcedReturnId() {
        return forcedReturnMap;
    }

    public MapleMap getForcedReturnMap() {
        return ChannelServer.getInstance(channel).getMapFactory().getMap(forcedReturnMap);
    }

    public void setForcedReturnMap(final int map) {
        this.forcedReturnMap = map;
    }

    public float getRecoveryRate() {
        return recoveryRate;
    }

    public void setRecoveryRate(final float recoveryRate) {
        this.recoveryRate = recoveryRate;
    }

    public int getFieldLimit() {
        return fieldLimit;
    }

    public void setFieldLimit(final int fieldLimit) {
        this.fieldLimit = fieldLimit;
    }

    public void setCreateMobInterval(final short createMobInterval) {
        this.createMobInterval = createMobInterval;
    }

    public void setTimeLimit(final int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public void setMapName(final String mapName) {
        this.mapName = mapName;
    }

    public String getMapName() {
        return mapName;
    }

    public String getStreetName() {
        return streetName;
    }

    public void setFirstUserEnter(final String onFirstUserEnter) {
        this.onFirstUserEnter = onFirstUserEnter;
    }

    public void setUserEnter(final String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public void setOnUserEnter(String onUserEnter) {
        this.onUserEnter = onUserEnter;
    }

    public boolean hasClock() {
        return clock;
    }

    public void setClock(final boolean hasClock) {
        this.clock = hasClock;
    }

    public boolean isTown() {
        return town;
    }

    public void setTown(final boolean town) {
        this.town = town;
    }

    public boolean allowPersonalShop() {
        return personalShop;
    }

    public void setPersonalShop(final boolean personalShop) {
        this.personalShop = personalShop;
    }

    public void setStreetName(final String streetName) {
        this.streetName = streetName;
    }

    public void setEverlast(final boolean everlast) {
        this.everlast = everlast;
    }

    public boolean getEverlast() {
        return everlast;
    }

    public int getHPDec() {
        return decHP;
    }

    public void setHPDec(final int delta) {
        if (delta > 0 || mapid == 749040100) { //pmd
            lastHurtTime = System.currentTimeMillis(); //start it up
        }
        decHP = (short) delta;
    }

    public int getHPDecInterval() {
        return decHPInterval;
    }

    public void setHPDecInterval(final int delta) {
        decHPInterval = delta;
    }

    public int getHPDecProtect() {
        return protectItem;
    }

    public void setHPDecProtect(final int delta) {
        this.protectItem = delta;
    }

    public int getCurrentPartyId() {
        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (chr.getPartyId() != -1) {
                    return chr.getPartyId();
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return -1;
    }

    public void addMapObject(final MapleMapObject mapobject) {
        runningOidLock.lock();
        int newOid;
        try {
            newOid = ++runningOid;
        } finally {
            runningOidLock.unlock();
        }

        mapobject.setObjectId(newOid);

        mapobjectlocks.get(mapobject.getType()).writeLock().lock();
        try {
            mapobjects.get(mapobject.getType()).put(newOid, mapobject);
        } finally {
            mapobjectlocks.get(mapobject.getType()).writeLock().unlock();
        }
    }

    private void spawnAndAddRangedMapObject(final MapleMapObject mapobject, DelayedPacketCreation packetbakery, SpawnCondition condition) {
        addMapObject(mapobject);

        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> itr = characters.iterator();
            MapleCharacter chr;
            while (itr.hasNext()) {
                chr = itr.next();
                if (condition == null || condition.canSpawn(chr)) {
                    if (!chr.isClone() && chr.getPosition().distanceSq(mapobject.getPosition()) <= GameConstants.maxViewRangeSq()) {
                        packetbakery.sendPackets(chr.getClient());
                        chr.addVisibleMapObject(mapobject);
                    }
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public void removeMapObject(final MapleMapObject obj) {
        mapobjectlocks.get(obj.getType()).writeLock().lock();
        try {
            mapobjects.get(obj.getType()).remove(obj.getObjectId());
        } finally {
            mapobjectlocks.get(obj.getType()).writeLock().unlock();
        }
    }

    public Point calcPointBelow(final Point initial) {
        MapleFoothold fh = footholds.findBelow(initial);
        if (fh == null) {
            return null;
        }
        int dropY = fh.getY1();
        if (!fh.isWall() && fh.getY1() != fh.getY2()) {
            double s1 = Math.abs(fh.getY2() - fh.getY1());
            double s2 = Math.abs(fh.getX2() - fh.getX1());
            if (fh.getY2() < fh.getY1()) {
                dropY = fh.getY1() - (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            } else {
                dropY = fh.getY1() + (int) (Math.cos(Math.atan(s2 / s1)) * (Math.abs(initial.x - fh.getX1()) / Math.cos(Math.atan(s1 / s2))));
            }
        }
        return new Point(initial.x, dropY);
    }

    public Point calcDropPos(final Point initial, Point fallback) {
        Point ret = calcPointBelow(new Point(initial.x, initial.y - 50));
        if (ret == null) {
            return fallback;
        }
        return ret;
    }

    private void dropFromMonster(final MapleCharacter chr, MapleMonster mob) {
        if (mob == null || chr == null || ChannelServer.getInstance(channel) == null || dropsDisabled || mob.dropsDisabled() || chr.getPyramidSubway() != null) { //no drops in pyramid ok? no cash either
            return;
        }

        //We choose not to readLock for this.
        //This will not affect the internal state, and we don't want to
        //introduce unneccessary locking, especially since this function
        //is probably used quite often.
        /*
         * if (mapobjects.get(MapleMapObjectType.ITEM).size() >= 999) {
         * removeDrops(); }
         */
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        byte droptype = (byte) (mob.getStats().isExplosiveReward() ? 3 : mob.getStats().isFfaLoot() ? 2 : chr.getParty() != null ? 1 : 0);
        int mobpos = mob.getPosition().x, cmServerrate = ChannelServer.getInstance(channel).getMesoRate(), chServerrate = ChannelServer.getInstance(channel).getDropRate(), caServerrate = ChannelServer.getInstance(channel).getCashRate();
        IItem idrop;
        byte d = 1;
        Point pos = new Point(0, mob.getPosition().y);
        double showdown = 100.0;
        MonsterStatusEffect mse = mob.getBuff(MonsterStatus.挑衅);
        if (mse != null) {
            showdown += mse.getX();
        }
        if (mob.getStats().isBoss()) {
            chServerrate = ChannelServer.getInstance(channel).getBossDropRate();
        }
        MapleMonsterInformationProvider mi = MapleMonsterInformationProvider.getInstance();

        // List<MonsterDropEntry> dropEntry = mi.retrieveDrop(mob.getId());

        //
        List<MonsterDropEntry> dropEntry = new ArrayList<>(mi.retrieveDrop(mob.getId()));

        Collections.shuffle(dropEntry);
        for (final MonsterDropEntry de : dropEntry) {
            if (de.itemId == mob.getStolen()) {
                continue;
            }

            // FIXED 除了BOSS以外都不爆金币
//            if (!mob.getStats().isBoss() && de.itemId == 0) {
//                continue;
//            }

            int Rand = Randomizer.nextInt(999999);
            int part1 = de.chance;
            int part2 = chServerrate;
            int part3 = chr.getDropMod();
            int part4 = (int) (chr.getStat().dropBuff / 100.0);
            int part5 = (int) (showdown / 100.0);
            int last = part1 * part2 * part3 * part4 * part5;
            if (Rand < last) {
                if (droptype == 3) {
                    pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                } else {
                    pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                }
                if (de.itemId == 0) { // meso
                    // FIXED 除了BOSS以外都不爆金币
                    int mesos = Randomizer.nextInt(1 + Math.abs(de.Maximum - de.Minimum)) + de.Minimum;
                    if (mesos > 0) {
                        spawnMobMesoDrop((int) (mesos * (chr.getStat().mesoBuff / 100.0) * chr.getDropMod() * cmServerrate), calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
                    }
                } else {
                    if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        int range = Math.abs(de.Maximum - de.Minimum);
                        idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(range <= 0 ? 1 : range) + de.Minimum : 1), (byte) 0, (byte) 0, (byte) 0);

                    }
                    // 枫叶
                    if (Randomizer.nextInt(100) <= 7 && !mob.getStats().isBoss() && chr.getEventInstance() == null) {
                        idrop = new Item(4001126, (byte) 0, (short) 1, (byte) 0, (byte) 0, (byte) 0);
                    }
                    // 铁块
                    if (Randomizer.nextInt(100) <= 10 && chr.getQuestStatus(28172) == 1) {
                        idrop = new Item(4001341, (byte) 0, (short) 1, (byte) 0, (byte) 0, (byte) 0);
                    }
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, droptype, de.questid);
                }
                d++;
            }
        }

        // FIXED 除了BOSS以外都不爆金币
//        double mesoDecrease = Math.pow(0.93, mob.getStats().getExp() / 350.0);
//        if (mesoDecrease > 1.0) {
//            mesoDecrease = 1.0;
//        }
//        int tempmeso = Math.min(30000, (int) (mesoDecrease * (mob.getStats().getExp()) * (1.0 + Math.random() * 5) / 10.0));
//        if (tempmeso > 0) {
//            pos.x = Math.min(Math.max(mobpos - 25 * (d / 2), footholds.getMinDropX() + 25), footholds.getMaxDropX() - d * 25);
//            spawnMobMesoDrop((int) (tempmeso * (chr.getStat().mesoBuff / 100.0) * chr.getDropMod() * cmServerrate), calcDropPos(pos, mob.getPosition()), mob, chr, false, droptype);
//        }

        List<MonsterGlobalDropEntry> globalEntry = new ArrayList<>(mi.getGlobalDrop());
        Collections.shuffle(globalEntry);
        int cashz = (mob.getStats().isBoss() && mob.getStats().getHPDisplayType() == 0 ? 20 : 1) * caServerrate;
        int cashModifier = (int) ((mob.getStats().isBoss() ? 0 : (mob.getMobExp() / 1000 + mob.getMobMaxHp() / 10000))); //no rate
        // Global Drops
        for (final MonsterGlobalDropEntry de : globalEntry) {
            if (Randomizer.nextInt(999999) < de.chance && (de.continent < 0 || (de.continent < 10 && mapid / 100000000 == de.continent) || (de.continent < 100 && mapid / 10000000 == de.continent) || (de.continent < 1000 && mapid / 1000000 == de.continent))) {
                if (droptype == 3) {
                    pos.x = (mobpos + (d % 2 == 0 ? (40 * (d + 1) / 2) : -(40 * (d / 2))));
                } else {
                    pos.x = (mobpos + ((d % 2 == 0) ? (25 * (d + 1) / 2) : -(25 * (d / 2))));
                }
                if (de.itemId == 0) {
                    // chr.modifyCSPoints(1, (int) ((Randomizer.nextInt(cashz) + cashz + cashModifier) * (chr.getStat().cashBuff / 100.0) * chr.getCashMod()), true);
                } else if (!gDropsDisabled) {
                    if (GameConstants.getInventoryType(de.itemId) == MapleInventoryType.EQUIP) {
                        idrop = ii.randomizeStats((Equip) ii.getEquipById(de.itemId));
                    } else {
                        idrop = new Item(de.itemId, (byte) 0, (short) (de.Maximum != 1 ? Randomizer.nextInt(de.Maximum - de.Minimum) + de.Minimum : 1), (byte) 0, (byte) 0, (byte) 0);
                    }
                    // 枫叶
                    if (Randomizer.nextInt(100) <= 7 && !mob.getStats().isBoss() && chr.getEventInstance() == null) {
                        idrop = new Item(4001126, (byte) 0, (short) 1, (byte) 0, (byte) 0, (byte) 0);
                    }
                    spawnMobDrop(idrop, calcDropPos(pos, mob.getPosition()), mob, chr, de.onlySelf ? 0 : droptype, de.questid);
                    d++;
                }
            }
        }
    }

    public void removeMonster(final MapleMonster monster) {
        spawnedMonstersOnMap.decrementAndGet();
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 0));
        removeMapObject(monster);
    }

    private void killMonster(final MapleMonster monster) { // For mobs with removeAfter
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);
        monster.spawnRevives(this);
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), 1));
        removeMapObject(monster);
    }

    public void killMonster(final MapleMonster monster, MapleCharacter chr, boolean withDrops, boolean second, byte animation) {
        killMonster(monster, chr, withDrops, second, animation, 0);
    }

    public void killMonster(final MapleMonster monster, MapleCharacter chr, boolean withDrops, boolean second, byte animation, int lastSkill) {
        if ((monster.getId() == 8810122 || monster.getId() == 8810018) && !second) {
            MapTimer.getInstance().schedule(() -> {
                killMonster(monster, chr, true, true, (byte) 1);
                killAllMonsters(true);
            }, 3000);
            return;
        }

        if (monster.getId() == 8820014) { //pb sponge, kills pb(w) first before dying
            killMonster(8820000);
        } else if (monster.getId() == 9300166) { //ariant pq bomb
            animation = 2; //or is it 3?
        } else if (this.getId() == 910320100) {
        }


        PlayerMapTimeExt.put(chr.getName(), chr.getMapId());

        spawnedMonstersOnMap.decrementAndGet();
        removeMapObject(monster);
        int dropOwner = monster.killBy(chr, lastSkill);
        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animation));

        if (monster.getBuffToGive() > -1) {
            int buffid = monster.getBuffToGive();
            MapleStatEffect buff = MapleItemInformationProvider.getInstance().getItemEffect(buffid);

            charactersLock.readLock().lock();
            try {
                for (final MapleCharacter mc : characters) {
                    if (mc.isAlive()) {
                        buff.applyTo(mc);

                        switch (monster.getId()) {
                            case 8810018:
                            case 8810122:
                            case 8820001:
                                mc.getClient().getSession().write(MaplePacketCreator.showOwnBuffEffect(buffid, 11)); // HT nine spirit
                                broadcastMessage(mc, MaplePacketCreator.showBuffeffect(mc.getId(), buffid, 11), false); // HT nine spirit
                                break;
                        }
                    }
                }
            } finally {
                charactersLock.readLock().unlock();
            }
        }
        int mobid = monster.getId();
        SpeedRunType type = SpeedRunType.NULL;
        MapleSquad sqd = getSquadByMap();
        if (mobid == 8810018 && mapid == 240060200) { // Horntail
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "经过无数次的挑战，" + chr.getName() + "所带领的队伍终于击败了暗黑龙王的远征队！你们才是龙之林的真正英雄~").getBytes());
            /*
             * for (MapleCharacter c : getCharactersThreadsafe()) {
             * c.finishAchievement(16); }
             */
            // FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = SpeedRunType.Horntail;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 8810122 && mapid == 240060201) { // Horntail
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, "经过无数次的挑战，" + chr.getName() + "所带领的队伍终于击败了混沌暗黑龙王的远征队！你们才是龙之林的真正英雄~").getBytes());
            /*
             * for (MapleCharacter c : getCharactersThreadsafe()) {
             * c.finishAchievement(24); }
             */
            //FileoutputUtil.log(FileoutputUtil.Horntail_Log, MapDebug_Log());
            if (speedRunStart > 0) {
                type = SpeedRunType.ChaosHT;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 8500002 && mapid == 220080001) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Papulatus;
            }
        } else if (mobid == 9400266 && mapid == 802000111) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Nameless_Magic_Monster;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 9400265 && mapid == 802000211) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Vergamot;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 9400270 && mapid == 802000411) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Dunas;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 9400273 && mapid == 802000611) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Nibergen;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 9400294 && mapid == 802000711) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Dunas_2;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 9400296 && mapid == 802000803) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Core_Blaze;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if (mobid == 9400289 && mapid == 802000821) {
            if (speedRunStart > 0) {
                type = SpeedRunType.Aufhaven;
            }
            if (sqd != null) {
                doShrine(true);
            }
        } else if ((mobid == 9420549 || mobid == 9420544) && mapid == 551030200) {
            if (speedRunStart > 0) {
                if (mobid == 9420549) {
                    type = SpeedRunType.Scarlion;
                } else {
                    type = SpeedRunType.Targa;
                }
            }
            //INSERT HERE: 2095_tokyo
        } else if (mobid == 8820001 && mapid == 270050100) {
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, chr.getName() + "经过带领的队伍经过无数次的挑战，终于击败了时间的宠儿－品克缤的远征队！你们才是时间神殿的真正英雄~").getBytes());
            /*
             * for (MapleCharacter c : getCharactersThreadsafe()) {
             * c.finishAchievement(17); }
             */
            if (speedRunStart > 0) {
                type = SpeedRunType.Pink_Bean;
            }
            if (sqd != null) {
                doShrine(true);
            }
            // FileoutputUtil.log(FileoutputUtil.Pinkbean_Log, MapDebug_Log());
            //  } else if (mobid == 8800002 && mapid == 280030000) {
            /*
             * for (MapleCharacter c : getCharactersThreadsafe()) {
             * c.finishAchievement(15); }
             */
            //FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            //     if (speedRunStart > 0) {
            //          type = SpeedRunType.Zakum;
            //      }
            //      if (sqd != null) {
            //          doShrine(true);
            //      }
            // } else if (mobid == 8800102 && mapid == 280030001) {
            /*
             * for (MapleCharacter c : getCharactersThreadsafe()) {
             * c.finishAchievement(23); }
             */
            //   FileoutputUtil.log(FileoutputUtil.Zakum_Log, MapDebug_Log());
            //   if (speedRunStart > 0) {
            //        type = SpeedRunType.Chaos_Zakum;
            //   }

            //    if (sqd != null) {
            //        doShrine(true);
            //    }
        } else if (mobid >= 8800003 && mobid <= 8800010) {
            boolean makeZakReal = true;
            Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800003 && mons.getId() <= 8800010) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMapObject object : monsters) {
                    MapleMonster mons = ((MapleMonster) object);
                    if (mons.getId() == 8800000) {
                        Point pos = mons.getPosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800000), pos);
                        break;
                    }
                }
            }
        } else if (mobid >= 8800103 && mobid <= 8800110) {
            boolean makeZakReal = true;
            Collection<MapleMonster> monsters = getAllMonstersThreadsafe();

            for (final MapleMonster mons : monsters) {
                if (mons.getId() >= 8800103 && mons.getId() <= 8800110) {
                    makeZakReal = false;
                    break;
                }
            }
            if (makeZakReal) {
                for (final MapleMonster mons : monsters) {
                    if (mons.getId() == 8800100) {
                        Point pos = mons.getPosition();
                        this.killAllMonsters(true);
                        spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(8800100), pos);
                        break;
                    }
                }
            }
        }
        if (type != SpeedRunType.NULL) {
            if (speedRunStart > 0 && speedRunLeader.length() > 0) {
                long endTime = System.currentTimeMillis();
                String time = StringUtil.getReadableMillis(speedRunStart, endTime);
                broadcastMessage(MaplePacketCreator.serverNotice(5, speedRunLeader + "'远征队花了 " + time + " 时间打败了 " + type + "!"));
                getRankAndAdd(speedRunLeader, time, type, (endTime - speedRunStart), (sqd == null ? null : sqd.getMembers()));
                endSpeedRun();
            }

        }
        if (mobid == 8820008) { //wipe out statues and respawn
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        } else if (mobid >= 8820010 && mobid <= 8820014) {
            for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
                MapleMonster mons = (MapleMonster) mmo;
                if (mons.getId() != 8820000 && mons.getObjectId() != monster.getObjectId() && mons.getLinkOid() != monster.getObjectId()) {
                    killMonster(mons, chr, false, false, animation);
                }
            }
        }
        if (withDrops) {
            MapleCharacter drop;
            if (dropOwner <= 0) {
                drop = chr;
            } else {
                drop = getCharacterById(dropOwner);
                if (drop == null) {
                    drop = chr;
                }
            }
            dropFromMonster(drop, monster);
        }
        chr.gainsg(1);//记录杀怪数量
    }

    public List<MapleReactor> getAllReactor() {
        return getAllReactorsThreadsafe();
    }

    public List<MapleReactor> getAllReactorsThreadsafe() {
        ArrayList<MapleReactor> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ret.add((MapleReactor) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllDoor() {
        return getAllDoorsThreadsafe();
    }

    public List<MapleMapObject> getAllDoorsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.DOOR).values()) {
                ret.add(mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.DOOR).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMapObject> getAllMerchant() {
        return getAllHiredMerchantsThreadsafe();
    }

    public List<MapleMapObject> getAllHiredMerchantsThreadsafe() {
        ArrayList<MapleMapObject> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.HIRED_MERCHANT).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.HIRED_MERCHANT).values()) {
                ret.add(mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.HIRED_MERCHANT).readLock().unlock();
        }
        return ret;
    }

    public List<MapleMonster> getAllMonster() {
        return getAllMonstersThreadsafe();
    }

    public List<MapleMonster> getAllMonstersThreadsafe() {
        ArrayList<MapleMonster> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
                ret.add((MapleMonster) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
        return ret;
    }

    public void killAllMonsters(final boolean animate) {
        for (final MapleMapObject monstermo : getAllMonstersThreadsafe()) {
            MapleMonster monster = (MapleMonster) monstermo;
            spawnedMonstersOnMap.decrementAndGet();
            monster.setHp(0);
            broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
            removeMapObject(monster);
            monster.killed();
        }
    }

    public void killMonster(final int monsId) {
        for (final MapleMapObject mmo : getAllMonstersThreadsafe()) {
            if (((MapleMonster) mmo).getId() == monsId) {
                spawnedMonstersOnMap.decrementAndGet();
                removeMapObject(mmo);
                broadcastMessage(MobPacket.killMonster(mmo.getObjectId(), 1));
                break;
            }
        }
    }

    private String MapDebug_Log() {
        StringBuilder sb = new StringBuilder("Defeat time : ");
        sb.append(FileoutputUtil.CurrentReadable_Time());

        sb.append(" | Mapid : ").append(this.mapid);

        charactersLock.readLock().lock();
        try {
            sb.append(" Users [").append(characters.size()).append("] | ");
            for (MapleCharacter mc : characters) {
                sb.append(mc.getName()).append(", ");
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return sb.toString();
    }

    public void limitReactor(final int rid, int num) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        Map<Integer, Integer> contained = new LinkedHashMap<Integer, Integer>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (contained.containsKey(mr.getReactorId())) {
                    if (contained.get(mr.getReactorId()) >= num) {
                        toDestroy.add(mr);
                    } else {
                        contained.put(mr.getReactorId(), contained.get(mr.getReactorId()) + 1);
                    }
                } else {
                    contained.put(mr.getReactorId(), 1);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public void destroyReactors(final int first, int last) {
        List<MapleReactor> toDestroy = new ArrayList<MapleReactor>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    toDestroy.add(mr);
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor mr : toDestroy) {
            destroyReactor(mr.getObjectId());
        }
    }

    public void destroyReactor(final int oid) {
        MapleReactor reactor = getReactorByOid(oid);
        broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
        reactor.setAlive(false);
        removeMapObject(reactor);
        reactor.setTimerActive(false);

        if (reactor.getDelay() > 0) {
            MapTimer.getInstance().schedule(new Runnable() {

                @Override
                public void run() {
                    respawnReactor(reactor);
                }
            }, reactor.getDelay());
        }
    }

    public void reloadReactors() {
        List<MapleReactor> toSpawn = new ArrayList<MapleReactor>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor reactor = (MapleReactor) obj;
                broadcastMessage(MaplePacketCreator.destroyReactor(reactor));
                reactor.setAlive(false);
                reactor.setTimerActive(false);
                toSpawn.add(reactor);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        for (MapleReactor r : toSpawn) {
            removeMapObject(r);
            if (r.getReactorId() != 9980000 && r.getReactorId() != 9980001) { //guardians cpq
                respawnReactor(r);
            }
        }
    }

    /*
     * command to reset all item-reactors in a map to state 0 for GM/NPC use -
     * not tested (broken reactors get removed from mapobjects when destroyed)
     * Should create instances for multiple copies of non-respawning reactors...
     */
    public void resetReactors() {
        setReactorState((byte) 0);
    }

    public void setReactorState() {
        setReactorState((byte) 1);
    }

    public void setReactorState(final byte state) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                ((MapleReactor) obj).forceHitReactor(state);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /*
     * command to shuffle the positions of all reactors in a map for PQ purposes
     * (such as ZPQ/LMPQ)
     */
    public void shuffleReactors() {
        shuffleReactors(0, 9999999); //all
    }

    public void shuffleReactors(int first, int last) {
        List<Point> points = new ArrayList<Point>();
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    points.add(mr.getPosition());
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
        Collections.shuffle(points);
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = (MapleReactor) obj;
                if (mr.getReactorId() >= first && mr.getReactorId() <= last) {
                    mr.setPosition(points.remove(points.size() - 1));
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /**
     * Automagically finds a new controller for the given monster from the chars
     * on the map...
     *
     * @param monster
     */
    public void updateMonsterController(final MapleMonster monster) {
        if (!monster.isAlive()) {
            return;
        }
        if (monster.getController() != null) {
            if (monster.getController().getMap() != this) {
                monster.getController().stopControllingMonster(monster);
            } else { // Everything is fine :)
                return;
            }
        }
        int mincontrolled = -1;
        MapleCharacter newController = null;

        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (!chr.isHidden() && !chr.isClone() && (chr.getControlledSize() < mincontrolled || mincontrolled == -1)) {
                    mincontrolled = chr.getControlledSize();
                    newController = chr;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        if (newController != null) {
            if (monster.isFirstAttack()) {
                newController.controlMonster(monster, true);
                monster.setControllerHasAggro(true);
                monster.setControllerKnowsAboutAggro(true);
            } else {
                newController.controlMonster(monster, false);
            }
        }
    }

    public MapleMapObject getMapObject(int oid, MapleMapObjectType type) {
        mapobjectlocks.get(type).readLock().lock();
        try {
            return mapobjects.get(type).get(oid);
        } finally {
            mapobjectlocks.get(type).readLock().unlock();
        }
    }

    public boolean containsNPC(int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC n = (MapleNPC) itr.next();
                if (n.getId() == npcid) {
                    return true;
                }
            }
            return false;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleNPC getNPCById(int id) {
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC n = (MapleNPC) itr.next();
                if (n.getId() == id) {
                    return n;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
    }

    public MapleMonster getMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            MapleMonster ret = null;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
            while (itr.hasNext()) {
                MapleMonster n = (MapleMonster) itr.next();
                if (n.getId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public int countMonsterById(int id) {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            int ret = 0;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.MONSTER).values().iterator();
            while (itr.hasNext()) {
                MapleMonster n = (MapleMonster) itr.next();
                if (n.getId() == id) {
                    ret++;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public MapleReactor getReactorById(int id) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            MapleReactor ret = null;
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.REACTOR).values().iterator();
            while (itr.hasNext()) {
                MapleReactor n = (MapleReactor) itr.next();
                if (n.getReactorId() == id) {
                    ret = n;
                    break;
                }
            }
            return ret;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    /**
     * returns a monster with the given oid, if no such monster exists returns
     * null
     *
     * @param oid
     * @return
     */
    public MapleMonster getMonsterByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.MONSTER);
        if (mmo == null) {
            return null;
        }
        return (MapleMonster) mmo;
    }

    public MapleNPC getNPCByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.NPC);
        if (mmo == null) {
            return null;
        }
        return (MapleNPC) mmo;
    }

    public MapleReactor getReactorByOid(final int oid) {
        MapleMapObject mmo = getMapObject(oid, MapleMapObjectType.REACTOR);
        if (mmo == null) {
            return null;
        }
        return (MapleReactor) mmo;
    }

    public MapleReactor getReactorByName(final String name) {
        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (MapleMapObject obj : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor mr = ((MapleReactor) obj);
                if (mr.getName().equalsIgnoreCase(name)) {
                    return mr;
                }
            }
            return null;
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public void spawnNpc(final int id, Point pos) {
        MapleNPC npc = MapleLifeFactory.getNPC(id);
        npc.setPosition(pos);
        npc.setCy(pos.y);
        npc.setRx0(pos.x + 50);
        npc.setRx1(pos.x - 50);
        npc.setFh(getFootholds().findBelow(pos).getId());
        npc.setCustom(true);
        addMapObject(npc);
        broadcastMessage(MaplePacketCreator.spawnNPC(npc, true));
    }

    public void removeNpc(final int npcid) {
        mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().lock();
        try {
            Iterator<MapleMapObject> itr = mapobjects.get(MapleMapObjectType.NPC).values().iterator();
            while (itr.hasNext()) {
                MapleNPC npc = (MapleNPC) itr.next();
                if (npc.isCustom() && npc.getId() == npcid) {
                    broadcastMessage(MaplePacketCreator.removeNPC(npc.getObjectId()));
                    itr.remove();
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).writeLock().unlock();
        }
    }

    public void spawnMonster_sSack(final MapleMonster mob, Point pos, int spawnType) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mob.setPosition(spos);
        spawnMonster(mob, spawnType);
    }

    public void spawnMonsterOnGroundBelow(final MapleMonster mob, Point pos) {
        spawnMonster_sSack(mob, pos, -2);
    }

    public void spawnMonster_sSack(final MapleMonster mob, Point pos, int spawnType, int hp) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mob.setPosition(spos);
        mob.setHp(hp);
        spawnMonster(mob, spawnType);
    }

    public void spawnMonsterOnGroundBelow(final MapleMonster mob, Point pos, int hp) {
        spawnMonster_sSack(mob, pos, -2, hp);
    }

    public int spawnMonsterWithEffectBelow(final MapleMonster mob, Point pos, int effect) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        return spawnMonsterWithEffect(mob, effect, spos);
    }

    public void spawnZakum(final int x, int y) {
        Point pos = new Point(x, y);
        MapleMonster mainb = MapleLifeFactory.getMonster(8800000);
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        int[] zakpart = {8800003, 8800004, 8800005, 8800006, 8800007,
                8800008, 8800009, 8800010};

        for (final int i : zakpart) {
            MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule();
            // broadcastMessage(MaplePacketCreator.stopClock());
        }
    }

    public void spawnChaosZakum(final int x, int y) {
        Point pos = new Point(x, y);
        MapleMonster mainb = MapleLifeFactory.getMonster(8800100);
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        mainb.setPosition(spos);
        mainb.setFake(true);

        // Might be possible to use the map object for reference in future.
        spawnFakeMonster(mainb);

        int[] zakpart = {8800103, 8800104, 8800105, 8800106, 8800107,
                8800108, 8800109, 8800110};

        for (final int i : zakpart) {
            MapleMonster part = MapleLifeFactory.getMonster(i);
            part.setPosition(spos);

            spawnMonster(part, -2);
        }
        if (squadSchedule != null) {
            cancelSquadSchedule();
            // broadcastMessage(MaplePacketCreator.stopClock());
        }
    }

    public List<MapleMist> getAllMistsThreadsafe() {
        ArrayList<MapleMist> ret = new ArrayList<MapleMist>();
        mapobjectlocks.get(MapleMapObjectType.MIST).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MIST).values()) {
                ret.add((MapleMist) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MIST).readLock().unlock();
        }
        return ret;
    }

    public void spawnFakeMonsterOnGroundBelow(final MapleMonster mob, Point pos) {
        Point spos = calcPointBelow(new Point(pos.x, pos.y - 1));
        spos.y -= 1;
        mob.setPosition(spos);
        spawnFakeMonster(mob);
    }

    public int getMobsSize() {
        return this.mapobjects.get(MapleMapObjectType.MONSTER).size();
    }

    private void checkRemoveAfter(final MapleMonster monster) {
        int ra = monster.getStats().getRemoveAfter();

        if (ra > 0) {
            MapTimer.getInstance().schedule(() -> {
                if (monster != null && monster == getMapObject(monster.getObjectId(), monster.getType())) {
                    killMonster(monster);
                }
            }, ra * 1000);
        }
    }

    public void spawnRevives(final MapleMonster monster, int oid) {
        monster.setMap(this);
        checkRemoveAfter(monster);
        monster.setLinkOid(oid);
        spawnAndAddRangedMapObject(monster, c -> {
            c.getSession().write(MobPacket.spawnMonster(monster, -2, 0, oid)); // TODO effect
        }, null);
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnMonster(final MapleMonster monster, int spawnType) {
        if (monster.getId() == 6090000) {
            return;
        }

        monster.setMap(this);
        checkRemoveAfter(monster);

        spawnAndAddRangedMapObject(monster, c -> c.getSession().write(MobPacket.spawnMonster(monster, spawnType, 0, 0)), null);
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public int spawnMonsterWithEffect(final MapleMonster monster, int effect, Point pos) {
        try {
            monster.setMap(this);
            monster.setPosition(pos);

            spawnAndAddRangedMapObject(monster, c -> c.getSession().write(MobPacket.spawnMonster(monster, -2, effect, 0)), null);
            updateMonsterController(monster);

            spawnedMonstersOnMap.incrementAndGet();
            return monster.getObjectId();
        } catch (Exception e) {
            return -1;
        }
    }

    public void spawnFakeMonster(final MapleMonster monster) {
        monster.setMap(this);
        monster.setFake(true);

        spawnAndAddRangedMapObject(monster, c -> {
            c.getSession().write(MobPacket.spawnMonster(monster, -2, 0xfc, 0));
//		c.getSession().write(MobPacket.spawnFakeMonster(monster, 0));
        }, null);
        updateMonsterController(monster);

        spawnedMonstersOnMap.incrementAndGet();
    }

    public void spawnReactor(final MapleReactor reactor) {
        reactor.setMap(this);

        spawnAndAddRangedMapObject(reactor, c -> c.getSession().write(MaplePacketCreator.spawnReactor(reactor)), null);
    }

    private void respawnReactor(final MapleReactor reactor) {
        reactor.setState((byte) 0);
        reactor.setAlive(true);
        spawnReactor(reactor);
    }

    public void spawnDoor(final MapleDoor door) {
        spawnAndAddRangedMapObject(door, c -> {
            c.getSession().write(MaplePacketCreator.spawnDoor(door.getOwner().getId(), door.getTargetPosition(), false));
            if (door.getOwner().getParty() != null && (door.getOwner() == c.getPlayer() || door.getOwner().getParty().containsMembers(new MaplePartyCharacter(c.getPlayer())))) {
                c.getSession().write(MaplePacketCreator.partyPortal(door.getTown().getId(), door.getTarget().getId(), door.getSkill(), door.getTargetPosition()));
            }
            c.getSession().write(MaplePacketCreator.spawnPortal(door.getTown().getId(), door.getTarget().getId(), door.getSkill(), door.getTargetPosition()));
            c.getSession().write(MaplePacketCreator.enableActions());
        }, chr -> door.getTarget().getId() == chr.getMapId() || door.getOwnerId() == chr.getId() || (door.getOwner() != null && door.getOwner().getParty() != null && door.getOwner().getParty().getMemberById(chr.getId()) != null));
    }

    public void spawnSummon(final MapleSummon summon) {
        summon.updateMap(this);
        spawnAndAddRangedMapObject(summon, c -> {
            if (!summon.isChangedMap() || summon.getOwnerId() == c.getPlayer().getId()) {
                c.getSession().write(MaplePacketCreator.spawnSummon(summon, true));
            }
        }, null);
    }

    public void spawnDragon(final MapleDragon summon) {
        spawnAndAddRangedMapObject(summon, c -> {
            //  c.getSession().write(MaplePacketCreator.spawnDragon(summon));
        }, null);
    }

    public void spawnMist(final MapleMist mist, int duration, boolean fake) {
        spawnAndAddRangedMapObject(mist, c -> mist.sendSpawnData(c), null);

        MapTimer tMan = MapTimer.getInstance();
        ScheduledFuture<?> poisonSchedule;
        switch (mist.isPoisonMist()) {
            case 1:
                //poison: 0 = none, 1 = poisonous, 2 = recovery aura
                MapleCharacter owner = getCharacterById(mist.getOwnerId());
                poisonSchedule = tMan.register(new Runnable() {

                    @Override
                    public void run() {
                        /*
                         * for (final MapleMapObject mo :
                         * getMapObjectsInRect(mist.getBox(),
                         * Collections.singletonList(MapleMapObjectType.MONSTER)))
                         * { if (mist.makeChanceResult()) { ((MapleMonster)
                         * mo).applyStatus(owner, new
                         * MonsterStatusEffect(MonsterStatus.POISON, 1,
                         * mist.getSourceSkill().getId(), null, false), true,
                         * duration, false); } }
                         */
                        for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.MONSTER))) {
                            /*
                             * if (mist.makeChanceResult() && ((MapleCharacter)
                             * mo).getId() != mist.getOwnerId()) {
                             * ((MapleCharacter)
                             * mo).setDOT(mist.getSource().getDOT(),
                             * mist.getSourceSkill().getId(),
                             * mist.getSkillLevel()); } else
                             */
                            if (mist.makeChanceResult() && !((MapleMonster) mo).isBuffed(MonsterStatus.中毒)) {
                                ((MapleMonster) mo).applyStatus(owner, new MonsterStatusEffect(MonsterStatus.中毒, 1, mist.getSourceSkill().getId(), null, false), true, duration, true/*
                                 * , mist.getSource()
                                 */);
                            }
                        }
                    }
                }, 2000, 2500);
                break;
            case 2:
                poisonSchedule = tMan.register(() -> {
                    for (final MapleMapObject mo : getMapObjectsInRect(mist.getBox(), Collections.singletonList(MapleMapObjectType.PLAYER))) {
                        if (mist.makeChanceResult()) {
                            MapleCharacter chr = ((MapleCharacter) mo);
                            chr.addMP((int) (mist.getSource().getX() * (chr.getStat().getMaxMp() / 100.0)));
                        }
                    }
                }, 2000, 2500);
                break;
            default:
                poisonSchedule = null;
                break;
        }
        tMan.schedule(() -> {
            //broadcastMessage(MaplePacketCreator.removeMist(mist.getObjectId()));
            broadcastMessage(MaplePacketCreator.removeMist(mist.getObjectId(), false));
            removeMapObject(mist);
            if (poisonSchedule != null) {
                poisonSchedule.cancel(false);
            }
        }, duration);
    }

    public void disappearingItemDrop(final MapleMapObject dropper, MapleCharacter owner, IItem item, Point pos) {
        Point droppos = calcDropPos(pos, pos);
        MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 1, false);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 3), drop.getPosition());
    }

    public void spawnMesoDrop(final int meso, Point position, MapleMapObject dropper, MapleCharacter owner, boolean playerDrop, byte droptype) {
        Point droppos = calcDropPos(position, position);
        MapleMapItem mdrop = new MapleMapItem(meso, droppos, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, c -> c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getPosition(), droppos, (byte) 1)), null);
        if (!everlast) {
            mdrop.registerExpire(120000);
            if (droptype == 0 || droptype == 1) {
                mdrop.registerFFA(30000);
            }
        }
    }

    public void spawnMobMesoDrop(final int meso, Point position, MapleMapObject dropper, MapleCharacter owner, boolean playerDrop, byte droptype) {
        MapleMapItem mdrop = new MapleMapItem(meso, position, dropper, owner, droptype, playerDrop);

        spawnAndAddRangedMapObject(mdrop, c -> c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, dropper.getPosition(), position, (byte) 1)), null);

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
    }

    public void spawnMobDrop(final IItem idrop, Point dropPos, MapleMonster mob, MapleCharacter chr, byte droptype, short questid) {
        MapleMapItem mdrop = new MapleMapItem(idrop, dropPos, mob, chr, droptype, false, questid);

        spawnAndAddRangedMapObject(mdrop, c -> {
            if (questid <= 0 || c.getPlayer().getQuestStatus(questid) == 1) {
                c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, mob.getPosition(), dropPos, (byte) 1));
            }
        }, null);
//	broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mdrop, mob.getPosition(), dropPos, (byte) 0));

        mdrop.registerExpire(120000);
        if (droptype == 0 || droptype == 1) {
            mdrop.registerFFA(30000);
        }
        activateItemReactors(mdrop, chr.getClient());
    }

    public void spawnRandDrop() {
        if (mapid != 910000000 || channel != 1) {
            return; //fm, ch1
        }

        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject o : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                if (((MapleMapItem) o).isRandDrop()) {
                    return;
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        MapTimer.getInstance().schedule(new Runnable() {

            public void run() {
                //final Point pos = new Point(Randomizer.nextInt(800) + 531, -806);               
                Point pos = new Point(Randomizer.nextInt(800) + 531, 34 - Randomizer.nextInt(800));

                //   int theItem = Randomizer.nextInt(1000);
                int itemid = 0;
                /*
                 * if (theItem < 950) { //0-949 = normal, 950-989 = rare,
                 * 990-999 = super itemid =
                 * GameConstants.normalDrops[Randomizer.nextInt(GameConstants.normalDrops.length)];
                 * } else if (theItem < 990) { itemid =
                 * GameConstants.rareDrops[Randomizer.nextInt(GameConstants.rareDrops.length)];
                 * } else { itemid =
                 * GameConstants.superDrops[Randomizer.nextInt(GameConstants.superDrops.length)];
                 * }
                 */
                itemid = 4000463;
                spawnAutoDrop(itemid, pos);
            }
        }, 600000);
    }

    public void spawnAutoDrop(final int itemid, Point pos) {
        IItem idrop = null;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        if (GameConstants.getInventoryType(itemid) == MapleInventoryType.EQUIP) {
            idrop = ii.randomizeStats((Equip) ii.getEquipById(itemid));
        } else {
            idrop = new Item(itemid, (byte) 0, (short) 1, (byte) 0, (byte) 0, (byte) 0);
        }
        MapleMapItem mdrop = new MapleMapItem(pos, idrop);
        spawnAndAddRangedMapObject(mdrop, c -> c.getSession().write(MaplePacketCreator.dropItemFromMapObject(mdrop, pos, pos, (byte) 1)), null);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(mdrop, pos, pos, (byte) 0));
        mdrop.registerExpire(120000);
    }

    public void spawnItemDrop(final MapleMapObject dropper, MapleCharacter owner, IItem item, Point pos, boolean ffaDrop, boolean playerDrop) {
        Point droppos = calcDropPos(pos, pos);
        MapleMapItem drop = new MapleMapItem(item, droppos, dropper, owner, (byte) 2, playerDrop);

        spawnAndAddRangedMapObject(drop, c -> c.getSession().write(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 1)), null);
        broadcastMessage(MaplePacketCreator.dropItemFromMapObject(drop, dropper.getPosition(), droppos, (byte) 0));

        if (!everlast) {
            drop.registerExpire(120000);
            activateItemReactors(drop, owner.getClient());
        }
    }

    private void activateItemReactors(final MapleMapItem drop, MapleClient c) {
        IItem item = drop.getItem();

        mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().lock();
        try {
            for (final MapleMapObject o : mapobjects.get(MapleMapObjectType.REACTOR).values()) {
                MapleReactor react = (MapleReactor) o;

                if (react.getReactorType() == 100) {
                    if (GameConstants.isCustomReactItem(react.getReactorId(), item.getItemId(), react.getReactItem().getLeft()) && react.getReactItem().getRight() == item.getQuantity()) {
                        if (react.getArea().contains(drop.getPosition())) {
                            if (!react.isTimerActive()) {
                                MapTimer.getInstance().schedule(new ActivateItemReactor(drop, react, c), 5000);
                                react.setTimerActive(true);
                                break;
                            }
                        }
                    }
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.REACTOR).readLock().unlock();
        }
    }

    public int getItemsSize() {
        return mapobjects.get(MapleMapObjectType.ITEM).size();
    }

    public List<MapleMapItem> getAllItems() {
        return getAllItemsThreadsafe(); //Which genius wrote this?
    }

    public List<MapleMapItem> getAllItemsThreadsafe() {
        ArrayList<MapleMapItem> ret = new ArrayList<>();
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                ret.add((MapleMapItem) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public void returnEverLastItem(final MapleCharacter chr) {
        for (final MapleMapObject o : getAllItemsThreadsafe()) {
            MapleMapItem item = ((MapleMapItem) o);
            if (item.getOwner() == chr.getId()) {
                item.setPickedUp(true);
                broadcastMessage(MaplePacketCreator.removeItemFromMap(item.getObjectId(), 2, chr.getId()), item.getPosition());
                if (item.getMeso() > 0) {
                    chr.gainMeso(item.getMeso(), false);
                } else {
                    MapleInventoryManipulator.addFromDrop(chr.getClient(), item.getItem(), false);
                }
                removeMapObject(item);
            }
        }
        spawnRandDrop();
    }

    public void talkMonster(final String msg, int itemId, int objectid) {
        if (itemId > 0) {
            startMapEffect(msg, itemId, false);
        }
        broadcastMessage(MobPacket.talkMonster(objectid, itemId, msg)); //5120035
        broadcastMessage(MobPacket.removeTalkMonster(objectid));
    }

    public void startMapEffect(final String msg, int itemId) {
        startMapEffect(msg, itemId, false);
    }

    public void startMapEffect(final String msg, int itemId, boolean jukebox) {
        if (mapEffect != null) {
            return;
        }
        mapEffect = new MapleMapEffect(msg, itemId);
        mapEffect.setJukebox(jukebox);
        broadcastMessage(mapEffect.makeStartData());
        MapTimer.getInstance().schedule(() -> {
            broadcastMessage(mapEffect.makeDestroyData());
            mapEffect = null;
        }, jukebox ? 300000 : 30000);
    }

    public void startExtendedMapEffect(final String msg, int itemId) {
        broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, true));
        MapTimer.getInstance().schedule(() -> {
            broadcastMessage(MaplePacketCreator.removeMapEffect());
            broadcastMessage(MaplePacketCreator.startMapEffect(msg, itemId, false));
            //dont remove mapeffect.
        }, 60000);
    }

    public void startJukebox(final String msg, int itemId) {
        startMapEffect(msg, itemId, true);
    }

    public void addPlayer(final MapleCharacter chr) {
        mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().lock();
        try {
            mapobjects.get(MapleMapObjectType.PLAYER).put(chr.getObjectId(), chr);
        } finally {
            mapobjectlocks.get(MapleMapObjectType.PLAYER).writeLock().unlock();
        }
        charactersLock.writeLock().lock();
        try {
            characters.add(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        boolean 进入地图开启显示数据 = false;
        if (mapid == 109080000 || mapid == 109080001 || mapid == 109080002 || mapid == 109080003 || mapid == 109080010 || mapid == 109080011 || mapid == 109080012) {
            chr.setCoconutTeam(getAndSwitchTeam() ? 0 : 1);
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据A");
            }
        }
        if (!chr.isHidden()) {
            broadcastMessage(chr, MaplePacketCreator.spawnPlayerMapobject(chr), false);
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据B");
            }

            for (final MaplePet pet : chr.getPets()) {
                if (pet.getSummoned()) {
                    pet.setPos(chr.getTruePosition());//设置宠物坐标。
                    chr.getClient().getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                    broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), false);
                    //broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), false);
                    if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                        System.out.println("进入地图加载数据B+");
                    }
                }
            }

            if (chr.isGM() && speedRunStart > 0) {
                endSpeedRun();
                broadcastMessage(MaplePacketCreator.serverNotice(5, "The speed run has ended."));
                if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                    System.out.println("进入地图加载数据C");
                }
            }
        }
        if (!chr.isClone()) {
            sendObjectPlacement(chr);
            chr.getClient().getSession().write(MaplePacketCreator.spawnPlayerMapobject(chr));
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据D");
            }
            switch (mapid) {
                case 109030001:
                case 109040000:
                case 109060001:
                case 109080000:
                case 109080010:
                    chr.getClient().getSession().write(MaplePacketCreator.showEventInstructions());
                    break;
                case 809000101:
                case 809000201:
                    chr.getClient().getSession().write(MaplePacketCreator.showEquipEffect());
                    break;
            }
        }
        for (MaplePet pet : chr.getPets()) {
            if (pet.getSummoned()) {
                pet.setPos(pet.getPos());//设置宠物坐标。.getTruePosition()
                chr.getClient().getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition()), true));
                broadcastMessage(chr, PetPacket.showPet(chr, pet, false, false), false);
            }
        }

        if (hasForcedEquip()) {
            chr.getClient().getSession().write(MaplePacketCreator.showForcedEquip());
        }
        chr.getClient().getSession().write(MaplePacketCreator.removeTutorialStats());
        if (chr.getMapId() >= 914000200 && chr.getMapId() <= 914000220) {
            chr.getClient().getSession().write(MaplePacketCreator.addTutorialStats());
        }
        if (chr.getMapId() >= 140090100 && chr.getMapId() <= 140090500 || chr.getJob() == 1000 && chr.getMapId() != 130030000) {
            chr.getClient().getSession().write(MaplePacketCreator.spawnTutorialSummon(1));
        }
        //FIXED 过图显示
        if (!onUserEnter.equals("")) {
            MapScriptMethods.startScript_User(chr.getClient(), onUserEnter);
        }
        if (!onFirstUserEnter.equals("")) {
            if (getCharacters().size() == 1) {
                MapScriptMethods.startScript_FirstUser(chr.getClient(), onFirstUserEnter);
            }
        }

        MapleStatEffect stat = chr.getStatForBuff(MapleBuffStat.SUMMON);
        if (stat != null && !chr.isClone()) {
            MapleSummon summon = chr.getSummons().get(stat.getSourceId());
            summon.setPosition(chr.getPosition());
            try {
                summon.setFh(getFootholds().findBelow(chr.getPosition()).getId());
            } catch (NullPointerException e) {
                summon.setFh(0); //lol, it can be fixed by movement
            }
            this.spawnSummon(summon);
            chr.addVisibleMapObject(summon);
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据H");
            }
        }
        if (chr.getChalkboard() != null) {
            chr.getClient().getSession().write(MTSCSPacket.useChalkboard(chr.getId(), chr.getChalkboard()));
        }
        broadcastMessage(MaplePacketCreator.loveEffect());
        if (timeLimit > 0 && getForcedReturnMap() != null && !chr.isClone()) {
            chr.startMapTimeLimitTask(timeLimit, getForcedReturnMap());
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据I");
            }
        }
        if (getSquadBegin() != null && getSquadBegin().getTimeLeft() > 0 && getSquadBegin().getStatus() == 1) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (getSquadBegin().getTimeLeft() / 1000)));
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据O");
            }
        }
        if (chr.getCarnivalParty() != null && chr.getEventInstance() != null) {
            chr.getEventInstance().onMapLoad(chr);
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据M");
            }
        }
        MapleEvent.mapLoad(chr, channel);
        if (chr.getEventInstance() != null && chr.getEventInstance().isTimerStarted() && !chr.isClone()) {
            chr.getClient().getSession().write(MaplePacketCreator.getClock((int) (chr.getEventInstance().getTimeLeft() / 1000)));
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据K");
            }
        }
        if (hasClock()) {
            Calendar cal = Calendar.getInstance();
            chr.getClient().getSession().write((MaplePacketCreator.getClockTime(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))));
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据L");
            }
        }
        if (isTown()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.RAINING_MINES);
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据W-------------完");
            }
        }
        if (chr.getParty() != null && !chr.isClone()) {
            chr.receivePartyMemberHP();
            chr.updatePartyMemberHP();
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据G");
            }
        }
        if (permanentWeather > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.startMapEffect("", permanentWeather, false)); //snow, no msg
        }
        if (getPlatforms().size() > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.getMovingPlatforms(this));
        }
        if (environment.size() > 0) {
            chr.getClient().getSession().write(MaplePacketCreator.getUpdateEnvironment(this));
        }
        if (isTown()) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.RAINING_MINES);
            if (ServerConstants.封包显示 || 进入地图开启显示数据) {
                System.out.println("进入地图加载数据W-------------完");
            }
        }
    }

    public int getNumItems() {
        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            return mapobjects.get(MapleMapObjectType.ITEM).size();
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
    }

    private boolean hasForcedEquip() {
        return fieldType == 81 || fieldType == 82;
    }

    public void setFieldType(int fieldType) {
        this.fieldType = fieldType;
    }

    public int getNumMonsters() {
        mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            return mapobjects.get(MapleMapObjectType.MONSTER).size();
        } finally {
            mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
    }

    public void doShrine(final boolean spawned) { //false = entering map, true = defeated
        if (squadSchedule != null) {
            cancelSquadSchedule();
        }
        int mode = (mapid == 280030000 ? 1 : (mapid == 280030001 ? 2 : (mapid == 240060200 || mapid == 240060201 ? 3 : 0)));
        //chaos_horntail message for horntail too because it looks nicer
        MapleSquad sqd = getSquadByMap();
        EventManager em = getEMByMap();
        if (sqd != null && em != null && getCharactersSize() > 0) {
            String leaderName = sqd.getLeaderName();
            String state = em.getProperty("state");
            Runnable run;
            MapleMap returnMapa = getForcedReturnMap();
            if (returnMapa == null || returnMapa.getId() == mapid) {
                returnMapa = getReturnMap();
            }
            if (mode == 1) { //zakum
                broadcastMessage(MaplePacketCreator.showZakumShrine(spawned, 5));
            } else if (mode == 2) { //chaoszakum
                broadcastMessage(MaplePacketCreator.showChaosZakumShrine(spawned, 5));
            } else if (mode == 3) { //ht/chaosht
                broadcastMessage(MaplePacketCreator.showChaosHorntailShrine(spawned, 5));
            } else {
                broadcastMessage(MaplePacketCreator.showHorntailShrine(spawned, 5));
            }
            if (mode == 1 || spawned) { //both of these together dont go well
                broadcastMessage(MaplePacketCreator.getClock(300)); //5 min
            }
            MapleMap returnMapz = returnMapa;
            if (!spawned) { //no monsters yet; inforce timer to spawn it quickly
                List<MapleMonster> monsterz = getAllMonstersThreadsafe();
                List<Integer> monsteridz = new ArrayList<Integer>();
                for (MapleMapObject m : monsterz) {
                    monsteridz.add(m.getObjectId());
                }
                run = () -> {
                    MapleSquad sqnow = MapleMap.this.getSquadByMap();
                    if (MapleMap.this.getCharactersSize() > 0 && MapleMap.this.getNumMonsters() == monsterz.size() && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                        boolean passed = monsterz.isEmpty();
                        for (MapleMapObject m : MapleMap.this.getAllMonstersThreadsafe()) {
                            for (int i : monsteridz) {
                                if (m.getObjectId() == i) {
                                    passed = true;
                                    break;
                                }
                            }
                            if (passed) {
                                break;
                            } //even one of the monsters is the same
                        }
                        if (passed) {
                            //are we still the same squad? are monsters still == 0?
                            MaplePacket packet;
                            if (mode == 1) { //zakum
                                packet = MaplePacketCreator.showZakumShrine(spawned, 0);
                            } else if (mode == 2) { //chaoszakum
                                packet = MaplePacketCreator.showChaosZakumShrine(spawned, 0);
                            } else {
                                packet = MaplePacketCreator.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
                            }
                            for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                                chr.getClient().getSession().write(packet);
                                chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                            }
                            checkStates("");
                            resetFully();
                        }
                    }

                };
            } else { //inforce timer to gtfo
                run = new Runnable() {

                    public void run() {
                        MapleSquad sqnow = MapleMap.this.getSquadByMap();
                        //we dont need to stop clock here because they're getting warped out anyway
                        if (MapleMap.this.getCharactersSize() > 0 && sqnow != null && sqnow.getStatus() == 2 && sqnow.getLeaderName().equals(leaderName) && MapleMap.this.getEMByMap().getProperty("state").equals(state)) {
                            //are we still the same squad? monsters however don't count
                            MaplePacket packet;
                            if (mode == 1) { //zakum
                                packet = MaplePacketCreator.showZakumShrine(spawned, 0);
                            } else if (mode == 2) { //chaoszakum
                                packet = MaplePacketCreator.showChaosZakumShrine(spawned, 0);
                            } else {
                                packet = MaplePacketCreator.showHorntailShrine(spawned, 0); //chaoshorntail message is weird
                            }
                            for (MapleCharacter chr : MapleMap.this.getCharactersThreadsafe()) { //warp all in map
                                chr.getClient().getSession().write(packet);
                                chr.changeMap(returnMapz, returnMapz.getPortal(0)); //hopefully event will still take care of everything once warp out
                            }
                            checkStates("");
                            resetFully();
                        }
                    }
                };
            }
            squadSchedule = MapTimer.getInstance().schedule(run, 300000); //5 mins
        }
    }

    public MapleSquad getSquadByMap() {
        MapleSquadType zz = null;
        switch (mapid) {
            case 105100400:
            case 105100300:
                zz = MapleSquadType.bossbalrog;
                break;
            case 280030000:
                zz = MapleSquadType.zak;
                break;
            case 280030001:
                zz = MapleSquadType.chaoszak;
                break;
            case 240060200:
                zz = MapleSquadType.horntail;
                break;
            case 240060201:
                zz = MapleSquadType.chaosht;
                break;
            case 270050100:
                zz = MapleSquadType.pinkbean;
                break;
            case 802000111:
                zz = MapleSquadType.nmm_squad;
                break;
            case 802000211:
                zz = MapleSquadType.vergamot;
                break;
            case 802000311:
                zz = MapleSquadType.tokyo_2095;
                break;
            case 802000411:
                zz = MapleSquadType.dunas;
                break;
            case 802000611:
                zz = MapleSquadType.nibergen_squad;
                break;
            case 802000711:
                zz = MapleSquadType.dunas2;
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                zz = MapleSquadType.core_blaze;
                break;
            case 802000821:
            case 802000823:
                zz = MapleSquadType.aufheben;
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                zz = MapleSquadType.vonleon;
                break;
            case 551030200:
                zz = MapleSquadType.scartar;
                break;
            case 271040100:
                zz = MapleSquadType.cygnus;
                break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getMapleSquad(zz);
    }

    public MapleSquad getSquadBegin() {
        if (squad != null) {
            return ChannelServer.getInstance(channel).getMapleSquad(squad);
        }
        return null;
    }

    public EventManager getEMByMap() {
        String em = null;
        switch (mapid) {
            case 105100400:
                em = "BossBalrog_EASY";
                break;
            case 105100300:
                em = "BossBalrog_NORMAL";
                break;
            case 280030000:
                em = "ZakumBattle";
                break;
            case 240060200:
                em = "HorntailBattle";
                break;
            case 280030001:
                em = "ChaosZakum";
                break;
            case 240060201:
                em = "ChaosHorntail";
                break;
            case 270050100:
                em = "PinkBeanBattle";
                break;
            case 802000111:
                em = "NamelessMagicMonster";
                break;
            case 802000211:
                em = "Vergamot";
                break;
            case 802000311:
                em = "2095_tokyo";
                break;
            case 802000411:
                em = "Dunas";
                break;
            case 802000611:
                em = "Nibergen";
                break;
            case 802000711:
                em = "Dunas2";
                break;
            case 802000801:
            case 802000802:
            case 802000803:
                em = "CoreBlaze";
                break;
            case 802000821:
            case 802000823:
                em = "Aufhaven";
                break;
            case 211070100:
            case 211070101:
            case 211070110:
                em = "VonLeonBattle";
                break;
            case 551030200:
                em = "ScarTarBattle";
                break;
            case 271040100:
                em = "CygnusBattle";
                break;
            case 262030300:
                em = "HillaBattle";
                break;
            case 262031300:
                em = "DarkHillaBattle";
                break;
            case 272020110:
            case 272030400:
                em = "ArkariumBattle";
                break;
            case 955000100:
            case 955000200:
            case 955000300:
                em = "AswanOffSeason";
                break;
            /*
             *
             */
            case 280030100:
                /*
                 * 2829
                 */
                em = "ZakumBattle";
                /*
                 * 2830
                 */
                break;
            /*
             *
             */
            case 272020200:
                /*
                 * 2862
                 */
                em = "Akayile";
                /*
                 * 2863
                 */
                break;
            /*
             *
             */
            case 689013000:
                /*
                 * 2888
                 */
                em = "PinkZakum";
                /*
                 * 2889
                 */
                break;
            /*
             *
             */
            case 703200400:
                /*
                 * 2891
                 */
                em = "0AllBoss";
                /*
                 * 2892
                 */
                break;
            //case 689010000:
            //    em = "PinkZakumEntrance";
            //    break;
            //case 689013000:
            //    em = "PinkZakumFight";
            //    break;
            default:
                return null;
        }
        return ChannelServer.getInstance(channel).getEventSM().getEventManager(em);
    }

    public void removePlayer(final MapleCharacter chr) {
        //log.warn("[dc] [level2] Player {} leaves map {}", new Object[] { chr.getName(), mapid });

        if (everlast) {
            returnEverLastItem(chr);
        }

        charactersLock.writeLock().lock();
        try {
            characters.remove(chr);
        } finally {
            charactersLock.writeLock().unlock();
        }
        removeMapObject(chr);
        chr.checkFollow();
        broadcastMessage(MaplePacketCreator.removePlayerFromMap(chr.getId()));
        if (!chr.isClone()) {
            List<MapleMonster> update = new ArrayList<>();
            Iterator<MapleMonster> controlled = chr.getControlled().iterator();

            while (controlled.hasNext()) {
                MapleMonster monster = controlled.next();
                if (monster != null) {
                    monster.setController(null);
                    monster.setControllerHasAggro(false);
                    monster.setControllerKnowsAboutAggro(false);
                    controlled.remove();
                    update.add(monster);
                }
            }
            for (MapleMonster mons : update) {
                updateMonsterController(mons);
            }
            chr.leaveMap();
            checkStates(chr.getName());
            if (mapid == 109020001) {
                chr.canTalk(true);
            }
            for (final WeakReference<MapleCharacter> chrz : chr.getClones()) {
                if (chrz.get() != null) {
                    removePlayer(chrz.get());
                }
            }
        }
        chr.cancelEffectFromBuffStat(MapleBuffStat.PUPPET);
        chr.cancelEffectFromBuffStat(MapleBuffStat.REAPER);
        boolean cancelSummons = false;
        for (final MapleSummon summon : chr.getSummons().values()) {
            if (summon.getMovementType() == SummonMovementType.STATIONARY || summon.getMovementType() == SummonMovementType.CIRCLE_STATIONARY || summon.getMovementType() == SummonMovementType.WALK_STATIONARY) {
                cancelSummons = true;
            } else {
                summon.setChangedMap(true);
                removeMapObject(summon);
            }
        }
        if (cancelSummons) {
            chr.cancelEffectFromBuffStat(MapleBuffStat.SUMMON);

        }
        if (chr.getDragon() != null) {
            removeMapObject(chr.getDragon());
        }
    }

    public List<MapleMapObject> getAllPlayers() {
        return getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.PLAYER));
    }

    public void broadcastMessage(final MaplePacket packet) {
        broadcastMessage(null, packet, Double.POSITIVE_INFINITY, null);
    }

    public void broadcastMessage(final MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    /*
     * public void broadcastMessage(MapleCharacter source, MaplePacket packet,
     * boolean repeatToSource, boolean ranged) { broadcastMessage(repeatToSource
     * ? null : source, packet, ranged ? MapleCharacter.MAX_VIEW_RANGE_SQ :
     * Double.POSITIVE_INFINITY, source.getPosition()); }
     */
    public void broadcastMessage(final MaplePacket packet, Point rangedFrom) {
        broadcastMessage(null, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    public void broadcastMessage(final MapleCharacter source, MaplePacket packet, Point rangedFrom) {
        broadcastMessage(source, packet, GameConstants.maxViewRangeSq(), rangedFrom);
    }

    private void broadcastMessage(final MapleCharacter source, MaplePacket packet, double rangeSq, Point rangedFrom) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : characters) {
                if (chr != source) {
                    if (rangeSq < Double.POSITIVE_INFINITY) {
                        if (rangedFrom.distanceSq(chr.getPosition()) <= rangeSq) {
                            chr.getClient().getSession().write(packet);
                        }
                    } else {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    private void sendObjectPlacement(final MapleCharacter c) {
        if (c == null || c.isClone()) {
            return;
        }
        for (final MapleMapObject o : this.getAllMonstersThreadsafe()) {
            updateMonsterController((MapleMonster) o);
        }
        for (final MapleMapObject o : getMapObjectsInRange(c.getPosition(), GameConstants.maxViewRangeSq(), GameConstants.rangedMapobjectTypes)) {
            if (o.getType() == MapleMapObjectType.REACTOR) {
                if (!((MapleReactor) o).isAlive()) {
                    continue;
                }
            }
            o.sendSpawnData(c.getClient());
            c.addVisibleMapObject(o);
        }
    }

    public List<MapleMapObject> getMapObjectsInRange(final Point from, double rangeSq) {
        List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapleMapObjectType.values()) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (from.distanceSq(mmo.getPosition()) <= rangeSq) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public List<MapleMapObject> getItemsInRange(Point from, double rangeSq) {
        return getMapObjectsInRange(from, rangeSq, Arrays.asList(MapleMapObjectType.ITEM));
    }

    public List<MapleMapObject> getMapObjectsInRange(final Point from, double rangeSq, List<MapleMapObjectType> MapObject_types) {
        List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (from.distanceSq(mmo.getPosition()) <= rangeSq) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public List<MapleMapObject> getMapObjectsInRect(final Rectangle box, List<MapleMapObjectType> MapObject_types) {
        List<MapleMapObject> ret = new ArrayList<>();
        for (MapleMapObjectType type : MapObject_types) {
            mapobjectlocks.get(type).readLock().lock();
            try {
                Iterator<MapleMapObject> itr = mapobjects.get(type).values().iterator();
                while (itr.hasNext()) {
                    MapleMapObject mmo = itr.next();
                    if (box.contains(mmo.getPosition())) {
                        ret.add(mmo);
                    }
                }
            } finally {
                mapobjectlocks.get(type).readLock().unlock();
            }
        }
        return ret;
    }

    public List<MapleCharacter> getPlayersInRectAndInList(final Rectangle box, List<MapleCharacter> chrList) {
        List<MapleCharacter> character = new LinkedList<>();

        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter a;
            while (ltr.hasNext()) {
                a = ltr.next();
                if (chrList.contains(a) && box.contains(a.getPosition())) {
                    character.add(a);
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return character;
    }

    public void addPortal(final MaplePortal myPortal) {
        portals.put(myPortal.getId(), myPortal);
    }

    public MaplePortal getPortal(final String portalname) {
        for (final MaplePortal port : portals.values()) {
            if (port.getName().equals(portalname)) {
                return port;
            }
        }
        return null;
    }

    public MaplePortal getPortal(final int portalid) {
        return portals.get(portalid);
    }

    public void resetPortals() {
        for (final MaplePortal port : portals.values()) {
            port.setPortalState(true);
        }
    }

    public void setFootholds(final MapleFootholdTree footholds) {
        this.footholds = footholds;
    }

    public MapleFootholdTree getFootholds() {
        return footholds;
    }

    public void loadMonsterRate(final boolean first) {
        int spawnSize = monsterSpawn.size();
        maxRegularSpawn = Math.round(spawnSize * monsterRate);
        if (maxRegularSpawn < 2) {
            maxRegularSpawn = 2;
        } else if (maxRegularSpawn > spawnSize) {
            maxRegularSpawn = spawnSize - (spawnSize / 15);
        }
        if (fixedMob > 0) {
            maxRegularSpawn = fixedMob;
        }
        Collection<Spawns> newSpawn = new LinkedList<>();
        Collection<Spawns> newBossSpawn = new LinkedList<>();
        for (final Spawns s : monsterSpawn) {
            if (s.getCarnivalTeam() >= 2) {
                continue; // Remove carnival spawned mobs
            }
            if (s.getMonster().getStats().isBoss()) {
                newBossSpawn.add(s);
            } else {
                newSpawn.add(s);
            }
        }
        monsterSpawn.clear();
        monsterSpawn.addAll(newBossSpawn);
        monsterSpawn.addAll(newSpawn);

        if (first && spawnSize > 0) {
            lastSpawnTime = System.currentTimeMillis();
            if (GameConstants.isForceRespawn(mapid)) {
                createMobInterval = 15000;
            }
        }
    }

    public SpawnPoint addMonsterSpawn(final MapleMonster monster, int mobTime, byte carnivalTeam, String msg) {
        Point newpos = calcPointBelow(monster.getPosition());
        newpos.y -= 1;
        SpawnPoint sp = new SpawnPoint(monster, newpos, mobTime, carnivalTeam, msg);
        if (carnivalTeam > -1) {
            monsterSpawn.add(0, sp); //at the beginning
        } else {
            monsterSpawn.add(sp);
        }
        return sp;
    }

    public void addAreaMonsterSpawn(final MapleMonster monster, Point pos1, Point pos2, Point pos3, int mobTime, String msg) {
        pos1 = calcPointBelow(pos1);
        pos2 = calcPointBelow(pos2);
        pos3 = calcPointBelow(pos3);
        if (pos1 != null) {
            pos1.y -= 1;
        }
        if (pos2 != null) {
            pos2.y -= 1;
        }
        if (pos3 != null) {
            pos3.y -= 1;
        }
        if (pos1 == null && pos2 == null && pos3 == null) {
            System.out.println("WARNING: mapid " + mapid + ", monster " + monster.getId() + " could not be spawned.");

            return;
        } else if (pos1 != null) {
            if (pos2 == null) {
                pos2 = new Point(pos1);
            }
            if (pos3 == null) {
                pos3 = new Point(pos1);
            }
        } else if (pos2 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos2);
            }
            if (pos3 == null) {
                pos3 = new Point(pos2);
            }
        } else if (pos3 != null) {
            if (pos1 == null) {
                pos1 = new Point(pos3);
            }
            if (pos2 == null) {
                pos2 = new Point(pos3);
            }
        }
        monsterSpawn.add(new SpawnPointAreaBoss(monster, pos1, pos2, pos3, mobTime, msg));
    }

    public List<MapleCharacter> getCharacters() {
        return getCharactersThreadsafe();
    }

    public List<MapleCharacter> getCharactersThreadsafe() {
        List<MapleCharacter> chars = new ArrayList<MapleCharacter>();

        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                chars.add(mc);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return chars;
    }

    public MapleCharacter getCharacterById_InMap(final int id) {
        return getCharacterById(id);
    }

    public MapleCharacter getCharacterById(final int id) {
        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                if (mc.getId() == id) {
                    return mc;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return null;
    }

    public void updateMapObjectVisibility(final MapleCharacter chr, MapleMapObject mo) {
        if (chr == null || chr.isClone()) {
            return;
        }
        if (!chr.isMapObjectVisible(mo)) { // monster entered view range
            if (mo.getType() == MapleMapObjectType.SUMMON || mo.getPosition().distanceSq(chr.getPosition()) <= GameConstants.maxViewRangeSq()) {
                chr.addVisibleMapObject(mo);
                mo.sendSpawnData(chr.getClient());
            }
        } else // monster left view range
            if (mo.getType() != MapleMapObjectType.SUMMON && mo.getPosition().distanceSq(chr.getPosition()) > GameConstants.maxViewRangeSq()) {
                chr.removeVisibleMapObject(mo);
                mo.sendDestroyData(chr.getClient());
            }
    }

    public void moveMonster(MapleMonster monster, Point reportedPos) {
        monster.setPosition(reportedPos);

        charactersLock.readLock().lock();
        try {
            for (MapleCharacter mc : characters) {
                updateMapObjectVisibility(mc, monster);
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public void movePlayer(final MapleCharacter player, Point newPosition) {
        player.setPosition(newPosition);
        if (!player.isClone()) {
            try {
                Collection<MapleMapObject> visibleObjects = player.getAndWriteLockVisibleMapObjects();
                ArrayList<MapleMapObject> copy = new ArrayList<>(visibleObjects);
                Iterator<MapleMapObject> itr = copy.iterator();
                while (itr.hasNext()) {
                    MapleMapObject mo = itr.next();
                    if (mo != null && getMapObject(mo.getObjectId(), mo.getType()) == mo) {
                        updateMapObjectVisibility(player, mo);
                    } else if (mo != null) {
                        visibleObjects.remove(mo);
                    }
                }
                for (MapleMapObject mo : getMapObjectsInRange(player.getPosition(), GameConstants.maxViewRangeSq())) {
                    if (mo != null && !player.isMapObjectVisible(mo)) {
                        mo.sendSpawnData(player.getClient());
                        visibleObjects.add(mo);
                    }
                }
            } finally {
                player.unlockWriteVisibleMapObjects();
            }
        }
    }

    public MaplePortal findClosestSpawnpoint(Point from) {
        MaplePortal closest = null;
        double distance, shortestDistance = Double.POSITIVE_INFINITY;
        for (MaplePortal portal : portals.values()) {
            distance = portal.getPosition().distanceSq(from);
            if (portal.getType() >= 0 && portal.getType() <= 2 && distance < shortestDistance && portal.getTargetMapId() == 999999999) {
                closest = portal;
                shortestDistance = distance;
            }
        }
        return closest;
    }

    public String spawnDebug() {
        StringBuilder sb = new StringBuilder("Mapobjects in map : ");
        sb.append(this.getMapObjectSize());
        sb.append(" spawnedMonstersOnMap: ");
        sb.append(spawnedMonstersOnMap);
        sb.append(" spawnpoints: ");
        sb.append(monsterSpawn.size());
        sb.append(" maxRegularSpawn: ");
        sb.append(maxRegularSpawn);
        sb.append(" actual monsters: ");
        sb.append(getNumMonsters());

        return sb.toString();
    }

    public int characterSize() {
        return characters.size();
    }

    public int getMapObjectSize() {
        return mapobjects.size() + getCharactersSize() - characters.size();
    }

    public int getCharactersSize() {
        int ret = 0;
        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter chr;
            while (ltr.hasNext()) {
                chr = ltr.next();
                if (!chr.isClone()) {
                    ret++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public Collection<MaplePortal> getPortals() {
        return Collections.unmodifiableCollection(portals.values());
    }

    public int getSpawnedMonstersOnMap() {
        return spawnedMonstersOnMap.get();
    }

    public void spawnLove(final MapleLove love) {
        addMapObject(love);
        broadcastMessage(love.makeSpawnData());
        MapTimer tMan = MapTimer.getInstance();
        tMan.schedule(() -> {
            removeMapObject(love);
            broadcastMessage(love.makeDestroyData());
        }, 1000 * 60 * 60);
    }

    public List<MapleCharacter> getCharactersIntersect(Rectangle box) {
        List ret = new ArrayList();
        this.charactersLock.readLock().lock();
        try {
            for (MapleCharacter chr : this.characters) {
                if (chr.getBounds().intersects(box)) {
                    ret.add(chr);
                }
            }
        } finally {
            this.charactersLock.readLock().unlock();
        }
        return ret;
    }


    public boolean isPvpMap() {
        return this.mapid == 910000013;//PK地图
    }

    public boolean isPartyPvpMap() {
        return (this.mapid == 910000019) || (this.mapid == 910000020);
    }

    public boolean isGuildPvpMap() {
        return (this.mapid == 910000021) || (this.mapid == 910000022);
    }

    private class ActivateItemReactor implements Runnable {

        private MapleMapItem mapitem;
        private MapleReactor reactor;
        private MapleClient c;

        public ActivateItemReactor(MapleMapItem mapitem, MapleReactor reactor, MapleClient c) {
            this.mapitem = mapitem;
            this.reactor = reactor;
            this.c = c;
        }

        @Override
        public void run() {
            if (mapitem != null && mapitem == getMapObject(mapitem.getObjectId(), mapitem.getType())) {
                if (mapitem.isPickedUp()) {
                    reactor.setTimerActive(false);
                    return;
                }
                mapitem.expire(MapleMap.this);
                reactor.hitReactor(c);
                reactor.setTimerActive(false);

                if (reactor.getDelay() > 0) {
                    MapTimer.getInstance().schedule(() -> reactor.forceHitReactor((byte) 0), reactor.getDelay());
                }
            } else {
                reactor.setTimerActive(false);
            }
        }
    }

    public void respawn(final boolean force) {
        respawned = true;
        lastSpawnTime = System.currentTimeMillis();
        if (force) { //cpq quick hack
            int numShouldSpawn = monsterSpawn.size() - spawnedMonstersOnMap.get();

            if (numShouldSpawn > 0) {
                int spawned = 0;

                for (Spawns spawnPoint : monsterSpawn) {
                    spawnPoint.spawnMonster(this);
                    spawned++;
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        } else {
            int numShouldSpawn = maxRegularSpawn - spawnedMonstersOnMap.get();
            if (numShouldSpawn > 0) {
                int spawned = 0;

                List<Spawns> randomSpawn = new ArrayList<>(monsterSpawn);
                Collections.shuffle(randomSpawn);

                for (Spawns spawnPoint : randomSpawn) {
                    if ((!isSpawns) && (spawnPoint.getMobTime() > 0)) {
                        continue;
                    }
                    if (spawnPoint.shouldSpawn() || GameConstants.isForceRespawn(mapid)) {
                        spawnPoint.spawnMonster(this);
                        spawned++;
                    }
                    if (spawned >= numShouldSpawn) {
                        break;
                    }
                }
            }
        }
    }

    public void respawn2(final boolean force) {
        respawned = true;
        lastSpawnTime = System.currentTimeMillis();
        int numShouldSpawn = maxRegularSpawn - spawnedMonstersOnMap.get();
        if (numShouldSpawn > 0 && monsterSpawn.size() > 0) {
            List<Spawns> randomSpawn = new ArrayList<>(monsterSpawn);
            Collections.shuffle(randomSpawn);
            for (Spawns spawnPoint : randomSpawn) {
                if (spawnPoint.shouldSpawn() || GameConstants.isForceRespawn(mapid)) {
                    spawnPoint.spawnMonster(this);
                }
            }
        }
    }

    private interface DelayedPacketCreation {
        void sendPackets(MapleClient c);
    }

    private interface SpawnCondition {
        boolean canSpawn(MapleCharacter chr);
    }

    public String getSnowballPortal() {
        int[] teamss = new int[2];
        for (MapleCharacter chr : getCharactersThreadsafe()) {
            if (chr.getPosition().y > -80) {
                teamss[0]++;
            } else {
                teamss[1]++;
            }
        }
        if (teamss[0] > teamss[1]) {
            return "st01";
        } else {
            return "st00";
        }
    }

    public boolean isDisconnected(int id) {
        return dced.contains(Integer.valueOf(id));
    }

    public void addDisconnected(int id) {
        dced.add(Integer.valueOf(id));
    }

    public void resetDisconnected() {
        dced.clear();
    }

    public void startSpeedRun() {
        MapleSquad squad = getSquadByMap();
        if (squad != null) {
            for (MapleCharacter chr : getCharactersThreadsafe()) {
                if (chr.getName().equals(squad.getLeaderName())) {
                    startSpeedRun(chr.getName());
                    return;
                }
            }
        }
    }

    public void startSpeedRun(String leader) {
        speedRunStart = System.currentTimeMillis();
        speedRunLeader = leader;
    }

    public void endSpeedRun() {
        speedRunStart = 0;
        speedRunLeader = "";
    }

    public void getRankAndAdd(String leader, String time, SpeedRunType type, long timz, Collection<String> squad) {
        try {
            //Pair<String, Map<Integer, String>>
            StringBuilder rett = new StringBuilder();
            if (squad != null) {
                for (String chr : squad) {
                    rett.append(chr);
                    rett.append(",");
                }
            }
            String z = rett.toString();
            if (squad != null) {
                z = z.substring(0, z.length() - 1);
            }
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("INSERT INTO speedruns(`type`, `leader`, `timestring`, `time`, `members`) VALUES (?,?,?,?,?)");
            ps.setString(1, type.name());
            ps.setString(2, leader);
            ps.setString(3, time);
            ps.setLong(4, timz);
            ps.setString(5, z);
            ps.executeUpdate();
            ps.close();

            if (SpeedRunner.getInstance().getSpeedRunData(type) == null) { //great, we just add it
                SpeedRunner.getInstance().addSpeedRunData(type, SpeedRunner.getInstance().addSpeedRunData(new StringBuilder("#rThese are the speedrun times for " + type + ".#k\r\n\r\n"), new HashMap<>(), z, leader, 1, time));
            } else {
                //i wish we had a way to get the rank
                //TODO revamp
                SpeedRunner.getInstance().removeSpeedRunData(type);
                SpeedRunner.getInstance().loadSpeedRunData(type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getSpeedRunStart() {
        return speedRunStart;
    }

    public void disconnectAll() {
        for (MapleCharacter chr : getCharactersThreadsafe()) {
            if (!chr.isGM()) {
                chr.getClient().disconnect(true, false);
                chr.getClient().getSession().close();
            }
        }
    }

    public List<MapleNPC> getAllNPCs() {
        return getAllNPCsThreadsafe();
    }

    public List<MapleNPC> getAllNPCsThreadsafe() {
        ArrayList<MapleNPC> ret = new ArrayList<MapleNPC>();
        mapobjectlocks.get(MapleMapObjectType.NPC).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.NPC).values()) {
                ret.add((MapleNPC) mmo);
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.NPC).readLock().unlock();
        }
        return ret;
    }

    public void resetNPCs() {
        List<MapleNPC> npcs = getAllNPCsThreadsafe();
        for (MapleNPC npc : npcs) {
            if (npc.isCustom()) {
                broadcastMessage(MaplePacketCreator.spawnNPC(npc, false));
                removeMapObject(npc);
            }
        }
    }

    public void resetFully() {
        resetFully(true);
    }

    public void resetFully(final boolean respawn) {
        killAllMonsters(false);
        reloadReactors();
        removeDrops();
        resetNPCs();
        resetSpawns();
        resetDisconnected();
        endSpeedRun();
        cancelSquadSchedule();
        resetPortals();
        environment.clear();
        if (respawn) {
            respawn(true);
        }
    }

    public void cancelSquadSchedule() {
        squadTimer = false;
        if (squadSchedule != null) {
            squadSchedule.cancel(false);
            squadSchedule = null;
        }
    }

    public void removeDrops() {
        List<MapleMapItem> items = this.getAllItemsThreadsafe();
        for (MapleMapItem i : items) {
            i.expire(this);
        }
    }

    public void resetAllSpawnPoint(int mobid, int mobTime) {
        Collection<Spawns> sss = new LinkedList<Spawns>(monsterSpawn);
        resetFully();
        monsterSpawn.clear();
        for (Spawns s : sss) {
            MapleMonster newMons = MapleLifeFactory.getMonster(mobid);
            MapleMonster oldMons = s.getMonster();
            newMons.setCy(oldMons.getCy());
            newMons.setF(oldMons.getF());
            newMons.setFh(oldMons.getFh());
            newMons.setRx0(oldMons.getRx0());
            newMons.setRx1(oldMons.getRx1());
            newMons.setPosition(new Point(oldMons.getPosition()));
            newMons.setHide(oldMons.isHidden());
            addMonsterSpawn(newMons, mobTime, (byte) -1, null);
        }
        loadMonsterRate(true);
    }

    public void resetSpawns() {
        boolean changed = false;
        Iterator<Spawns> sss = monsterSpawn.iterator();
        while (sss.hasNext()) {
            if (sss.next().getCarnivalId() > -1) {
                sss.remove();
                changed = true;
            }
        }
        setSpawns(true);
        if (changed) {
            loadMonsterRate(true);
        }
    }

    public boolean makeCarnivalSpawn(final int team, MapleMonster newMons, int num) {
        MonsterPoint ret = null;
        for (MonsterPoint mp : nodes.getMonsterPoints()) {
            if (mp.team == team || mp.team == -1) {
                Point newpos = calcPointBelow(new Point(mp.x, mp.y));
                newpos.y -= 1;
                boolean found = false;
                for (Spawns s : monsterSpawn) {
                    if (s.getCarnivalId() > -1 && (mp.team == -1 || s.getCarnivalTeam() == mp.team) && s.getPosition().x == newpos.x && s.getPosition().y == newpos.y) {
                        found = true;
                        break; //this point has already been used.
                    }
                }
                if (!found) {
                    ret = mp; //this point is safe for use.
                    break;
                }
                if (!found) {
                    ret = mp;
                    break;
                }
            }
        }
        if (ret != null) {
            newMons.setCy(ret.cy);
            newMons.setF(0); //always.
            newMons.setFh(ret.fh);
            newMons.setRx0(ret.x + 50);
            newMons.setRx1(ret.x - 50); //does this matter
            newMons.setPosition(new Point(ret.x, ret.y));
            newMons.setHide(false);
            SpawnPoint sp = addMonsterSpawn(newMons, 1, (byte) team, null);
            sp.setCarnival(num);
        }
        return ret != null;
    }

    public boolean makeCarnivalReactor(final int team, int num) {
        MapleReactor old = getReactorByName(team + "" + num);
        if (old != null && old.getState() < 5) { //already exists
            return false;
        }
        Point guardz = null;
        List<MapleReactor> react = getAllReactorsThreadsafe();
        for (Pair<Point, Integer> guard : nodes.getGuardians()) {
            if (guard.right == team || guard.right == -1) {
                boolean found = false;
                for (MapleReactor r : react) {
                    if (r.getPosition().x == guard.left.x && r.getPosition().y == guard.left.y && r.getState() < 5) {
                        found = true;
                        break; //already used
                    }
                }
                if (!found) {
                    guardz = guard.left; //this point is safe for use.
                    break;
                }
            }
        }
        if (guardz != null) {
            MapleReactorStats stats = MapleReactorFactory.getReactor(9980000 + team);
            MapleReactor my = new MapleReactor(stats, 9980000 + team);
            stats.setFacingDirection((byte) 0); //always
            my.setPosition(guardz);
            my.setState((byte) 1);
            my.setDelay(0);
            my.setName(team + "" + num); //lol
            //with num. -> guardians in factory
            spawnReactor(my);
            MCSkill skil = MapleCarnivalFactory.getInstance().getGuardian(num);
            for (MapleMonster mons : getAllMonstersThreadsafe()) {
                if (mons.getCarnivalTeam() == team) {
                    skil.getSkill().applyEffect(null, mons, false);
                }
            }
        }
        return guardz != null;
    }

    public void blockAllPortal() {
        for (MaplePortal p : portals.values()) {
            p.setPortalState(false);
        }
    }

    public boolean getAndSwitchTeam() {
        return getCharactersSize() % 2 != 0;
    }

    public void setSquad(MapleSquadType s) {
        this.squad = s;

    }

    public int getChannel() {
        return channel;
    }

    public int getConsumeItemCoolTime() {
        return consumeItemCoolTime;
    }

    public void setConsumeItemCoolTime(int ciit) {
        this.consumeItemCoolTime = ciit;
    }

    public void setPermanentWeather(int pw) {
        this.permanentWeather = pw;
    }

    public int getPermanentWeather() {
        return permanentWeather;
    }

    public void checkStates(final String chr) {
        MapleSquad sqd = getSquadByMap();
        EventManager em = getEMByMap();
        int size = getCharactersSize();
        if (sqd != null) {
            sqd.removeMember(chr);
            if (em != null) {
                if (sqd.getLeaderName().equals(chr)) {
                    em.setProperty("leader", "false");
                }
                if (chr.equals("") || size == 0) {
                    sqd.clear();
                    em.setProperty("state", "0");
                    em.setProperty("leader", "true");
                    cancelSquadSchedule();
                }
            }
        }
        if (em != null && em.getProperty("state") != null) {
            if (size == 0) {
                em.setProperty("state", "0");
                if (em.getProperty("leader") != null) {
                    em.setProperty("leader", "true");
                }
            }
        }
        if (speedRunStart > 0 && speedRunLeader.equalsIgnoreCase(chr)) {
            if (size > 0) {
                broadcastMessage(MaplePacketCreator.serverNotice(5, "队长不在地图上！你的挑战失败"));
            }
            endSpeedRun();
        }
    }

    public void setNodes(final MapleNodes mn) {
        this.nodes = mn;
    }

    public List<MaplePlatform> getPlatforms() {
        return nodes.getPlatforms();
    }

    public Collection<MapleNodeInfo> getNodes() {
        return nodes.getNodes();
    }

    public MapleNodeInfo getNode(final int index) {
        return nodes.getNode(index);
    }

    public List<Rectangle> getAreas() {
        return nodes.getAreas();
    }

    public Rectangle getArea(final int index) {
        return nodes.getArea(index);
    }

    public void changeEnvironment(final String ms, int type) {
        broadcastMessage(MaplePacketCreator.environmentChange(ms, type));
    }

    public void toggleEnvironment(final String ms) {
        if (environment.containsKey(ms)) {
            moveEnvironment(ms, environment.get(ms) == 1 ? 2 : 1);
        } else {
            moveEnvironment(ms, 1);
        }
    }

    public void moveEnvironment(final String ms, int type) {
        //  broadcastMessage(MaplePacketCreator.environmentMove(ms, type));
        environment.put(ms, type);
    }

    public Map<String, Integer> getEnvironment() {
        return environment;
    }

    public int getNumPlayersInArea(final int index) {
        int ret = 0;

        charactersLock.readLock().lock();
        try {
            Iterator<MapleCharacter> ltr = characters.iterator();
            MapleCharacter a;
            while (ltr.hasNext()) {
                if (getArea(index).contains(ltr.next().getPosition())) {
                    ret++;
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
        return ret;
    }

    public void broadcastGMMessage(MapleCharacter source, MaplePacket packet, boolean repeatToSource) {
        broadcastGMMessage(repeatToSource ? null : source, packet, Double.POSITIVE_INFINITY, source.getPosition());
    }

    private void broadcastGMMessage(MapleCharacter source, MaplePacket packet, double rangeSq, Point rangedFrom) {
        charactersLock.readLock().lock();
        try {
            if (source == null) {
                for (MapleCharacter chr : characters) {
                    if (chr.isStaff()) {
                        chr.getClient().getSession().write(packet);
                    }
                }
            } else {
                for (MapleCharacter chr : characters) {
                    if (chr != source && (chr.getGMLevel() >= source.getGMLevel())) {
                        chr.getClient().getSession().write(packet);
                    }
                }
            }
        } finally {
            charactersLock.readLock().unlock();
        }
    }

    public List<Pair<Integer, Integer>> getMobsToSpawn() {
        return nodes.getMobsToSpawn();
    }

    public List<Integer> getSkillIds() {
        return nodes.getSkillIds();
    }

    public boolean canSpawn2() {
        return lastSpawnTime > 0 && isSpawns && lastSpawnTime + createMobInterval < System.currentTimeMillis();
    }

    public boolean canSpawn() {
//        1、第一次的時候 刷怪时间为1s
//        2、其他时候 刷新时间为9s
        if (!respawned) {
//            说明此地图还没有刷过怪，使用第一次的间隔时间刷怪
            return lastSpawnTime + firstCreateMobInterval < System.currentTimeMillis();
        }
        return lastSpawnTime > 0 && isSpawns && lastSpawnTime + createMobInterval < System.currentTimeMillis();
    }

    public boolean canHurt() {
        if (lastHurtTime > 0 && lastHurtTime + decHPInterval < System.currentTimeMillis()) {
            lastHurtTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    public List<Integer> getAllUniqueMonsters() {
        ArrayList ret = new ArrayList();
        this.mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.MONSTER).values()) {
                int theId = ((MapleMonster) mmo).getId();
                if (!ret.contains(Integer.valueOf(theId))) {
                    ret.add(Integer.valueOf(theId));
                }
            }
        } finally {
            this.mapobjectlocks.get(MapleMapObjectType.MONSTER).readLock().unlock();
        }
        return ret;
    }

    public int getNumPlayersItemsInArea(int index) {
        return getNumPlayersItemsInRect(getArea(index));
    }

    public int getNumPlayersItemsInRect(final Rectangle rect) {
        int ret = getNumPlayersInRect(rect);

        mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().lock();
        try {
            for (MapleMapObject mmo : mapobjects.get(MapleMapObjectType.ITEM).values()) {
                if (rect.contains(mmo.getPosition())) {
                    ret++;
                }
            }
        } finally {
            mapobjectlocks.get(MapleMapObjectType.ITEM).readLock().unlock();
        }
        return ret;
    }

    public int getNumPlayersInRect(Rectangle rect) {
        int ret = 0;
        this.charactersLock.readLock().lock();
        try {
            Iterator ltr = this.characters.iterator();

            while (ltr.hasNext()) {
                if (rect.contains(((MapleCharacter) ltr.next()).getTruePosition())) {
                    ret++;
                }
            }
        } finally {
            this.charactersLock.readLock().unlock();
        }
        return ret;
    }

    public void resetAriantPQ(int level) {
        killAllMonsters(true);
        reloadReactors();
        removeDrops();
        resetNPCs();
        resetSpawns();
        resetDisconnected();
        endSpeedRun();
        resetPortals();
        environment.clear();
        respawn(true);
        for (MapleMonster mons : getAllMonstersThreadsafe()) {
            mons.changeLevel(level, true);
        }
        resetSpawnLevel(level);
    }

    public void resetSpawnLevel(int level) {
        for (Spawns spawn : monsterSpawn) {
            if (spawn instanceof SpawnPoint) {
                ((SpawnPoint) spawn).setLevel(level);
            }
        }
    }

    public void setDocked(boolean x) {
    }

    public void spawnRabbit(int hp) {
        hp = 100000;
        int mid = 9300061;
        MapleMonster onemob = MapleLifeFactory.getMonster(mid);
        OverrideMonsterStats overrideStats = new OverrideMonsterStats(hp, onemob.getMobMaxMp(), 0, false);
        MapleMonster mob = MapleLifeFactory.getMonster(mid);
        mob.setHp(hp);
        mob.setOverrideStats(overrideStats);
        spawnMonsterOnGroundBelow(mob, new Point(-183, -433));
    }

    public void KillFk(final boolean animate) {
        List<MapleMapObject> monsters = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
        for (MapleMapObject monstermo : monsters) {
            MapleMonster monster = (MapleMonster) monstermo;
            if (monster.getId() == 3230300 || monster.getId() == 3230301) {
                spawnedMonstersOnMap.decrementAndGet();
                monster.setHp(0);
                broadcastMessage(MobPacket.killMonster(monster.getObjectId(), animate ? 1 : 0));
                removeMapObject(monster);
                monster.killed();
            }
        }
    }

    public int mobCount() {
        List<MapleMapObject> mobsCount = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.MONSTER));
        return mobsCount.size();
    }

    public int playerCount() {
        List<MapleMapObject> players = getMapObjectsInRange(new Point(0, 0), Double.POSITIVE_INFINITY, Arrays.asList(MapleMapObjectType.PLAYER));
        return players.size();
    }

    public void killMonster_2(final MapleMonster monster) { // For mobs with removeAfter
        if (monster == null) {
            return;
        }
        spawnedMonstersOnMap.decrementAndGet();
        monster.setHp(0);

        broadcastMessage(MobPacket.killMonster(monster.getObjectId(), monster.getStats().getSelfD() < 0 ? 1 : monster.getStats().getSelfD()));
        removeMapObject(monster);
        monster.killed();
    }

    public void reloadCPQ() {
        int[] maps = {980000101, 980000201, 980000301, 980000401, 980000501, 980000601};
        for (int i = 0; i < maps.length; i++) {
            int mapid = maps[i];
            for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                cserv.getMapFactory().destroyMap(mapid, true);
                cserv.getMapFactory().HealMap(mapid);
            }
        }
    }

}
