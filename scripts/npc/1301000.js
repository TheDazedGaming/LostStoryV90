var coconutid = 4000465; 
var lidiumid = 4010007;
var price = [1, 1, 6];
var maxstats = false;
var status = -1;
var PACKETALERTER;
var maxstatus = false;
var max = 32767;

function start() {
    action(1,0,0);
}

function action(m,t,s) {
    if (m != 1) {
		cm.dispose();
		return;
    } else {
        status++;
    }
    if (status == 0) {
        cm.sendSimple("#eHello #b#h ##k, i am the voting points npc. Have you voted for the server ? If so, you can spend your points here. If you just voted and have not received your points, you most likely didn't disconnect. So, would you like to trade some points?\r\n\r\nYou currently have #b"+cm.getVPoints()+" #kvoting points.\r\n\r\n#r#L0#1 vote point - 15 coconuts#v"+coconutid+"##l\r\n#L1#1 vote point - 5 lidium#v"+lidiumid+"##l\r\n#L2#6 vote points - Max any stats you want (str,int,dex,luk)");
	} else if (status == 1) {
        if (cm.getVPoints() >= 1) {
            if (s == 0) {
                cm.gainItem(coconutid, 15);
                cm.gainVPoints(-price[s]);
				cm.sendOk("#eThanks! 1 point has been deducted from your vote points!");
            } else if (s == 1) {
                cm.gainItem(lidiumid, 5);
                cm.gainVPoints(-price[s]);
				cm.sendOk("#eThanks! 1 point has been deducted from your vote points!");
            } else if (s == 2) {
				if (cm.getVPoints() >= 6) {
					maxstatus = true;
					//cm.sendOk("#eThis feature has been disabled for now as i didn't have time to fix it. It will be fixed very soon!\r\n\r\n#b- 26/01/2012");
					cm.sendSimple("#eWhich stats would you like to max?\r\n\r\n#r#L15#Str#l\r\n#L16#Int#l\r\n#L17#Dex#l\r\n#L18#Luk#l");
				} else {
					cm.sendOk("#eHEY. We don't give free stats. Sorry.");
					cm.dispose();
				}
            }
        } else {
                cm.sendOk("#eLooks like you're missing some voting points for that!");
                cm.dispose();
            }
    } else if (status == 2) {
		if (maxstatus == true) {
			if (s == 15) {
				if (cm.getPlayer().getStat().getStr() >= 32767) {
					cm.gainVPoints(-price[2]);
					cm.getPlayer().getStat().setStr(max);
					cm.sendOk("#eYour strength has been maxed!");
					cm.dispose();
				} else {
					cm.sendOk("#eYour strength has already been maxed!");
					cm.dispose();
				}
			} else if (s == 16) {
				if (cm.getPlayer().getStat().getInt() >= 32767) {
					cm.gainVPoints(-price[2]);
					ccm.getPlayer().getStat().setInt(max);
					cm.sendOk("#eYour Intelligence has been maxed!");
					cm.dispose();
				} else {
					cm.sendOk("#eYour intelligence has already been maxed!");
					cm.dispose();
				}
			} else if (s == 17) {
				if (cm.getPlayer().getStat().getDex() >= 32767) {
					cm.gainVPoints(-price[2]);
					cm.getPlayer().getStat().setDex(max);
					cm.sendOk("#eYour Dexterity has been maxed!");
					cm.dispose();
				} else {
					cm.sendOK("#eYour Dexterity has already been maxed!");
					cm.dispose();
				}
			} else if (s == 18) {
				if (cm.getPlayer().getStat().getStr() >= 32767) {
					cm.gainVPoints(-price[2]);
					cm.getPlayer().getStat().setLuk(max);
					cm.sendOk("#eYour Luck has been maxed!");
					cm.dispose();
				} else {
					cm.sendOk("#eYour luk has already been maxed!");
					cm.dispose();
				}
			}
		} else {
			for (i = 0; i > 100; i++)  {
				PACKETALERTER += "INVALID_, PACKETALERT. print DO NOT PACKET EDIT ON TROPIKMS.";
				}
            cm.sendOk(PACKETALERTER);
        }
	}
	cm.dispose();
}