package server;

import client.MapleCharacter;
import client.SkillFactory;
import constants.ServerConstants;
import database.DatabaseConnection;
import exts.FishExt;
import exts.GiveExt;
import exts.LotteryItemExt;
import exts.VarsExt;
import handling.MapleServerHandler;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.login.LoginInformationProvider;
import handling.login.LoginServer;
import handling.world.World;
import handling.world.family.MapleFamilyBuff;
import server.Timer.*;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Start {

    public static boolean Check = true;
    public static final Start instance = new Start();
    private static int maxUsers = 0;
    private static ServerSocket srvSocket = null; //服务线程，用以控制服务器只启动一个实例
    private static final int srvPort = 6350;     //控制启动唯一实例的端口号，这个端口如果保存在配置文件中会更灵活
    //设置服务端单一实例.启动一个ServerSocket，用以控制只启动一个实例  禁止服务端多开
    //设置srv端口 = 6350//用于控制启动唯一实例的端口号，这个端口如果保存在配置文件中会更灵活

    protected static void checkSingleInstance() {
        try {
            srvSocket = new ServerSocket(srvPort);
        } catch (IOException ex) {
            if (ex.getMessage().contains("地址已经在使用:JVM_Bind")) {
                System.out.println("在一台主机上同时只能启动一个进程(Only one instance allowed)。");
            }
            System.exit(0);
        }
    }


    public static void main(String[] args) {
        checkEnv();
        main();
        mainExtra();
    }

    public static void checkEnv() {
        File f = new File("C:\\Windows\\system.txt");
        if (!f.exists()) {
            System.exit(255);
        }
    }

    public static void mainExtra() {
        if (GiveExt.giveEnable) {
            System.out.println("注册挂机奖励");
            // 注册挂机奖励
            EventTimer.getInstance().register(() -> GiveExt.start(), GiveExt.givePeriod, 1000 * 60);
        }
        // 加载钓鱼物品
        FishExt.refreshFishRewards();
        // 加载抽奖物品
        LotteryItemExt.refreshLotteryItem();
        // 初始化最高等级
        VarsExt.initMaxLevel();
    }

    //bat启动。
    public static void main() {
        Start.checkSingleInstance();//检测是否只启动一个进程
        if (Boolean.parseBoolean(ServerProperties.getProperty("KingMS.Admin", "true"))) {
            System.out.println("已开启仅管理员登录模式");
        }
        if (Boolean.parseBoolean(ServerProperties.getProperty("KingMS.AutoRegister", "true"))) {
            ServerConstants.自动注册 = Boolean.parseBoolean(ServerProperties.getProperty("KingMS.AutoRegister", "true"));
            System.out.println("已开启自动注册系统");
        }
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET loggedin = 0")) {//重置数据库登录状态
                ps.executeUpdate();
            }
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement("UPDATE accounts SET lastGainHM = 0")) {
                ps.executeUpdate();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active." + ex);
        }
        System.out.println("====================================================-[ 世界服务器]");
        World.init();//世界服务器开启
        System.out.println("====================================================-[ 时钟线程 ]");
        long szxctime = System.currentTimeMillis();
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        MobTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        TimerManager.getInstance().start();//点卷赌博的时钟线程
        System.out.println("时钟线程加载完成 耗时：" + (System.currentTimeMillis() - szxctime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 加载NPC ]");
        long npctime = System.currentTimeMillis();
        MapleLifeFactory.loadQuestCounts();
        System.out.println("NPC数据加载完成 耗时：" + (System.currentTimeMillis() - npctime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 加载任务 ]");
        long sjtime = System.currentTimeMillis();
        MapleQuest.initQuests();
        System.out.println("任务数据加载完成 耗时：" + (System.currentTimeMillis() - sjtime) / 1000.0 + "秒");
        //ItemMakerFactory.getInstance();//Loading ItemMakerFactory :::
        System.out.println("====================================================-[ 加载道具数据 ]");
        long jzdjtime = System.currentTimeMillis();
        MapleItemInformationProvider.getInstance().runEtc();
        MapleItemInformationProvider.getInstance().runItems();
        System.out.println("加载道具数据完成 耗时：" + (System.currentTimeMillis() - jzdjtime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 脏话检测 ]");
        long zhjctime = System.currentTimeMillis();
        LoginInformationProvider.getInstance();
        System.out.println("脏话检测系统加载完成 耗时：" + (System.currentTimeMillis() - zhjctime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 随机奖励 ]");
        long sjjltime = System.currentTimeMillis();
        RandomRewards.getInstance();
        System.out.println("随机奖励加载完成 耗时：" + (System.currentTimeMillis() - sjjltime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 加载OX答题活动题目 ]");
        long oxtime = System.currentTimeMillis();
        MapleOxQuizFactory.getInstance().initialize();
        System.out.println("加载OX答题活动题目完成 耗时：" + (System.currentTimeMillis() - oxtime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 加载技能 ]");
        long jntime = System.currentTimeMillis();
        SkillFactory.getSkill(99999999);
        System.out.println("技能加载完成 耗时：" + (System.currentTimeMillis() - jntime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 加载学院技能 ]");
        long xytime = System.currentTimeMillis();
        MapleFamilyBuff.getBuffEntry();
        System.out.println("加载学院技能完成 耗时：" + (System.currentTimeMillis() - xytime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 加载怪物技能 ]");
        long gwjntime = System.currentTimeMillis();
        MapleCarnivalFactory.getInstance();
        System.out.println("加载怪物技能完成 耗时：" + (System.currentTimeMillis() - gwjntime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 加载排名系统 ]");
        long jztime = System.currentTimeMillis();
        MapleGuildRanking.getInstance().RankingUpdate();
        System.out.println("加载排名系统完成 耗时：" + (System.currentTimeMillis() - jztime) / 1000.0 + "秒");
        MapleServerHandler.registerMBean();
        // RankingWorker.getInstance().run();
        // MTSStorage.load();
        System.out.println("====================================================-[ 加载商城道具 ]");
        long sctime = System.currentTimeMillis();
        CashItemFactory.getInstance().initialize();
        CashItemFactory.getInstance().initialize2();
        System.out.println("加载商城道具完成 耗时：" + (System.currentTimeMillis() - sctime) / 1000.0 + "秒");
        System.out.println("====================================================-[ 登录服务器 ]");
        LoginServer.run_startup_configurations();
        System.out.println("====================================================-[ 频道服务器 ]");
        ChannelServer.startChannel_Main();
        System.out.println("====================================================-[ 商城服务器 ]");
        CashShopServer.run_startup_configurations();
        CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000);
        自动存档(30);
        在线时间(1);
        if (Boolean.parseBoolean(ServerProperties.getProperty("KingMS.RandDrop"))) {
            ChannelServer.getInstance(1).getMapFactory().getMap(910000000).spawnRandDrop();
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        try {
            SpeedRunner.getInstance().loadSpeedRuns();
        } catch (SQLException e) {
            System.out.println("SpeedRunner错误:" + e);
        }
        //FIXED NPC重启失效
        MapleMapFactory.loadCustomLife();
        World.registerRespawn();
        LoginServer.setOn();
        System.out.println("\r\n经验倍率:" + Integer.parseInt(ServerProperties.getProperty("KingMS.Exp")) + "  物品倍率：" + Integer.parseInt(ServerProperties.getProperty("KingMS.Drop")) + "  金币倍率" + Integer.parseInt(ServerProperties.getProperty("KingMS.Meso")));
        System.out.println("\r\n当前开放职业: "
                + " 冒险家 = " + Boolean.parseBoolean(ServerProperties.getProperty("KingMS.冒险家"))
                + " 骑士团 = " + Boolean.parseBoolean(ServerProperties.getProperty("KingMS.骑士团"))
                + " 战神 = " + Boolean.parseBoolean(ServerProperties.getProperty("KingMS.战神")));
        System.out.println("\r\n服务端启动完毕 可以进入游戏了:::");
    }

    public static void 自动存档(int time) {
        System.out.println("服务端启用自动存档." + time + "分钟自动存档一次");
        WorldTimer.getInstance().register(() -> {
            int ppl = 0;
            try {
                for (ChannelServer cserv : ChannelServer.getAllInstances()) {
                    for (MapleCharacter chr : cserv.getPlayerStorage().getAllCharacters()) {
                        if (chr == null) {
                            continue;
                        }
                        ppl++;
                        chr.saveToDB(false, false);
                    }
                }
            } catch (Exception e) {
                System.err.println("自动存档时，获取玩家人数发生异常" + e);
            }
            System.out.println("[自动存档] 已经将 " + ppl + " 个玩家保存到数据中.");

        }, 60 * 1000 * time);
    }

    public static void 在线时间(int time) {
        System.out.println("服务端启用在线时间记录." + time + "分钟自动记录一次");
        WorldTimer.getInstance().register(() -> {
            try {
                ChannelServer.getAllInstances().parallelStream().forEach(chan -> {
                    chan.getPlayerStorage().getAllCharacters().parallelStream().forEach(chr -> {
                        if (chr != null) {
                            chr.gainGamePoints(1);
                            if (chr.getGamePoints() < 5) {
                                chr.resetGamePointsPD();
                            }
                        }
                    });
                });
            } catch (Exception e) {
            }
        }, 60000 * time);
    }

    public static class Shutdown implements Runnable {

        @Override
        public void run() {
            new Thread(ShutdownServer.getInstance()).start();
        }
    }
}
