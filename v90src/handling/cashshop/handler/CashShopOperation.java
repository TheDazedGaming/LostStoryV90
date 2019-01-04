package handling.cashshop.handler;

import java.util.Map;
import java.util.HashMap;

import constants.GameConstants;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleCharacterUtil;
import client.inventory.MapleInventoryType;
import client.inventory.MapleInventoryIdentifier;
import client.inventory.IItem;
import client.inventory.MapleRing;
import handling.cashshop.CashShopServer;
import handling.channel.ChannelServer;
import handling.world.CharacterTransfer;
import handling.world.World;
import java.util.ArrayList;
import java.util.List;
import server.cashShop.CashCouponData;
import server.cashShop.CashItemFactory;
import server.cashShop.CashItemInfo;
import server.cashShop.CashShopCoupon;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.RandomRewards;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Triple;
import tools.packet.MTSCSPacket;
import tools.data.input.SeekableLittleEndianAccessor;

public class CashShopOperation {

    public static void LeaveCS(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        CashShopServer.getPlayerStorage().deregisterPlayer(chr);
        c.updateLoginState(MapleClient.LOGIN_SERVER_TRANSITION, c.getSessionIPAddress());

        try {
            World.ChannelChange_Data(new CharacterTransfer(chr), chr.getId(), c.getChannel());
            c.getSession().write(MaplePacketCreator.getChannelChange(Integer.parseInt(ChannelServer.getInstance(c.getChannel()).getIP().split(":")[1])));
        } finally {
            c.getSession().close();
            chr.saveToDB(false, true);
            c.setPlayer(null);
            c.setReceiving(false);
        }
    }

    public static void EnterCS(final int playerid, final MapleClient c) {
        CharacterTransfer transfer = CashShopServer.getPlayerStorage().getPendingCharacter(playerid);
        if (transfer == null) {
            c.getSession().close();
            return;
        }
        MapleCharacter chr = MapleCharacter.ReconstructChr(transfer, c, false);

        c.setPlayer(chr);
        c.setAccID(chr.getAccountID());

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
        CashShopServer.getPlayerStorage().registerPlayer(chr);
        c.getSession().write(MTSCSPacket.warpCS(c));
        CSUpdate(c);
    }

    public static void CSUpdate(final MapleClient c) {
        c.getSession().write(MTSCSPacket.getCSGifts(c));
        doCSPackets(c);
        c.getSession().write(MTSCSPacket.sendWishList(c.getPlayer(), false));
    }

