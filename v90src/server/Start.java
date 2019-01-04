package server;

import client.MapleCharacter;
import client.MapleFaction;
import client.SkillFactory;
import constants.JobConstants;
import handling.channel.ChannelServer;
import handling.channel.MapleGuildRanking;
import handling.login.LoginServer;
import handling.cashshop.CashShopServer;
import handling.login.LoginInformationProvider;
import handling.world.World;
import java.sql.SQLException;
import database.DatabaseConnection;
import handling.world.family.MapleFamily;
import handling.world.family.MapleFamilyBuff;
import handling.world.guild.MapleGuild;
import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Scanner;
import java.util.regex.Pattern;
import server.Timer.*;
import server.events.MapleOxQuizFactory;
import server.life.MapleLifeFactory;
import server.life.MapleMonsterInformationProvider;
import server.life.PlayerNPC;
import server.maps.AramiaFireWorks;
import server.maps.MapleMapFactory;
import server.quest.MapleQuest;
import server.quest.custom.CustomQuestProvider;
import tools.StringUtil;

public class Start {

    public static long startTime = System.currentTimeMillis();
    public static final Start instance = new Start();

    public static void main(String[] args) throws InterruptedException {
        instance.run();
    }

    public void run() throws InterruptedException {
        //InputStreamReader input = new InputStreamReader(null);
        
        if (Boolean.parseBoolean(ServerProperties.getProperty("net.sf.odinms.world.admin"))) {
            System.out.println("[!!! Admin Only Mode Active !!!]");
        }
        try {
            DatabaseConnection.setProps(); // call now
        } catch (SQLException ex) {
            throw new RuntimeException("[SQL EXCEPTION] Error connecting to the database.");
        }

        try {
            Connection con = DatabaseConnection.getConnection();
            PreparedStatement ps = con.prepareStatement("UPDATE accounts SET loggedin = 0");
            ps.executeUpdate();
            ps.close();
            // Fixes null family
            ps = con.prepareStatement("UPDATE characters SET familyid = 0 WHERE seniorid = 0 and junior1 = 0 and junior2 = 0");
            ps.executeUpdate();
            ps.close();
        } catch (SQLException ex) {
            throw new RuntimeException("[EXCEPTION] Please check if the SQL server is active.");
        }

        System.out.println("[" + ServerProperties.getProperty("net.sf.odinms.login.serverName") + "]");
        World.init();
        WorldTimer.getInstance().start();
        EtcTimer.getInstance().start();
        MapTimer.getInstance().start();
        MobTimer.getInstance().start();
        CloneTimer.getInstance().start();
        EventTimer.getInstance().start();
        BuffTimer.getInstance().start();
        PingTimer.getInstance().start();

        printLoad("WorldLoader");
        MapleGuildRanking.getInstance().getRank();
        MapleGuild.loadAll();

        printLoad("FamilyLoader");
        World.Family.lastDayTime = System.currentTimeMillis();// count begins now
        MapleFamily.loadAll();

        printLoad("QuestLoader");
        MapleQuest.initQuests();
        MapleLifeFactory.loadQuestCounts();
        CustomQuestProvider.initQuests();

        printLoad("ProviderLoader");
        MapleItemInformationProvider.getInstance().load();

        printLoad("MonsterLoader");
        MapleMonsterInformationProvider.getInstance().load();

        printLoad("SkillFactoryLoader");
        SkillFactory.getSkill(99999999);
        JobConstants.loadAllSkills();

        printLoad("BasicLoader");
        LoginInformationProvider.getInstance();
        RandomRewards.getInstance();
        MapleOxQuizFactory.getInstance().initialize();
        MapleCarnivalFactory.getInstance().initialize();
        SpeedRunner.getInstance().loadSpeedRuns();
        SpeedQuizFactory.getInstance().initialize();
        ItemMakerFactory.getInstance();
        MapleFamilyBuff.getBuffEntry();
        MapleMapFactory.loadCustomLife();

        printLoad("CashItemLoader");
       //CashItemFactory.getInstance().initialize();

        System.out.println("[Loading Login]");
        LoginServer.run_startup_configurations();
        System.out.println("[Login Initialized]");

        System.out.println("[Loading Channel]");
        ChannelServer.startChannel_Main();
        System.out.println("[Channel Initialized]");

        System.out.println("[Loading CS]");
        CashShopServer.run_startup_configurations();
        System.out.println("[CS Initialized]");

        CheatTimer.getInstance().register(AutobanManager.getInstance(), 60000);
        Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
        World.registerRespawn();
        if (ShutdownServer.getInstance() == null) {
            ShutdownServer.registerMBean();
        } else {
            System.out.println("--MBean server was already active--");
        }
        PlayerNPC.loadAll();
        LoginServer.setOn();
        System.out.println("[Fully Initialized in " + (System.currentTimeMillis() - startTime) / 1000L + " seconds]");
        RankingWorker.getInstance().run();
        AramiaFireWorks.getInstance().randomizeKegs();
        System.out.println("[MapleBang set at "+AramiaFireWorks.getInstance().getKegs()+"]");
        MapleFaction.loadFactions();
        System.out.println("[MapleFaction] loaded " + World.Faction.getContainerSize() + " factions and added to faction object container.");
        System.out.println("[/////////////////////////////////////////////////]");
        System.out.println("Console Commands: ");
        System.out.println("say | prefixsay | shutdown | restart");
        listenCommand();
    }

