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

import constants.GameConstants;
import java.util.ArrayList;
import java.util.List;

import client.ISkill;
import client.MapleClient;
import client.MapleCharacter;
import client.MapleStat;
import client.PlayerStats;
import client.SkillFactory;
import server.AutobanManager;
import server.Randomizer;
import tools.FileoutputUtil;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.data.input.SeekableLittleEndianAccessor;

public class StatsHandling {

    public static final void DistributeAP(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        chr.dropMessage(1, "Please use @str, @dex, @int or @luk commands.");
		c.getSession().write(MaplePacketCreator.enableActions());
    }

    public static final void DistributeSP(final int skillid, final MapleClient c, final MapleCharacter chr) {
        boolean isBeginnerSkill = false;
        final int remainingSp;

        switch (skillid) {
            case 1000:
            case 1001:
            case 1002: {
                final int snailsLevel = chr.getSkillLevel(SkillFactory.getSkill(1000));
                final int recoveryLevel = chr.getSkillLevel(SkillFactory.getSkill(1001));
                final int nimbleFeetLevel = chr.getSkillLevel(SkillFactory.getSkill(1002));
                remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
                isBeginnerSkill = true;
                break;
            }
            case 10001000:
            case 10001001:
            case 10001002: {
                final int snailsLevel = chr.getSkillLevel(SkillFactory.getSkill(10001000));
                final int recoveryLevel = chr.getSkillLevel(SkillFactory.getSkill(10001001));
                final int nimbleFeetLevel = chr.getSkillLevel(SkillFactory.getSkill(10001002));
                remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
                isBeginnerSkill = true;
                break;
            }
            case 20001000:
            case 20001001:
            case 20001002: {
                final int snailsLevel = chr.getSkillLevel(SkillFactory.getSkill(20001000));
                final int recoveryLevel = chr.getSkillLevel(SkillFactory.getSkill(20001001));
                final int nimbleFeetLevel = chr.getSkillLevel(SkillFactory.getSkill(20001002));
                remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
                isBeginnerSkill = true;
                break;
            }
            case 20011000:
            case 20011001:
            case 20011002: {
                final int snailsLevel = chr.getSkillLevel(SkillFactory.getSkill(20011000));
                final int recoveryLevel = chr.getSkillLevel(SkillFactory.getSkill(20011001));
                final int nimbleFeetLevel = chr.getSkillLevel(SkillFactory.getSkill(20011002));
                remainingSp = Math.min((chr.getLevel() - 1), 6) - snailsLevel - recoveryLevel - nimbleFeetLevel;
                isBeginnerSkill = true;
                break;
            }
            case 30001000:
            case 30001001:
            case 30000002: {
                final int snailsLevel = chr.getSkillLevel(SkillFactory.getSkill(30001000));
                final int recoveryLevel = chr.getSkillLevel(SkillFactory.getSkill(30001001));
                final int nimbleFeetLevel = chr.getSkillLevel(SkillFactory.getSkill(30000002));
                remainingSp = Math.min((chr.getLevel() - 1), 9) - snailsLevel - recoveryLevel - nimbleFeetLevel;
                isBeginnerSkill = true; //resist can max ALL THREE
                break;
            }
            default: {
                chr.dropMessage(1, "You may not add this skill.");
                return;
            }
        }
        final ISkill skill = SkillFactory.getSkill(skillid);

        if (skill.hasRequiredSkill()) {
            if (chr.getSkillLevel(SkillFactory.getSkill(skill.getRequiredSkillId())) < skill.getRequiredSkillLevel()) {
                AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to learn a skill without the required skill (" + skillid + ")");
                return;
            }
        }
        final int maxlevel = skill.isFourthJob() ? chr.getMasterLevel(skill) : skill.getMaxLevel();
        final int curLevel = chr.getSkillLevel(skill);

        if (skill.isInvisible() && chr.getSkillLevel(skill) == 0) {
            if ((skill.isFourthJob() && chr.getMasterLevel(skill) == 0) || (!skill.isFourthJob() && maxlevel < 10 && !isBeginnerSkill)) {
                //AutobanManager.getInstance().addPoints(c, 1000, 0, "Illegal distribution of SP to invisible skills (" + skillid + ")");
                return;
            }
        }
        if ((remainingSp > 0 && (curLevel + 1) <= maxlevel) && skill.canBeLearnedBy(chr.getJob())) {
            c.getSession().write(MaplePacketCreator.updateSp(chr, false));
            chr.changeSkillLevel(skill, (byte) (curLevel + 1), chr.getMasterLevel(skill));
        } else if (!skill.canBeLearnedBy(chr.getJob())) {
            AutobanManager.getInstance().addPoints(c, 1000, 0, "Trying to learn a skill for a different job (" + skillid + ")");
        }
    }

    public static final void AutoAssignAP(final SeekableLittleEndianAccessor slea, final MapleClient c, final MapleCharacter chr) {
        chr.dropMessage(1, "This function is disabled.");
		c.getSession().write(MaplePacketCreator.enableActions());
    }
}
