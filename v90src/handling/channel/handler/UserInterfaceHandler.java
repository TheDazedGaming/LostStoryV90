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

import client.MapleCharacter;
import client.MapleClient;
import client.MapleCharacterUtil;
import constants.ServerConstants;
import handling.channel.ChannelServer;
import scripting.NPCScriptManager;
import scripting.EventManager;
import server.Timer.EventTimer;
import tools.MaplePacketCreator;
import tools.data.input.SeekableLittleEndianAccessor;

public class UserInterfaceHandler {

    public static final void CygnusSummon_NPCRequest(final MapleClient c) {
        if (c.getPlayer().getJob() == 2000) {
            NPCScriptManager.getInstance().start(c, 1202000);
        } else if (c.getPlayer().getJob() == 1000) {
            NPCScriptManager.getInstance().start(c, 1101008);
        }
    }

//    public static final void LuckyLogout(final SeekableLittleEndianAccessor slea, final MapleClient c) {
//        // this is only called the when player log outs, i've no idea how to make it disappear, but oh well..just dc the client, since
//        // they attempted to log out
//        if (ServerConstants.LuckyLogoutGift) {
//            final int selection = slea.readInt();
//            // if (selected) { // return / dc.
//            final MapleCharacter player = c.getPlayer();
//            if (selection >= 0 && selection <= 2) { // 0, 1 or 2
//                // gifting here..etc, i don't have the official one, so i'll use my own.
//                // add a check to the disconnect function to see if the char have any logout gift, and keep shows till the user selected
//                // reload every 1 day, or 3 days?
//                // I have to use a custom one.
//                // god..how to make the box disappear? ...
//                c.getPlayer().dropMessage(1, "hmm");
//                // I'm sure there's a packet to make the box disappear ==
//                c.getSession().write(MaplePacketCreator.luckyLogoutGift((byte) 1, "70000393")); // sn?
//            }
//        }
//    }

    public static final void ShipObjectRequest(final int mapid, final MapleClient c) {
        // BB 00 6C 24 05 06 00 - Ellinia
        // BB 00 6E 1C 4E 0E 00 - Leafre

        EventManager em;
        int effect = 3; // 1 = Coming, 3 = going, 1034 = balrog

        switch (mapid) {
            case 101000300: // Ellinia Station >> Orbis
            case 200000111: // Orbis Station >> Ellinia
                em = c.getChannelServer().getEventSM().getEventManager("Boats");
                if (em != null && em.getProperty("docked").equals("true")) {
                    effect = 1;
                }
                break;
            case 200000121: // Orbis Station >> Ludi
            case 220000110: // Ludi Station >> Orbis
                em = c.getChannelServer().getEventSM().getEventManager("Trains");
                if (em != null && em.getProperty("docked").equals("true")) {
                    effect = 1;
                }
                break;
            case 200000151: // Orbis Station >> Ariant
            case 260000100: // Ariant Station >> Orbis
                em = c.getChannelServer().getEventSM().getEventManager("Geenie");
                if (em != null && em.getProperty("docked").equals("true")) {
                    effect = 1;
                }
                break;
            case 240000110: // Leafre Station >> Orbis
            case 200000131: // Orbis Station >> Leafre
                em = c.getChannelServer().getEventSM().getEventManager("Flight");
                if (em != null && em.getProperty("docked").equals("true")) {
                    effect = 1;
                }
                break;
            case 200090010: // During the ride to Orbis
            case 200090000: // During the ride to Ellinia
                em = c.getChannelServer().getEventSM().getEventManager("Boats");
                if (em != null && em.getProperty("haveBalrog").equals("true")) {
                    effect = 1;
                } else {
                    return; // shyt, fixme!
                }
                break;
            default:
                System.out.println("Unhandled ship object, MapID : " + mapid);
                break;
        }
        c.getSession().write(MaplePacketCreator.boatPacket(effect));
    }
}
