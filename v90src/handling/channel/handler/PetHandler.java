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

import client.inventory.IItem;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleDisease;
import client.inventory.MapleInventoryType;
import client.inventory.MaplePet;
import constants.GameConstants;
import client.inventory.PetCommand;
import client.inventory.PetDataFactory;
import handling.world.party.MaplePartyCharacter;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import server.Randomizer;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.life.MapleMonster;
import server.movement.LifeMovementFragment;
import server.maps.FieldLimitType;
import server.maps.MapleMapItem;
import server.quest.custom.CustomQuestProvider;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.packet.PetPacket;
import tools.data.input.SeekableLittleEndianAccessor;

public class PetHandler {

    public static final void SpawnPet(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        chr.updateTick(slea.readInt());
        chr.spawnPet(slea.readByte(), slea.readByte() > 0);
    }

    public static final void Pet_AutoPotion(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final int petid = (int) slea.readLong();
        slea.skip(1);
        c.getPlayer().updateTick(slea.readInt());
        final byte slot = (byte) slea.readShort();
        final int itemId = slea.readInt();
        if (chr == null || !chr.isAlive() || chr.getPetIndex(petid) < 0 || chr.getMap() == null || chr.getMapId() == 749040100 || chr.getMap() == null || chr.hasDisease(MapleDisease.POTION)) {
            return;
        }
        final IItem toUse = chr.getInventory(MapleInventoryType.USE).getItem(slot);
        if (toUse == null || toUse.getQuantity() < 1 || toUse.getItemId() != itemId) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        final long time = System.currentTimeMillis();
        if (chr.getNextConsume() > time) {
            chr.dropMessage(5, "You may not use this item yet.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        if (!FieldLimitType.PotionUse.check(chr.getMap().getFieldLimit()) || chr.getMapId() == 610030600) { //cwk quick hack
            if (MapleItemInformationProvider.getInstance().getItemEffect(toUse.getItemId()).applyTo(chr)) {
                MapleInventoryManipulator.removeFromSlot(c, MapleInventoryType.USE, slot, (short) 1, false);
                if (chr.getMap().getConsumeItemCoolTime() > 0) {
                    chr.setNextConsume(time + (chr.getMap().getConsumeItemCoolTime() * 1000));
                }
            }
        } else {
            c.getSession().write(MaplePacketCreator.enableActions());
        }
    }

    public static final void PetChat(final int petid, final short command, final String text, MapleCharacter chr) {
        if (chr == null || chr.getMap() == null || chr.getPetIndex(petid) < 0) {
            return;
        }
        chr.getMap().broadcastMessage(chr, PetPacket.petChat(chr.getId(), command, text, chr.getPetIndex(petid)), true);
    }

    public static final void PetCommand(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        final byte petIndex = chr.getPetIndex((int) slea.readLong());
        if (petIndex == -1) {
            return;
        }
        MaplePet pet = chr.getPet(petIndex);
        if (pet == null) {
            return;
        }
        slea.skip(1);
        byte command = slea.readByte();
        if (pet.getPetItemId() == 5000042 && command == 1) {
            command = 9; // kino bug (sit replace with poop)
        }
        final PetCommand petCommand = PetDataFactory.getPetCommand(pet.getPetItemId(), (int) command);
        if (petCommand == null) {
            return;
        }

        boolean success = false;
        if (Randomizer.nextInt(99) <= petCommand.getProbability()) {
            success = true;
            if (pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + petCommand.getIncrease();
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);
                    c.getSession().write(PetPacket.showOwnPetLevelUp(petIndex));
                    chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, petIndex));
                }
                c.getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            }
        }
        chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), command, petIndex, success, false), true);
    }

    public static final void PetFood(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        int previousFullness = 100;
        MaplePet pet = null;
        if (chr == null) {
            return;
        }
        for (final MaplePet pets : chr.getPets()) {
            if (pets.getSummoned()) {
                if (pets.getFullness() < previousFullness) {
                    previousFullness = pets.getFullness();
                    pet = pets;
                }
            }
        }
        if (pet == null) {
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }

        slea.skip(6);
        final int itemId = slea.readInt();

        boolean gainCloseness = false;

        if (Randomizer.nextInt(99) <= 50) {
            gainCloseness = true;
        }
        if (pet.getFullness() < 100) {
            int newFullness = pet.getFullness() + 30;
            if (newFullness > 100) {
                newFullness = 100;
            }
            pet.setFullness(newFullness);
            final byte index = chr.getPetIndex(pet);

            if (gainCloseness && pet.getCloseness() < 30000) {
                int newCloseness = pet.getCloseness() + 1;
                if (newCloseness > 30000) {
                    newCloseness = 30000;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness >= GameConstants.getClosenessNeededForLevel(pet.getLevel() + 1)) {
                    pet.setLevel(pet.getLevel() + 1);

                    c.getSession().write(PetPacket.showOwnPetLevelUp(index));
                    chr.getMap().broadcastMessage(PetPacket.showPetLevelUp(chr, index));
                }
            }
            c.getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            chr.getMap().broadcastMessage(c.getPlayer(), PetPacket.commandResponse(chr.getId(), (byte) 1, index, true, true), true);
        } else {
            if (gainCloseness) {
                int newCloseness = pet.getCloseness() - 1;
                if (newCloseness < 0) {
                    newCloseness = 0;
                }
                pet.setCloseness(newCloseness);
                if (newCloseness < GameConstants.getClosenessNeededForLevel(pet.getLevel())) {
                    pet.setLevel(pet.getLevel() - 1);
                }
            }
            c.getSession().write(PetPacket.updatePet(pet, chr.getInventory(MapleInventoryType.CASH).getItem((byte) pet.getInventoryPosition())));
            chr.getMap().broadcastMessage(chr, PetPacket.commandResponse(chr.getId(), (byte) 1, chr.getPetIndex(pet), false, true), true);
        }
        MapleInventoryManipulator.removeById(c, MapleInventoryType.USE, itemId, 1, true, false);
        c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void MovePet(final SeekableLittleEndianAccessor slea, final MapleCharacter chr) {
        final int petId = slea.readInt();
        slea.skip(12);
        final List<LifeMovementFragment> res = MovementParse.parseMovement(slea, 3);

        if (res != null && chr != null && res.size() != 0) { // map crash hack
            final byte slot = chr.getPetIndex(petId);
            if (slot == -1) {
                return;
            }
            chr.getPet(slot).updatePosition(res);
            chr.getMap().broadcastMessage(chr, PetPacket.movePet(chr.getId(), petId, slot, res), false);
        }
    }

    public static void PetIgnoreTag(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) { // 170001
        final int petId = (int) slea.readLong();
        if (chr == null || chr.getMap() == null || chr.getPetIndex(petId) < 0) {
            return;
        }
        final MaplePet pet = chr.getPetByUID(petId);
        if (pet == null) {
            return;
        }
        if (chr.getPetIndex(petId) != 0) {
            c.getPlayer().dropMessage(1, "Only Boss Pets can use this.");
            c.getSession().write(MaplePacketCreator.enableActions());
            return;
        }
        // Store in quest data will do
        final byte size = slea.readByte();
        if (size <= 0) {
            chr.removeCustomQuest(CustomQuestProvider.getInstance(170001));
        } else {
            final StringBuilder st = new StringBuilder();
            for (int i = 0; i < size; i++) {
                if (i > 10) {
                    break;
                }
                st.append(slea.readInt()).append(",");
            }
            st.deleteCharAt(st.length() - 1);
            chr.getCustomQuest(CustomQuestProvider.getInstance(170001)).setCustomData(st.toString());
        }
    }
}