    public static void CouponCode(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final boolean gift = slea.readShort() > 0;
        if (gift) {
            c.getSession().write(MTSCSPacket.sendCouponFail(c, 0x30));
            doCSPackets(c);
            return;
        }
        final String code = slea.readMapleAsciiString();
        if (code == null || code.length() < 16 || code.length() > 32) { // Please check and see if the coupon id is correct or not.
            // XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX-XXXX
            c.getSession().write(MTSCSPacket.sendCouponFail(c, 0x0E));
            doCSPackets(c);
            return;
        }
        final boolean validcode = CashShopCoupon.getCouponCodeValid(code.toUpperCase());
        if (!validcode) {
            c.getSession().write(MTSCSPacket.sendCouponFail(c, 0x0E));
            doCSPackets(c);
            return;
        }
        final List<CashCouponData> rewards = CashShopCoupon.getCcData(code.toUpperCase());
        if (rewards == null) { // Actually impossible
            CashShopCoupon.setCouponCodeUsed("ERROR", code);
            c.getSession().write(MTSCSPacket.sendCouponFail(c, 0x11));
            doCSPackets(c);
            return;
        }
        // maple point, cs item, normal, mesos
        final Pair<Pair<Integer, Integer>, Pair<List<IItem>, Integer>> cscsize = CashShopCoupon.getSize(rewards);
        if ((c.getPlayer().getCSPoints(2) + cscsize.getLeft().getLeft()) < 0) {
            c.getPlayer().dropMessage(1, "You have too much Maple Points.");
            doCSPackets(c);
            return;
        }
        if (c.getPlayer().getCashInventory().getItemsSize() >= (100 - cscsize.getLeft().getRight())) {
            c.getSession().write(MTSCSPacket.sendCSFail(0x0A));
            doCSPackets(c);
            return;
        }
        if (c.getPlayer().getMeso() + cscsize.getRight().getRight() < 0) {
            c.getPlayer().dropMessage(1, "You have too much mesos.");
            doCSPackets(c);
            return;
        }
        if (!haveSpace(c.getPlayer(), cscsize.getRight().getLeft())) {
            c.getSession().write(MTSCSPacket.sendCSFail(0x19));
            doCSPackets(c);
            return;
        }

        CashShopCoupon.setCouponCodeUsed(c.getPlayer().getName(), code);

        int MaplePoints = 0, mesos = 0;
        final Map<Integer, IItem> togiveCS = new HashMap<>();
        final List<Pair<Integer, Integer>> togiveII = new ArrayList<>();
        for (final CashCouponData reward : rewards) {
            switch (reward.getType()) {
                case 0: { // MaplePoints
                    if (reward.getData() > 0) {
                        c.getPlayer().modifyCSPoints(2, reward.getData(), false);
                        MaplePoints = reward.getData();
                    }
                    break;
                }
                case 1: { // Cash Shop Items
                    final CashItemInfo item = CashItemFactory.getInstance().getItem(reward.getData());
                    if (item != null) {
                        final IItem itemz = c.getPlayer().getCashInventory().toItemWithLog(item, "Obtained from coupon code: " + code + " by " + c.getPlayer().getName() + " on " + FileoutputUtil.CurrentReadable_Date(), "MapleSystem");
                        if (itemz != null && itemz.getUniqueId() > 0) {
                            togiveCS.put(item.getSN(), itemz);
                            c.getPlayer().getCashInventory().addToInventory(itemz);
                        }
                    }
                    break;
                }
                case 2: { // Normal Items
                    if (reward.getQuantity() <= Short.MAX_VALUE && reward.getQuantity() > 0) {
                        final byte pos = MapleInventoryManipulator.addId(c, reward.getData(), (short) reward.getQuantity(), "MapleSystem", "Obtained from coupon code: " + code + " by " + c.getPlayer().getName() + " on " + FileoutputUtil.CurrentReadable_Date());
                        if (pos >= 0) { // Failed
                            togiveII.add(new Pair<>(reward.getData(), (int) reward.getQuantity()));
                        }
                    }
                    break;
                }
                case 3: { // Mesos
                    if (reward.getData() > 0) {
                        c.getPlayer().gainMeso(reward.getData(), false);
                        mesos = reward.getData();
                    }
                    break;
                }
            }
        }
        CashShopCoupon.deleteCouponData(c.getPlayer().getName(), code);
        c.getSession().write(MTSCSPacket.showCouponRedeemedItem(c.getAccID(), MaplePoints, togiveCS, togiveII, mesos));
        doCSPackets(c);
    }

    private static boolean haveSpace(final MapleCharacter chr, final List<IItem> items) {
        byte eq = 0, use = 0, setup = 0, etc = 0, cash = 0;
        for (IItem item : items) {
            final MapleInventoryType invtype = GameConstants.getInventoryType(item.getItemId());
            if (invtype == MapleInventoryType.EQUIP) {
                eq++;
            } else if (invtype == MapleInventoryType.USE) {
                use++;
            } else if (invtype == MapleInventoryType.SETUP) {
                setup++;
            } else if (invtype == MapleInventoryType.ETC) {
                etc++;
            } else if (invtype == MapleInventoryType.CASH) {
                cash++;
            }
        }
        if (chr.getInventory(MapleInventoryType.EQUIP).getNumFreeSlot() < eq || chr.getInventory(MapleInventoryType.USE).getNumFreeSlot() < use || chr.getInventory(MapleInventoryType.SETUP).getNumFreeSlot() < setup || chr.getInventory(MapleInventoryType.ETC).getNumFreeSlot() < etc || chr.getInventory(MapleInventoryType.CASH).getNumFreeSlot() < cash) {
            return false;
        }
        return true;
    }

