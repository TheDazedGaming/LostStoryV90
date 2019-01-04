var coconut = 4000465;
var status = -1;

function action(mode, type, selection) {
	if (mode == 1) {
		status++;
	} else {
		cm.dispose();
		return;
	}
	if (status == 0) {
		cm.sendSimple("#e#rHello "+cm.getName()+" and welcome to the monster bang system. Players can donate coconut and after a certain amount of them is reached, monster will spawn everywhere in Henesys. The required amount is alway a minimum of 500 and a maximum of 750, which is set at the server startup and when this amount is reached.\r\n \n\r #b#L0# Here, I brought the coconuts.#l#k\r\n#b#L1# Please show me the current status on collecting the coconuts.#l#k");
	} else if (status == 1) {
		if (selection == 1) {
			cm.sendNext("#e#r"+cm.getKegs()+"#k #k/ #b"+cm.getRequiredKegs()+"");
			cm.safeDispose();
		} else if (selection == 0) {
			cm.sendGetNumber("#eDid you bring the coconut with you? Then, please give me the #rCoconuts#k you have. How many are you willing to give me? \n\r #b< Number of Coconuts in inventory : "+cm.getPlayer().getItemQuantity(coconut,true)+" >", 0, 0, 1000);
		}
	} else if (status == 2) {
		var num = selection;
		if (num == 0) {
			cm.sendOk("#eI will need the Coconuts to start the monster bang....\r\n Please think again and talk to me.");
		} else if (cm.haveItem(coconut, num)) {
			cm.gainItem(coconut, -num);
			cm.giveKegs(num);
			cm.sendOk("#eDon't forget to give me the coconuts when you want to participate in the event again.");
		} else {
            cm.sendOk("#eYou do not have #r"+num+"#k coconuts.");
            cm.dispose();
        }
		cm.safeDispose();
	}
}