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
package server.maps;

import java.awt.Point;

import client.MapleCharacter;
import handling.world.World;
import java.util.Random;
import server.MapleItemInformationProvider;
import server.Randomizer;
import server.Timer.EventTimer;
import server.life.MapleLifeFactory;
import tools.MaplePacketCreator;

public class AramiaFireWorks {

    public final static int KEG_ID = 4000465, SUN_ID = 4001246;
    public final static int MAX_SUN = 14000;
    public static int MAX_KEGS;
    private short kegs = 0;
    private short sunshines = MAX_SUN / 6; //start at 1/6 then go from that
    private static final AramiaFireWorks instance = new AramiaFireWorks();
    private static final int[] arrayX = {-798, 518, 1686, 3147, 3921, 3885, 5298};
    private static final int[] arrayY = {274, 274, 334, 334, 124, 454, -116};

    public static final AramiaFireWorks getInstance() {
        return instance;
    }
    
    public int getKegsAmount() {
        return kegs;
    }
    
    public void randomizeKegs() {
        int Min = 500,Max = 750;
        MAX_KEGS = Min + (int)(Math.random() * ((Max - Min) + 1));
    }
    
    public int getKegs() {
        return MAX_KEGS;
    }
  
    public int getCollectedKegs() {
        return kegs;
    }

    public final void giveKegs(final MapleCharacter c, final int kegs) {
        if (MAX_KEGS == 0) {
            MAX_KEGS = 500 + Randomizer.nextInt(250);
        }
        this.kegs += kegs;
        if (this.kegs >= MAX_KEGS) {
            this.kegs = 0;
            this.MAX_KEGS = 0;
            broadcastEvent(c);
        }
    }

    private final void broadcastServer(final MapleCharacter c, final int itemid) {
        World.Broadcast.broadcastMessage(MaplePacketCreator.serverNotice(6, itemid, "<Channel " + c.getClient().getChannel() + "> " + c.getMap().getMapName() + " : The amount of {" + MapleItemInformationProvider.getInstance().getName(itemid) + "} has reached the limit!"));
    }

    public final short getKegsPercentage() {
        if (MAX_KEGS == 0) {
            MAX_KEGS = 500 + Randomizer.nextInt(250);
        }
        return (short) ((kegs / MAX_KEGS) * 100);
    }

    private final void broadcastEvent(final MapleCharacter c) {
        broadcastServer(c, KEG_ID);
        // Henesys
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startEvent(c.getClient().getChannelServer().getMapFactory().getMap(100000000));
            }
        }, 10000);
    }


    public final void startEvent(final MapleMap map) {
        map.startMapEffect("Who's going crazy with the monsters?", 5121010);
        randomizeKegs();

        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                spawnMonster(map);
            }
        }, 5000);
    }

    private final void spawnMonster(final MapleMap map) {
        Point pos;
        int spawn;

        for (int i = 0; i < 20; i++) {
            spawn = Randomizer.nextInt(arrayX.length);
            pos = new Point(arrayX[spawn], arrayY[spawn]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(3110300), pos);
        }

        for (int i = 0; i < 75; i++) {
            spawn = Randomizer.nextInt(arrayX.length);
            pos = new Point(arrayX[spawn], arrayY[spawn]);
            map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9400203), pos);
        }

        spawn = Randomizer.nextInt(arrayX.length);
        pos = new Point(arrayX[spawn], arrayY[spawn]);
        map.spawnMonsterOnGroundBelow(MapleLifeFactory.getMonster(9500400), pos);
    }

    public final void giveSuns(final MapleCharacter c, final int kegs) {
        this.sunshines += kegs;
        //have to broadcast a Reactor?
        final MapleMap map = c.getClient().getChannelServer().getMapFactory().getMap(970010000);
        final MapleReactor reactor = map.getReactorByName("mapleTree");
        for (int gogo = kegs + (MAX_SUN / 6); gogo > 0; gogo -= (MAX_SUN / 6)) {
            switch (reactor.getState()) {
                case 0: //first state
                case 1: //first state
                case 2: //first state
                case 3: //first state
                case 4: //first state
                    if (this.sunshines >= (MAX_SUN / 6) * (2 + reactor.getState())) {
                        reactor.setState((byte) (reactor.getState() + 1));
                        reactor.setTimerActive(false);
                        map.broadcastMessage(MaplePacketCreator.triggerReactor(reactor, reactor.getState()));
                    }
                    break;
                default:
                    if (this.sunshines >= (MAX_SUN / 6)) {
                        map.resetReactors(); //back to state 0
                    }
                    break;
            }
        }
        if (this.sunshines >= MAX_SUN) {
            this.sunshines = 0;
            broadcastSun(c);
        }
    }

    public final short getSunsPercentage() {
        return (short) ((sunshines / MAX_SUN) * 100);
    }

   public final short getSunshine() {
       return sunshines;
   }
    
    private final void broadcastSun(final MapleCharacter c) {
        broadcastServer(c, SUN_ID);
        // Henesys Park
        EventTimer.getInstance().schedule(new Runnable() {

            @Override
            public final void run() {
                startSun(c.getClient().getChannelServer().getMapFactory().getMap(970010000));
            }
        }, 10000);
    }

    private final void startSun(final MapleMap map) {
        if (map == null) {
            return;
        }
        map.startMapEffect("The tree is bursting with sunshine!", 5120008);
        for (int i = 0; i < 3; i++) {
            EventTimer.getInstance().schedule(new Runnable() {

                @Override
                public final void run() {
                    spawnItem(map);
                }
            }, 5000 + (i * 10000));
        }
    }
    private static final int[] array_X = {720, 180, 630, 270, 360, 540, 450, 142,
        142, 218, 772, 810, 848, 232, 308, 142};
    private static final int[] array_Y = {1234, 1234, 1174, 1234, 1174, 1174, 1174, 1260,
        1234, 1234, 1234, 1234, 1234, 1114, 1114, 1140};

    private final void spawnItem(final MapleMap map) {
        Point pos;
        for (int i = 0; i < Randomizer.nextInt(5) + 10; i++) {
            pos = new Point(array_X[i], array_Y[i]);
            int rand = Randomizer.nextInt(100);
            int materialid;
            //ItemID calculation start
            if (rand <= 70) {
                materialid = 4010007; // lidium
            } else if (rand >= 71 && rand <= 73) {
                materialid = 4005004; // dark crystal
            } else if (rand >= 74 && rand <= 77) {
                materialid = 4005003; // luk crystal
            } else if (rand >= 78 && rand <= 80) {
                materialid = 4005000; // power crystal
            } else if (rand >= 81 && rand <= 83) {
                materialid = 4005001; // wisdom crystal
            } else if (rand >= 84 && rand <= 87) {
                materialid = 4005002; //dex crystal;
            } else if (rand >= 88 && rand <= 94) {
                materialid = 4021006; // Topaz
            } else {
                materialid = 4021005;
            }
            //ItemID calculation end
            map.spawnAutoDrop(materialid, pos); // drop the material
            
        }
    }
}