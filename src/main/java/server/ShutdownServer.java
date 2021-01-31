package server;

import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World.Alliance;
import handling.world.World.Broadcast;
import handling.world.World.Family;
import handling.world.World.Guild;
import server.Timer.*;
import tools.MaplePacketCreator;

import java.util.Set;

public class ShutdownServer implements Runnable {

    private static final ShutdownServer instance = new ShutdownServer();
    public static boolean running = false;
    public int mode = 0;

    public static ShutdownServer getInstance() {
        return instance;
    }

    public void shutdown() {
        run();
    }

    @Override
    public void run() {

        //Timer
        WorldTimer.getInstance().stop();
        MapTimer.getInstance().stop();
        BuffTimer.getInstance().stop();
        CloneTimer.getInstance().stop();
        EventTimer.getInstance().stop();
        EtcTimer.getInstance().stop();

        //Merchant
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            cs.closeAllMerchant();
        }

        try {
            //Guild
            Guild.save();
            //Alliance
            Alliance.save();
            //Family
            Family.save();
        } catch (Exception ex) {
        }

        Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, " 游戏服务器将关闭维护，请玩家安全下线..."));
        for (ChannelServer cs : ChannelServer.getAllInstances()) {
            try {
                cs.setServerMessage("游戏服务器将关闭维护，请玩家安全下线...");
            } catch (Exception ex) {
            }
        }
        Set<Integer> channels = ChannelServer.getAllInstance();

        for (Integer channel : channels) {
            try {
                ChannelServer cs = ChannelServer.getInstance(channel);
                cs.saveAll();
                cs.setFinishShutdown();
                cs.shutdown();
            } catch (Exception e) {
                System.out.println("频道" + channel + " 关闭错误.");
            }
        }

        System.out.println("服务端关闭事件 1 已完成.");
        System.out.println("服务端关闭事件 2 开始...");

        try {
            LoginServer.shutdown();
            System.out.println("登录伺服器关闭完成...");
        } catch (Exception e) {
        }
        try {
            CashShopServer.shutdown();
            System.out.println("商城伺服器关闭完成...");
        } catch (Exception e) {
        }
        try {
            DatabaseConnection.closeAll();
        } catch (Exception e) {
        }
        //Timer.PingTimer.getInstance().stop();
        System.out.println("服务端关闭事件 2 已完成.");
        try {
            Thread.sleep(1000);
        } catch (Exception e) {
            System.out.println("关闭服务端错误 - 2" + e);

        }
        System.exit(0);
    }
}