    public static void CashShopSurprise(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        if (c.getPlayer().getCashInventory().getItemsSize() >= 100) {
            c.getSession().write(MTSCSPacket.sendCSFail(0x0A));
            doCSPackets(c);
            return;
        }
        final int uniqueId = (int) slea.readLong();
        final IItem box = c.getPlayer().getCashInventory().findByCashId(uniqueId);
        final CashItemInfo ciibox = CashItemFactory.getInstance().getItem(10102345);
        if (box != null && box.getQuantity() > 0 && box.getItemId() == 5222000 && ciibox != null) {
            boolean success = false;
            while (!success) {
                final CashItemInfo cii = CashItemFactory.getInstance().getItem(RandomRewards.getInstance().getCSSReward(), true);
                if (cii != null) {
                    final IItem itemz = c.getPlayer().getCashInventory().toItemWithLog(cii, "Obtained from cash shop surprise on " + FileoutputUtil.CurrentReadable_Date(), "MapleSystem");
                    IItem newBox = null;
                    if (box.getQuantity() > 1) {
                        newBox = c.getPlayer().getCashInventory().toItemWithQuantity(ciibox, (box.getQuantity() - 1), "");
                    }
                    if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == cii.getId() && itemz.getQuantity() == cii.getCount()) {
                        c.getPlayer().getCashInventory().removeFromInventory(box);
                        if (newBox != null && newBox.getUniqueId() > 0 && newBox.getItemId() == ciibox.getId()) {
                            c.getPlayer().getCashInventory().addToInventory(newBox); // add the balance back
                        }
                        c.getPlayer().getCashInventory().addToInventory(itemz);
                        c.getSession().write(MTSCSPacket.showCashShopSurprise(uniqueId, itemz, c.getAccID()));
                        success = true;
                    }
                }
            }
        } else {
            c.getSession().write(MTSCSPacket.sendCSFail(0));
        }
        doCSPackets(c);
    }

    public static void TwinDragonEgg(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        final int uniqueId = (int) slea.readLong();
        c.getSession().write(MTSCSPacket.showTwinDragonEgg(uniqueId));
        doCSPackets(c);
    }

    public static void BuyCashItem(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int action = slea.readByte();
        final boolean isNxWhore = (chr.getOccupation() / 100 == 5);
        if (action == 3) { // Buy Cash Item
            slea.skip(1);
            final int toCharge = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
if ((item.getId() >= 1812000 && item.getId() <= 1812006) || (item.getId() >= 5211000 && item.getId() <= 5211048) || (item.getId() >= 5360000 && item.getId() <=5360014) || (item.getId() >= 5030000 && item.getId() <= 5030012) || (item.getId() >= 5140000 && item.getId() <= 5140006) || item.getId() == 5360042) { 
	chr.dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
        return; 
}
            if (item == null || (!isNxWhore && chr.getCSPoints(toCharge) < item.getPrice()) || (isNxWhore && toCharge != 2 && chr.getCSPoints(toCharge) < item.getPrice()) || item.getPrice() <= 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(chr.getGender())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x0B));
                doCSPackets(c);
                return;
            } else if (chr.getCashInventory().getItemsSize() >= 100) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x0A));
                doCSPackets(c);
                return;
            }
            for (final int i : GameConstants.cashBlock) {
                if (item.getId() == i && !chr.isGM()) {
                    chr.dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            if (!isNxWhore || toCharge != 2) {
                chr.modifyCSPoints(toCharge, -item.getPrice(), false);
            }
            final IItem itemz = chr.getCashInventory().toItem(item);
            if (itemz != null && itemz.getUniqueId() > 0 && itemz.getItemId() == item.getId() && itemz.getQuantity() == item.getCount()) {
                chr.getCashInventory().addToInventory(itemz);
                c.getSession().write(MTSCSPacket.showBoughtCSItem(itemz, item.getSN(), c.getAccID()));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
            }
        } else if (action == 4 || action == 32) { // Gifting Items / Package
            slea.skip(4); // birthday
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            if (action == 4) {
                slea.skip(1); // size?
            }
            final String partnerName = slea.readMapleAsciiString();
            final String msg = slea.readMapleAsciiString();
            if (item == null || chr.getCSPoints(1) < item.getPrice() || msg.length() > 73 || msg.length() < 1 || item.getPrice() <= 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            final Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, chr.getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == chr.getId() || info.getMid().intValue() == c.getAccID()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x07));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(info.getRight().intValue())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x08));
                doCSPackets(c);
                return;
            }
            for (final int i : GameConstants.cashBlock) {
                if (item.getId() == i && !chr.isGM()) {
                    chr.dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            chr.getCashInventory().gift(info.getLeft().intValue(), chr.getName(), msg, item.getSN(), MapleInventoryIdentifier.getInstance());
            chr.modifyCSPoints(1, -item.getPrice(), false);
            c.getSession().write(MTSCSPacket.showGiftSucceed(item.getPrice(), item.getId(), item.getCount(), partnerName, action == 32));
        } else if (action == 5) { // Wishlist
            chr.clearWishlist();
            if (slea.available() < 40) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            int[] wishlist = new int[10];
            for (int i = 0; i < 10; i++) {
                wishlist[i] = slea.readInt();
            }
            chr.setWishlist(wishlist);
            c.getSession().write(MTSCSPacket.sendWishList(chr, true));
        } else if (action == 7) { // Increase Storage Slots
            slea.skip(1);
            final int toCharge = slea.readInt();
            if (toCharge == 2) { // Maple Points
                chr.dropMessage(1, "You cannot use MaplePoints to buy this item.");
                doCSPackets(c);
                return;
            }
            final int coupon = slea.readByte() > 0 ? 2 : 1;
            if (coupon > 1) {
                final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
                if (item == null || chr.getCSPoints(toCharge) < item.getPrice() || chr.getStorage().getSlots() >= 41 || item.getPrice() <= 0) {
                    c.getSession().write(MTSCSPacket.sendCSFail(0));
                    doCSPackets(c);
                    return;
                }
            }
            if (chr.getCSPoints(toCharge) >= 4000 && chr.getStorage().getSlots() < 45) {
                chr.modifyCSPoints(toCharge, -(4000 * coupon), false);
                chr.getStorage().increaseSlots((byte) (4 * coupon));
                chr.getStorage().saveToDB();
                c.getSession().write(MTSCSPacket.increasedStorageSlots(chr.getStorage().getSlots()));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
            }
        } else if (action == 14) { // Take from Cash Inventory (UniqueId -> type(byte) -> position(short))
            final IItem item = chr.getCashInventory().findByCashId((int) slea.readLong());
            if (item != null && item.getQuantity() > 0 && MapleInventoryManipulator.checkSpace(c, item.getItemId(), item.getQuantity(), item.getOwner())) {
                final IItem item_ = item.copy();
                short pos = MapleInventoryManipulator.addbyItem(c, item_, true);
                if (pos >= 0) {
                    if (item_.getPet() != null) {
                        item_.getPet().setInventoryPosition(pos);
                        chr.addPet(item_.getPet());
                    }
                    chr.getCashInventory().removeFromInventory(item);
                    c.getSession().write(MTSCSPacket.confirmFromCSInventory(item_, pos));
                } else {
                    c.getSession().write(MTSCSPacket.sendCSFail(0x19));
                }
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0x19));
            }
        } else if (action == 15) { // Put Into Cash Inventory
            final int uniqueid = (int) slea.readLong();
            final MapleInventoryType type = MapleInventoryType.getByType(slea.readByte());
            final IItem item = chr.getInventory(type).findByUniqueId(uniqueid);
            if (item != null && item.getQuantity() > 0 && item.getUniqueId() > 0 && chr.getCashInventory().getItemsSize() < 100) {
                //final int sn = CashItemFactory.getInstance().getSNFromItemId(item.getItemId());
                final IItem item_ = item.copy();
                MapleInventoryManipulator.removeFromSlot(c, type, item.getPosition(), item.getQuantity(), false);
                if (item_.getPet() != null) {
                    chr.removePetCS(item_.getPet());
                }
                item_.setPosition((byte) 0);
                chr.getCashInventory().addToInventory(item_);
                //c.getSession().write(MTSCSPacket.confirmToCSInventory(item, c.getAccID(), sn > 0 ? sn : 0));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0xB1));
            }
        } else if (action == 36) { //36 = friendship, 30 = crush
            //c.getSession().write(MTSCSPacket.sendCSFail(0));
            slea.readInt(); // birthday
            final int useNx = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            final String partnerName = slea.readMapleAsciiString();
            final String msg = slea.readMapleAsciiString();
            if (item == null || !GameConstants.isEffectRing(item.getId()) || (!isNxWhore && chr.getCSPoints(useNx) < item.getPrice()) || (isNxWhore && useNx != 2 && chr.getCSPoints(useNx) < item.getPrice()) || msg.length() > 73 || msg.length() < 1 || item.getPrice() <= 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(chr.getGender())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x0B));
                doCSPackets(c);
                return;
            } else if (chr.getCashInventory().getItemsSize() >= 100) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x0A));
                doCSPackets(c);
                return;
            }
            for (int i : GameConstants.cashBlock) {
                if (item.getId() == i && !chr.isGM()) {
                    chr.dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            final Triple<Integer, Integer, Integer> info = MapleCharacterUtil.getInfoByName(partnerName, chr.getWorld());
            if (info == null || info.getLeft().intValue() <= 0 || info.getLeft().intValue() == chr.getId() || info.getMid().intValue() == c.getAccID()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x07));
                doCSPackets(c);
                return;
            } else if (info.getRight().intValue() == chr.getGender() && action == 30) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x08));
                doCSPackets(c);
                return;
            }

            int err = MapleRing.createRing(item.getId(), chr, partnerName, msg, info.getLeft().intValue(), item.getSN());
            if (err != 1) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            }
            if (!isNxWhore || useNx != 2) {
                chr.modifyCSPoints(useNx, -item.getPrice(), false);
            }
        } else if (action == 31) { // Buying Packages
            slea.skip(1);
            final int useNx = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(slea.readInt());
            List<CashItemInfo> ccc = null;
            if (item != null) {
                ccc = CashItemFactory.getInstance().getPackageItems(item.getId());
            }
            if (item == null || ccc == null || (!isNxWhore && chr.getCSPoints(useNx) < item.getPrice()) || (isNxWhore && useNx != 2 && chr.getCSPoints(useNx) < item.getPrice()) || item.getPrice() <= 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (!item.genderEquals(chr.getGender())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x0B));
                doCSPackets(c);
                return;
            } else if (chr.getCashInventory().getItemsSize() >= (100 - ccc.size())) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x0A));
                doCSPackets(c);
                return;
            }
            for (final int i : GameConstants.cashBlock) {
                if (item.getId() == i && !chr.isGM()) {
                    chr.dropMessage(1, GameConstants.getCashBlockedMsg(item.getId()));
                    doCSPackets(c);
                    return;
                }
            }
            final Map<Integer, IItem> ccz = new HashMap<>();
            for (final CashItemInfo i : ccc) {
                for (final int iz : GameConstants.cashBlock) {
                    if (i.getId() == iz && !chr.isGM()) {
                        continue;
                    }
                }
                final IItem itemz = chr.getCashInventory().toItem(i);
                if (itemz == null || itemz.getUniqueId() <= 0 || itemz.getItemId() != i.getId()) {
                    continue;
                }
                ccz.put(i.getSN(), itemz);
                chr.getCashInventory().addToInventory(itemz);
            }
            if (!isNxWhore || useNx != 2) {
                chr.modifyCSPoints(useNx, -item.getPrice(), false);
            }
            c.getSession().write(MTSCSPacket.showBoughtCSPackage(ccz, c.getAccID()));
        } else if (action == 33) { // Buying Quest Items
            final int sn = slea.readInt();
            final CashItemInfo item = CashItemFactory.getInstance().getItem(sn);
            if (item == null || !MapleItemInformationProvider.getInstance().isQuestItem(item.getId()) || item.getPrice() <= 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0));
                doCSPackets(c);
                return;
            } else if (chr.getMeso() < item.getPrice()) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x20));
                doCSPackets(c);
                return;
            } else if (chr.getInventory(GameConstants.getInventoryType(item.getId())).getNextFreeSlot() < 0) {
                c.getSession().write(MTSCSPacket.sendCSFail(0x19));
                doCSPackets(c);
                return;
            }
            final byte pos = MapleInventoryManipulator.addId(c, item.getId(), (short) item.getCount(), null, "Quest Item : Bought from Cash Shop. SN: " + sn);
            if (pos >= 0) {
                chr.gainMeso(-item.getPrice(), false);
                c.getSession().write(MTSCSPacket.showBoughtCSQuestItem(item.getPrice(), (short) item.getCount(), item.getId()));
            } else {
                c.getSession().write(MTSCSPacket.sendCSFail(0x19));
            }
        } else if (action == 6) { // Increase Character Inventory Slots
            c.getSession().write(MTSCSPacket.sendCSFail(0x1A));
        } else if (action == 8) { // Increase Character Slots
            c.getSession().write(MTSCSPacket.sendCSFail(0x1A));
        } else if (action == 9) { // Pendant Slot Expansion
            c.getSession().write(MTSCSPacket.sendCSFail(0x1A));
        } else if (action == 43) { // Received upon entering Cash Shop 
            c.getSession().write(MTSCSPacket.redeemResponse(slea.readInt()));
        } else {
            System.out.println("Unhandled operation found. Remaining: " + slea.toString());
            c.getSession().write(MTSCSPacket.sendCSFail(0));
        }
        doCSPackets(c);
    }

    private static void doCSPackets(final MapleClient c) {
        c.getSession().write(MTSCSPacket.getCSInventory(c));
        c.getSession().write(MTSCSPacket.showNXMapleTokens(c.getPlayer()));
        c.getSession().write(MTSCSPacket.enableCSUse());
        c.getPlayer().getCashInventory().checkExpire(c);
    }
}
