package client.messages.commands;

import client.inventory.IItem;
import server.RankingWorker;
import client.MapleCharacter;
import constants.ServerConstants.PlayerGMRank;
import client.MapleClient;
import client.MapleCustomQuestStatus;
import client.MapleStat;
import client.anticheat.ReportType;
import client.inventory.MapleInventory;
import client.inventory.MapleInventoryType;
import client.messages.commands.CommandExecute.TradeExecute;
import constants.GameConstants;
import constants.MapConstants;
import constants.OccupationConstants;
import handling.world.World;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import scripting.NPCScriptManager;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.RankingWorker.RankingInformation;
import server.life.MapleLifeFactory;
import server.life.MapleMonster;


import server.life.MapleMonsterInformationProvider;
import server.life.MonsterDropEntry;
import server.maps.MapleMap;
import server.maps.MapleMapObject;
import server.maps.MapleMapObjectType;
import server.maps.SavedLocationType;
import server.quest.MapleQuest;
import server.quest.custom.CustomQuestProvider;
import tools.MaplePacketCreator;
import tools.StringUtil;

/**
 *
 * @author Emilyx3
 */
public class PlayerCommand {
    
    public static PlayerGMRank getPlayerLevelRequired() {
        return PlayerGMRank.NORMAL;
    }

    public static class STR extends DistributeStatCommands {

        public STR() {
            stat = MapleStat.STR;
        }
    }

    public static class DEX extends DistributeStatCommands {

        public DEX() {
            stat = MapleStat.DEX;
        }
    }

    public static class INT extends DistributeStatCommands {

        public INT() {
            stat = MapleStat.INT;
        }
    }

    public static class LUK extends DistributeStatCommands {

        public LUK() {
            stat = MapleStat.LUK;
        }
    }

    public abstract static class DistributeStatCommands extends CommandExecute {

        protected MapleStat stat = null;
        private static int statLim = 30000;

        private void setStat(MapleCharacter player, int amount) {
            switch (stat) {
                case STR:
                    player.getStat().setStr((short) amount);
                    player.updateSingleStat(MapleStat.STR, player.getStat().getStr());
                    break;
                case DEX:
                    player.getStat().setDex((short) amount);
                    player.updateSingleStat(MapleStat.DEX, player.getStat().getDex());
                    break;
                case INT:
                    player.getStat().setInt((short) amount);
                    player.updateSingleStat(MapleStat.INT, player.getStat().getInt());
                    break;
                case LUK:
                    player.getStat().setLuk((short) amount);
                    player.updateSingleStat(MapleStat.LUK, player.getStat().getLuk());
                    break;
            }
        }

        private int getStat(MapleCharacter player) {
            switch (stat) {
                case STR:
                    return player.getStat().getStr();
                case DEX:
                    return player.getStat().getDex();
                case INT:
                    return player.getStat().getInt();
                case LUK:
                    return player.getStat().getLuk();
                default:
                    throw new RuntimeException(); //Will never happen.
            }
        }

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return 0;
            }
            int change = 0;
            try {
                change = Integer.parseInt(splitted[1]);
            } catch (NumberFormatException nfe) {
                c.getPlayer().dropMessage(5, "Invalid number entered.");
                return 0;
            }
            if (change <= 0) {
                c.getPlayer().dropMessage(5, "You must enter a number greater than 0.");
                return 0;
            }
            if (c.getPlayer().getRemainingAp() < change) {
                c.getPlayer().dropMessage(5, "You don't have enough AP for that.");
                return 0;
            }
            if (getStat(c.getPlayer()) + change > statLim) {
                c.getPlayer().dropMessage(5, "The stat limit is " + statLim + ".");
                return 0;
            }
            setStat(c.getPlayer(), getStat(c.getPlayer()) + change);
            c.getPlayer().setRemainingAp((c.getPlayer().getRemainingAp() - change));
            c.getPlayer().updateSingleStat(MapleStat.AVAILABLEAP, Math.min(199, c.getPlayer().getRemainingAp()));
			c.getPlayer().dropMessage(5, "You've " + c.getPlayer().getRemainingAp() + " remaining ability points.");

