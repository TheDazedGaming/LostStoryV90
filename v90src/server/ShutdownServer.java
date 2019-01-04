package server;

import client.MapleFaction;

import database.DatabaseConnection;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.login.LoginServer;
import handling.world.World;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import server.Timer.*;
import tools.MaplePacketCreator;

public class ShutdownServer implements ShutdownServerMBean {

    public static ShutdownServer instance;
    public int mode = 0;

    public static void registerMBean() {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            instance = new ShutdownServer();
            mBeanServer.registerMBean(instance, new ObjectName("server:type=ShutdownServer"));
        } catch (Exception e) {
            System.out.println("Error registering Shutdown MBean");
            e.printStackTrace();
        }
    }

    public static ShutdownServer getInstance() {
        return instance;
    }
    
    public void run() {
        if (this.mode == 0) {
            int ret = 0;
            World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, "The world is going to shutdown soon. Please log off safely."));
            for (ChannelServer cs : ChannelServer.getAllInstances()) {
                cs.setShutdown();
                cs.setServerMessage("The world is going to shutdown soon. Please log off safely.");
                //ret += cs.closeAllMerchant();
            }
            World.Guild.save();
            World.Alliance.save();
            World.Family.save();
            System.out.println("Shutdown 1 has completed. Hired merchants saved: " + ret);
            this.mode += 1;
        } else if (this.mode == 1) {
            this.mode += 1;
            System.out.println("Shutdown 2 commencing...");

            try {
                World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(0, "The world is going to shutdown now. Please log off safely."));
                Integer[] chs = ChannelServer.getAllInstance().toArray(new Integer[0]);

                for (int i : chs) {
                    try {
                        ChannelServer cs = ChannelServer.getInstance(i);
                        synchronized (this) {
                            cs.shutdown();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                LoginServer.shutdown();
                CashShopServer.shutdown();

                WorldTimer.getInstance().start();
                EtcTimer.getInstance().start();
                MapTimer.getInstance().start();
                MobTimer.getInstance().start();
                CloneTimer.getInstance().start();
                EventTimer.getInstance().start();
                BuffTimer.getInstance().start();
                PingTimer.getInstance().start();
                World.Faction.save();
            } catch (Exception e) {
                System.out.println("Failed to shutdown..." + e);
            }

            System.out.println("Shutdown 2 has finished.");
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException ex) {
//                ex.printStackTrace();
//            }
            DatabaseConnection.closeAll();
            this.mode = 0;
            System.out.println("Done.");
        }
    }
    
    /*
    public static void saveFaction() {
        for(MapleFactionType mft : MapleFactionType.values()) {
            MapleFaction.saveFaction();
        }
    }*/
    
    public void shutdown() {
        run();
    }
}
