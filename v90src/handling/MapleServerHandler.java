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
package handling;

import constants.ServerConstants;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import client.MapleClient;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.cashshop.handler.*;
import handling.channel.handler.*;
import handling.login.LoginServer;
import handling.login.handler.*;
import handling.mina.MaplePacketDecoder;
import server.Randomizer;
import tools.MapleAESOFB;
import tools.packet.LoginPacket;
import tools.data.input.ByteArrayByteStream;
import tools.data.input.GenericSeekableLittleEndianAccessor;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.Pair;

import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IdleStatus;
import org.apache.mina.common.IoSession;
import tools.FileoutputUtil;
import tools.HexTool;
import tools.MaplePacketCreator;
import tools.packet.MTSCSPacket;

public class MapleServerHandler extends IoHandlerAdapter {

    public static final boolean Log_Packets = true;
    private int channel = -1;
    private boolean cs;
    private boolean login;
    private final List<String> BlockedIP = new ArrayList<String>();
    private final Map<String, Pair<Long, Byte>> tracker = new ConcurrentHashMap<String, Pair<Long, Byte>>();

    public MapleServerHandler(final int channel, final boolean cs) {
        this.channel = channel;
        this.cs = cs;
    }

    @Override
    public void messageSent(final IoSession session, final Object message) throws Exception {
            //System.out.println(message);
		super.messageSent(session, message);
    }

    @Override
    public void exceptionCaught(final IoSession session, final Throwable cause) throws Exception {
        /*	MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
        log.error(MapleClient.getLogMessage(client, cause.getMessage()), cause);*/
//	cause.printStackTrace();
    }

    @Override
    public void sessionOpened(final IoSession session) throws Exception {
        // Start of IP checking
        final String address = session.getRemoteAddress().toString().split(":")[0];

        if (BlockedIP.contains(address)) {
            session.close();
            return;
        }
        final Pair<Long, Byte> track = tracker.get(address);

        byte count;
        if (track == null) {
            count = 1;
        } else {
            count = track.right;

            final long difference = System.currentTimeMillis() - track.left;
            if (difference < 2000) { // Less than 2 sec
                count++;
            } else if (difference > 20000) { // Over 20 sec
                count = 1;
            }
            if (count >= 10) {
                BlockedIP.add(address);
                tracker.remove(address); // Cleanup
                session.close();
                return;
            }
        }
        tracker.put(address, new Pair<Long, Byte>(System.currentTimeMillis(), count));
        // End of IP checking.

        if (channel > -1) {
            if (ChannelServer.getInstance(channel).isShutdown()) {
                session.close();
                return;
            }
        } else if (cs) {
            if (CashShopServer.isShutdown()) {
                session.close();
                return;
            }
        } else {
            if (LoginServer.isShutdown()) {
                session.close();
                return;
            }
        }
        final byte serverRecv[] = new byte[]{70, 114, 122, (byte) Randomizer.nextInt(255)};
        final byte serverSend[] = new byte[]{82, 48, 120, (byte) Randomizer.nextInt(255)};
        final byte ivRecv[] = ServerConstants.Use_Fixed_IV ? new byte[]{9, 0, 0x5, 0x5F} : serverRecv;
        final byte ivSend[] = ServerConstants.Use_Fixed_IV ? new byte[]{1, 0x5F, 4, 0x3F} : serverSend;
        byte key[] = {0x13, 0x00, 0x00, 0x00, 0x08, 0x00, 0x00, 0x00, 0x06, 0x00, 0x00, 0x00, (byte) 0xB4, 0x00, 0x00, 0x00, 0x1B, 0x00, 0x00, 0x00, 0x0F, 0x00, 0x00, 0x00, 0x33, 0x00, 0x00, 0x00, 0x52, 0x00, 0x00, 0x00};

        final MapleClient client = new MapleClient(
                new MapleAESOFB(ivSend, (short) (0xFFFF - ServerConstants.MAPLE_VERSION)), // Sent Cypher
                new MapleAESOFB(ivRecv, ServerConstants.MAPLE_VERSION), // Recv Cypher
                session);
        client.setChannel(channel);

        MaplePacketDecoder.DecoderState decoderState = new MaplePacketDecoder.DecoderState();
        session.setAttribute(MaplePacketDecoder.DECODER_STATE_KEY, decoderState);


        session.write(LoginPacket.getHello(ServerConstants.MAPLE_VERSION,
                ServerConstants.Use_Fixed_IV ? serverSend : ivSend, ServerConstants.Use_Fixed_IV ? serverRecv : ivRecv));
        session.setAttribute(MapleClient.CLIENT_KEY, client);
        session.setIdleTime(IdleStatus.READER_IDLE, 60);
        session.setIdleTime(IdleStatus.WRITER_IDLE, 60);

        StringBuilder sb = new StringBuilder();
        if (channel > -1) {
            sb.append("[Channel Server] Channel ").append(channel).append(" : ");
        } else if (cs) {
            sb.append("[Cash Server]");
        } else if (login) {
            sb.append("[Login Server]");
        } else {
            return;
        }
        sb.append("IoSession opened ").append(address);
        System.out.println(sb.toString());
    }

