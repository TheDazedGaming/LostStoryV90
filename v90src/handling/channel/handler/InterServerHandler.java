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
package handling.channel.handler;

import java.util.List;

import handling.world.buddy.BuddyListEntry;
import client.MapleCharacter;
import client.MapleClient;
import client.MapleCustomQuestStatus;
import client.MapleQuestStatus;
import client.SkillFactory;
import constants.MapConstants;
import constants.ServerConstants;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.channel.handler.PartyHandler.PartyOperation;
import handling.world.CharacterTransfer;
import handling.world.MapleMessenger;
import handling.world.MapleMessengerCharacter;
import handling.world.CharacterIdChannelPair;
import handling.world.party.MapleParty;
import handling.world.party.MaplePartyCharacter;
import handling.world.PlayerBuffStorage;
import handling.world.World;
import handling.world.expedition.MapleExpedition;
import handling.world.guild.MapleGuild;
import java.util.ArrayList;
import server.maps.FieldLimitType;
import server.quest.custom.CustomQuestProvider;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.FamilyPacket;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.MapleUserPackets;

public class InterServerHandler {

    public static final void EnterCS(final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.getEventInstance() != null || c.getChannelServer() == null || MapConstants.isStorylineMap(chr.getMapId())) {
            c.getSession().write(MaplePacketCreator.serverBlocked(2));
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        //if (c.getChannel() == 1 && !c.getPlayer().isGM()) {
        //    c.getPlayer().dropMessage(5, "You may not enter on this channel. Please change channels and try again.");
        //    c.getSession().write(MaplePacketCreator.enableActions());
        //    return;
        //}
        final ChannelServer ch = ChannelServer.getInstance(c.getChannel());

        chr.changeRemoval();

        if (chr.getMessenger() != null) {
            MapleMessengerCharacter messengerplayer = new MapleMessengerCharacter(chr);
            World.Messenger.leaveMessenger(chr.getMessenger().getId(), messengerplayer);
        }
        PlayerBuffStorage.addBuffsToStorage(chr.getId(), chr.getAllBuffs());
        PlayerBuffStorage.addCooldownsToStorage(chr.getId(), chr.getCooldowns());
        PlayerBuffStorage.addDiseaseToStorage(chr.getId(), chr.getAllDiseases());
        World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), -10);
        ch.removePlayer(chr);
        c.updateLoginState(MapleClient.CHANGE_CHANNEL, c.getSessionIPAddress());

        c.getSession().write(MaplePacketCreator.getChannelChange(Integer.parseInt(CashShopServer.getIP().split(":")[1])));
        chr.saveToDB(false, false);
        chr.getMap().removePlayer(chr);
        c.setPlayer(null);
        c.setReceiving(false);
    }

    public static final void Loggedin(final int playerid, final MapleClient c) {
        final ChannelServer channelServer = c.getChannelServer();
        MapleCharacter player;
        final CharacterTransfer transfer = channelServer.getPlayerStorage().getPendingCharacter(playerid);

        if (transfer == null) { // Player isn't in storage, probably isn't CC
            player = MapleCharacter.loadCharFromDB(playerid, c, true);
            player.setLoginTime(System.currentTimeMillis());
        } else {
            player = MapleCharacter.ReconstructChr(transfer, c, true);
        }
        c.setPlayer(player);
        c.setAccID(player.getAccountID());

        if (!c.CheckIPAddress()) { // Remote hack
            c.getSession().close();
            return;
        }

        final int state = c.getLoginState();
        boolean allowLogin = false;

        if (state == MapleClient.LOGIN_SERVER_TRANSITION || state == MapleClient.CHANGE_CHANNEL) {
            if (!World.isCharacterListConnected(c.loadCharacterNames(c.getWorld()))) {
                allowLogin = true;
            }
        }
        if (!allowLogin) {
            c.setPlayer(null);
            c.getSession().close();
            return;
        }
        c.updateLoginState(MapleClient.LOGIN_LOGGEDIN, c.getSessionIPAddress());
        channelServer.addPlayer(player);

        c.getSession().write(MaplePacketCreator.getCharInfo(player));
        if (player.isGM()) {
            SkillFactory.getSkill(9101004).getEffect(1).applyTo(player);
        }
        player.sendSkills();
        c.getSession().write(MaplePacketCreator.temporaryStats_Reset());
        player.getMap().addPlayer(player);

        try {
            player.silentGiveBuffs(PlayerBuffStorage.getBuffsFromStorage(player.getId()));
            player.giveCoolDowns(PlayerBuffStorage.getCooldownsFromStorage(player.getId()));
            player.giveSilentDebuff(PlayerBuffStorage.getDiseaseFromStorage(player.getId()));

            // Start of buddylist
            final int buddyIds[] = player.getBuddylist().getBuddyIds();
            World.Buddy.loggedOn(player.getName(), player.getId(), c.getChannel(), buddyIds, player.getGMLevel(), player.isHidden());
            if (player.getParty() != null) {
                player.receivePartyMemberHP();
                player.updatePartyMemberHP();

                final MapleParty party = player.getParty();
                World.Party.updateParty(party.getId(), PartyOperation.LOG_ONOFF, new MaplePartyCharacter(player));
                if (party != null && party.getExpeditionId() > 0) {
                    MapleExpedition me = World.Party.getExped(party.getExpeditionId());
                    if (me != null) {
                        c.getSession().write(MapleUserPackets.showExpedition(me, false, true));
                    }
                }
            }
            final CharacterIdChannelPair[] onlineBuddies = World.Find.multiBuddyFind(player.getId(), buddyIds);
            for (CharacterIdChannelPair onlineBuddy : onlineBuddies) {
                final BuddyListEntry ble = player.getBuddylist().get(onlineBuddy.getCharacterId());
                ble.setChannel(onlineBuddy.getChannel());
                player.getBuddylist().put(ble);
            }
            c.getSession().write(MaplePacketCreator.updateBuddylist(BuddyListHandler.UPDATE, player.getBuddylist().getBuddies()));

            // Start of Messenger
            final MapleMessenger messenger = player.getMessenger();
            if (messenger != null) {
                World.Messenger.silentJoinMessenger(messenger.getId(), new MapleMessengerCharacter(c.getPlayer()));
                World.Messenger.updateMessenger(messenger.getId(), c.getPlayer().getName(), c.getChannel());
            }

            // Start of Guild and alliance
            if (player.getGuildId() > 0) {
                World.Guild.setGuildMemberOnline(player.getMGC(), true, c.getChannel());
                c.getSession().write(MaplePacketCreator.showGuildInfo(player));
                final MapleGuild gs = World.Guild.getGuild(player.getGuildId());
                if (gs != null) {
                    final List<byte[]> packetList = World.Alliance.getAllianceInfo(gs.getAllianceId(), true);
                    if (packetList != null) {
                        for (byte[] pack : packetList) {
                            if (pack != null) {
                                c.getSession().write(pack);
                            }
                        }
                    }
                } else { //guild not found, change guild id
                    player.setGuildId(0);
                    player.setGuildRank((byte) 5);
                    player.setAllianceRank((byte) 5);
                    player.saveGuildStatus();
                }
            }

            if (player.getFamilyId() > 0) {
                World.Family.setFamilyMemberOnline(player.getMFC(), true, c.getChannel(), player.getLoginTime());
            }
            c.getSession().write(FamilyPacket.getFamilyInfo(player));
        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.Login_Error, e);
        }
        c.getSession().write(FamilyPacket.getFamilyData());
        player.sendMacros();
        player.showNote();
        player.updatePartyMemberHP();
        if (transfer == null) { // Login
            player.startFairySchedule();
        }
        //player.send
        //c.getSession().write(MaplePacketCreator.getKeymap(player.getKeyLayout()));
        final String data = player.getCustomQuestData(170000);
        if (data.equals("")) { //this is needed here temporary, for old characters
            final MapleCustomQuestStatus stat = new MapleCustomQuestStatus(CustomQuestProvider.getInstance(170000), (byte) 1);
            stat.setCustomData("42,82,71,73,29,83,79,81");
            c.getPlayer().updateCustomQuest(stat);
        }
        c.getSession().write(MaplePacketCreator.getQuickSlot(player.getCustomQuestData(170000)));

        for (MapleQuestStatus status : player.getStartedQuests()) {
            if (status.hasMobKills()) {
                c.getSession().write(MaplePacketCreator.updateQuestMobKills(status));
            }
        }
        player.expirationTask(true, transfer == null);
        if (player.getJob() == 132) { // DARKKNIGHT
            player.checkBerserk();
        }
        player.spawnClones();
        player.spawnSavedPets();
        player.updatePetAuto();
        if (ServerConstants.SHOP_DISCOUNT) {
            c.getSession().write(MaplePacketCreator.enableShopDiscount((byte) ServerConstants.SHOP_DISCOUNT_PERCENT));
        }
        //if (ServerConstants.boomUpEvent()) {
        //    player.dropMessage(-3, "The 2011 Winter Boom Up event is in progress. (Hunting Bonus +" + GameConstants.getBoomUpExp(player) + "%)");
        //}
        final List<Integer> ii = new ArrayList<>();
        ii.add(9250130);
        ii.add(9250131);
        ii.add(1013103);
        ii.add(2006);
        ii.add(2082005);
        ii.add(9010014);
        ii.add(2060103);
        ii.add(1052119);
        ii.add(9270064);
        ii.add(1013101);
        ii.add(1300005);
        ii.add(2082010);
        ii.add(9201137);
        ii.add(9201136);
        ii.add(1102006);
        ii.add(9010012);
        player.getClient().getSession().write(MaplePacketCreator.setNPCScriptable(ii));
    }

    public static final void ChangeChannel(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        if (!chr.isAlive() || chr.getEventInstance() != null || chr.getMap() == null || FieldLimitType.ChannelSwitch.check(chr.getMap().getFieldLimit())) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        chr.changeChannel(slea.readByte() + 1);
    }
}