            return 1;
        }
    }

    public static class MobDebug extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleMonster mob = null;
            for (final MapleMapObject monstermo : c.getPlayer().getMap().getMapObjectsInRange(c.getPlayer().getPosition(), 100000, Arrays.asList(MapleMapObjectType.MONSTER))) {
                mob = (MapleMonster) monstermo;
                if (mob.isAlive()) {
                    c.getPlayer().dropMessage(6, "Monster " + mob.toString());
                    break; //only one
                }
            }
            if (mob == null) {
                c.getPlayer().dropMessage(6, "No monster was found.");
            }
            return 1;
        }
    }

    public abstract static class OpenNPCCommand extends CommandExecute {

        protected int npc = -1;
        private static int[] npcs = { //Ish yur job to make sure these are in order and correct ;(
            9270035,
            9010017,
            9000000,
            1013105,
            1011101,
            9000020,
            1012117};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (npc != 1 && c.getPlayer().getMapId() != 910000000) { //drpcash can use anywhere
                for (int i : GameConstants.blockedMaps) {
                    if (c.getPlayer().getMapId() == i) {
                        c.getPlayer().dropMessage(5, "You may not use this command here.");
                        return 0;
                    }
                }
                if (c.getPlayer().getLevel() < 10) {
                    c.getPlayer().dropMessage(5, "You must be over level 10 to use this command.");
                    return 0;
                }
                if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || c.getPlayer().getMapId() >= 990000000/* || FieldLimitType.VipRock.check(c.getPlayer().getMap().getFieldLimit())*/) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                    return 0;
                }
                if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000)) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                    return 0;
                }
            }
            if (c.getPlayer().getConversation() != 0 || MapConstants.isStorylineMap(c.getPlayer().getMapId())/* && c.getPlayer().getCustomQuestStatus(200000) != 2*/) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            NPCScriptManager.getInstance().start(c, npcs[npc]);
            return 1;
        }
    }

    public static class Event extends OpenNPCCommand {

        public Event() {
            npc = 2;
        }
    }

    public static class ClearSlot extends CommandExecute {

        private static MapleInventoryType[] invs = {
            MapleInventoryType.EQUIP,
            MapleInventoryType.USE,
            MapleInventoryType.SETUP,
            MapleInventoryType.ETC,
            MapleInventoryType.CASH,};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(5, "@clearslot <eq/use/setup/etc/cash/all>");
                return 0;
            } else if (MapConstants.isStorylineMap(c.getPlayer().getMapId())) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            } else {
                MapleInventoryType type;
                if (splitted[1].equalsIgnoreCase("eq")) {
                    type = MapleInventoryType.EQUIP;
                } else if (splitted[1].equalsIgnoreCase("use")) {
                    type = MapleInventoryType.USE;
                } else if (splitted[1].equalsIgnoreCase("setup")) {
                    type = MapleInventoryType.SETUP;
                } else if (splitted[1].equalsIgnoreCase("etc")) {
                    type = MapleInventoryType.ETC;
                } else if (splitted[1].equalsIgnoreCase("cash")) {
                    type = MapleInventoryType.CASH;
                } else if (splitted[1].equalsIgnoreCase("all")) {
                    type = null;
                } else {
                    c.getPlayer().dropMessage(5, "@clearslot <eq/use/setup/etc/cash/all>");
                    return 0;
                }
                if (type == null) { //All, a bit hacky, but it's okay
                    for (MapleInventoryType t : invs) {
                        type = t;
                        MapleInventory inv = c.getPlayer().getInventory(type);
                        byte start = -1;
                        for (byte i = 0; i < inv.getSlotLimit(); i++) {
                            if (inv.getItem(i) != null) {
                                start = i;
                                break;
                            }
                        }
                        if (start == -1) {
                            c.getPlayer().dropMessage(5, "There are no items in that inventory.");
                            return 0;
                        }
                        int end = 0;
                        for (byte i = start; i < inv.getSlotLimit(); i++) {
                            if (inv.getItem(i) != null) {
                                if (inv.getItem(i).getItemId() != 4031753 && !GameConstants.isPet(inv.getItem(i).getItemId())) { // Zeta Residue
                                    MapleInventoryManipulator.removeFromSlot(c, type, i, inv.getItem(i).getQuantity(), true);
                                }
                            } else {
                                end = i;
                                break;//Break at first empty space.
                            }
                        }
                        c.getPlayer().dropMessage(5, "Cleared slots " + start + " to " + end + ".");
                    }
                } else {
                    MapleInventory inv = c.getPlayer().getInventory(type);
                    byte start = -1;
                    for (byte i = 0; i < inv.getSlotLimit(); i++) {
                        if (inv.getItem(i) != null) {
                            start = i;
                            break;
                        }
                    }
                    if (start == -1) {
                        c.getPlayer().dropMessage(5, "There are no items in that inventory.");
                        return 0;
                    }
                    byte end = 0;
                    for (byte i = start; i < inv.getSlotLimit(); i++) {
                        if (inv.getItem(i) != null) {
                            if (inv.getItem(i).getItemId() != 4031753 && !GameConstants.isPet(inv.getItem(i).getItemId())) { // Zeta Residue
                                MapleInventoryManipulator.removeFromSlot(c, type, i, inv.getItem(i).getQuantity(), true);
                            }
                        } else {
                            end = i;
                            break;//Break at first empty space.
                        }
                    }
                    c.getPlayer().dropMessage(5, "Cleared slots " + start + " to " + end + ".");
                }
                return 1;
            }
        }
    }

    public static class FM extends Warp {

        public FM() {
            map = 0;
        }
    }
    
    public static class Henesys extends Warp {
        public Henesys() {
            map = 2;
        }
    }
    
    public static class Home extends Warp {
        public Home() {
            map = 2;
        }
    }
    

    public abstract static class Warp extends CommandExecute {

        protected int map = -1;
        private static int[] maps = {910000000, 100000203, 100000000};

        @Override
        public int execute(MapleClient c, String[] splitted) {
            for (int i : GameConstants.blockedMaps) {
                if (c.getPlayer().getMapId() == i) {
                    c.getPlayer().dropMessage(5, "You may not use this command here.");
                    return 0;
                }
            }
            if (c.getPlayer().getMap().getSquadByMap() != null || c.getPlayer().getEventInstance() != null || c.getPlayer().getMap().getEMByMap() != null || MapConstants.isStorylineMap(c.getPlayer().getMapId()) || (maps[map] == c.getPlayer().getMapId())) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            if ((c.getPlayer().getMapId() >= 680000210 && c.getPlayer().getMapId() <= 680000502) || (c.getPlayer().getMapId() / 1000 == 980000 && c.getPlayer().getMapId() != 980000000) || (c.getPlayer().getMapId() / 100 == 1030008) || (c.getPlayer().getMapId() / 100 == 922010) || (c.getPlayer().getMapId() / 10 == 13003000) || (c.getPlayer().getMapId() >= 990000000)) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return 0;
            }
            if (map == 0) {
                c.getPlayer().saveLocation(SavedLocationType.FREE_MARKET, c.getPlayer().getMap().getReturnMap().getId());
            }
            final MapleMap smap = c.getChannelServer().getMapFactory().getMap(maps[map]);
            if (smap == null) {
                c.getPlayer().dropMessage(5, "An error has occured.");
                return 0;
            }
            c.getPlayer().changeMap(smap, smap.getPortal(map == 1 ? 18 : 0));
            return 1;
        }
    }

    public static class ea extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            return 1;
        }
    }
    
    public static class dispose extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            NPCScriptManager.getInstance().dispose(c);
            c.getSession().write(MaplePacketCreator.enableActions());
            return 1;
        }
    }

    public static class toggle extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().setSmega();
            return 1;
        }
    }

    public static class Ranking extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) { //job start end
                c.getPlayer().dropMessage(5, "Use @ranking [job] [start number] [end number] where start and end are ranks of the players");
                final StringBuilder builder = new StringBuilder("JOBS: ");
                for (String b : RankingWorker.getInstance().getJobCommands().keySet()) {
                    builder.append(b);
                    builder.append(" ");
                }
                c.getPlayer().dropMessage(5, builder.toString());
            } else {
                int start = 1, end = 20;
                try {
                    start = Integer.parseInt(splitted[2]);
                    end = Integer.parseInt(splitted[3]);
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(5, "You didn't specify start and end number correctly, the default values of 1 and 20 will be used.");
                }
                if (end < start || end - start > 20) {
                    c.getPlayer().dropMessage(5, "End number must be greater, and end number must be within a range of 20 from the start number.");
                } else {
                    final Integer job = RankingWorker.getInstance().getJobCommand(splitted[1]);
                    if (job == null) {
                        c.getPlayer().dropMessage(5, "Please use @ranking to check the job names.");
                    } else {
                        final List<RankingInformation> ranks = RankingWorker.getInstance().getRankingInfo(job.intValue());
                        if (ranks == null || ranks.size() <= 0) {
                            c.getPlayer().dropMessage(5, "No ranking was returned.");
                        } else {
                            int num = 0;
                            for (RankingInformation rank : ranks) {
                                if (rank.rank >= start && rank.rank <= end) {
                                    if (num == 0) {
                                        c.getPlayer().dropMessage(6, "Rankings for " + splitted[1] + " - from " + start + " to " + end);
                                        c.getPlayer().dropMessage(6, "--------------------------------------");
                                    }
                                    c.getPlayer().dropMessage(6, rank.toString());
                                    num++;
                                }
                            }
                            if (num == 0) {
                                c.getPlayer().dropMessage(5, "No ranking was returned.");
                            }
                        }
                    }
                }
            }
            return 1;
        }
    }
    

    public static class CheckDrop extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 4) { //start end
                c.getPlayer().dropMessage(5, "Use @checkdrop [monsterID] [start number] [end number] where start and end are the number of the drop");
                c.getPlayer().dropMessage(5, "You can get the monsterID through @mobdebug command.");
            } else {
                int start = 1, end = 10;
                try {
                    start = Integer.parseInt(splitted[2]);
                    end = Integer.parseInt(splitted[3]);
                } catch (NumberFormatException e) {
                    c.getPlayer().dropMessage(5, "You didn't specify start and end number correctly, the default values of 1 and 10 will be used.");
                }
                if (end < start || end - start > 10) {
                    c.getPlayer().dropMessage(5, "End number must be greater, and end number must be within a range of 10 from the start number.");
                } else {
                    final MapleMonster job = MapleLifeFactory.getMonster(Integer.parseInt(splitted[1]));
                    if (job == null) {
                        c.getPlayer().dropMessage(5, "Please use @mobdebug to check monsterID properly.");
                    } else {
                        final List<MonsterDropEntry> ranks = MapleMonsterInformationProvider.getInstance().retrieveDrop(job.getId());
                        if (ranks == null || ranks.size() <= 0) {
                            c.getPlayer().dropMessage(5, "No drops was returned.");
                        } else {
                            final int originalEnd = end;
                            int num = 0;
                            MonsterDropEntry de;
                            String name;
                            MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                            for (int i = 0; i < ranks.size(); i++) {
                                if (i >= (start - 1) && i < end) {
                                    de = ranks.get(i);
                                    name = ii.getName(de.itemId);
                                    if (de.chance > 0 && name != null && name.length() > 0 && (de.questid <= 0 || (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0))) {
                                        if (num == 0) {
                                            c.getPlayer().dropMessage(6, "Drops for " + job.getStats().getName() + " - from " + start + " to " + originalEnd);
                                            c.getPlayer().dropMessage(6, "--------------------------------------");
                                        }
                                        c.getPlayer().dropMessage(6, ii.getName(de.itemId) + " (" + de.itemId + "), anywhere from " + de.Minimum + " to " + de.Maximum + " quantity. " + (Integer.valueOf(de.chance == 999999 ? 1000000 : de.chance).doubleValue() / 10000.0) + "% chance. " + (de.questid > 0 && MapleQuest.getInstance(de.questid).getName().length() > 0 ? ("Requires quest " + MapleQuest.getInstance(de.questid).getName() + " to be started.") : ""));
                                        num++;
                                    } else {
                                        end++; //go more. 10 drops plz
                                    }
                                }
                            }
                            if (num == 0) {
                                c.getPlayer().dropMessage(5, "No drops was returned.");
                            }
                        }
                    }
                }
            }
            return 1;
        }
    }
	
	public static class spinel extends OpenNPCCommand {

        public spinel() {
            npc = 5;
        }
    }


    public static class buycoco extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter player = c.getPlayer();
            if (player.getMeso() >= 2100000000) { // 1 B
                if (player.getInventory(MapleInventoryType.ETC).getNumFreeSlot() >= 1) {
                    player.gainMeso(-2100000000, true, true); // item first, then only product LOL
                    MapleInventoryManipulator.addById(c, 4000465, (short) 1, "Bought using @buycoco");
                    player.dropMessage(5, "You've bought a coconut for 2.1 billion mesos.");
                } else {
                    player.dropMessage(5, "Please make some space.");
                }
            } else {
                player.dropMessage(5, "Please make sure that you have 2.1 billion mesos.");
            }
            return 1;
        }
    }

    public static class sellcoco extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter player = c.getPlayer();
            if (player.itemQuantity(4000465) >= 1) {
                if (player.getMeso() <= (Integer.MAX_VALUE - 2100000000)) { // 2.1 B
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000465, 1, true, false); // item first, then only product LOL
                    player.gainMeso(2100000000, true, true);
                    player.dropMessage(5, "You've sold a coconut for 2.1 billion mesos.");
                } else {
                    player.dropMessage(5, "You have too much mesos.");
                }
            } else {
                player.dropMessage(5, "Please make sure that you have a coconut");
            }
            return 1;
        }
    }

    public static class Check extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getCSPoints(1) + " nexon cash");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getPoints() + " points.");
            c.getPlayer().dropMessage(6, "You currently have " + c.getPlayer().getVPoints() + " voting points.");
            return 1;
        }
    }

    public static class Report extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 3) {
                StringBuilder ret = new StringBuilder("Please use @report [ign] [");
                for (ReportType type : ReportType.values()) {
                    ret.append(type.theId).append('/');
                }
                ret.setLength(ret.length() - 1);
                c.getPlayer().dropMessage(6, ret.append(']').toString());
                return 0;
            }

            final MapleCharacter other = c.getPlayer().getMap().getCharacterByName(splitted[1]);
            final ReportType type = ReportType.getByString(splitted[2]);
            if (other == null || type == null || (other.isGM() && !c.getPlayer().isGM()) || other.getName().equals(c.getPlayer().getName())) {
                c.getPlayer().dropMessage(5, "You've entered the wrong character name.");
                return 0;
            }
            final MapleCustomQuestStatus stat = c.getPlayer().getCustomQuest(CustomQuestProvider.getInstance(170004));
            if (stat == null) {
				c.getPlayer().dropMessage(5, "An error has occured. Please try again later");
				return 0;
			}
			if (stat.getCustomData() == null) {
                stat.setCustomData("0");
            }
            long currentTime = System.currentTimeMillis();
            long theTime = Long.parseLong(stat.getCustomData());
            if ((theTime + 7200000 > currentTime) && !c.getPlayer().isGM()) {
                c.getPlayer().dropMessage(5, "You may only report once every 2 hours.");
            } else {
                stat.setCustomData(String.valueOf(currentTime));
                other.addReport(type);
                c.getPlayer().dropMessage(5, "You have successfully reported " + splitted[1] + " for " + splitted[2] + ".");
            }
            return 1;
        }
    }

    public static class CloneMe extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            final short occ = chr.getOccupation();
            if (occ / 100 != 1) {
                chr.dropMessage(6, "Only Ninjas can use this command.");
                return 0;
            }
            // Todo: add checks in event map = cannot use
            final String time = chr.getCustomQuestData(190012);
            final long now = System.currentTimeMillis();
            if (!time.equals("")) { // Contains data
                boolean can = (Long.parseLong(time) + 3600000) < now;
                if (!can) {
                    int remaining = (int) ((((Long.parseLong(time) + 3600000) - now) / 1000) / 60);
                    chr.dropMessage(6, "You've already spawned clones in the past hour. Please try again in " + remaining + " minutes.");
                    return 0;
                }
            }
            final MapleCustomQuestStatus stat = new MapleCustomQuestStatus(CustomQuestProvider.getInstance(190012), (byte) 1); // Always 1, since its in the middle of the quest
            stat.setCustomData(String.valueOf(now));
            chr.updateCustomQuest(stat);
            final byte size = OccupationConstants.getNinjaClones(occ);
            for (byte i = 0; i < size; i++) {
                chr.cloneLook();
            }
            chr.dropMessage(6, "You've managed to summon " + size + " clones.");
            return 1;
        }
    }

    public static class RemoveClones extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (chr.getOccupation() / 100 != 1) {
                chr.dropMessage(6, "Only Ninjas can use this command.");
                return 0;
            }
            chr.dropMessage(6, c.getPlayer().getCloneSize() + " clones disposed.");
            chr.disposeClones();
            return 1;
        }
    }

    public static class Kill extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (chr.getOccupation() / 100 != 5) {
                chr.dropMessage(6, "Only NX Whores can use this command.");
                return 0;
            }
            if (splitted.length < 2) {
                chr.dropMessage(6, "Please use @kill <name>. The player must be in the same map.");
                return 0;
            }
            if (chr.getOccupation() < 510) {
                chr.dropMessage(6, "You do not have the required Occupation Level to use this command.");
                return 0;
            }
            final int delayMins = 360;
            final String time = chr.getCustomQuestData(190016);
            final long now = System.currentTimeMillis();
            if (!time.equals("")) { // Contains data
                boolean can = (Long.parseLong(time) + (delayMins * 60 * 1000)) < now;
                if (!can) {
                    int remaining = (int) ((((Long.parseLong(time) + (delayMins * 60 * 1000)) - now) / 1000) / 60);
                    chr.dropMessage(6, "You've already used this command in the past " + delayMins + " minutes. Please try again in " + remaining + " minutes.");
                    return 0;
                }
            }
            boolean success = MapChecks(c, chr); // check for chr 1st
            if (!success) {
                return 0;
            }
            final String target = splitted[1];
            if (target.equalsIgnoreCase(chr.getName())) {
                chr.dropMessage(6, "You've cannot kill yourself!");
                return 0;
            }
            final MapleCharacter victim = chr.getMap().getCharacterByName(target);
            if (victim == null || victim.isGM() && !chr.isGM()) {
                chr.dropMessage(6, "Unable to find the player. Please make sure that he/she is in the same map.");
                return 0;
            }
            success = MapChecks(c, victim); // check for victim
            if (!success) {
                return 0;
            }
            final MapleCustomQuestStatus stat = new MapleCustomQuestStatus(CustomQuestProvider.getInstance(190016), (byte) 1); // Always 1, since its in the middle of the quest
            stat.setCustomData(String.valueOf(now));
            chr.updateCustomQuest(stat);

            victim.getStat().setHp((short) 0);
            victim.getStat().setMp((short) 0);
            victim.updateSingleStat(MapleStat.HP, 0);
            victim.updateSingleStat(MapleStat.MP, 0);
            victim.dropMessage(6, "You've been killed by " + chr.getName());
            chr.dropMessage(6, "You've successfully killed " + victim.getName());
            return 1;
        }
    }

    public static class Strip extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (chr.getOccupation() / 100 != 5) {
                chr.dropMessage(6, "Only NX Whores can use this command.");
                return 0;
            }
            if (splitted.length < 2) {
                chr.dropMessage(6, "Please use @strip <name>. The player must be in the same map.");
                return 0;
            }
            if (chr.getOccupation() < 508) {
                chr.dropMessage(6, "You do not have the required Occupation Level to use this command.");
                return 0;
            }
            final int delayMins = (chr.getOccupation() == 508 ? 180 : 150);
            final String time = chr.getCustomQuestData(190015);
            final long now = System.currentTimeMillis();
            if (!time.equals("")) { // Contains data
                boolean can = (Long.parseLong(time) + (delayMins * 60 * 1000)) < now;
                if (!can) {
                    int remaining = (int) ((((Long.parseLong(time) + (delayMins * 60 * 1000)) - now) / 1000) / 60);
                    chr.dropMessage(6, "You've already used this command in the past " + delayMins + " minutes. Please try again in " + remaining + " minutes.");
                    return 0;
                }
            }
            boolean success = MapChecks(c, chr); // check for chr 1st
            if (!success) {
                return 0;
            }
            final String target = splitted[1];
            if (target.equalsIgnoreCase(chr.getName())) {
                chr.dropMessage(6, "You've cannot strip yourself!");
                return 0;
            }
            final MapleCharacter victim = chr.getMap().getCharacterByName(target);
            if (victim == null || victim.isGM() && !chr.isGM()) {
                chr.dropMessage(6, "Unable to find the player. Please make sure that he/she is in the same map.");
                return 0;
            }
            success = MapChecks(c, victim); // check for victim
            if (!success) {
                return 0;
            }
            final MapleCustomQuestStatus stat = new MapleCustomQuestStatus(CustomQuestProvider.getInstance(190015), (byte) 1); // Always 1, since its in the middle of the quest
            stat.setCustomData(String.valueOf(now));
            chr.updateCustomQuest(stat);

            MapleInventory equipped = victim.getInventory(MapleInventoryType.EQUIPPED);
            MapleInventory equip = victim.getInventory(MapleInventoryType.EQUIP);
            List<Short> ids = new LinkedList<>();
            for (IItem item : equipped.list()) {
                ids.add(item.getPosition());
            }
            for (short id : ids) {
                MapleInventoryManipulator.unequip(victim.getClient(), id, equip.getNextFreeSlot());
            }
            victim.dropMessage(-3, "I've been stripped!", true);
            victim.dropMessage(6, "You've been stripped by " + chr.getName());
            chr.dropMessage(6, "You've successfully strip " + victim.getName());
            return 1;
        }
    }

    public static class Heal extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (chr.getOccupation() / 100 != 5) {
                chr.dropMessage(6, "Only NX Whores can use this command.");
                return 0;
            }
            if (splitted.length < 2) {
                chr.dropMessage(6, "Please use @heal <name>. The player must be in the same map.");
                return 0;
            }
            if (chr.getOccupation() < 502) {
                chr.dropMessage(6, "You do not have the required Occupation Level to use this command.");
                return 0;
            }
            final int delayMins = (chr.getOccupation() == 502 ? 15 : (chr.getOccupation() == 503 ? 10 : (chr.getOccupation() == 504 ? 5 : 1)));
            final String time = chr.getCustomQuestData(190014);
            final long now = System.currentTimeMillis();
            if (!time.equals("")) { // Contains data
                boolean can = (Long.parseLong(time) + (delayMins * 60 * 1000)) < now;
                if (!can) {
                    int remaining = (int) ((((Long.parseLong(time) + (delayMins * 60 * 1000)) - now) / 1000) / 60);
                    chr.dropMessage(6, "You've already used this command in the past " + delayMins + " minutes. Please try again in " + remaining + " minutes.");
                    return 0;
                }
            }
            boolean success = MapChecks(c, chr); // check for chr 1st
            if (!success) {
                return 0;
            }
            final String target = splitted[1];
            if (target.equalsIgnoreCase(chr.getName())) {
                final MapleCustomQuestStatus stat = new MapleCustomQuestStatus(CustomQuestProvider.getInstance(190014), (byte) 1); // Always 1, since its in the middle of the quest
                stat.setCustomData(String.valueOf(now));
                chr.updateCustomQuest(stat);
                chr.getStat().setHp(chr.getStat().getCurrentMaxHp());
                chr.getStat().setMp(chr.getStat().getCurrentMaxMp());
                chr.updateSingleStat(MapleStat.HP, chr.getStat().getCurrentMaxHp());
                chr.updateSingleStat(MapleStat.MP, chr.getStat().getCurrentMaxMp());
                chr.dropMessage(6, "You've successfully healed yourself!");
                return 1;
            }
            final MapleCharacter victim = chr.getMap().getCharacterByName(target);
            if (victim == null || victim.isGM() && !chr.isGM()) {
                chr.dropMessage(6, "Unable to find the player. Please make sure that he/she is in the same map.");
                return 0;
            }
            success = MapChecks(c, victim); // check for victim
            if (!success) {
                return 0;
            }
            final MapleCustomQuestStatus stat = new MapleCustomQuestStatus(CustomQuestProvider.getInstance(190014), (byte) 1); // Always 1, since its in the middle of the quest
            stat.setCustomData(String.valueOf(now));
            chr.updateCustomQuest(stat);

            victim.getStat().setHp(victim.getStat().getCurrentMaxHp());
            victim.getStat().setMp(victim.getStat().getCurrentMaxMp());
            victim.updateSingleStat(MapleStat.HP, victim.getStat().getCurrentMaxHp());
            victim.updateSingleStat(MapleStat.MP, victim.getStat().getCurrentMaxMp());
            victim.dropMessage(6, "You've been healed by " + chr.getName());
            chr.dropMessage(6, "You've successfully healed " + victim.getName());
            return 1;
        }
    }

    public static class Bomb extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (chr.getOccupation() / 100 != 5) {
                chr.dropMessage(6, "Only NX Whores can use this command.");
                return 0;
            }
            if (chr.getOccupation() < 506) {
                chr.dropMessage(6, "You do not have the required Occupation Level to use this command.");
                return 0;
            }
            boolean success = MapChecks(c, chr); // check for chr 1st
            if (!success) {
                return 0;
            }
            final int delayMins = (chr.getOccupation() == 506 ? 20 : 10);
            final String time = chr.getCustomQuestData(190013);
            final long now = System.currentTimeMillis();
            if (!time.equals("")) { // Contains data
                boolean can = (Long.parseLong(time) + (delayMins * 60 * 1000)) < now;
                if (!can) {
                    int remaining = (int) ((((Long.parseLong(time) + (delayMins * 60 * 1000)) - now) / 1000) / 60);
                    chr.dropMessage(6, "You've already used this command in the past " + delayMins + " minutes. Please try again in " + remaining + " minutes.");
                    return 0;
                }
            }
            final MapleCustomQuestStatus stat = new MapleCustomQuestStatus(CustomQuestProvider.getInstance(190013), (byte) 1); // Always 1, since its in the middle of the quest
            stat.setCustomData(String.valueOf(now));
            chr.updateCustomQuest(stat);
            chr.getMap().spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9300166), chr.getPosition());
            chr.dropMessage(-3, "I've spawned a bomb!", true);
            return 1;
        }
    }

    public static class Player extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter chr = c.getPlayer();
            if (chr.getOccupation() / 100 != 5) {
                chr.dropMessage(6, "Only NX Whores can use this command.");
                return 0;
            }
            if (splitted.length < 2) {
                chr.dropMessage(6, "Please use @player <name>. The player must be in the same channel.");
                return 0;
            }
            final String victimName = splitted[1];
            if (victimName.equalsIgnoreCase(chr.getName())) {
                chr.dropMessage(6, "You cannot use this command on yourself.");
                return 0;
            }
            MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(victimName);
            if (victim == null || victim.isGM() && !chr.isGM()) {
                chr.dropMessage(6, "The player could not be found in the current channel.");
                return 0;
            }
            if (victim.getMapId() == chr.getMapId()) {
                chr.dropMessage(6, "The player is in the same map as you.");
                return 0;
            }
            boolean success = MapChecks(c, chr); // check for chr 1st
            if (!success) {
                return 0;
            }
            success = MapChecks(c, victim); // check for victim
            if (!success) {
                return 0;
            }
            chr.dropMessage(6, "Changing map...Please hold on a moment...");
            chr.changeMap(victim.getMap(), victim.getMap().findClosestSpawnpoint(victim.getPosition()));
            return 1;
        }
    }

    public static boolean MapChecks(final MapleClient c, final MapleCharacter other) {
        for (int i : GameConstants.blockedMaps) {
            if (other.getMapId() == i) {
                c.getPlayer().dropMessage(5, "You may not use this command here.");
                return false;
            }
        }
        if (other.getLevel() < 10) {
            c.getPlayer().dropMessage(5, "You must be over level 10 to use this command.");
            return false;
        }
        if (other.getMap().getSquadByMap() != null || other.getEventInstance() != null || other.getMap().getEMByMap() != null || MapConstants.isStorylineMap(other.getMapId())) {
            c.getPlayer().dropMessage(5, "You may not use this command here.");
            return false;
        }
        if ((other.getMapId() >= 680000210 && other.getMapId() <= 680000502) || (other.getMapId() / 1000 == 980000 && other.getMapId() != 980000000) || (other.getMapId() / 100 == 1030008) || (other.getMapId() / 100 == 922010) || (other.getMapId() / 10 == 13003000) || (other.getMapId() >= 990000000)) {
            c.getPlayer().dropMessage(5, "You may not use this command here.");
            return false;
        }
        return true;
    }

    public static class rbhelper extends CommandExecute {
        protected int jobid;
        @Override
        public int execute(MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            if (!player.isAlive() || player.getConversation() != 0) {
                player.dropMessage(5, "You may not use this command here.");
                return 0;
            }
            if (player.getLevel() >= 200) {
                player.doRB(jobid);
                player.dcolormsg(5, "You now have "+player.getReborns()+" reborns!");
                //NPCScriptManager.getInstance().start(c, 9250125, "reborn"); // old reborning
            } else {
                player.dropMessage(5, "Please train harder and achieve level 200.");
                return 0;
            }
            return 1;
        }
    }
    
    public static class reborn extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter player = c.getPlayer();
           player.dcolormsg(6, "Please do one of the following:");
           player.dcolormsg(6, "@reborne - Reborn into an Explorer");
           player.dcolormsg(6, "@rebornc - Reborn into a Cygnus");
           player.dcolormsg(6, "@reborna - Reborn into an Aran");
           player.dcolormsg(6, "@rebornev - Reborrn into an Evan");
           return 0;
        }
    }

    public static class reborne extends rbhelper {
        public reborne() {
            jobid = 0;
        }
    }
    
    public static class rebornc extends rbhelper {
        public rebornc() {
            jobid = 1000;
        }
    }
    
    public static class reborna extends rbhelper {
        public reborna() {
            jobid = 2000;
        }
    }
    
    public class rebornev extends rbhelper {
        public rebornev() {
            jobid = 2200;
        }
    }
    
    public class reborndb extends rbhelper {
        public reborndb() {
            jobid = 2200;
        }
    }
    
    public static class tradehelp extends TradeExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dropMessage(-2, "Command : <@offerequip, @offeruse, @offersetup, @offeretc, @offercash> <name of the item>");
            return 1;
        }
    }

    public abstract static class OfferCommand extends TradeExecute {

        protected int invType = -1;

        @Override
        public int execute(MapleClient c, String[] splitted) {
            if (splitted.length < 2) {
                c.getPlayer().dropMessage(-2, "Error : <name of item>");
            } else {
                //int quantity = 1;
                //try {
                //    quantity = Integer.parseInt(splitted[1]);
                //} catch (Exception e) { //swallow and just use 1
                //}
                String search = StringUtil.joinStringFrom(splitted, 1).toLowerCase();
                IItem found = null;
                final MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
                for (IItem inv : c.getPlayer().getInventory(MapleInventoryType.getByType((byte) invType))) {
                    if (ii.getName(inv.getItemId()) != null && ii.getName(inv.getItemId()).toLowerCase().contains(search)) {
                        found = inv;
                        break;
                    }
                }
                if (found == null) {
                    c.getPlayer().dropMessage(-2, "Error : No such item was found (" + search + ")");
                    return 0;
                }
                if (GameConstants.isPet(found.getItemId())) {
                    c.getPlayer().dropMessage(-2, "Error : You may not trade a pet");
                    return 0;
                }
                if (1 > found.getQuantity()) {
                    c.getPlayer().dropMessage(-2, "Error : Item cannot be traded");
                    return 0;
                }
                if (!c.getPlayer().getTrade().setItems(c, found, (byte) -1, 1)) {
                    c.getPlayer().dropMessage(-2, "Error : This item cannot be placed");
                    return 0;
                }
            }
            return 1;
        }
    }

    public static class OfferEquip extends OfferCommand {

        public OfferEquip() {
            invType = 1;
        }
    }

    public static class OfferUse extends OfferCommand {

        public OfferUse() {
            invType = 2;
        }
    }

    public static class OfferSetup extends OfferCommand {

        public OfferSetup() {
            invType = 3;
        }
    }

    public static class OfferEtc extends OfferCommand {

        public OfferEtc() {
            invType = 4;
        }
    }

    public static class OfferCash extends OfferCommand {

        public OfferCash() {
            invType = 5;
        }
    }
    
    public static class butcher extends CommandExecute {
      public int execute(MapleClient c, String[] splitted) {
         final MapleCharacter victim = c.getPlayer().getMap().getCharacterByName(splitted[2]);
            if (victim == null) {
                c.getPlayer().dropMessage(6,"This player is offline!");
            } else if (!victim.isAlive()) {
                c.getPlayer().dropMessage(6,"The player is already dead!");
            } else {
                if(c.getPlayer().haveItem(4000465, 25)) {
                    MapleInventoryManipulator.removeById(c, MapleInventoryType.ETC, 4000465, 25, false, false);
                    c.getPlayer().dropMessage(-1, "The player has been killed.");
                } else {
                    c.getPlayer().dcolormsg(5, "You do not have enough coconuts.");
                    victim.dcolormsg(5, "The faggot "+c.getPlayer()+" has tried to use @butcher on you but did not have enough coconuts. Shame on him! You should take your vengeance. ");
                }
            }
         return 1;
        }
    }
        
   public static class Info extends OpenNPCCommand {
        public Info() {
            npc = 3;
        }
    }
  
   public static class DropNx extends OpenNPCCommand {
        public DropNx() {
            npc = 4;
        }
    }
   
   public static class style extends OpenNPCCommand {
        public style() {
            npc = 6;
        }
    }
   
   public static class ap extends CommandExecute {
       @Override
        public int execute(MapleClient c, String[] splitted) {
            c.getPlayer().dcolormsg(4, "You currently have "+c.getPlayer().getRemainingAp()+" remaining AP");//String.valueOf(chr.getRemainingAp()));
            return 0;
        }
    }
   
   public static class online extends CommandExecute {

        @Override
        public int execute(MapleClient c, String[] splitted) {       
            c.getPlayer().dropMessage(6, "Total amount of players connected to TropikMS:");
            c.getPlayer().dropMessage(6, ""+World.getConnected()+"");
            c.getPlayer().dropMessage(6, "Characters connected to channel " + c.getChannel() + ":");
            c.getPlayer().dropMessage(6, c.getChannelServer().getPlayerStorage().getOnlinePlayers(true));
            return 0;
        }
    }
   
       public static class myshit extends CommandExecute {
        public int execute(final MapleClient c, String[] splitted) {
            MapleCharacter player = c.getPlayer();
            player.dropMessage(6, "You currently have:");
            player.dropMessage(6, "Vote Points: " + player.getVotePoints());
            player.dropMessage(6, "Reborns: " + player.getReborns());
            player.dropMessage(6, "Agent Points: " + player.getAgentPoint());
            player.dropMessage(6, "Coconuts: " + player.getItemQuantity(4000465, true));
            player.dropMessage(6, "Donator Points: " + player.getDP());
            return 0;
        }
    }
       public static class hisshit extends CommandExecute {
            public int execute(final MapleClient c, String[] splitted) {
                MapleCharacter victim = c.getChannelServer().getPlayerStorage().getCharacterByName(splitted[1]);
                MapleCharacter player = c.getPlayer();
                player.dropMessage(6, "You currently have:");
                player.dropMessage(6, "Vote Points: " + victim.getVotePoints());
                player.dropMessage(6, "Reborns: " + victim.getReborns());
                player.dropMessage(6, "Agent Points: " + victim.getAgentPoint());
                player.dropMessage(6, "Coconuts: " + victim.getItemQuantity(4000465, true));
                player.dropMessage(6, "Donator Points: " + victim.getDP());
                victim.dropMessage(6, " " + player.getName() + " has looked up your shit with @hisshit! ");
                return 0;
            }
      }
              
       public static class go extends CommandExecute {
           @Override
           public int execute(MapleClient c, String[] splitted) {
               MapleCharacter chr = c.getPlayer();
            HashMap<String, Integer> maps = new HashMap<String, Integer>();
                    maps.put("henesys", 100000000);
                    maps.put("ellinia", 101000000);
                    maps.put("perion", 102000000);
                    maps.put("kerning", 103000000);
                    maps.put("lith", 104000000);
                    maps.put("sleepywood", 105040300);
                    maps.put("florina", 110000000);
                    maps.put("orbis", 200000000);
                    maps.put("happy", 209000000);
                    maps.put("elnath", 211000000);
                    maps.put("ereve", 130000000);
                    maps.put("ludi", 220000000);
                    maps.put("omega", 221000000);
                    maps.put("korean", 222000000);
                    maps.put("aqua", 230000000);
                    maps.put("leafre", 240000000);
                    maps.put("mulung", 250000000);
                    maps.put("herb", 251000000);
                    maps.put("nlc", 600000000);
                    maps.put("shrine", 800000000);
                    maps.put("showa", 801000000);
                    maps.put("fm", 910000000);
                    maps.put("guild", 200000301);
                    maps.put("fog", 105040306);
            if (splitted.length != 2) {
                StringBuilder builder = new StringBuilder("Syntax: @goto <mapname>");
                int i = 0;
                for (String mapss : maps.keySet()) {
                    if (1 % 10 == 0) {// 10 maps per line
                        chr.dropMessage(5, builder.toString());
                    } else {
                        builder.append(mapss + ", ");
                    }
                }
                chr.dropMessage(5, builder.toString());
            } else if (maps.containsKey(splitted[1])) {
                int map = maps.get(splitted[1]);
                chr.changeMap(map);
            } else {
                        chr.dropMessage(5, "________________________________________________________________________");
                        chr.dropMessage(5, "                ..::| TropikMS Goto Map list |::..                 ");
                        chr.dropMessage(5, "");
                        chr.dropMessage(5, "| henesys | ellinia | perion | kerning | lith   | sleepywood | florina |");
                        chr.dropMessage(5, "| fog     | orbis   | happy  | elnath  | ereve  | ludi       | omega   |");
                        chr.dropMessage(5, "| korean  | aqua    | leafre | mulung  | herb   | nlc        | shrine  |");
                        chr.dropMessage(5, "| shower  | fm      | guild  |");
                }
            maps.clear();
               return 0;
           }
       }
    public static class Commands extends CommandExecute {
        @Override
        public int execute(MapleClient c, String[] splitted) {
            final MapleCharacter player = c.getPlayer();
                if (splitted.length < 2) {
                        player.dcolormsg(5, "Please, use one of the following syntaxes");
                        player.dcolormsg(5, "@commands general");
                        player.dcolormsg(5, "@commands travel");
                        player.dcolormsg(5, "@commands fun");
                        player.dcolormsg(5, "@commands essential");
                    return 0;
                }
                final String type = splitted[1];
                    if (type.equalsIgnoreCase("general")) {
			player.dcolormsg(5, "@str / dex / int / luk <val> : add ap to your stats");
			player.dcolormsg(5, "@clearslot <slot to clear> : clear your inventory");
			player.dcolormsg(5, "@dropnx : open the npc to drop nx items");
			player.dcolormsg(5, "@toggle : turn of smegas");
			player.dcolormsg(5, "@ranking <job> <range1> <range2> : show the ranking");
			player.dcolormsg(5, "@report <player> <offense> : report a player");
                        player.dcolormsg(5, "@online : see the online players");
                } else if (type.equalsIgnoreCase("travel")) {
                        player.dcolormsg(5, "@henesys : warp to henesys");
                        player.dcolormsg(5, "@home : warp to henesys");
                        player.dcolormsg(5, "@fm : warp to free market");
                        player.dcolormsg(5, "@go <warppoint> : warp to a name. do @go without an arg to see the map options");
                        player.dcolormsg(5, "@spinel : open up spinel npc warper");
                } else if (type.equalsIgnoreCase("fun")) {
                        player.dcolormsg(5, "@butcher <playername> : kill the player for the cost of 25 coconuts");                   
                        player.dcolormsg(5, "@style : open up the stylist npc");               
                } else if (type.equalsIgnoreCase("essential")) {
                        player.dcolormsg(5, "@buycoco : exchange 2.1b mesos for a coconut");
                        player.dcolormsg(5, "@sellcoco : exchange a coconuts for 2.1b mesos");
                        player.dcolormsg(5, "@myshit : look up your stats");
                        player.dcolormsg(5, "@hisshit <name> : look up someone's stats");
                        player.dcolormsg(5, "@reborn : reborn to lvl 1 once you are lvl 200");
                        player.dcolormsg(5, "@ap : show you your available AP");
                } else {
                        player.dcolormsg(5, "Please, use one of the following syntaxes");
                        player.dcolormsg(5, "@commands general");
                        player.dcolormsg(5, "@commands travel");
                        player.dcolormsg(5, "@commands fun");
                        player.dcolormsg(5, "@commands essential");
                return 0;
            }
            return 1;
        }
    }
       
       public static class help extends Commands {
           help() {
               
           }
       }
}