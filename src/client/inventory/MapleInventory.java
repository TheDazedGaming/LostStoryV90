/*
 * This file is part of the OdinMS Maple Story Server
 * Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
 * Matthias Butz <matze@odinms.de>
 * Jan Christian Meyer <vimes@odinms.de>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation version 3 as published by
 * the Free Software Foundation. You may not use, modify or distribute
 * this program under any other version of the GNU Affero General Public
 * License.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package client.inventory;

import java.util.*;

import client.MapleClient;
import constants.ItemConstants;
import server.ItemInformationProvider;

/**
 * @author Matze
 */
public class MapleInventory implements Iterable<Item>{

	private Map<Short, Item> inventory = new LinkedHashMap<>();
	private byte slotLimit;
	private MapleInventoryType type;
	private boolean checked = false;

	public MapleInventory(MapleInventoryType type, byte slotLimit){
		this.inventory = new LinkedHashMap<>();
		this.type = type;
		this.slotLimit = slotLimit;
	}

	public boolean isExtendableInventory(){ // not sure about cash, basing this on the previous one.
		return !(type.equals(MapleInventoryType.UNDEFINED) || type.equals(MapleInventoryType.EQUIPPED) || type.equals(MapleInventoryType.CASH));
	}

	public boolean isEquipInventory(){
		return type.equals(MapleInventoryType.EQUIP) || type.equals(MapleInventoryType.EQUIPPED);
	}

	public byte getSlotLimit(){
		return slotLimit;
	}

	public void setSlotLimit(int newLimit){
		slotLimit = (byte) newLimit;
	}

	public Item findById(int itemId){
		for(Item item : inventory.values()){
			if(item.getItemId() == itemId) return item;
		}
		return null;
	}

	public int countById(int itemId){
		int possesed = 0;
		for(Item item : inventory.values()){
			if(item.getItemId() == itemId){
				possesed += item.getQuantity();
			}
		}
		return possesed;
	}

	public List<Item> listById(int itemId){
		List<Item> ret = new ArrayList<>();
		for(Item item : inventory.values()){
			if(item.getItemId() == itemId){
				ret.add(item);
			}
		}
		if(ret.size() > 1){
			Collections.sort(ret);
		}
		return ret;
	}

	public Collection<Item> list(){
		return inventory.values();
	}

	public short addItem(Item item){
		short slotId = getNextFreeSlot();
		if(slotId < 0 || item == null) return -1;
		inventory.put(slotId, item);
		item.setPosition(slotId);
		return slotId;
	}

	public void addFromDB(Item item){
		if(item.getPosition() < 0 && !type.equals(MapleInventoryType.EQUIPPED)) return;
		inventory.put(item.getPosition(), item);
	}

	public void move(short sSlot, short dSlot, short slotMax){
		Item source = (Item) inventory.get(sSlot);
		Item target = (Item) inventory.get(dSlot);
		if(source == null) return;
		if(target == null){
			source.setPosition(dSlot);
			inventory.put(dSlot, source);
			inventory.remove(sSlot);
		}else if(target.getItemId() == source.getItemId() && !ItemConstants.isRechargable(source.getItemId()) && source.getExpiration() == target.getExpiration()){
			if(type.getType() == MapleInventoryType.EQUIP.getType()){
				swap(target, source);
			}
			if(source.getQuantity() + target.getQuantity() > slotMax){
				short rest = (short) ((source.getQuantity() + target.getQuantity()) - slotMax);
				source.setQuantity(rest);
				target.setQuantity(slotMax);
			}else{
				target.setQuantity((short) (source.getQuantity() + target.getQuantity()));
				inventory.remove(sSlot);
				ItemFactory.deleteItem(source);
			}
		}else{
			swap(target, source);
		}
	}

	private void swap(Item source, Item target){
		inventory.remove(source.getPosition());
		inventory.remove(target.getPosition());
		short swapPos = source.getPosition();
		source.setPosition(target.getPosition());
		target.setPosition(swapPos);
		inventory.put(source.getPosition(), source);
		inventory.put(target.getPosition(), target);
	}

	public Item getItem(short slot){
		return inventory.get(slot);
	}

	public void removeItem(short slot){
		removeItem(slot, (short) 1, false);
	}

	public void removeItem(short slot, short quantity, boolean allowZero){
		Item item = inventory.get(slot);
		if(item == null){// TODO is it ok not to throw an exception here?
			return;
		}
		item.setQuantity((short) (item.getQuantity() - quantity));
		if(item.getQuantity() < 0){
			item.setQuantity((short) 0);
		}
		if(item.getQuantity() == 0 && !allowZero){
			removeSlot(slot);
		}
	}

	public void removeSlot(short slot){
		inventory.remove(slot);
	}

	public boolean isFull(){
		return inventory.size() >= slotLimit;
	}

	public boolean isFull(int margin){
		return inventory.size() + margin >= slotLimit;
	}

	public boolean hasRoom(MapleClient c, Item item){
		int itemid = item.getItemId();
		int quantity = item.getQuantity();
		ItemInformationProvider ii = ItemInformationProvider.getInstance();
		double max = ii.getItemData(itemid).getSlotMax(c);// making it a double for math later
		if(isFull() && max == 1) return false;// if the inventory is full, and the maxstack is 1, we can't add it
		boolean rechargeable = ItemConstants.isRechargable(itemid);
		if(isFull() && rechargeable) return false;// bullet/star - Same as ^. stars/bullets don't stack
		int newQ = quantity;
		for(Item i : listById(itemid)){// check to see if any room exists
			if(i.softEquals(item)){
				int qLeft = (int) (max - i.getQuantity());
				newQ -= qLeft;
			}
		}
		if(newQ <= 0) return true;// if the current items can store all the quantity
		double slotsRequired = newQ / max;
		double slotsAvailable = getNumFreeSlot();
		if(slotsAvailable >= slotsRequired) return true;
		return false;
	}

	public short getNextFreeSlot(){
		if(isFull()) return -1;
		for(short i = 1; i <= slotLimit; i++){
			if(!inventory.keySet().contains(i)) return i;
		}
		return -1;
	}

	public short getNumFreeSlot(){
		if(isFull()) return 0;
		short free = 0;
		for(short i = 1; i <= slotLimit; i++){
			if(!inventory.keySet().contains(i)){
				free++;
			}
		}
		return free;
	}

	public MapleInventoryType getType(){
		return type;
	}

	@Override
	public Iterator<Item> iterator(){
		return Collections.unmodifiableCollection(inventory.values()).iterator();
	}

	public Collection<MapleInventory> allInventories(){
		return Collections.singletonList(this);
	}

	public Item findByCashId(int cashId){
		boolean isRing = false;
		Equip equip = null;
		for(Item item : inventory.values()){
			if(item.getType() == MapleInventoryType.EQUIP.getType()){
				equip = (Equip) item;
				isRing = equip.getRingId() > -1;
			}
			if((item.getPetId() > -1 ? item.getPetId() : isRing ? equip.getRingId() : item.getCashId()) == cashId) return item;
		}
		return null;
	}

	public boolean checked(){
		return checked;
	}

	public void checked(boolean yes){
		checked = yes;
	}
}