    @Override
    public void sessionClosed(final IoSession session) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);

        if (client != null) {
            try {
                client.disconnect(true, cs);
            } finally {
                session.close();
                session.removeAttribute(MapleClient.CLIENT_KEY);
            }
        }
        super.sessionClosed(session);
    }

    @Override
    public void messageReceived(final IoSession session, final Object message) {
        try {
            final SeekableLittleEndianAccessor slea = new GenericSeekableLittleEndianAccessor(new ByteArrayByteStream((byte[]) message));
            if (slea.available() < 2) {
                return;
            }
            final short header_num = slea.readShort();
            
            //System.out.println(message);
            
            // special handling to get the packet recieved by someone
            /*
            final StringBuilder sb = new StringBuilder("Received data : ");
				sb.append(HexTool.toString((byte[]) message)).append("\n").append(HexTool.toStringFromAscii((byte[]) message));
				System.out.println(sb.toString());*/

            for (final RecvPacketOpcode recv : RecvPacketOpcode.values()) {
                if (recv.getValue() == header_num) {
                    final MapleClient c = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);
                    if (!c.isReceiving()) {
                        return;
                    }
                    if (recv.NeedsChecking()) {
                        if (!c.isLoggedIn()) {
                            return;
                        }
                    }
                    handlePacket(recv, slea, c, cs);
                    return;
                }
            }

            if (slea.available() == 0) { // we don't want to log headers only
                return;
            }

            // not handled or unknown packets
           // final StringBuilder sb = new StringBuilder("Received data : ");
            final StringBuilder sb = new StringBuilder("Received data : ");
            sb.append(HexTool.toString((byte[]) message)).append("\n").append(HexTool.toStringFromAscii((byte[]) message));
            if (!ServerConstants.VPS && !ServerConstants.HAMACHI) {
                System.out.println(sb.toString());
            } else {
                FileoutputUtil.logPacket(String.valueOf(header_num), sb.toString());
            }

        } catch (Exception e) {
            FileoutputUtil.outputFileError(FileoutputUtil.PacketEx_Log, e);
            e.printStackTrace();
        }

    }

    @Override
    public void sessionIdle(final IoSession session, final IdleStatus status) throws Exception {
        final MapleClient client = (MapleClient) session.getAttribute(MapleClient.CLIENT_KEY);

        /*	if (client != null && client.getPlayer() != null) {
        System.out.println("Player "+ client.getPlayer().getName() +" went idle");
        }*/
        if (client != null) {
            client.sendPing();
        }
        super.sessionIdle(session, status);
    }

    public static final void handlePacket(final RecvPacketOpcode header, final SeekableLittleEndianAccessor slea, final MapleClient c, final boolean cs) throws Exception {
        switch (header) {
            case PONG:
                c.pongReceived();
                break;
            case AFTER_LOGIN:
                c.getSession().write(LoginPacket.pinOperation((byte) 0)); // Accept 
                break;
            case LOGIN_PASSWORD:
                CharLoginHandler.login(slea, c);
                break;
            case SERVERLIST_REQUEST:
                CharLoginHandler.ServerListRequest(slea, c);
                break;
            case CHARLIST_REQUEST:
                CharLoginHandler.CharlistRequest(slea, c);
                break;
            case SERVERSTATUS_REQUEST:
                slea.readShort(); // World 
                CharLoginHandler.ServerStatusRequest(c);
                break;
            case CHECK_CHAR_NAME:
                CharLoginHandler.CheckCharName(slea.readMapleAsciiString(), c);
                break;
            case CREATE_CHAR:
                CharLoginHandler.CreateChar(slea, c);
                break;
            case DELETE_CHAR:
                CharLoginHandler.DeleteChar(slea, c);
                break;
            case CHAR_SELECT:
            case CHAR_SELECT_WITH_PIC:
                CharLoginHandler.Character_WithSecondPassword(slea, c, header == RecvPacketOpcode.CHAR_SELECT_WITH_PIC);
                break;
            case RELOG:
                c.getSession().write(LoginPacket.getRelogResponse());
                break;
            case VIEW_ALL_CHAR:
                c.getSession().write(LoginPacket.blockViewAll());
                break;
            case PICK_ALL_CHAR:
            case VIEW_ALL_WITH_PIC:
            case VIEW_ALL_PIC_REGISTER:
                c.getSession().close();
                break;
            // END OF LOGIN SERVER
            case CHANGE_CHANNEL:
                InterServerHandler.ChangeChannel(slea, c, c.getPlayer());
                break;
            case PLAYER_LOGGEDIN:
                final int playerid = slea.readInt();
                if (cs) {
                    CashShopOperation.EnterCS(playerid, c);
                } else {
                    InterServerHandler.Loggedin(playerid, c);
                }
                break;
            case ENTER_CASH_SHOP:
                InterServerHandler.EnterCS(c, c.getPlayer());
                break;
            case MOVE_PLAYER:
                PlayerHandler.MovePlayer(slea, c, c.getPlayer());
                break;
            case CHAR_INFO_REQUEST:
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.CharInfoRequest(slea.readInt(), c, c.getPlayer());
                break;
            case CLOSE_RANGE_ATTACK:
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), false);
                break;
            case RANGED_ATTACK:
                PlayerHandler.rangedAttack(slea, c, c.getPlayer());
                break;
            case MAGIC_ATTACK:
                PlayerHandler.MagicDamage(slea, c, c.getPlayer());
                break;
            case SPECIAL_MOVE:
                PlayerHandler.SpecialMove(slea, c, c.getPlayer());
                break;
            case PASSIVE_ENERGY:
                PlayerHandler.closeRangeAttack(slea, c, c.getPlayer(), true);
                break;
            case FACE_EXPRESSION:
                PlayerHandler.ChangeEmotion(slea.readInt(), c.getPlayer());
                break;
            case TAKE_DAMAGE:
                PlayerHandler.TakeDamage(slea, c, c.getPlayer());
                break;
            case HEAL_OVER_TIME:
                PlayerHandler.Heal(slea, c.getPlayer());
                break;
            case CANCEL_BUFF:
                PlayerHandler.CancelBuffHandler(slea.readInt(), c.getPlayer());
                break;
            case CANCEL_ITEM_EFFECT:
                PlayerHandler.CancelItemEffect(slea.readInt(), c.getPlayer());
                break;
            case USE_CHAIR:
                PlayerHandler.UseChair(slea.readInt(), c, c.getPlayer());
                break;
            case CANCEL_CHAIR:
                PlayerHandler.CancelChair(slea.readShort(), c, c.getPlayer());
                break;
            case USE_ITEMEFFECT:
            case WHEEL_OF_FORTUNE:
                PlayerHandler.UseItemEffect(slea.readInt(), c, c.getPlayer());
                break;
            case SKILL_EFFECT:
                PlayerHandler.SkillEffect(slea, c.getPlayer());
                break;
            case MESO_DROP:
                c.getPlayer().updateTick(slea.readInt());
                PlayerHandler.DropMeso(slea.readInt(), c.getPlayer());
                break;
            case MONSTER_BOOK_COVER:
                PlayerHandler.ChangeMonsterBookCover(slea.readInt(), c, c.getPlayer());
                break;
            case CHANGE_KEYMAP:
                PlayerHandler.ChangeKeymap(slea, c.getPlayer());
                break;
            case CHANGE_MAP:
                if (cs) {
                    CashShopOperation.LeaveCS(slea, c, c.getPlayer());
                } else {
                    PlayerHandler.ChangeMap(slea, c, c.getPlayer());
                }
                break;
            case CHANGE_MAP_SPECIAL:
                slea.skip(1);
                PlayerHandler.ChangeMapSpecial(slea.readMapleAsciiString(), c, c.getPlayer());
                break;
            case USE_INNER_PORTAL:
                slea.skip(1);
                PlayerHandler.InnerPortal(slea, c, c.getPlayer());
                break;
            case TROCK_ADD_MAP:
                PlayerHandler.TrockAddMap(slea, c, c.getPlayer());
                break;
            case ARAN_COMBO:
                PlayerHandler.AranCombo(c, c.getPlayer());
                break;
            case SKILL_MACRO:
                PlayerHandler.ChangeSkillMacro(slea, c.getPlayer());
                break;
            case GIVE_FAME:
                PlayersHandler.GiveFame(slea, c, c.getPlayer());
                break;
            case TRANSFORM_PLAYER:
                PlayersHandler.TransformPlayer(slea, c, c.getPlayer());
                break;
            case NOTE_ACTION:
                PlayersHandler.Note(slea, c.getPlayer());
                break;
            case USE_DOOR:
                PlayersHandler.UseDoor(slea, c.getPlayer());
                break;
            case DAMAGE_REACTOR:
                PlayersHandler.HitReactor(slea, c);
                break;
            case TOUCH_REACTOR:
                PlayersHandler.TouchReactor(slea, c);
                break;
            case CLOSE_CHALKBOARD:
                c.getPlayer().setChalkboard(null);
                break;
            case ITEM_MAKER:
                ItemMakerHandler.ItemMaker(slea, c);
                break;
            case ITEM_SORT:
                InventoryHandler.ItemSort(slea, c);
                break;
            case ITEM_GATHER:
                InventoryHandler.ItemGather(slea, c);
                break;
            case ITEM_MOVE:
                InventoryHandler.ItemMove(slea, c);
                break;
            case ITEM_PICKUP:
                InventoryHandler.Pickup_Player(slea, c, c.getPlayer());
                break;
            case USE_CASH_ITEM:
                InventoryHandler.UseCashItem(slea, c);
                break;
            case USE_ITEM:
                InventoryHandler.UseItem(slea, c, c.getPlayer());
                break;
            case USE_MAGNIFY_GLASS:
                InventoryHandler.UseMagnify(slea, c);
                break;
            case USE_SCRIPTED_NPC_ITEM:
                InventoryHandler.UseScriptedNPCItem(slea, c, c.getPlayer());
                break;
            case USE_RETURN_SCROLL:
                InventoryHandler.UseReturnScroll(slea, c, c.getPlayer());
                break;
            case USE_UPGRADE_SCROLL:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll((byte) slea.readShort(), (byte) slea.readShort(), (byte) slea.readShort(), c, c.getPlayer(), (byte) 0);
                break;
            case USE_POTENTIAL_SCROLL:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll((byte) slea.readShort(), (byte) slea.readShort(), (byte) 0, c, c.getPlayer(), (byte) 1);
                break;
            case USE_EQUIP_SCROLL:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseUpgradeScroll((byte) slea.readShort(), (byte) slea.readShort(), (byte) 0, c, c.getPlayer(), (byte) 2);
                break;
            case USE_SUMMON_BAG:
                InventoryHandler.UseSummonBag(slea, c, c.getPlayer());
                break;
            case USE_TREASUER_CHEST:
                InventoryHandler.UseTreasureChest(slea, c, c.getPlayer());
                break;
            case USE_SKILL_BOOK:
                c.getPlayer().updateTick(slea.readInt());
                InventoryHandler.UseSkillBook((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case USE_CATCH_ITEM:
                InventoryHandler.UseCatchItem(slea, c, c.getPlayer());
                break;
            case USE_MOUNT_FOOD:
                InventoryHandler.UseMountFood(slea, c, c.getPlayer());
                break;
            case REWARD_ITEM:
                InventoryHandler.UseRewardItem((byte) slea.readShort(), slea.readInt(), c, c.getPlayer());
                break;
            case HYPNOTIZE_DMG:
                MobHandler.HypnotizeDmg(slea, c.getPlayer());
                break;
            case MOB_NODE:
                MobHandler.MobNode(slea, c.getPlayer());
                break;
            case DISPLAY_NODE:
                MobHandler.DisplayNode(slea, c.getPlayer());
                break;
            case MOVE_LIFE:
                MobHandler.MoveMonster(slea, c, c.getPlayer());
                break;
            case AUTO_AGGRO:
                MobHandler.AutoAggro(slea.readInt(), c.getPlayer());
                break;
            case FRIENDLY_DAMAGE:
                MobHandler.FriendlyDamage(slea, c.getPlayer());
                break;
            case NPC_SHOP:
                NPCHandler.NPCShop(slea, c, c.getPlayer());
                break;
            case NPC_TALK:
                NPCHandler.NPCTalk(slea, c, c.getPlayer());
                break;
            case NPC_TALK_MORE:
                NPCHandler.NPCMoreTalk(slea, c);
                break;
            case NPC_ACTION:
                NPCHandler.NPCAnimation(slea, c);
                break;
            case QUEST_ACTION:
                NPCHandler.QuestAction(slea, c, c.getPlayer());
                break;
            case STORAGE:
                NPCHandler.Storage(slea, c, c.getPlayer());
                break;
            case GENERAL_CHAT:
                c.getPlayer().updateTick(slea.readInt());
                ChatHandler.GeneralChat(slea.readMapleAsciiString(), slea.readByte(), c, c.getPlayer());
                break;
            case PARTYCHAT:
                c.getPlayer().updateTick(slea.readInt());
                ChatHandler.Others(slea, c, c.getPlayer());
                break;
            case WHISPER:
                ChatHandler.Whisper_Find(slea, c);
                break;
            case MESSENGER:
                ChatHandler.Messenger(slea, c);
                break;
            case AUTO_ASSIGN_AP:
                StatsHandling.AutoAssignAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_AP:
                StatsHandling.DistributeAP(slea, c, c.getPlayer());
                break;
            case DISTRIBUTE_SP:
                c.getPlayer().updateTick(slea.readInt());
                StatsHandling.DistributeSP(slea.readInt(), c, c.getPlayer());
                break;
            case PLAYER_INTERACTION:
                System.out.println("..." + slea.toString());
                PlayerInteractionHandler.PlayerInteraction(slea, c, c.getPlayer());
                break;
            case GUILD_OPERATION:
                GuildHandler.Guild(slea, c);
                break;
            case DENY_GUILD_REQUEST:
                slea.skip(1);
                GuildHandler.DenyGuildRequest(slea.readMapleAsciiString(), c);
                break;
            case ALLIANCE_OPERATION:
                AllianceHandler.HandleAlliance(slea, c, false);
                break;
            case DENY_ALLIANCE_REQUEST:
                AllianceHandler.HandleAlliance(slea, c, true);
                break;
            case BBS_OPERATION:
                BBSHandler.BBSOperatopn(slea, c);
                break;
            case PARTY_OPERATION:
                PartyHandler.PartyOperation(slea, c);
                break;
            case DENY_PARTY_REQUEST:
                PartyHandler.PartyResponse(slea, c);
                break;
            case BUDDYLIST_MODIFY:
                BuddyListHandler.BuddyOperation(slea, c);
                break;
            case CYGNUS_SUMMON:
                UserInterfaceHandler.CygnusSummon_NPCRequest(c);
                break;
            case SHIP_OBJECT:
                UserInterfaceHandler.ShipObjectRequest(slea.readInt(), c);
                break;
            case BUY_CS_ITEM:
                CashShopOperation.BuyCashItem(slea, c, c.getPlayer());
                break;
            case COUPON_CODE:
                CashShopOperation.CouponCode(slea, c);
                break;
            case CS_UPDATE:
                CashShopOperation.CSUpdate(c);
                break;
            case CS_SURPRISE:
                CashShopOperation.CashShopSurprise(slea, c);
                break;
            case TWIN_DRAGON_EGG:
                CashShopOperation.TwinDragonEgg(slea, c);
                break;
            case DAMAGE_SUMMON:
                slea.skip(4);
                SummonHandler.DamageSummon(slea, c.getPlayer());
                break;
            case MOVE_SUMMON:
                SummonHandler.MoveSummon(slea, c.getPlayer());
                break;
            case SUMMON_ATTACK:
                SummonHandler.SummonAttack(slea, c, c.getPlayer());
                break;
            case MOVE_DRAGON:
                SummonHandler.MoveDragon(slea, c.getPlayer());
                break;
            case SPAWN_PET:
                PetHandler.SpawnPet(slea, c, c.getPlayer());
                break;
            case MOVE_PET:
                PetHandler.MovePet(slea, c.getPlayer());
                break;
            case PET_CHAT:
                if (slea.available() < 12) {
                    break;
                }
                PetHandler.PetChat((int) slea.readLong(), slea.readShort(), slea.readMapleAsciiString(), c.getPlayer());
                break;
            case PET_COMMAND:
                PetHandler.PetCommand(slea, c, c.getPlayer());
                break;
            case PET_FOOD:
                PetHandler.PetFood(slea, c, c.getPlayer());
                break;
            case PET_LOOT:
                InventoryHandler.Pickup_Pet(slea, c, c.getPlayer());
                break;
            case PET_AUTO_POT:
                PetHandler.Pet_AutoPotion(slea, c, c.getPlayer());
                break;
            case PET_IGNORE:
                PetHandler.PetIgnoreTag(slea, c, c.getPlayer());
                break;
            case MONSTER_CARNIVAL:
                MonsterCarnivalHandler.MonsterCarnival(slea, c);
                break;
            case USE_HIRED_MERCHANT:
                HiredMerchantHandler.UseHiredMerchant(slea, c);
                break;
            case MERCH_ITEM_STORE:
                HiredMerchantHandler.MerchantItemStore(slea, c);
                break;
            case CANCEL_DEBUFF:
                // Ignore for now
                break;
            //case MAPLETV:
            //MapleTVEffect.startTV();
            //break;
            case LEFT_KNOCK_BACK:
                PlayerHandler.leftKnockBack(slea, c);
                break;
            case SNOWBALL:
                PlayerHandler.snowBall(slea, c);
                break;
            case COCONUT:
                PlayersHandler.hitCoconut(slea, c);
                break;
            case REPAIR:
                NPCHandler.repair(slea, c);
                break;
            case REPAIR_ALL:
                NPCHandler.repairAll(c);
                break;
            case OWL:
                InventoryHandler.Owl(slea, c);
                break;
            case OWL_WARP:
                InventoryHandler.OwlWarp(slea, c);
                break;
            case USE_OWL_MINERVA:
                InventoryHandler.OwlMinerva(slea, c);
                break;
            case RPS_GAME:
                NPCHandler.RPSGame(slea, c);
                break;
            case UPDATE_QUEST:
                NPCHandler.UpdateQuest(slea, c);
                break;
            case USE_ITEM_QUEST:
                NPCHandler.UseItemQuest(slea, c);
                break;
            case FOLLOW_REQUEST:
                PlayersHandler.FollowRequest(slea, c);
                break;
            case FOLLOW_REPLY:
                PlayersHandler.FollowReply(slea, c);
                break;
            case RING_ACTION:
                PlayersHandler.RingAction(slea, c);
                break;
            //case LUCKY_LOGOUT_GIFT:
            //UserInterfaceHandler.LuckyLogout(slea, c);
            //break;
            case REQUEST_FAMILY:
                FamilyHandler.RequestFamily(slea, c);
                break;
            case OPEN_FAMILY:
                FamilyHandler.OpenFamily(slea, c);
                break;
            case FAMILY_OPERATION:
                FamilyHandler.FamilyOperation(slea, c);
                break;
            case DELETE_JUNIOR:
                FamilyHandler.DeleteJunior(slea, c);
                break;
            case DELETE_SENIOR:
                FamilyHandler.DeleteSenior(slea, c);
                break;
            case USE_FAMILY:
                FamilyHandler.UseFamily(slea, c);
                break;
            case FAMILY_PRECEPT:
                FamilyHandler.FamilyPrecept(slea, c);
                break;
            case FAMILY_SUMMON:
                FamilyHandler.FamilySummon(slea, c);
                break;
            case ACCEPT_FAMILY:
                FamilyHandler.AcceptFamily(slea, c);
                break;
            case QUICK_SLOT:
                PlayerHandler.QuickSlot(slea, c.getPlayer());
                break;
            case UPDATE_CHARACTER:
                PlayerHandler.updateCharacter(c.getPlayer()); // received when change map
                break;
            case EXPEDITION_OPERATION:
                ExpeditionHandler.ExpeditionOperation(slea, c, c.getPlayer());
                break;
            case PARTY_LISTING:
                c.getPlayer().dropMessage(1, "The Party Listing is currently unavailable.");
                c.getSession().write(MaplePacketCreator.enableActions());
                break;
            case VICIOUS_HAMMER:
                slea.skip(8);
                c.getSession().write(MTSCSPacket.ViciousHammer(false, (byte) 0));
                break;
            case THROW_SKILL:
                PlayerHandler.BombSkill(slea, c);
                break;
            case MONSTER_BOMB:
                MobHandler.MonsterBomb(slea, c.getPlayer(), c);
                break;
            case SELF_DESTRUCT:
                MobHandler.SelfDestruct(slea.readInt(), c.getPlayer());
                break;
            case ESCORT_RESULT:
                FileoutputUtil.logPacket(String.valueOf(header.getValue()), "[ESCORT_RESULT] " + slea.toString());
                break;
            case FIRST_LOGIN:
            case ENTER_MAP:
            case RELOAD_MAP:
                //do nothing
                break;
            default:
                if (slea.available() >= 0) { // we don't want to log headers only
                    FileoutputUtil.logPacket(String.valueOf(header), "[" + header.toString() + "] " + slea.toString());
                    //System.out.println("[UNHANDLED] Recv [" + header.toString() + "] found");
                }
                break;
        }
    }
}
