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
package handling.channel.handler;

import client.MapleCharacter;
import client.MapleClient;
import handling.world.party.MapleParty;
import handling.world.party.MaplePartyCharacter;
import handling.world.World;
import handling.world.expedition.MapleExpedition;
import tools.data.input.SeekableLittleEndianAccessor;
import tools.packet.MapleUserPackets;

public class PartyHandler {

    public enum PartyOperation {

        JOIN, LEAVE, MOVE_MEMBER, EXPEL, DISBAND, DISBAND_IN_EXPEDITION, SILENT_UPDATE, LOG_ONOFF, CHANGE_LEADER, CHANGE_LEADER_DC
    }
    public static final byte // Party actions
            BEGINNER_NO_PARTY = 0x0A, // A beginner can't create a party.
            NOT_IN_PARTY = 0x0D, // You have yet to join a party.
            JOINED_PARTY = 0x10, // You have joined the party.
            ALREADY_JOINED = 0x11, // Already have joined a party.
            PARTY_FULL = 0x12, // The party you're trying to join is already in full capacity.
            INVITE_MSG = 0x16, // You have invited <name> to your party. (Popup)
            NO_EXPEL = 0x1D, // Cannot kick another user in this map | Expel function is not available in this map.
            NOT_SAME_MAP = 0x20, // This can only be given to a party member within the vicinity. | The Party Leader can only be handed over to the party member in the same map.
            FAILED_TO_HAND_OVER = 0x21, // Unable to hand over the leadership post; No party member is currently within the vicinity of the party leader | There is no party member in the same field with party leader for the hand over.
            NOT_SAME_MAP1 = 0x22, // You may only change with the party member that's on the same channel. | You can only hand over to the party member within the same map.
            NO_GM_CREATES = 0x24, // As a GM, you're forbidden from creating a party.
            NON_EXISTANT = 0x25; // Unable to find the character.

    public static void PartyResponse(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        //System.out.println("party response..." + slea.toString());
        final int action = slea.readByte();
        final int partyid = slea.readInt();
        final MapleCharacter chr = c.getPlayer();
        if (chr.getParty() != null) {
            chr.dropMessage(5, "You can't join the party as you are already in one.");
            return;
        }

        final MapleParty party = World.Party.getParty(partyid);
        if (party == null) {
            if (action == 0x1B) {
                chr.dropMessage(5, "The party you are trying to join does not exist.");
            }
            return;
        }
        if (party != null && party.getExpeditionId() > 0) {
            chr.dropMessage(5, "The party you are trying to join does not exist.");
            return;
        }
        if (action == 0x1B) { // Accept
            if (party.getMembers().size() < 6) {
                chr.setParty(party);
                World.Party.updateParty(partyid, PartyOperation.JOIN, new MaplePartyCharacter(chr));
                chr.receivePartyMemberHP();
                chr.updatePartyMemberHP();
            } else {
                c.getSession().write(MapleUserPackets.partyStatusMessage(PARTY_FULL));
            }
        } else {
            final MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(party.getLeader().getId());
            if (cfrom != null) {
                if (action == 0x16) {
                    cfrom.getClient().getSession().write(MapleUserPackets.partyStatusMessage(INVITE_MSG, chr.getName()));
                } else if (action == 0x19) {
                    cfrom.dropMessage(5, "You have already invited '" + chr.getName() + "' to your party.");
                } else { // Deny
                    cfrom.dropMessage(5, chr.getName() + " have denied request to the party.");
                }
            }
        }
    }

