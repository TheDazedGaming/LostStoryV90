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
package tools.packet;

import java.sql.SQLException;
import java.sql.ResultSet;


import java.util.List;
import client.MapleClient;
import client.MapleCharacter;
import client.inventory.IItem;
import server.cashShop.CashShop;
import server.cashShop.CashItemFactory;
import server.cashShop.CashItemInfo;
import server.cashShop.CashItemInfo.CashModInfo;
import handling.SendPacketOpcode;
import java.util.Collection;
import tools.Pair;
import java.util.Map;
import java.util.Map.Entry;
import tools.data.output.MaplePacketLittleEndianWriter;

public class MTSCSPacket {

    public static byte[] warpCS(MapleClient c) {
        final MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPEN.getValue());

        PacketHelper.addCharacterInfo(mplew, c.getPlayer());

        mplew.write(1);
        mplew.writeMapleAsciiString(c.getAccountName());

        mplew.writeInt(0); // limit sell data, for each, one int

        Collection<CashModInfo> cmi = CashItemFactory.getInstance().getAllModInfo();
        mplew.writeShort(cmi.size());
        for (CashModInfo cm : cmi) {
            addModCashItemInfo(mplew, cm);
        }

        mplew.write(0); // for each, 3 bytes

        mplew.writeZeroBytes(120);
        for (int i = 1; i <= 8; i++) {
            for (int j = 0; j < 2; j++) {
                mplew.writeInt(i);
                mplew.writeInt(j);
                mplew.writeInt(50200004);
                mplew.writeInt(i);
                mplew.writeInt(j);
                mplew.writeInt(50200069);
                mplew.writeInt(i);
                mplew.writeInt(j);
                mplew.writeInt(50200117);
                mplew.writeInt(i);
                mplew.writeInt(j);
                mplew.writeInt(50100008);
                mplew.writeInt(i);
                mplew.writeInt(j);
                mplew.writeInt(50000047);
            }
        }
        mplew.writeShort(0);// stock
        mplew.writeShort(0); // limit goods (for each size, 104 bytes. each)
        mplew.writeShort(0); // for each 68 bytes each
        mplew.write(0); //eventON
        mplew.writeInt(0x8D); // ? 0 also works