    private static void printLoad(String thread) {
        System.out.println("[Loading Completed] " + thread + " | Completed in " + (System.currentTimeMillis() - startTime) + " Milliseconds.");
    }

    public static class Shutdown implements Runnable {

        @Override
        public void run() {
            ShutdownServer.getInstance().run();
        }
    }
    
    public static void listenCommand() {
        Scanner sc = new Scanner(System.in);
        String input;
        input = sc.nextLine();
        String command = input;
        if (command.equalsIgnoreCase("say")) {
            System.out.println("[console] Your message? write exit to go back to the menu");
            input = sc.nextLine();
            String message = input;
            if(message.equalsIgnoreCase("exit")) {
                restartlistener();
            }
            for(ChannelServer ch : ChannelServer.getAllInstances()) {
                for(MapleCharacter chr : ch.getPlayerStorage().getAllCharacters()) {
                    chr.dcolormsg(6, "[Console] " + message);
                }
            }
            System.out.println("[Console] " + message);
            restartlistener();
        } else if (command.contains("shutdown")) {
            Thread t = null;
            if (t == null || !t.isAlive()) {
                t = new Thread(ShutdownServer.getInstance());
                ShutdownServer.getInstance().shutdown();
                t.start();
            }
        } else if (command.contains("restart")) {
            Thread t = null;
            t = new Thread(ShutdownServer.getInstance());
            ShutdownServer.getInstance().shutdown();
            t.start();
            EtcTimer.getInstance().schedule(new Runnable() {
                public void run() {
                    String[] args = {"restart"};
                    try {
                        main(args);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 3 * 1000);
        } else if(command.contains("prefixsay")) {
            StringBuilder sb = new StringBuilder();
            System.out.println("What would you like the msg prefix to be ?");
            input = sc.nextLine();
            String prefix = input;
            sb.append("[").append(prefix).append("] ");
            System.out.println("What message to broadcast?");
            String input2 = sc.nextLine();
            String message = input2; //?
            sb.append(message);
            for(ChannelServer ch : ChannelServer.getAllInstances()) {
                for (MapleCharacter plr : ch.getPlayerStorage().getAllCharacters()) {
                    plr.dcolormsg(5, sb.toString());
                }
            }
            restartlistener();
        }
    }
    
    public static void restartlistener() {
        listenCommand();
    }
}