    public static void PartyOperation(final SeekableLittleEndianAccessor slea, final MapleClient c) {
        //System.out.println("party operation..." + slea.toString());

        final int operation = slea.readByte();
        final MapleCharacter chr = c.getPlayer();
        final MaplePartyCharacter partyplayer = new MaplePartyCharacter(chr);
        MapleParty party = chr.getParty();

        switch (operation) {
            case 1: // Create
                if (((chr.getJob() == 0 || chr.getJob() == 1000 || chr.getJob() == 2000 || chr.getJob() == 2001) && chr.getLevel() <= 10) || chr.getLevel() <= 10) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(BEGINNER_NO_PARTY));
                    return;
                }
                if (chr.getParty() == null) {
                    party = World.Party.createParty(partyplayer);
                    if (party == null) {
                        c.getSession().write(MapleUserPackets.partyStatusMessage(NOT_IN_PARTY));
                        return;
                    }
                    chr.setParty(party);
                    c.getSession().write(MapleUserPackets.partyCreated(party.getId()));
                } else {
                    if (party.getExpeditionId() > 0) {
                        chr.dropMessage(5, "You may not do party operations while in a raid.");
                        return;
                    }
                    if (partyplayer.equals(party.getLeader()) && party.getMembers().size() == 1) { //only one, reupdate
                        c.getSession().write(MapleUserPackets.partyCreated(party.getId()));
                    } else {
                        c.getSession().write(MapleUserPackets.partyStatusMessage(ALREADY_JOINED));
                    }
                }
                break;
            case 2: // Leave
                if (party != null) {
                    if (party.getExpeditionId() > 0) {
                        final MapleExpedition exped1 = World.Party.getExped(party.getExpeditionId());
                        if (exped1 != null) {
                            if (exped1.getLeader() == chr.getId()) {
                                World.Party.expedPacket(exped1.getId(), MapleUserPackets.removeExpedition(64), null);
                                World.Party.disbandExped(exped1.getId());
                                if (chr.getEventInstance() != null) {
                                    chr.getEventInstance().disbandParty();
                                }
                            } else {
                                if (party.getLeader().getId() == chr.getId()) {
                                    World.Party.updateParty(party.getId(), PartyOperation.DISBAND_IN_EXPEDITION, new MaplePartyCharacter(chr));
                                    if (chr.getEventInstance() != null) {
                                        chr.getEventInstance().disbandParty();
                                    }
                                    World.Party.expedPacket(exped1.getId(), MapleUserPackets.showExpedition(exped1, false, true), null);
                                } else {
                                    World.Party.updateParty(party.getId(), PartyOperation.LEAVE, new MaplePartyCharacter(chr));
                                    if (chr.getEventInstance() != null) {
                                        chr.getEventInstance().leftParty(chr);
                                    }
                                }
                            }
                            if (chr.getPyramidSubway() != null) {
                                chr.getPyramidSubway().fail(chr);
                            }
                        }
                    } else {
                        if (partyplayer.equals(party.getLeader())) { // disband
                            World.Party.updateParty(party.getId(), PartyOperation.DISBAND, partyplayer);
                            if (chr.getEventInstance() != null) {
                                chr.getEventInstance().disbandParty();
                            }
                            if (chr.getPyramidSubway() != null) {
                                chr.getPyramidSubway().fail(chr);
                            }
                        } else {
                            World.Party.updateParty(party.getId(), PartyOperation.LEAVE, partyplayer);
                            if (chr.getEventInstance() != null) {
                                chr.getEventInstance().leftParty(chr);
                            }
                            if (chr.getPyramidSubway() != null) {
                                chr.getPyramidSubway().fail(chr);
                            }
                        }
                    }
                } else {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(NOT_IN_PARTY));
                }
                chr.setParty(null);
                break;
            case 4: // Invite
                final String name = slea.readMapleAsciiString();
                final MapleCharacter invited = c.getChannelServer().getPlayerStorage().getCharacterByName(name);
                if (invited == null) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(NON_EXISTANT));
                    return;
                }
                if (invited.getParty() != null) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(ALREADY_JOINED));
                    return;
                }
                if (party == null) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(NOT_IN_PARTY));
                    return;
                }
                if (party.getExpeditionId() > 0) {
                    chr.dropMessage(5, "You cannot send an invite when you are in a raid.");
                    return;
                }
                if (party.getMembers().size() < 6) {
                    invited.getClient().getSession().write(MapleUserPackets.partyInvite(chr));
                } else {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(PARTY_FULL)); // Full capacity
                }
                break;
            case 5: // Expel
                final int cid = slea.readInt();
                if (party == null || partyplayer == null) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(NOT_IN_PARTY));
                    return;
                }
                if (!partyplayer.equals(party.getLeader())) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(NO_EXPEL));
                    return;
                }
                if (party.getExpeditionId() > 0) {
                    chr.dropMessage(5, "You may not do party operations while in a raid.");
                    return;
                }
                final MaplePartyCharacter expelled = party.getMemberById(cid);
                if (expelled != null) { // todo: add map field limit check
                    World.Party.updateParty(party.getId(), PartyOperation.EXPEL, expelled);
                    if (chr.getEventInstance() != null && expelled.isOnline()) {
                        chr.getEventInstance().disbandParty();
                    }
                    if (chr.getPyramidSubway() != null && expelled.isOnline()) {
                        chr.getPyramidSubway().fail(chr);
                    }
                }
                break;
            case 6: // Change leader
                final int newLeader = slea.readInt();
                if (party == null) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(NOT_IN_PARTY));
                    return;
                }
                if (!partyplayer.equals(party.getLeader())) {
                    c.getSession().write(MapleUserPackets.partyStatusMessage(NO_EXPEL));
                    return;
                }
                if (party.getExpeditionId() > 0) {
                    chr.dropMessage(5, "You may not do party operations while in a raid.");
                    return;
                }
                final MaplePartyCharacter newLeadr = party.getMemberById(newLeader);
                final MapleCharacter cfrom = c.getChannelServer().getPlayerStorage().getCharacterById(newLeader);
                if (newLeadr != null && cfrom.getMapId() == chr.getMapId()) { // todo: add map field limit check
                    World.Party.updateParty(party.getId(), PartyOperation.CHANGE_LEADER, newLeadr);
                } else {
                    chr.dropMessage(5, "The Party Leader can only be handed over to the party member in the same map.");
                }
                break;
            default:
                System.out.println("Unhandled Party function." + operation);
                break;
        }
    }
}
