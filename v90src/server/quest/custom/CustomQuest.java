/*
 * This file is part of the OdinMS MapleStory Private Server
 * Copyright (C) 2011 Patrick Huy and Matthias Butz
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package server.quest.custom;

import client.MapleCharacter;
import client.MapleCustomQuestStatus;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tools.MaplePacketCreator;
import tools.Pair;

/**
 *
 * @author AuroX
 * Character type, Holds data of custom quests only. (Per quest). Clone of MapleQuest.java
 */
// TODO: linked quest, job checks, restore lost item
public class CustomQuest {

    private final int id, skillId;
    private final boolean repeatable;
    private final Map<Integer, Integer> reqMobs; // Mob id, quantity
    private final Map<Integer, Integer> reqItems; // Item id, quantity
    private List<CustomQuestRequirement> startReqs;
    private List<CustomQuestRequirement> completeReqs;

    protected CustomQuest(int id, int skillId, boolean repeatable, List<CustomQuestRequirement> startReqs, List<CustomQuestRequirement> completeReqs) {
        this.id = id;
        this.skillId = skillId;
        this.repeatable = repeatable;
        this.reqMobs = new LinkedHashMap<>();
        this.reqItems = new LinkedHashMap<>();
        this.startReqs = startReqs;
        this.completeReqs = completeReqs;
        for (CustomQuestRequirement cqr : completeReqs) {
            if (cqr.getType().equals(CustomQuestRequirementType.mob)) {
                List<Pair<Integer, Integer>> mobList = cqr.getDataStore();
                for (Pair<Integer, Integer> mobs : mobList) {
                    reqMobs.put(mobs.getLeft(), mobs.getRight());
                }
            } else if (cqr.getType().equals(CustomQuestRequirementType.item)) {
                List<Pair<Integer, Integer>> itemss = cqr.getDataStore();
                for (Pair<Integer, Integer> it : itemss) {
                    reqItems.put(it.getLeft(), it.getRight());
                }
            }
        }
    }

    public int getQuestId() {
        return id;
    }

    public int getSkillId() {
        return skillId;
    }

    public Map<Integer, Integer> getReqMobs() {
        return reqMobs;
    }

    public Map<Integer, Integer> getReqItems() {
        return reqItems;
    }

    public boolean canStart(MapleCharacter c, Integer npcid) {
        if (c.getCustomQuest(this).getStatus() != 0 && !(c.getCustomQuest(this).getStatus() == 2 && repeatable)) {
            return false;
        }
        for (CustomQuestRequirement r : startReqs) {
            if (!r.check(c, npcid)) {
                return false;
            }
        }
        return true;
    }

    public boolean canComplete(MapleCharacter c, Integer npcid) {
        if (c.getCustomQuest(this).getStatus() != 1) {
            return false;
        }
        for (CustomQuestRequirement r : completeReqs) {
            if (!r.check(c, npcid)) {
                return false;
            }
        }
        return true;
    }

    public boolean start(MapleCharacter c, int npc) {
        if (canStart(c, npc)) {
            final MapleCustomQuestStatus newStatus = new MapleCustomQuestStatus(this, (byte) 1, npc);
            newStatus.setCompletionTime(c.getCustomQuest(this).getCompletionTime());
            c.updateCustomQuest(newStatus);
            return true;
        }
        return false;
    }

    public boolean complete(MapleCharacter c, int npc) {
        if (canComplete(c, npc)) {
            final MapleCustomQuestStatus newStatus = new MapleCustomQuestStatus(this, (byte) 2, npc);
            c.updateCustomQuest(newStatus);
			//c.getClient().getSession().write(MaplePacketCreator.playSound("Dojan/clear"));
            //c.getClient().getSession().write(MaplePacketCreator.showEffect("praid/clear"));
            c.getClient().getSession().write(MaplePacketCreator.showEffect("praid/clear")); // Quest completion
            return true;
        }
        return false;
    }
}