        return mplew.getPacket();
    }

    public static byte[] playCashSong(int itemid, String name) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CASH_SONG.getValue());
        mplew.writeInt(itemid);
        mplew.writeMapleAsciiString(name);
        return mplew.getPacket();
    }

    public static byte[] useCharm(byte charmsleft, byte daysleft) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(6);
        mplew.write(1);
        mplew.write(charmsleft);
        mplew.write(daysleft);

        return mplew.getPacket();
    }

    public static byte[] useWheel(byte charmsleft) {
        // You have used 1 Wheel of Destiny in order to revive at the current map. (<left> left)
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_ITEM_GAIN_INCHAT.getValue());
        mplew.write(21);
        mplew.write(charmsleft); // left

        return mplew.getPacket();
    }

    public static byte[] ViciousHammer(boolean start, int hammered) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VICIOUS_HAMMER.getValue());
        if (start) {
            mplew.write(60);
            mplew.writeInt(0);
            mplew.writeInt(hammered);
        } else {
            mplew.write(64);
            mplew.writeInt(0);
        }

        return mplew.getPacket();
    }

    public static byte[] VegasScroll(byte action) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.VEGAS_SCROLL.getValue());
        mplew.write(action);
        // 1: This item cannot be used.

        return mplew.getPacket();
    }

    public static byte[] changePetName(MapleCharacter chr, String newname, int slot) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.PET_NAMECHANGE.getValue());

        mplew.writeInt(chr.getId());
        mplew.write(0);
        mplew.writeMapleAsciiString(newname);
        mplew.write(slot);

        return mplew.getPacket();
    }

    public static byte[] showNotes(ResultSet notes, int count) throws SQLException {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.SHOW_NOTES.getValue());
        mplew.write(3);
        mplew.write(count);
        for (int i = 0; i < count; i++) {
            mplew.writeInt(notes.getInt("id"));
            mplew.writeMapleAsciiString(notes.getString("from"));
            mplew.writeMapleAsciiString(notes.getString("message"));
            mplew.writeLong(PacketHelper.getKoreanTimestamp(notes.getLong("timestamp")));
            mplew.write(notes.getInt("gift"));
            notes.next();
        }

        return mplew.getPacket();
    }

    public static byte[] useChalkboard(final int charid, final String msg) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CHALKBOARD.getValue());

        mplew.writeInt(charid);
        if (msg == null || msg.length() <= 0) {
            mplew.write(0);
        } else {
            mplew.write(1);
            mplew.writeMapleAsciiString(msg);
        }

        return mplew.getPacket();
    }

    public static byte[] receiveGachaponTicket(int amount) {
        // You have acquired <> Gachapon Stamps by purchasing the Gachapon Ticket.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_GACHAPON_STAMPS.getValue());
        mplew.write(amount > 0 ? 1 : 0);
        if (amount > 0) {
            mplew.writeInt(amount);
        }

        return mplew.getPacket();
    }

    public static byte[] getTrockRefresh(MapleCharacter chr, boolean vip, boolean delete) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.TROCK_LOCATIONS.getValue());
        mplew.write(delete ? 2 : 3);
        mplew.write(vip ? 1 : 0);
        if (vip) {
            int[] map = chr.getRocks();
            for (int i = 0; i < 10; i++) {
                mplew.writeInt(map[i]);
            }
        } else {
            int[] map = chr.getRegRocks();
            for (int i = 0; i < 5; i++) {
                mplew.writeInt(map[i]);
            }
        }
        return mplew.getPacket();
    }

    public static byte[] sendWishList(MapleCharacter chr, boolean update) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(update ? 0x61 : 0x5B); //+12
        int[] list = chr.getWishlist();
        for (int i = 0; i < 10; i++) {
            mplew.writeInt(list[i] != -1 ? list[i] : 0);
        }
        return mplew.getPacket();
    }

    public static byte[] showNXMapleTokens(MapleCharacter chr) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        // Combined both NxCredit and NxPrepaid
        mplew.writeShort(SendPacketOpcode.CS_UPDATE.getValue());
        mplew.writeInt(0); // NXCredit [1]
        mplew.writeInt((chr.getOccupation() / 100 == 5) ? 1333337 : chr.getCSPoints(2)); // MPoint [2]
        mplew.writeInt(chr.getCSPoints(1)); // NXPrepaid [4]

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSPackage(Map<Integer, IItem> ccc, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x97);
        mplew.write(ccc.size());
        for (Entry<Integer, IItem> sn : ccc.entrySet()) {
            addCashItemInfo(mplew, sn.getValue(), accid, sn.getKey().intValue());
        }
        mplew.writeShort(0);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSItem(int itemid, int sn, int uniqueid, int accid, int quantity, String giftFrom, long expire) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x63); //use to be 4a
        addCashItemInfo(mplew, uniqueid, accid, itemid, sn, quantity, giftFrom, expire);

        return mplew.getPacket();
    }

    public static byte[] showBoughtCSItem(IItem item, int sn, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x63);
        addCashItemInfo(mplew, item, accid, sn);

        return mplew.getPacket();
    }

    public static byte[] redeemResponse(final int sn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0xAC);
        mplew.writeInt(sn); // ? sn?
        mplew.write(1); // byte, must be 1.

        return mplew.getPacket();
    }

    public static byte[] cashShopSurpriseFail() {
        // Please check and see if you have exceeded the number of cash items you can have.
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_SURPRISE.getValue());
        mplew.write(189);

        return mplew.getPacket();
    }

    public static byte[] showCashShopSurprise(int idFirst, IItem item, int accid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_SURPRISE.getValue());
        mplew.write(190);
        mplew.writeLong(idFirst); //uniqueid of the xmas surprise itself
        mplew.writeInt(0);
        addCashItemInfo(mplew, item, accid, 0); //info of the new item, but packet shows 0 for sn?
        mplew.writeInt(item.getItemId());
        mplew.write(1);
        mplew.write(1);

        return mplew.getPacket();
    }

    public static byte[] showTwinDragonEgg(int idFirst) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_TWIN_DRAGON_EGG.getValue());
        mplew.writeLong(idFirst); //uniqueid of the dragon egg itself
        mplew.writeInt(0);
        mplew.writeZeroBytes(12);

        return mplew.getPacket();
    }

    public static void addCashItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, int accId, int sn) {
        addCashItemInfo(mplew, item, accId, sn, true);
    }

    public static void addCashItemInfo(MaplePacketLittleEndianWriter mplew, IItem item, int accId, int sn, boolean isFirst) {
        addCashItemInfo(mplew, item.getUniqueId(), accId, item.getItemId(), sn, item.getQuantity(), item.getGiftFrom(), item.getExpiration(), isFirst); //owner for the lulz
    }

    public static void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire) {
        addCashItemInfo(mplew, uniqueid, accId, itemid, sn, quantity, sender, expire, true);
    }

    public static void addCashItemInfo(MaplePacketLittleEndianWriter mplew, int uniqueid, int accId, int itemid, int sn, int quantity, String sender, long expire, boolean isFirst) {
        mplew.writeLong(uniqueid > 0 ? uniqueid : 0);
        mplew.writeLong(accId);
        mplew.writeInt(itemid);
        mplew.writeInt(isFirst ? sn : 0);
        mplew.writeShort(quantity);
        mplew.writeAsciiString(sender, 13); //owner for the lulzlzlzl
        PacketHelper.addExpirationTime(mplew, expire);
        mplew.writeLong(isFirst ? 0 : sn);
        //if (isFirst && uniqueid > 0 && GameConstants.isEffectRing(itemid)) {
        //	MapleRing ring = MapleRing.loadFromDb(uniqueid);
        //	if (ring != null) { //or is this only for friendship rings, i wonder. and does isFirst even matter
        //		mplew.writeMapleAsciiString(ring.getPartnerName());
        //		mplew.writeInt(itemid);
        //		mplew.writeShort(quantity);
        //	}
        //}
    }

    public static void addModCashItemInfo(MaplePacketLittleEndianWriter mplew, CashModInfo item) {
        int flags = item.flags;
        mplew.writeInt(item.sn);
        mplew.writeInt(flags);
        if ((flags & 0x1) != 0) {
            mplew.writeInt(item.itemid);
        }
        if ((flags & 0x2) != 0) {
            mplew.writeShort(item.count);
        }
        if ((flags & 0x4) != 0) {
            mplew.writeInt(item.discountPrice);
        }
        if ((flags & 0x8) != 0) {
            mplew.write(item.unk_1 - 1);
        }
        if ((flags & 0x10) != 0) {
            mplew.write(item.priority);
        }
        if ((flags & 0x20) != 0) {
            mplew.writeShort(item.period);
        }
        if ((flags & 0x40) != 0) {
            mplew.writeInt(0);
        }
        if ((flags & 0x80) != 0) {
            mplew.writeInt(item.meso);
        }
        if ((flags & 0x100) != 0) {
            mplew.write(item.unk_2 - 1);
        }
        if ((flags & 0x200) != 0) {
            mplew.write(item.gender);
        }
        if ((flags & 0x400) != 0) {
            mplew.write(item.showUp ? 1 : 0);
        }
        if ((flags & 0x800) != 0) {
            mplew.write(item.mark);
        }
        if ((flags & 0x1000) != 0) {
            mplew.write(item.unk_3 - 1);
        }
        if ((flags & 0x2000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x4000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x8000) != 0) {
            mplew.writeShort(0);
        }
        if ((flags & 0x10000) != 0) {
            List<CashItemInfo> pack = CashItemFactory.getInstance().getPackageItems(item.sn);
            if (pack == null) {
                mplew.write(0);
            } else {
                mplew.write(pack.size());
                for (int i = 0; i < pack.size(); i++) {
                    mplew.writeInt(pack.get(i).getSN());
                }
            }
        }
    }

    public static byte[] showBoughtCSQuestItem(int price, short quantity, int itemid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x9B);
        mplew.writeInt(1); // size.
        // for each size above = 8 bytes below.
        mplew.writeInt(quantity);
        mplew.writeInt(itemid);

        return mplew.getPacket();
    }

    public static byte[] sendCouponFail(final MapleClient c, int err) {
        c.csAttempt++;
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(c.csAttempt > 2 ? 0x58 : 0x62);
        mplew.write(err);

        return mplew.getPacket();
    }

    public static byte[] sendCSFail(int err) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x62);
        mplew.write(err);

        return mplew.getPacket();
    }

    public static byte[] showCouponRedeemedItem(final int accid, final int MaplePoints, final Map<Integer, IItem> items1, final List<Pair<Integer, Integer>> items2, final int mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x65);
        mplew.write(items1.size()); // Cash Item
        for (Entry<Integer, IItem> sn : items1.entrySet()) {
            addCashItemInfo(mplew, sn.getValue(), accid, sn.getKey().intValue());
        }
        mplew.writeInt(MaplePoints);
        mplew.writeInt(items2.size()); // Normal items size
        for (Pair<Integer, Integer> item : items2) {
            mplew.writeInt(item.getRight()); // Count
            mplew.writeInt(item.getLeft());  // Item ID
        }
        mplew.writeInt(mesos);

        return mplew.getPacket();
    }

    public static byte[] enableCSUse() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(0x12);
        mplew.write(1);
        mplew.writeInt(0);

        return mplew.getPacket();
    }

    public static byte[] getCSInventory(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x57); // use to be 3e
        CashShop mci = c.getPlayer().getCashInventory();
        mplew.writeShort(mci.getItemsSize());
        for (IItem itemz : mci.getInventory()) {
            addCashItemInfo(mplew, itemz, c.getAccID(), 0); //test
        }
        mplew.writeShort(c.getPlayer().getStorage().getSlots());
        mplew.writeInt(c.getCharacterSlots());
        mplew.writeShort(4); //00 00 04 00 <-- added?

        return mplew.getPacket();
    }

    //work on this packet a little more
    public static byte[] getCSGifts(MapleClient c) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());

        mplew.write(0x59); //use to be 40
        List<Pair<IItem, String>> mci = c.getPlayer().getCashInventory().loadGifts();
        mplew.writeShort(mci.size());
        for (Pair<IItem, String> mcz : mci) {
            mplew.writeLong(mcz.getLeft().getUniqueId());
            mplew.writeInt(mcz.getLeft().getItemId());
            mplew.writeAsciiString(mcz.getLeft().getGiftFrom(), 13);
            mplew.writeAsciiString(mcz.getRight(), 73);
        }

        return mplew.getPacket();
    }

    public static byte[] cashItemExpired(int uniqueid) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x7C); //use to be 5d
        mplew.writeLong(uniqueid);
        return mplew.getPacket();
    }

    public static byte[] OnCashItemResCoupleDone(IItem item, int sn, int accid, String receiver, boolean couple) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(couple ? 0x95 : 0x9F); // Same as friendship. (0x9F)
        addCashItemInfo(mplew, item, accid, sn);
        mplew.writeMapleAsciiString(receiver); // parter name?
        mplew.writeInt(item.getItemId());
        mplew.writeShort(1); // Count

        return mplew.getPacket();
    }

    public static byte[] showGiftSucceed(int price, int itemid, int quantity, String receiver, boolean packages) {
        // "%d [ %s ] \r\nwas sent to %s. \r\n%d NX Prepaid \r\nwere spent in the process.",
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(packages ? 0x99 : 0x6A);
        mplew.writeMapleAsciiString(receiver);
        mplew.writeInt(itemid);
        mplew.writeShort(quantity);
        if (packages) {
            mplew.writeShort(0);
        }
        mplew.writeInt(price);

        return mplew.getPacket();
    }

    public static byte[] increasedInvSlots(int inv, int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6C);
        mplew.write(inv);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    //also used for character slots !
    public static byte[] increasedStorageSlots(int slots) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x6E);
        mplew.writeShort(slots);

        return mplew.getPacket();
    }

    public static byte[] confirmToCSInventory(IItem item, int accId, int sn) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x78);
        addCashItemInfo(mplew, item, accId, sn, false);

        return mplew.getPacket();
    }

    public static byte[] confirmFromCSInventory(IItem item, short pos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();

        mplew.writeShort(SendPacketOpcode.CS_OPERATION.getValue());
        mplew.write(0x76);
        mplew.writeShort(pos);
        PacketHelper.addItemInfo(mplew, item, true, true);

        return mplew.getPacket();
    }

    public static byte[] sendMesobagFailed() {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESOBAG_FAILURE.getValue());
        return mplew.getPacket();
    }

    public static byte[] sendMesobagSuccess(int mesos) {
        MaplePacketLittleEndianWriter mplew = new MaplePacketLittleEndianWriter();
        mplew.writeShort(SendPacketOpcode.MESOBAG_SUCCESS.getValue());
        mplew.writeInt(mesos);
        return mplew.getPacket();
    }
}